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
- Rate limiting por IP utilizando Bucket4j
- Comunicação protegida com TLS/HTTPS (certificado autoassinado)
- Criptografia de dados sensíveis em repouso (AES-256/GCM)
- Envio assíncrono de e-mails com Spring Async
- Sanitização de logs para mitigação de Log Injection
- Conformidade com a LGPD: minimização de dados, consentimento e direitos do titular
- Auditoria e logs de eventos críticos de segurança

---

## Estrutura dos repositórios

**authdemo (backend)**

```text
authdemo/
├── src/main/java/com/pfc/suaoficina/authdemo/
│   ├── config/
│   │   ├── AsyncConfig.java          # Habilita processamento assíncrono
│   │   ├── RateLimitFilter.java      # Rate limiting por IP
│   │   └── SecurityConfig.java       # Configurações de segurança e CORS
│   ├── controller/
│   │   ├── AuthController.java       # Endpoints REST de autenticação e LGPD
│   │   └── TestController.java       # Endpoint de teste da API
│   ├── exception/
│   │   └── GlobalExceptionHandler.java # Tratamento centralizado de exceções
│   ├── model/
│   │   └── User.java                 # Entidade de usuário
│   ├── repository/
│   │   └── UserRepository.java       # Acesso ao banco de dados
│   ├── service/
│   │   └── UserService.java          # Regras de negócio e segurança
│   ├── util/
│   │   └── AesEncryptor.java         # Criptografia AES/GCM em repouso
│   └── AuthdemoApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── logback-spring.xml
│   └── keystore.p12
├── logs/                             # Criado apenas em ambiente local
├── .env                              # Variáveis de ambiente (não versionado)
└── pom.xml    # Certificado TLS autoassinado
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
Expõe os endpoints REST sob o prefixo `/auth`. Atua apenas como camada de apresentação, recebendo requisições, delegando ao serviço e retornando respostas.

**`UserService.java`**  
Núcleo da aplicação. Contém toda a lógica de segurança: autenticação, 2FA, bloqueio por tentativas inválidas, geração de tokens de sessão e recuperação de senha, envio assíncrono de e-mails e funcionalidades LGPD.

**`SecurityConfig.java`**  
Define as políticas de segurança da aplicação, incluindo rotas públicas, CORS restrito às origens autorizadas e configuração da cadeia de filtros do Spring Security.

**`RateLimitFilter.java`**  
Filtro responsável por limitar requisições por endereço IP utilizando Bucket4j, mitigando ataques automatizados e abuso da API.

**`AsyncConfig.java`**  
Habilita processamento assíncrono através da anotação `@EnableAsync`, permitindo o envio não bloqueante de e-mails.

**`GlobalExceptionHandler.java`**  
Centraliza o tratamento de exceções dos controllers REST, padronizando respostas de erro e o fluxo de autenticação com 2FA.

**`AesEncryptor.java`**  
Conversor JPA (`AttributeConverter`) responsável pela criptografia transparente de atributos sensíveis utilizando AES-256/GCM com IV aleatório por operação.

**`User.java`**  
Entidade JPA mapeada para a tabela `users`. Utiliza `@JsonIgnore` para evitar exposição de informações sensíveis e criptografia em repouso para o segredo do 2FA.

---

## Segurança implementada

| Mecanismo | Detalhe |
|---|---|
| Hash de senhas | BCrypt com salt automático |
| Criptografia em trânsito | TLS 1.2+ via certificado PKCS12 autoassinado |
| Criptografia em repouso | AES-256/GCM com IV aleatório e autenticação integrada |
| Proteção contra força bruta | Bloqueio automático após 5 tentativas (5 min) |
| Rate Limiting | 20 requisições por minuto por IP (Bucket4j) |
| 2FA | TOTP via Google Authenticator (RFC 6238) |
| Sessão | Token UUID com expiração de 10 minutos |
| Reset de senha | Token UUID de uso único com expiração de 10 minutos |
| Minimização de dados | `@JsonIgnore` em campos sensíveis |
| Sanitização de logs | Mitigação de Log Injection por remoção de CR/LF/TAB |
| Consentimento LGPD | Registro de aceite com timestamp no banco |
| Direitos do titular | Endpoints de exportação, revogação e exclusão de conta |
| Auditoria | Persistência em arquivo rotativo, console e banco de dados |

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

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 (LTS) |
| Framework | Spring Boot 3.x |
| Banco de dados | PostgreSQL |
| Segurança | Spring Security + BCrypt |
| Criptografia em repouso | AES-256/GCM (JPA AttributeConverter) |
| Comunicação segura | TLS/HTTPS (PKCS12) |
| 2FA | Google Authenticator (TOTP) |
| Rate Limiting | Bucket4j |
| Processamento assíncrono | Spring Async (`@Async`) |
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

A aplicação utiliza variáveis de ambiente para armazenar informações sensíveis. Configure as credenciais de acesso ao banco conforme o exemplo abaixo:

```env
DB_URL=jdbc:postgresql://localhost:5432/db_suaoficina
DB_USERNAME=SEU_USUARIO
DB_PASSWORD=SUA_SENHA
```

As propriedades do Spring Boot são carregadas automaticamente a partir dessas variáveis:

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```
---

