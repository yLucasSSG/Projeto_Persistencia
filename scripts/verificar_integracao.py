"""Verifica os três serviços com um MySQL temporário; requer Java, Docker e JARs compilados."""

from concurrent.futures import ThreadPoolExecutor
from decimal import Decimal
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, ProxyHandler, build_opener
import json
import os
import secrets
import shutil
import socket
import subprocess
import tempfile
import time
import uuid


ROOT = Path(__file__).resolve().parents[1]
SERVICOS = ("saldo", "transferencias", "pagamentos")
HTTP = build_opener(ProxyHandler({}))


def conferir(condicao, mensagem):
    if not condicao:
        raise AssertionError(mensagem)


def porta_livre():
    with socket.socket() as sock:
        sock.bind(("127.0.0.1", 0))
        return sock.getsockname()[1]


def requisitar(url, dados=None):
    corpo = None if dados is None else json.dumps(dados).encode()
    request = Request(url, data=corpo, headers={"Content-Type": "application/json"})
    try:
        with HTTP.open(request, timeout=15) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        return error.code, json.load(error)


def executar():
    java = str(Path(os.environ["JAVA_HOME"]) / "bin" / "java") if os.getenv("JAVA_HOME") else shutil.which("java")
    conferir(java is not None, "Java não encontrado.")
    conferir(shutil.which("docker") is not None, "Docker não encontrado.")
    jars = {nome: ROOT / f"{nome}-service/target/{nome}-service-0.0.1-SNAPSHOT.jar" for nome in SERVICOS}
    for jar in jars.values():
        conferir(jar.is_file(), f"Compile os serviços antes do teste: JAR ausente em {jar}.")

    logs = Path(tempfile.mkdtemp(prefix="persistencia-integracao-"))
    container = "persistencia-it-" + uuid.uuid4().hex[:10]
    senha_root = secrets.token_urlsafe(24)
    senhas = {nome: secrets.token_urlsafe(24) for nome in SERVICOS}
    portas = {nome: porta_livre() for nome in SERVICOS}
    conferir(len(set(portas.values())) == 3, "Não foi possível reservar três portas distintas; execute novamente.")
    processos, arquivos = {}, []
    ambiente_mysql = {**os.environ, "MYSQL_ROOT_PASSWORD": senha_root, "MYSQL_PWD": senha_root}

    def comando(args, **kwargs):
        return subprocess.run(args, capture_output=True, text=True, **kwargs)

    def sql(texto, usuario="root", senha=None):
        ambiente = {**os.environ, "MYSQL_PWD": senha or senha_root}
        return comando(["docker", "exec", "-e", "MYSQL_PWD", container, "mysql", "--protocol=TCP",
                        "-h127.0.0.1", "-u" + usuario, "-N", "-B", "-e", texto], env=ambiente)

    def url(nome, caminho):
        return f"http://127.0.0.1:{portas[nome]}{caminho}"

    def parar(nome):
        processo = processos.pop(nome, None)
        if processo is not None and processo.poll() is None:
            processo.terminate()
            try:
                processo.wait(timeout=15)
            except subprocess.TimeoutExpired:
                processo.kill()
                processo.wait(timeout=5)

    def iniciar(nome):
        arquivo = (logs / f"{nome}-{len(arquivos)}.log").open("w")
        arquivos.append(arquivo)
        ambiente = {**os.environ, "MYSQL_HOST": "127.0.0.1", "MYSQL_PORT": porta_mysql,
                    "MYSQL_DATABASE": "banco_" + nome, "MYSQL_USER": nome,
                    "MYSQL_PASSWORD": senhas[nome], "SERVER_PORT": str(portas[nome]),
                    "SALDO_URL": url("saldo", "")}
        processo = subprocess.Popen([java, "-Xmx384m", "-jar", str(jars[nome])],
                                    env=ambiente, stdout=arquivo, stderr=subprocess.STDOUT)
        processos[nome] = processo
        consulta = "/contas/9223372036854775807" if nome == "saldo" else f"/{nome}/{uuid.uuid4()}"
        limite = time.monotonic() + 90
        while time.monotonic() < limite:
            conferir(processo.poll() is None, f"{nome} encerrou antes de iniciar. Consulte os logs em {logs}.")
            try:
                status, _ = requisitar(url(nome, consulta))
                if status == 404:
                    return
            except (URLError, OSError):
                pass
            time.sleep(0.5)
        raise AssertionError(f"{nome} não iniciou dentro do prazo. Logs em {logs}.")

    def saldo(conta):
        status, corpo = requisitar(url("saldo", f"/contas/{conta}"))
        conferir(status == 200, "Falha ao consultar saldo.")
        return Decimal(str(corpo["saldo"]))

    def cadastrar(numero, valor):
        status, corpo = requisitar(url("saldo", "/contas"), {"numero": numero, "saldoInicial": valor})
        conferir(status == 201, "Falha ao cadastrar conta.")
        return corpo["id"]

    def ordem(nome, conta, modalidade, identificador=None):
        dados = {"idTransacao": identificador or str(uuid.uuid4()), "contaId": conta,
                 "valor": 10, "modalidade": modalidade}
        dados["codigoBarras" if modalidade == "BOLETO" else "destino"] = "destino-didatico"
        return dados

    try:
        print("Iniciando MySQL temporário e três serviços reais...", flush=True)
        resultado = comando(["docker", "run", "--detach", "--rm", "--name", container,
                             "-e", "MYSQL_ROOT_PASSWORD", "-p", "127.0.0.1::3306", "mysql:8.4"], env=ambiente_mysql)
        conferir(resultado.returncode == 0, "Não foi possível iniciar o MySQL temporário: " + resultado.stderr)
        porta_mysql = comando(["docker", "port", container, "3306/tcp"]).stdout.strip().rsplit(":", 1)[1]
        limite = time.monotonic() + 120
        while time.monotonic() < limite:
            if sql("SELECT 1").returncode == 0:
                break
            time.sleep(1)
        else:
            raise AssertionError("MySQL não iniciou dentro do prazo.")

        for nome in SERVICOS:
            resultado = sql(f"CREATE DATABASE banco_{nome}; CREATE USER '{nome}'@'%' IDENTIFIED BY '{senhas[nome]}'; "
                            f"GRANT ALL PRIVILEGES ON banco_{nome}.* TO '{nome}'@'%';")
            conferir(resultado.returncode == 0, "Falha ao preparar a base " + nome)
            iniciar(nome)

        conta = cadastrar("1001", 1000)
        vazia = cadastrar("1002", 0)
        concluidas = []
        for nome, modalidades in (("transferencias", ("PIX", "TED", "DOC")),
                                  ("pagamentos", ("PIX", "BOLETO", "SALDO_EM_CONTA"))):
            for modalidade in modalidades:
                dados = ordem(nome, conta, modalidade)
                status, corpo = requisitar(url(nome, "/" + nome), dados)
                conferir(status == 200 and corpo["situacao"] == "CONCLUIDA", f"Falha em {nome}/{modalidade}: {corpo}")
                conferir(corpo["protocoloBancoCentral"] == "SIM-" + dados["idTransacao"], "Comunicação simulada ausente.")
                status, repetida = requisitar(url(nome, "/" + nome), dados)
                conferir(status == 200 and repetida == corpo, "Repetição alterou o resultado da operação.")
                concluidas.append((nome, dados["idTransacao"]))
                status, recusada = requisitar(url(nome, "/" + nome), ordem(nome, vazia, modalidade))
                conferir(status == 200 and recusada["situacao"] == "SALDO_INSUFICIENTE", "Saldo insuficiente não tratado.")
                conferir(recusada["protocoloBancoCentral"] is None, "Operação recusada gerou comunicação de conclusão.")
        conferir(saldo(conta) == Decimal("940.00"), "Reenvio descontou saldo novamente.")
        print("Seis modalidades, dois caminhos e repetição: OK", flush=True)

        concorrente = cadastrar("1003", 100)
        solicitacoes = [ordem("transferencias", concorrente, "PIX") for _ in range(2)]
        for dados in solicitacoes:
            dados["valor"] = 80
        with ThreadPoolExecutor(max_workers=2) as pool:
            respostas = list(pool.map(lambda dados: requisitar(url("transferencias", "/transferencias"), dados), solicitacoes))
        conferir(all(status == 200 for status, _ in respostas), "Falha nas solicitações concorrentes.")
        conferir(sorted(corpo["situacao"] for _, corpo in respostas) == ["CONCLUIDA", "SALDO_INSUFICIENTE"], "Concorrência autorizou saldo indisponível.")
        conferir(saldo(concorrente) == Decimal("20.00"), "Saldo final incorreto após concorrência.")

        compartilhado = str(uuid.uuid4())
        status, _ = requisitar(url("transferencias", "/transferencias"), ordem("transferencias", conta, "PIX", compartilhado))
        conferir(status == 200, "Falha na ordem de referência.")
        status, _ = requisitar(url("pagamentos", "/pagamentos"), ordem("pagamentos", conta, "PIX", compartilhado))
        conferir(status == 409, "O mesmo UUID foi aceito por duas operações distintas.")
        conferir(saldo(conta) == Decimal("930.00"), "Conflito entre serviços repetiu o débito.")
        print("Concorrência e identificador compartilhado entre serviços: OK", flush=True)

        pendente = ordem("transferencias", conta, "PIX")
        parar("saldo")
        status, _ = requisitar(url("transferencias", "/transferencias"), pendente)
        conferir(status == 503, "Indisponibilidade de Saldo não foi sinalizada.")
        status, consulta = requisitar(url("transferencias", "/transferencias/" + pendente["idTransacao"]))
        conferir(status == 200 and consulta["situacao"] == "PENDENTE", "A ordem pendente foi perdida.")
        iniciar("saldo")
        # Simula resposta perdida após Saldo já ter confirmado o débito.
        status, debito = requisitar(url("saldo", f"/contas/{conta}/debitos"), {
            "idTransacao": pendente["idTransacao"], "valor": 10,
            "referencia": "TRANSFERENCIA:PIX:destino-didatico"})
        conferir(status == 200 and debito["resultado"] == "AUTORIZADO", "Falha ao preparar recuperação de débito confirmado.")
        status, retomada = requisitar(url("transferencias", "/transferencias"), pendente)
        conferir(status == 200 and retomada["situacao"] == "CONCLUIDA", "A ordem pendente não foi retomada.")
        conferir(saldo(conta) == Decimal("920.00"), "A recuperação repetiu um débito confirmado.")
        print("Retomada de ordem pendente e resposta perdida: OK", flush=True)

        for nome in reversed(SERVICOS):
            parar(nome)
        for nome in SERVICOS:
            iniciar(nome)
        conferir(saldo(conta) == Decimal("920.00"), "Saldo não persistiu após reinício.")
        for nome, identificador in concluidas:
            status, consulta = requisitar(url(nome, f"/{nome}/{identificador}"))
            conferir(status == 200 and consulta["situacao"] == "CONCLUIDA", "Ordem não persistiu após reinício.")
        for nome in ("transferencias", "pagamentos"):
            resultado = sql(f"SELECT (SELECT COUNT(*) FROM banco_{nome}.{nome} WHERE situacao='CONCLUIDA') "
                            f"= (SELECT COUNT(*) FROM banco_{nome}.comunicacoes_banco_central)")
            conferir(resultado.returncode == 0 and resultado.stdout.strip() == "1", "Comunicações não correspondem às ordens concluídas.")
        resultado = sql("SELECT COUNT(*) FROM banco_saldo.contas", usuario="transferencias", senha=senhas["transferencias"])
        conferir(resultado.returncode != 0, "A credencial de Transferências acessou a base de Saldo.")
        print("Persistência após reinício, comunicações e isolamento das bases: OK", flush=True)
        print("Integração concluída com sucesso. Logs: " + str(logs), flush=True)
    finally:
        for nome in reversed(SERVICOS):
            parar(nome)
        for arquivo in arquivos:
            arquivo.close()
        comando(["docker", "rm", "--force", container])


if __name__ == "__main__":
    executar()
