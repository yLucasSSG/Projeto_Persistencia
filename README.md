# Projeto de Persistência Bancária

Implementação em Java com Spring Boot, Spring Data JPA e MySQL, baseada nos diagramas do sistema bancário das atividades anteriores.

## Objetivo da atividade

Implementar a persistência das classes que precisam ser armazenadas em um banco de dados, utilizando Spring. A entrega é o código-fonte ou o link de um repositório público no GitHub.

O enunciado permite SQLite, mas este projeto utiliza MySQL. O foco é a persistência dos dados de Saldo, Transferências e Pagamentos.

## Desenvolvimento por etapas

O projeto está na **etapa 2: cadastro e consulta de contas no MySQL**. Esta versão ainda não é a entrega completa: falta a persistência de Transferências e Pagamentos.

| Etapa | Situação |
| --- | --- |
| Configurar Spring Boot, Spring Data JPA e o driver MySQL | Implementada |
| Mapear Conta e criar seu repositório | Implementada |
| Gravar e consultar contas no MySQL pela API | Implementada |
| Validar cadastro e impedir números de conta repetidos | Implementada |
| Consultar o saldo atual da conta | Implementada |
| Implementar débito de saldo e tratar saldo insuficiente | Próxima etapa |
| Implementar a persistência de Transferências | Pendente |
| Implementar a persistência de Pagamentos | Pendente |
| Verificar contas após reiniciar a aplicação | Verificada no teste de integração |

## Relação com os diagramas

Os três serviços mantêm as responsabilidades definidas nos diagramas:

| Serviço | Dados previstos |
| --- | --- |
| Saldo | Contas e saldos disponíveis |
| Transferências | Identificador da transação, origem, destino, valor, modalidade e situação |
| Pagamentos | Identificador da transação, conta, destino ou código de barras, valor, modalidade e situação |

Cada serviço terá uma aplicação Spring Boot e uma base própria no mesmo servidor MySQL. Neste momento, somente o serviço Saldo foi iniciado. O saldo fica armazenado em Conta; o nome do serviço não obriga a criação de uma entidade chamada Saldo.

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

O cadastro com saldo inicial é uma operação de apoio para demonstrar a persistência. Não há operação de edição direta do saldo. Débitos, transferências e pagamentos serão implementados nas próximas etapas.

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
