# Projeto de Persistência Bancária

Implementação em Java com Spring Boot, Spring Data JPA e MySQL, baseada nos diagramas do sistema bancário das atividades anteriores.

## Objetivo da atividade

Implementar a persistência das classes que precisam ser armazenadas em um banco de dados, utilizando Spring. A entrega é o código-fonte ou o link de um repositório público no GitHub.

O enunciado permite SQLite, mas este projeto utiliza MySQL. O foco é a persistência dos dados de Saldo, Transferências e Pagamentos.

## Desenvolvimento por etapas

O projeto está na **etapa 1: estrutura do serviço Saldo e mapeamento de Conta**. Esta versão ainda não é a entrega completa.

| Etapa | Situação |
| --- | --- |
| Configurar Spring Boot, Spring Data JPA e o driver MySQL | Implementada |
| Mapear Conta e criar seu repositório | Implementada |
| Demonstrar gravação e consulta de contas no MySQL | Próxima etapa |
| Implementar as regras de consulta e débito de saldo | Pendente |
| Implementar a persistência de Transferências | Pendente |
| Implementar a persistência de Pagamentos | Pendente |
| Verificar os dados após reiniciar as aplicações | Pendente |

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
│   │   ├── model/Conta.java
│   │   ├── repository/ContaRepository.java
│   │   └── SaldoServiceApplication.java
│   ├── src/main/resources/application.properties
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

Ainda serão adicionadas as validações de entrada e as operações de negócio. Esta etapa não disponibiliza endpoints HTTP nem uma demonstração de gravação e consulta.

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

Nesta etapa, a aplicação inicializa o JPA e cria ou atualiza a tabela `contas`, mas ainda não executa movimentações. Como ainda não há servidor HTTP ou interface, o processo pode encerrar após a inicialização. Para conferir a estrutura no MySQL:

```sql
USE banco_saldo;
DESCRIBE contas;
```

## Compilar sem conectar ao banco

Dentro de `saldo-service`, execute:

```powershell
.\mvnw.cmd clean package
```

No Linux ou macOS:

```bash
./mvnw clean package
```

A compilação gera o JAR em `saldo-service/target/`. Compilar não comprova a conexão nem a persistência no MySQL; a gravação, a consulta e a permanência dos dados serão verificadas nas próximas etapas.
