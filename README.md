# Projeto de Persistência Bancária

Projeto acadêmico em Java com Spring Boot, Spring Data JPA e MySQL, baseado nos diagramas de pacotes, sequência e implantação do sistema bancário.

A implementação contempla persistência de contas, débitos, transferências e pagamentos, com consulta dos registros após reiniciar as aplicações. A comunicação ao Banco Central é simulada e registrada no MySQL.

## Requisitos da atividade

| Requisito apresentado | Como foi atendido |
| --- | --- |
| Considerar o projeto bancário anterior | Mantidos os serviços Saldo, Transferências e Pagamentos, suas modalidades e os dois caminhos de movimentação |
| Persistir as classes que precisam ser armazenadas | Entidades JPA e repositórios para Conta, Debito, Transferencia, Pagamento e ComunicacaoBancoCentral |
| Utilizar a estrutura do Spring | Spring Boot, Spring Data JPA, Hibernate, serviços transacionais e controllers REST |
| Utilizar banco de dados | MySQL; SQLite foi apresentado como uma possibilidade, não uma obrigação |
| Entregar código ou GitHub público | Código disponível neste repositório público |

O cenário anterior exige comunicar todas as transferências e pagamentos concluídos ao serviço do Banco Central. Isso inclui pagamento com saldo em conta. Operações recusadas por saldo insuficiente seguem o caminho alternativo dos diagramas e não geram comunicação de conclusão.

## Organização

São três aplicações independentes, com uma base para cada serviço no mesmo servidor MySQL:

| Aplicação | Porta padrão | Base padrão | Tabelas |
| --- | --- | --- | --- |
| `saldo-service` | 8081 | `banco_saldo` | `contas`, `debitos` |
| `transferencias-service` | 8082 | `banco_transferencias` | `transferencias`, `comunicacoes_banco_central` |
| `pagamentos-service` | 8083 | `banco_pagamentos` | `pagamentos`, `comunicacoes_banco_central` |

Transferências e Pagamentos acessam o saldo pela API de Saldo. Não consultam diretamente as tabelas de outro serviço. Por isso, o identificador da conta nas ordens é um valor, e não uma associação JPA entre bancos distintos.

Cada aplicação utiliza **Controller → Service → Repository → MySQL**. Os DTOs definem os dados de entrada e saída; as entidades representam as tabelas. Enums são armazenados como texto. Valores monetários usam `BigDecimal` e colunas `DECIMAL(19,2)`.

```text
Projeto_Persistencia/
├── database/                    # Scripts para criar as três bases
├── saldo-service/               # Contas e autorização de débito
├── transferencias-service/      # PIX, TED e DOC
├── pagamentos-service/          # PIX, boleto e saldo em conta
├── scripts/verificar_integracao.py
├── pom.xml                      # Compila os três serviços
├── mvnw / mvnw.cmd
└── README.md
```

## Pré-requisitos

- JDK 21 configurado no `JAVA_HOME`.
- MySQL iniciado. Os testes foram executados com MySQL 8.4.
- Acesso à internet na primeira compilação para baixar Maven e dependências.

O Maven Wrapper está incluído; não é necessário instalar Maven separadamente. Docker e Python são necessários somente para os testes indicados adiante, não para executar as aplicações com seu MySQL instalado.

## Preparar e compilar

Clone o repositório:

```bash
git clone https://github.com/yLucasSSG/Projeto_Persistencia.git
cd Projeto_Persistencia
```

No MySQL Workbench ou em outro cliente SQL, execute os três arquivos de `database/`. Eles criam as bases sem apagar registros existentes. O usuário configurado em cada aplicação precisa ter acesso à sua base e permissão para criar e atualizar tabelas.

As tabelas são criadas pelo JPA na primeira inicialização. Durante este projeto acadêmico, `spring.jpa.hibernate.ddl-auto=update` mantém os mapeamentos sincronizados com o banco.

Compile os três serviços na raiz do projeto:

```powershell
# Windows PowerShell
.\mvnw.cmd clean package
```

```bash
# Linux ou macOS
./mvnw clean package
```

Os JARs são gerados na pasta `target/` de cada serviço. Esse comando não conecta ao MySQL e não executa os testes de integração.

## Executar com seu MySQL

Cada serviço precisa de um terminal próprio. Configure a conexão em **cada terminal**, substituindo os valores de exemplo pelos dados da sua instalação. A senha não deve ser salva no código ou enviada ao GitHub.

```powershell
# Windows PowerShell: repetir em cada terminal
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "SUA_SENHA_LOCAL"
```

Na raiz do repositório, execute os comandos correspondentes em três terminais:

```powershell
# Terminal 1: Saldo
$env:MYSQL_DATABASE = "banco_saldo"
$env:SERVER_PORT = "8081"
.\mvnw.cmd -pl saldo-service spring-boot:run
```

```powershell
# Terminal 2: Transferências
$env:MYSQL_DATABASE = "banco_transferencias"
$env:SERVER_PORT = "8082"
$env:SALDO_URL = "http://localhost:8081"
.\mvnw.cmd -pl transferencias-service spring-boot:run
```

