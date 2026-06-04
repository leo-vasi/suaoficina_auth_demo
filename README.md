# Sua Oficina — Auth Demo

> Demonstração técnica de funcionalidades de segurança da informação, desenvolvida como base de estudo para o Projeto Final de Curso.

![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow)
![TLS](https://img.shields.io/badge/TLS-HTTPS-green?logo=letsencrypt)

---

## Sobre o projeto

Este repositório contém uma aplicação de demonstração focada exclusivamente na camada de autenticação e segurança de um sistema. Ele **não representa o produto final**, mas sim um ambiente controlado para validar as funcionalidades de segurança que serão incorporadas ao PFC definitivo.

O projeto não contempla as funcionalidades completas de um CRUD convencional. A proposta é demonstrar, de forma prática e testável, os mecanismos de proteção de dados exigidos pela LGPD e pelas boas práticas de segurança da informação.

> **Stack planejada para o PFC:** PostgreSQL + Spring Boot + Flutter
> **Stack desta demo:** PostgreSQL + Spring Boot + HTML/CSS/JS (frontend web desacoplado)

Este projeto está dividido em dois repositórios independentes:

- **authdemo** — backend Spring Boot (este repositório)
- **authdemo-frontend** — frontend web em HTML/CSS/JS

---

## Funcionalidades implementadas

- Cadastro de usuário com senha criptografada (BCrypt)
- Login com validação de credenciais
- Autenticação em dois fatores (2FA) via Google Authenticator (TOTP)
- Controle de sessão com token e expiração automática
- Recuperação de senha via e-mail com token temporário
- Proteção contra força bruta com bloqueio automático de conta
- Comunicação protegida com TLS/HTTPS (certificado autoassinado)
- Criptografia de dados sensíveis em repouso (AES-128)
- Conformidade com a LGPD: minimização de dados, consentimento e direitos do titular
- Auditoria e logs de eventos críticos de segurança

---

## Estrutura dos repositórios

**authdemo (backend)**
```
authdemo/
├── src/main/java/com/pfc/suaoficina/authdemo/
│   ├── controller/
│   │   └── AuthController.java       # Endpoints REST de autenticação e LGPD
│   ├── model/
│   │   └── User.java                 # Entidade de usuário com campos auditados
│   ├── repository/
│   │   └── UserRepository.java       # Acesso ao banco de dados (Spring Data JPA)
│   ├── service/
│   │   └── UserService.java          # Lógica de negócio, segurança e auditoria
│   └── util/
│       └── AesEncryptor.java         # Conversor JPA para criptografia AES em repouso
├── src/main/resources/
│   ├── application.properties        # Configurações da aplicação
│   └── keystore.p12                  # Certificado TLS autoassinado
└── logs/
    └── auditoria.log                 # Registro de eventos críticos de segurança
```

**authdemo-frontend (frontend)**
```
authdemo-frontend/
├── api.js                            # Camada de comunicação com o backend
├── index.html                        # Tela de login
├── register.html                     # Tela de cadastro com aceite de consentimento
├── dashboard.html                    # Painel do usuário (2FA, sessão, LGPD)
└── forgot-password.html              # Recuperação de senha
```

---

## Componentes principais

**`AuthController.java`**
Expõe os endpoints REST sob o prefixo `/auth`. Atua apenas como camada de apresentação, recebendo requisições, delegando ao serviço e retornando respostas. Não contém lógica de negócio.

**`UserService.java`**
Núcleo da aplicação. Contém toda a lógica de segurança: validação de credenciais, controle de bloqueio por força bruta, geração de tokens de sessão e reset, integração com 2FA, envio de e-mail e registro de auditoria via SLF4J.

**`AesEncryptor.java`**
Conversor JPA (`AttributeConverter`) que intercepta leitura e escrita de campos sensíveis no banco de dados, aplicando criptografia AES-128/CBC com vetor de inicialização (IV) aleatório e codificação Base64 de forma transparente.

**`User.java`**
Entidade JPA mapeada para a tabela `users`. Campos sensíveis protegidos com `@JsonIgnore` (senha, segredo 2FA, tokens) e criptografia em repouso nos campos `twoFactorSecret` e `resetToken`.

---

## Segurança implementada

| Mecanismo | Detalhe |
|---|---|
| Hash de senhas | BCrypt com salt automático |
| Criptografia em trânsito | TLS 1.2+ via certificado PKCS12 autoassinado |
| Criptografia em repouso | AES-128/CBC com IV aleatório e Base64 nos campos sensíveis |
| Protecao contra forca bruta | Bloqueio automático após 5 tentativas (5 min) |
| 2FA | TOTP via Google Authenticator (RFC 6238) |
| Sessão | Token UUID com expiração de 10 minutos |
| Reset de senha | Token UUID de uso único com expiração de 10 minutos |
| Minimização de dados | `@JsonIgnore` em todos os campos não necessários ao frontend |
| Consentimento LGPD | Registro de aceite com timestamp no banco |
| Direitos do titular | Endpoints de exportação, revogação e exclusão de conta |
| Auditoria | Log estruturado em arquivo separado (`logs/auditoria.log`) |

---

## Endpoints disponíveis

| Método | Endpoint | Descrição |
|---|---|---|
| POST | `/auth/register` | Cadastro de novo usuário |
| POST | `/auth/login` | Login com e-mail e senha |
| POST | `/auth/enable-2fa` | Ativar autenticação de dois fatores |
| POST | `/auth/verify-2fa` | Validar código TOTP |
| POST | `/auth/forgot-password` | Solicitar recuperação de senha |
| POST | `/auth/reset-password` | Redefinir senha com token |
| GET | `/auth/validate-session` | Verificar validade do token de sessão |
| POST | `/auth/consent` | Registrar consentimento do titular |
| GET | `/auth/export-data` | Exportar dados pessoais (LGPD) |
| POST | `/auth/revoke-consent` | Revogar consentimento (LGPD) |
| DELETE | `/auth/delete-account` | Deletar conta (LGPD) |

---

## Tecnologias utilizadas

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 (LTS) |
| Framework | Spring Boot 3.x |
| Banco de dados | PostgreSQL |
| Segurança | Spring Security + BCrypt |
| Criptografia em repouso | AES-128 (JPA AttributeConverter) |
| Comunicação segura | TLS/HTTPS (keytool + PKCS12) |
| 2FA | Google Authenticator (TOTP) |
| E-mail | Spring Boot Starter Mail |
| Logs | SLF4J + Logback |
| Frontend | HTML + CSS + JavaScript (fetch async/await) |
| Testes de API | Postman |

---

## Pre-requisitos

- Java 25+
- PostgreSQL instalado e em execução
- Maven
- IntelliJ IDEA (recomendado) ou VS Code com Live Server
- Conta Gmail com verificação em duas etapas ativa

---

## Configuração do ambiente

### 1. Banco de dados

Crie um banco de dados no PostgreSQL:

```sql
CREATE DATABASE db_suaoficina;
```

Configure o acesso no `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/db_suaoficina
spring.datasource.username=SEU_USUARIO
spring.datasource.password=SUA_SENHA
spring.jpa.hibernate.ddl-auto=update
```

---

### 2. Configuração de e-mail (recuperação de senha)

A aplicação utiliza uma conta Gmail para envio do token de recuperação de senha. É necessário gerar uma **senha de aplicativo** na conta Google (não a senha normal).

Configure no `application.properties`:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=SEU_EMAIL@gmail.com
spring.mail.password=SENHA_DE_APLICATIVO
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> Nunca versione o `application.properties` com credenciais reais. Utilize variáveis de ambiente em produção.

---

### 3. TLS/HTTPS (certificado autoassinado)

Gere o certificado com o comando abaixo na raiz do projeto:

```powershell
keytool -genkeypair -alias authdemo -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore src/main/resources/keystore.p12 -validity 365 -storepass senha123 -dname "CN=localhost, OU=TCC, O=SuaOficina, L=SP, ST=SP, C=BR"
```

Configure no `application.properties`:

```properties
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=senha123
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=authdemo
```

> Por ser autoassinado, o browser exibirá um aviso de segurança. Acesse `https://localhost:8443/auth/validate-session?token=teste` diretamente e aceite o certificado antes de usar o frontend.

---

### 4. Criptografia em repouso

Defina a chave AES no `application.properties`:

```properties
encryption.secret=MinhaChaveSecreta
```

> A chave é ajustada automaticamente para 16 bytes (AES-128). Os campos `twoFactorSecret` e `resetToken` são criptografados de forma transparente via `AesEncryptor.java`.

---

### 5. Executando a aplicação

```bash
mvn spring-boot:run
```

A aplicação estará disponível em `https://localhost:8443`. O frontend deve ser aberto no browser via Live Server ou abrindo diretamente o arquivo `index.html` do repositório **authdemo-frontend**.

---

## Auditoria e logs

Eventos críticos são registrados automaticamente no arquivo `logs/auditoria.log` na raiz do projeto.

Configure no `application.properties`:

```properties
logging.file.name=logs/auditoria.log
logging.level.com.pfc.suaoficina.authdemo.service.UserService=INFO
```

Exemplos de eventos registrados:

```
[REGISTRO]  Novo usuário registrado: usuario@email.com
[LOGIN]     Login bem-sucedido: usuario@email.com
[LOGIN]     Senha inválida para: usuario@email.com. Tentativas: 3
[LOGIN]     Conta bloqueada por excesso de tentativas: usuario@email.com
[LOGIN]     Tentativa de acesso em conta bloqueada: usuario@email.com
[2FA]       2FA ativado para: usuario@email.com
[2FA]       Verificação bem-sucedida para: usuario@email.com
[RESET]     Solicitação de recuperação de senha para: usuario@email.com
[RESET]     Senha redefinida com sucesso para: usuario@email.com
[LGPD]      Consentimento registrado para: usuario@email.com
[LGPD]      Exportação de dados solicitada para: usuario@email.com
[LGPD]      Consentimento revogado para: usuario@email.com
[LGPD]      Conta deletada: usuario@email.com
```

---

## Conformidade com a LGPD

| Princípio | Implementação |
|---|---|
| Minimização de dados | Campos desnecessários ocultados com `@JsonIgnore` |
| Finalidade | Apenas dados essenciais coletados e expostos |
| Consentimento | Checkbox obrigatório no cadastro com timestamp registrado |
| Direito de acesso | Endpoint `/export-data` retorna todos os dados do titular |
| Direito de revogação | Endpoint `/revoke-consent` remove o consentimento |
| Direito ao esquecimento | Endpoint `/delete-account` remove a conta permanentemente |

---

## Observações

- Este projeto é uma **demonstração acadêmica** e não deve ser utilizado em produção sem as devidas adaptações (certificado TLS valido, variáveis de ambiente, HTTPS forcado, etc.).
- O certificado autoassinado é adequado apenas para ambiente local e demonstrações.
- Os tokens de sessão e reset são armazenados no banco de dados. Em produção, recomenda-se algo mais robusto.
