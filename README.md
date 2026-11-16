# Projeto de Persistência Bancária

Implementação em Java com Spring Boot, Spring Data JPA e MySQL, baseada nos diagramas do sistema bancário das atividades anteriores.

## Objetivo da atividade

Implementar a persistência das classes que precisam ser armazenadas em um banco de dados, utilizando Spring. A entrega é o código-fonte ou o link de um repositório público no GitHub.

O enunciado permite SQLite, mas este projeto utiliza MySQL. O foco é a persistência dos dados de Saldo, Transferências e Pagamentos.

## Desenvolvimento por etapas

O projeto está na **etapa 5: persistência de pagamentos e transferências**. A implementação das três áreas está pronta; a conferência integrada e a documentação final são a etapa seguinte.

| Etapa | Situação |
| --- | --- |
| Configurar Spring Boot, Spring Data JPA e o driver MySQL | Implementada |
| Mapear Conta e criar seu repositório | Implementada |
| Gravar e consultar contas no MySQL pela API | Implementada |
| Validar cadastro e impedir números de conta repetidos | Implementada |
| Consultar o saldo atual da conta | Implementada |
| Implementar débito de saldo e tratar saldo insuficiente | Implementada |
| Implementar a persistência de Transferências | Implementada |
| Implementar a persistência de Pagamentos | Implementada |
| Verificar contas após reiniciar a aplicação | Verificada no teste de integração |

## Relação com os diagramas

Os três serviços mantêm as responsabilidades definidas nos diagramas:

| Serviço | Dados previstos |
| --- | --- |
| Saldo | Contas e saldos disponíveis |
| Transferências | Identificador da transação, origem, destino, valor, modalidade e situação |
| Pagamentos | Identificador da transação, conta, destino ou código de barras, valor, modalidade e situação |

Cada serviço terá uma aplicação Spring Boot e uma base própria no mesmo servidor MySQL. Os serviços Saldo, Transferências e Pagamentos estão implementados. O saldo fica armazenado em Conta; o nome do serviço não obriga a criação de uma entidade chamada Saldo.

As transferências contemplarão PIX, TED e DOC, e os pagamentos contemplarão PIX, boleto e saldo em conta. DOC será mantido no modelo didático por fazer parte do enunciado.

A exigência anterior de comunicar as transferências e os pagamentos ao serviço do Banco Central permanece como regra do projeto. A implementação de uma integração real com o Banco Central não é solicitada no enunciado de persistência apresentado.

## Tecnologias

- Java 21
- Spring Boot 4.1.2
- Spring Data JPA e Hibernate
- MySQL
- Maven Wrapper, incluído no projeto

## Estrutura atual

```text
Projeto_Persistencia/
├── database/
│   └── 01-criar-banco-saldo.sql
├── saldo-service/
│   ├── src/main/java/br/com/banco/saldo/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/Conta.java
│   │   ├── repository/ContaRepository.java
│   │   ├── service/ContaService.java
│   │   └── SaldoServiceApplication.java
│   ├── src/main/resources/application.properties
│   ├── src/test/java/br/com/banco/saldo/ContaPersistenciaIT.java
│   ├── contas.http
│   ├── pom.xml
│   ├── mvnw
│   └── mvnw.cmd
└── README.md
```

## O que foi implementado

`Conta` é uma entidade JPA mapeada para a tabela `contas`:

| Campo | Tipo Java | Mapeamento |
| --- | --- | --- |
| id | Long | Chave primária gerada pelo MySQL |
| numero | String | Obrigatório, único e com até 30 caracteres |
| saldo | BigDecimal | Obrigatório, com 19 dígitos no total e 2 casas decimais |

`ContaRepository` estende `JpaRepository<Conta, Long>`. O Spring fornece operações como `save`, `findById` e `findAll`. O método `findByNumero` permite buscar a conta pelo número.

`ContaController` recebe as requisições HTTP e valida os dados. `ContaService` coordena o cadastro e a consulta em transações. `ContaRepository` utiliza o JPA para acessar o MySQL.

O fluxo é **Controller → Service → Repository → MySQL**. Os DTOs definem os campos recebidos e devolvidos pela API. As respostas não expõem diretamente a entidade JPA.

No cadastro, o número é obrigatório, tem até 30 caracteres e é armazenado sem espaços nas extremidades. O saldo inicial é obrigatório, não pode ser negativo e aceita até 17 dígitos inteiros e 2 casas decimais. Valores com casas decimais excedentes são rejeitados, sem arredondamento silencioso. A restrição única do MySQL impede que duas requisições cadastrem o mesmo número de conta.

O cadastro com saldo inicial é uma operação de apoio para demonstrar a persistência. Não há operação de edição direta do saldo. As ordens de transferência e pagamento utilizam o serviço Saldo para autorizar o débito.

### Débito atômico