```powershell
# Terminal 3: Pagamentos
$env:MYSQL_DATABASE = "banco_pagamentos"
$env:SERVER_PORT = "8083"
$env:SALDO_URL = "http://localhost:8081"
.\mvnw.cmd -pl pagamentos-service spring-boot:run
```

No Linux ou macOS, use `export VARIAVEL=valor` no lugar de `$env:VARIAVEL = "valor"` e `./mvnw` no lugar de `.\mvnw.cmd`. Por exemplo, no terminal de Saldo:

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_USER=root
export MYSQL_PASSWORD='SUA_SENHA_LOCAL'
export MYSQL_DATABASE=banco_saldo
export SERVER_PORT=8081
./mvnw -pl saldo-service spring-boot:run
```

Se utilizar uma IDE, configure essas variáveis na execução de cada aplicação. Inicie Saldo antes de processar transferências ou pagamentos.

## Operações disponíveis

| Serviço | Método e endereço | Resultado |
| --- | --- | --- |
| Saldo | `POST /contas` | Cadastra conta e saldo inicial; HTTP 201 |
| Saldo | `GET /contas/{id}` | Consulta conta e saldo; HTTP 200 |
| Saldo | `POST /contas/{id}/debitos` | Autoriza ou recusa débito; HTTP 200 |
| Transferências | `POST /transferencias` | Registra e processa uma ordem; HTTP 200 |
| Transferências | `GET /transferencias/{idTransacao}` | Consulta ordem persistida; HTTP 200 |
| Pagamentos | `POST /pagamentos` | Registra e processa uma ordem; HTTP 200 |
| Pagamentos | `GET /pagamentos/{idTransacao}` | Consulta ordem persistida; HTTP 200 |

Arquivos `*.http` em cada serviço contêm exemplos para clientes HTTP de IDEs. Também é possível usar Postman, Insomnia, curl ou PowerShell.

### 1. Cadastrar uma conta

Envie para `POST http://localhost:8081/contas`:

```json
{
  "numero": "1001",
  "saldoInicial": 150.25
}
```

Exemplo no PowerShell, com os serviços em execução:

```powershell
$dados = @{ numero = "1001"; saldoInicial = 150.25 } | ConvertTo-Json
$conta = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/contas" -ContentType "application/json" -Body $dados
$conta
Invoke-RestMethod -Uri "http://localhost:8081/contas/$($conta.id)"
```

No Linux ou macOS:

```bash
curl -i -X POST http://localhost:8081/contas \
  -H 'Content-Type: application/json' \
  -d '{"numero":"1001","saldoInicial":150.25}'
```

A resposta contém `id`, `numero` e `saldo`. Anote o `id` para as próximas operações. O cadastro com saldo inicial é uma operação de apoio para demonstrar a persistência; não existe edição direta do saldo pela API.

### 2. Fazer uma transferência

Envie para `POST http://localhost:8082/transferencias`, usando o identificador da conta criada:

```json
{
  "idTransacao": "7097fe9d-0405-4809-86b9-6ba794b29ec7",
  "contaId": 1,
  "destino": "chave-pix-destino",
  "valor": 10.00,
  "modalidade": "PIX"
}
```

As modalidades são `PIX`, `TED` e `DOC`. Para cada nova operação, gere um novo UUID. Para repetir uma solicitação, mantenha o UUID e todos os dados originais.

### 3. Fazer um pagamento

Envie para `POST http://localhost:8083/pagamentos`:

```json
{
  "idTransacao": "9d352e18-f35c-46b7-be94-8a39b260e3f6",
  "contaId": 1,
  "codigoBarras": "codigo-de-barras-didatico",
  "valor": 10.00,
  "modalidade": "BOLETO"
}
```

As modalidades são `PIX`, `BOLETO` e `SALDO_EM_CONTA`. Boleto exige `codigoBarras` e não recebe `destino`. As outras duas modalidades exigem `destino` e não recebem `codigoBarras`.

A resposta da ordem contém os dados persistidos, a situação e o protocolo da comunicação simulada, quando concluída. A consulta usa o mesmo UUID: `/transferencias/{idTransacao}` ou `/pagamentos/{idTransacao}`.

## Regras e consistência

- Número de conta obrigatório, único e com até 30 caracteres; espaços nas extremidades são removidos.
- Saldo inicial não negativo. Movimentações com valor estritamente positivo.
- Valores com até 17 dígitos inteiros e 2 casas decimais. Casas excedentes são rejeitadas, sem arredondamento silencioso.
- Saldo usa bloqueio de escrita na conta durante a transação MySQL. Verificação, desconto e registro do débito são confirmados juntos.
- Repetir um débito com o mesmo UUID, conta, valor e referência devolve o resultado anterior sem novo desconto, inclusive após reinício. Dados diferentes para o mesmo UUID retornam HTTP 409.
- Transferências e Pagamentos gravam a ordem como `PENDENTE` em uma transação antes de chamar Saldo. O processamento usa outro limite transacional e bloqueia a ordem para impedir processamento simultâneo duplicado.
- Se autorizado, o serviço registra a comunicação simulada e conclui a ordem. Se faltar saldo, grava `SALDO_INSUFICIENTE`, mantendo o saldo da conta.
- Se a chamada a Saldo falhar, a ordem permanece pendente. Reenviar a mesma solicitação permite retomar; a idempotência em Saldo protege também o caso em que o débito foi confirmado, mas sua resposta se perdeu.
- Repetir uma ordem concluída ou recusada não repete o débito nem a comunicação. Uma nova tentativa de negócio exige novo UUID.

