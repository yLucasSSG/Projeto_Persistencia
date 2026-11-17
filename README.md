# Projeto Persistência — Sistema Bancário

Persistência em banco de dados das classes do sistema bancário modelado nos diagramas (pacotes, sequência e implantação).
Tecnologias: **Java 21, Spring Boot, Spring Data JPA (Hibernate) e MySQL**.

## Arquitetura: 3 microsserviços

Segue o diagrama de pacotes. Cada microsserviço é uma aplicação Spring Boot independente, com **seu próprio banco MySQL**:

| Microsserviço | Pasta | Porta | Banco | Tabelas |
| --- | --- | --- | --- | --- |
| Saldo | `saldo-service` | 8081 | `banco_saldo` | `contas` |
| Transferências (PIX, TED, DOC) | `transferencias-service` | 8082 | `banco_transferencias` | `transferencias`, `comunicacoes_banco_central` |
| Pagamentos (PIX, Boleto, Saldo em conta) | `pagamentos-service` | 8083 | `banco_pagamentos` | `pagamentos`, `comunicacoes_banco_central` |

Como no diagrama, Transferências e Pagamentos **chamam o Saldo por HTTP** ("Valida e debita saldo"): `POST /contas/{id}/debitos`. Eles não acessam o banco do Saldo; guardam só o número (`contaId`) da conta.

O Banco Central é um serviço externo no diagrama. Aqui ele é **simulado** (`BancoCentralService`): gera um protocolo e grava a comunicação na tabela `comunicacoes_banco_central` do próprio serviço.

Estrutura de cada serviço (pacote `br.com.banco.<servico>`): `Controller` (recebe a requisição) → `Service` (regra de negócio) → `Repository` (grava no banco) → entidade (`@Entity`).

## Classes persistidas → tabelas

| Classe (`@Entity`) | Serviço | Tabela |
| --- | --- | --- |
| `Conta` | Saldo | `contas` (`numero` único, `saldo` em `DECIMAL(19,2)`) |
| `Transferencia` | Transferências | `transferencias` |
| `Pagamento` | Pagamentos | `pagamentos` |
| `ComunicacaoBancoCentral` | Transferências e Pagamentos | `comunicacoes_banco_central` (uma linha por operação concluída, com o protocolo) |

As tabelas são criadas automaticamente pelo Hibernate (`spring.jpa.hibernate.ddl-auto=update`); não há script SQL.
`ComunicacaoBancoCentral` aparece nos dois serviços porque cada microsserviço tem seu banco e não compartilha classes com os outros.

## Diagrama de sequência → código

Transferência e pagamento seguem o mesmo fluxo, em `TransferenciaService.transferir` e `PagamentoService.pagar`:

| Diagrama de sequência | No código |
| --- | --- |
| `solicitarTransferenciaPIX(...)` / `solicitarPagamento...(...)` | `POST /transferencias` / `POST /pagamentos` (controller) |
| `validarEDebitarAtomicamente` (Saldo) | `SaldoClient.debitar` → `POST /contas/{id}/debitos` → `ContaService.debitar` → `conta.debitar(valor)` (`false` se o saldo for insuficiente) |
| `[debitoAutorizado]` → `comunicar...` ao Banco Central | grava a operação como `CONCLUIDA`, chama `BancoCentralService.comunicar` e guarda o protocolo |
| `[saldoInsuficiente]` | grava a operação como `SALDO_INSUFICIENTE`; saldo intacto; sem comunicação |
| Consulta de saldo (diagrama 01) | `GET /contas/{id}` |

## Como executar

Requisitos: JDK 21 e MySQL em execução. Os três bancos são criados sozinhos.

Usuário e senha ficam em `application.properties` de cada serviço (padrão: usuário `root`, senha da variável `MYSQL_PASSWORD`). **Abra 3 terminais** na raiz do projeto e **inicie o Saldo primeiro**:

```powershell
# Terminal 1 - Saldo (8081)
$env:MYSQL_PASSWORD = "SUA_SENHA"
.\mvnw.cmd -pl saldo-service spring-boot:run
```

```powershell
# Terminal 2 - Transferências (8082)
$env:MYSQL_PASSWORD = "SUA_SENHA"
.\mvnw.cmd -pl transferencias-service spring-boot:run
```

```powershell
# Terminal 3 - Pagamentos (8083)
$env:MYSQL_PASSWORD = "SUA_SENHA"
.\mvnw.cmd -pl pagamentos-service spring-boot:run
```

O console mostra o SQL executado.

## Testar (exemplos)

```powershell
# 1. criar conta com saldo 100 (serviço Saldo)
Invoke-RestMethod -Method Post -Uri http://localhost:8081/contas -ContentType application/json -Body '{"numero":"1001","saldoInicial":100.00}'

# 2. transferir 30 por PIX (caminho com saldo)
Invoke-RestMethod -Method Post -Uri http://localhost:8082/transferencias -ContentType application/json -Body '{"contaId":1,"destino":"chave-pix","valor":30.00,"modalidade":"PIX"}'

# 3. pagar boleto de 500 (caminho sem saldo: SALDO_INSUFICIENTE)
Invoke-RestMethod -Method Post -Uri http://localhost:8083/pagamentos -ContentType application/json -Body '{"contaId":1,"destino":"codigo-de-barras","valor":500.00,"modalidade":"BOLETO"}'

# 4. consultar saldo (deve estar em 70.00)
Invoke-RestMethod -Uri http://localhost:8081/contas/1
```

Modalidades: transferência `PIX`, `TED`, `DOC`; pagamento `PIX`, `BOLETO`, `SALDO_EM_CONTA`.
Em pagamento, o campo `destino` guarda a chave PIX, o código de barras do boleto ou o destino do pagamento com saldo em conta.

Para ver os dados gravados, no MySQL:

```sql
SELECT * FROM banco_saldo.contas;
SELECT * FROM banco_transferencias.transferencias;
SELECT * FROM banco_transferencias.comunicacoes_banco_central;
SELECT * FROM banco_pagamentos.pagamentos;
SELECT * FROM banco_pagamentos.comunicacoes_banco_central;
```

Reinicie as aplicações e consulte de novo: os dados continuam lá, pois estão no MySQL.

## Testes automáticos

```powershell
.\mvnw.cmd test
```

Cada serviço tem um teste que cobre os dois caminhos (saldo suficiente e insuficiente) e confere o que foi gravado. Usam banco H2 em memória (não precisam do MySQL). Nos testes de Transferências e Pagamentos, o serviço Saldo é simulado (mock).

## Simplificações (escopo didático)

- A comunicação com o Banco Central é simulada e apenas registrada em tabela; não há crédito na conta de destino.
- Não há transação distribuída entre serviços: se o Saldo debitar e o banco de Transferências/Pagamentos falhar logo depois, o débito não é desfeito. Tratar isso exigiria estorno ou o padrão Saga, fora do escopo desta atividade.