`POST /contas/{id}/debitos` recebe `idTransacao` (UUID) `valor` positivo, com no máximo duas casas decimais, e `referencia` da operação. Retorna `AUTORIZADO` ou `SALDO_INSUFICIENTE`. Ambos são resultados de negócio com HTTP 200.

```json
{
  "idTransacao": "8752570c-bf72-4ba5-a27c-e1c4c88b7b18",
  "valor": 30.25,
  "referencia": "DEBITO-DEMONSTRACAO"
}
```

Saldo bloqueia a conta para escrita durante a transação no MySQL. A validação, o desconto e o registro em `debitos` são confirmados juntos. Operações simultâneas não conseguem gastar o mesmo saldo.

Reenviar o mesmo identificador, conta, valor e referência devolve o resultado registrado sem novo desconto, inclusive após reiniciar a aplicação. Reutilizar o identificador com dados diferentes retorna HTTP 409. Uma tentativa recusada também permanece registrada; para tentar uma nova operação, utilize um novo identificador.

## Preparar o MySQL

Com o MySQL iniciado, execute o arquivo `database/01-criar-banco-saldo.sql` pelo MySQL Workbench ou por outro cliente SQL. Ele cria a base `banco_saldo` sem apagar registros existentes.

O usuário configurado na aplicação precisa ter permissão para criar e alterar tabelas nessa base. O projeto usa `spring.jpa.hibernate.ddl-auto=update` para criar ou atualizar a tabela a partir da entidade durante o desenvolvimento.

## Configurar e executar

Abra um terminal na pasta do repositório e configure as variáveis de ambiente da conexão. Substitua os valores de exemplo pelos dados da sua instalação. A senha não deve ser escrita no código nem enviada ao GitHub.

### Windows PowerShell

```powershell
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_DATABASE = "banco_saldo"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "SUA_SENHA_LOCAL"

cd saldo-service
.\mvnw.cmd spring-boot:run
```

### Linux ou macOS

```bash
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=banco_saldo
export MYSQL_USER=root
export MYSQL_PASSWORD='SUA_SENHA_LOCAL'

cd saldo-service
./mvnw spring-boot:run
```

Configure as variáveis no mesmo terminal que executará o comando. Se utilizar uma IDE, cadastre essas variáveis na configuração de execução.

Não é necessário instalar Maven separadamente. O Maven Wrapper faz o download na primeira execução. É necessário ter o JDK 21 configurado e acesso à internet para baixar as dependências.

Após iniciar, a API fica disponível em `http://localhost:8081`. A porta pode ser alterada pela variável `SERVER_PORT`. Para conferir a estrutura no MySQL:

```sql
USE banco_saldo;
DESCRIBE contas;
```

## Cadastrar e consultar uma conta

| Método | Endereço | Resultado |
| --- | --- | --- |
| POST | `/contas` | Cadastra uma conta e retorna HTTP 201 |
| GET | `/contas/{id}` | Consulta a conta e seu saldo atual; retorna HTTP 200 |

Exemplo do corpo JSON do cadastro:

```json
{
  "numero": "1001",
  "saldoInicial": 150.25
}
```

### Windows PowerShell

Com a aplicação executando, abra outro terminal:

```powershell
$dados = @{ numero = "1001"; saldoInicial = 150.25 } | ConvertTo-Json
$conta = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/contas" -ContentType "application/json" -Body $dados
$conta
Invoke-RestMethod -Uri "http://localhost:8081/contas/$($conta.id)"
```

### Linux ou macOS

```bash
curl -i -X POST http://localhost:8081/contas \
  -H 'Content-Type: application/json' \
  -d '{"numero":"1001","saldoInicial":150.25}'

# Substitua 1 pelo id devolvido no cadastro.
curl -i http://localhost:8081/contas/1
```

A resposta contém `id`, `numero` e `saldo`. O cabeçalho `Location` do cadastro informa o endereço de consulta. O arquivo `saldo-service/contas.http` também contém exemplos para clientes HTTP de IDEs.

Se cadastrar novamente o mesmo número, a API retorna HTTP 409. Para criar outra conta, utilize outro número.

| Situação | Resposta HTTP |
| --- | --- |
| Dados obrigatórios ausentes, saldo inválido ou JSON inválido | 400 |
| Identificador com formato inválido, como `/contas/abc` | 400 |
| Conta não encontrada | 404 |
| Número de conta já cadastrado | 409 |

Os erros retornam um JSON com `mensagem` e `detalhes`.

## Conferir a persistência após reiniciar

1. Cadastre uma conta e anote o `id` retornado.
2. Confira o registro diretamente no MySQL:

```sql
SELECT id, numero, saldo FROM banco_saldo.contas;
```

3. Encerre a aplicação com `Ctrl+C` e inicie novamente, mantendo a mesma configuração de conexão.
4. Consulte `/contas/{id}` com o identificador anotado. O número e o saldo devem continuar disponíveis.

Essa verificação demonstra que os dados estão no MySQL, e não apenas na memória da aplicação.

## Compilar

Dentro de `saldo-service`, execute:

