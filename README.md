# Sua Oficina — Auth Demo

> Demonstração técnica de funcionalidades de segurança da informação e conformidade com a LGPD, desenvolvida como base de estudo para o Projeto Final de Curso (PFC).

---

## Sobre o projeto

Este repositório contém uma aplicação de demonstração focada exclusivamente na camada de autenticação e segurança de um sistema. Ele **não representa o produto final**, mas sim um ambiente controlado para validar as funcionalidades de segurança que serão incorporadas ao PFC definitivo.

O projeto não contempla as funcionalidades completas de um CRUD convencional. A proposta é demonstrar, de forma prática e testável, os mecanismos de proteção de dados exigidos pela LGPD e pelas boas práticas de segurança da informação.

> **Stack planejada para o PFC:** PostgreSQL + Spring Boot + Flutter  
> **Stack desta demo:** PostgreSQL + Spring Boot + HTML/CSS/JS (frontend web desacoplado)

---

## Funcionalidades implementadas

- Cadastro de usuário com senha criptografada (BCrypt)
- Login com validação de credenciais
- Autenticação em dois fatores (2FA) via Google Authenticator
- Controle de sessão com token e expiração automática
- Recuperação de senha via e-mail com token temporário
- Proteção contra força bruta com bloqueio automático de conta

---

## Tecnologias utilizadas

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 (LTS) |
| Framework | Spring Boot |
| Banco de dados | PostgreSQL |
| Segurança | Spring Security + BCrypt |
| 2FA | Google Authenticator (TOTP) |
| E-mail | Spring Boot Starter Mail |
| Frontend | HTML + CSS + JavaScript (fetch async/await) |
| Testes de API | Postman |

---

## Pré-requisitos

- Java 25+
- PostgreSQL instalado e em execução
- Maven
- IntelliJ IDEA (recomendado) ou VS Code com Live Server
- Conta Gmail com verificação em duas etapas ativa

---

## Configuração do ambiente

### 1. Banco de dados

Crie um banco de dados no PostgreSQL com o nome:

```
db_suaoficina
```

Execute o script de criação da tabela `users` disponível no repositório e configure o acesso no arquivo `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/db_suaoficina
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
```

---

### 2. Configuração de e-mail (recuperação de senha)