As situações de ordem são `PENDENTE`, `CONCLUIDA` e `SALDO_INSUFICIENTE`. O resultado do serviço Saldo é `AUTORIZADO` ou `SALDO_INSUFICIENTE`.

| Situação | Resposta HTTP |
| --- | --- |
| Dados, JSON, modalidade ou identificador inválidos | 400 |
| Conta ou ordem não encontrada | 404 |
| Número de conta repetido ou UUID reutilizado com outros dados | 409 |
| Saldo indisponível ou resposta inválida | 503; ordem preservada como pendente |

Os erros de domínio retornam `mensagem` e `detalhes`. Saldo insuficiente é um resultado de negócio e utiliza HTTP 200.

### Banco Central e escopo didático

`BancoCentralSimulado` persiste uma comunicação por ordem concluída e gera o protocolo `SIM-{idTransacao}`. Todas as modalidades usam esse adaptador, inclusive pagamento com saldo em conta, conforme o enunciado anterior.

Não há integração com instituições financeiras reais, liquidação externa ou crédito na conta de destino. O objetivo desta entrega é persistir e demonstrar as classes e os fluxos do projeto acadêmico. DOC permanece como modalidade didática, conforme solicitado no cenário. Identificadores de destino e códigos de barras são dados de demonstração.

## Comprovar a persistência

1. Cadastre uma conta e faça uma operação; anote o ID da conta e o UUID da ordem.
2. Confira os registros diretamente no MySQL:

```sql
SELECT id, numero, saldo FROM banco_saldo.contas;
SELECT id_transacao, conta_id, valor, resultado FROM banco_saldo.debitos;
SELECT id_transacao, modalidade, situacao, valor FROM banco_transferencias.transferencias;
SELECT id_transacao, modalidade, situacao, valor FROM banco_pagamentos.pagamentos;
SELECT id_transacao, protocolo FROM banco_transferencias.comunicacoes_banco_central;
SELECT id_transacao, protocolo FROM banco_pagamentos.comunicacoes_banco_central;
```

3. Encerre as aplicações com `Ctrl+C` e inicie novamente, com a mesma configuração de bases.
4. Consulte os mesmos identificadores. Os saldos, as ordens e os protocolos devem continuar disponíveis.
5. Reenvie uma ordem concluída com os mesmos dados e confira que o saldo não foi descontado novamente.

Para demonstrar o caminho de saldo insuficiente, crie outra conta com saldo zero e solicite uma operação de valor positivo.

## Testes automatizados

### Testes de cada serviço

É necessário ter Docker em execução. Testcontainers inicia um MySQL 8.4 temporário, sem acessar o MySQL instalado no seu computador.

```powershell
# Windows: na raiz do projeto
.\mvnw.cmd -Pmysql-it clean verify
```

```bash
# Linux ou macOS
./mvnw -Pmysql-it clean verify
```

São 24 testes de integração: 11 em Saldo, 6 em Transferências e 7 em Pagamentos. Cobrem persistência após reinício, validações, saldo insuficiente, idempotência, concorrência, modalidades e retomada de ordens pendentes. Os testes de Transferências e Pagamentos usam um simulador HTTP de Saldo para controlar respostas e falhas.

Os relatórios ficam em `target/failsafe-reports/` de cada serviço. Para testar somente um serviço, adicione `-pl nome-do-service` ao comando da raiz.

### Integração dos três serviços reais

Com os JARs compilados, JDK 21, Docker e Python 3 disponíveis:

```bash
python scripts/verificar_integracao.py
```

O script cria um MySQL temporário com três bases e três usuários restritos às suas respectivas bases. Inicia os três JARs reais e verifica:

- As seis modalidades, com sucesso e saldo insuficiente.
- Reenvios sem débito ou comunicação duplicados.
- Concorrência sobre o mesmo saldo e conflito de UUID entre serviços.
- Ordem pendente após indisponibilidade e recuperação de um débito cuja resposta se perdeu.
- Consulta após reiniciar as três aplicações e correspondência entre ordens concluídas e comunicações.
- Bloqueio do acesso direto de um serviço à base de outro.

Os processos e o contêiner de teste são removidos ao final. Os logs ficam em uma pasta temporária informada pelo script.

## Etapas e entrega

O histórico do Git separa a implementação em estrutura inicial, cadastro de contas, débito atômico, Transferências, Pagamentos e verificação integrada. O README contém as instruções para executar e demonstrar a atividade.

Repositório público para entrega: https://github.com/yLucasSSG/Projeto_Persistencia