```powershell
.\mvnw.cmd clean package
```

No Linux ou macOS:

```bash
./mvnw clean package
```

A compilação gera o JAR em `saldo-service/target/`. O comando `clean package` não precisa de uma conexão com MySQL e não executa os testes de integração.

## Testes de integração com MySQL

Os testes utilizam Testcontainers para iniciar um MySQL 8.4 temporário. É necessário ter Docker em execução. A aplicação normal utiliza o MySQL configurado nas variáveis de ambiente; Docker é necessário somente para executar estes testes.

Dentro de `saldo-service`, execute no Windows:

```powershell
.\mvnw.cmd -Pmysql-it clean verify
```

No Linux ou macOS:

```bash
./mvnw -Pmysql-it clean verify
```

Os testes verificam:

- Gravação pela API, consulta direta no MySQL e consulta HTTP após reiniciar a aplicação.
- Rejeição de dados inválidos sem gravar registros.
- Cadastro com saldo zero e rejeição de número repetido sem alterar a conta existente.
- Respostas para conta inexistente e identificador inválido.
- Duas tentativas simultâneas de cadastro com o mesmo número, persistindo apenas uma conta.

Os testes usam exclusivamente a base temporária do contêiner, com credenciais próprias, e não dependem da senha do MySQL local. Os relatórios ficam em `saldo-service/target/failsafe-reports/`.


## Transferências

O serviço `transferencias-service` utiliza a porta 8082 e a base `banco_transferencias`. Execute `database/02-criar-banco-transferencias.sql` e inicie o serviço em outro terminal, com as variáveis da conexão MySQL configuradas e `MYSQL_DATABASE=banco_transferencias`. Depois execute o Maven Wrapper na pasta `transferencias-service`.

`SALDO_URL` define a URL de Saldo e usa `http://localhost:8081` por padrão. Cada aplicação precisa de seu próprio terminal e de sua própria configuração de base e porta. Não reutilize a base `banco_saldo` para Transferências.

- `POST /transferencias`: registra e processa uma ordem PIX, TED ou DOC.
- `GET /transferencias/{idTransacao}`: consulta uma ordem persistida.

Exemplo de JSON:

```json
{
  "idTransacao": "7097fe9d-0405-4809-86b9-6ba794b29ec7",
  "contaId": 1,
  "destino": "chave-pix-destino",
  "valor": 10.00,
  "modalidade": "PIX"
}
```

A solicitação é gravada como `PENDENTE` antes de chamar Saldo. Se autorizada, a ordem e sua comunicação ao Banco Central simulado são confirmadas na base de Transferências. Se faltar saldo, a situação fica `SALDO_INSUFICIENTE`. Uma indisponibilidade de Saldo retorna HTTP 503 e mantém a ordem pendente para reenvio com os mesmos dados.

Reenviar o mesmo UUID e os mesmos dados devolve a ordem existente sem repetir o débito nem a comunicação. Alterar os dados de um UUID já utilizado retorna HTTP 409. Os endpoints de operação retornam HTTP 200 com a situação de negócio.

O adaptador `BancoCentralSimulado` registra a comunicação na tabela `comunicacoes_banco_central`, gerando um protocolo com prefixo `SIM-`. Não há chamada ao Banco Central real, liquidação externa ou crédito real no destino. DOC é mantido para atender ao cenário didático.

Os testes de Transferências usam MySQL real em Testcontainers e um simulador HTTP de Saldo, verificando todas as modalidades, reinício, saldo insuficiente, repetição, concorrência e retomada de ordens pendentes. Execute `./mvnw -Pmysql-it clean verify` nessa pasta, ou `mvnw.cmd` no Windows.


## Pagamentos

O serviço `pagamentos-service` utiliza a porta 8083 e a base `banco_pagamentos`. Execute `database/03-criar-banco-pagamentos.sql` e inicie o serviço em um terceiro terminal com `MYSQL_DATABASE=banco_pagamentos`, a conexão MySQL configurada e Saldo em execução.

- `POST /pagamentos`: registra e processa PIX, boleto ou saldo em conta.
- `GET /pagamentos/{idTransacao}`: consulta uma ordem persistida.

O corpo segue o modelo de Transferências, com as modalidades `PIX`, `BOLETO` e `SALDO_EM_CONTA`. PIX e saldo em conta exigem `destino`. Boleto exige `codigoBarras` e não recebe `destino`. Veja os três exemplos em `pagamentos-service/pagamentos.http`.

Todas as modalidades concluídas geram uma comunicação persistida ao Banco Central simulado, inclusive saldo em conta, conforme a regra do professor. Os caminhos de saldo insuficiente, repetição e retomada são os mesmos de Transferências. Cada serviço possui suas próprias tabelas de ordens e comunicações.

Os testes de Pagamentos incluem a validação dos campos específicos de boleto e das demais modalidades. Execute `./mvnw -Pmysql-it clean verify` na pasta do serviço, ou `mvnw.cmd` no Windows.