### 2. Configuração de e-mail (recuperação de senha)

A aplicação utiliza SMTP do Gmail para envio dos tokens de recuperação de senha.

Para isso, é necessário habilitar a verificação em duas etapas na conta Google e gerar uma **senha de aplicativo**.

Configure as variáveis de ambiente:

```env
MAIL_USERNAME=SEU_EMAIL@gmail.com
MAIL_PASSWORD=SENHA_DE_APLICATIVO
```

As propriedades são carregadas automaticamente pelo Spring Boot:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> Credenciais de e-mail não devem ser armazenadas diretamente no código-fonte ou versionadas no repositório.
---

Configure no `application.properties`:

```properties
server.port=8443
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=authdemo
```

Defina também a variável de ambiente:

```env
SSL_KEYSTORE_PASSWORD=SUA_SENHA_DO_CERTIFICADO

```

> Por ser autoassinado, o browser exibirá um aviso de segurança. Acesse `GET /auth/validate-session Authorization: Bearer <token>` diretamente e aceite o certificado antes de usar o frontend.

---

### 4. Criptografia em repouso

A chave utilizada para criptografia dos dados sensíveis é carregada por variável de ambiente:

```env
ENCRYPTION_SECRET=SUA_CHAVE_SECRETA
```

A configuração é realizada através da propriedade:

```properties
encryption.secret=${ENCRYPTION_SECRET}
```

O sistema utiliza AES-256/GCM com IV aleatório por operação, garantindo confidencialidade e integridade dos dados armazenados.

Atualmente o campo `twoFactorSecret` é criptografado automaticamente através do conversor JPA `AesEncryptor.java`.

### 5. Executando a aplicação

```bash
mvn spring-boot:run
```

A aplicação estará disponível em `https://localhost:8443`. O frontend deve ser aberto no browser via Live Server ou abrindo diretamente o arquivo `index.html` do repositório **authdemo-frontend**.

---

## Auditoria e logs

Eventos críticos de segurança são registrados utilizando SLF4J e Logback.

Os registros de auditoria são persistidos simultaneamente em:

- Console da aplicação
- Arquivos rotativos em `logs/auditoria.log`
- Banco de dados PostgreSQL via `DBAppender`

A rotação dos arquivos de auditoria é realizada diariamente, mantendo histórico de até 30 dias.

Exemplos de eventos registrados:

```text
[REGISTRO] Novo usuário registrado
[LOGIN] Login bem-sucedido
[LOGIN] Senha inválida
[LOGIN] Conta bloqueada por excesso de tentativas
[2FA] Verificação bem-sucedida
[RESET] Solicitação de recuperação de senha
[LGPD] Consentimento registrado
[LGPD] Exportação de dados solicitada
[LGPD] Conta deletada
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

- Este projeto é uma demonstração acadêmica e não deve ser utilizado em produção sem as devidas adaptações.
- Credenciais, segredos criptográficos e senhas de certificado são carregados por variáveis de ambiente.
- O certificado TLS autoassinado é adequado apenas para ambiente local e demonstrações.
- O sistema implementa autenticação em dois fatores, rate limiting por IP, criptografia AES-256/GCM e auditoria persistida.
- Os tokens de sessão e recuperação são armazenados no banco de dados para fins acadêmicos e demonstrativos.
