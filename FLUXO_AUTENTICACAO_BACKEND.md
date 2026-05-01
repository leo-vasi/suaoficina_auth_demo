# Fluxo de Autenticação do Backend

Este documento complementa o README principal, detalhando o fluxo interno de autenticação e validação executado pelo backend da aplicação.

---

## Fluxo de Login

```text
Frontend
   ↓
AuthController
   ↓
UserService
   ↓
Validação de Credenciais
   ↓
Verificação de Bloqueio
   ↓
Validação 2FA
   ↓
Geração de Sessão
   ↓
Resposta ao Cliente
```

### Etapas executadas

### 1. Recebimento da requisição

O endpoint:

```http
POST /auth/login
```

recebe e-mail e senha enviados pelo frontend.

---

### 2. Busca do usuário

O sistema consulta o banco via:

```java
findByEmail()
```

---

### 3. Validação de bloqueio

Verifica se a conta está temporariamente bloqueada por excesso de tentativas inválidas.

---

### 4. Validação de senha

A senha informada é comparada ao hash BCrypt armazenado.

---

### 5. Verificação de autenticação em dois fatores

Se o 2FA estiver habilitado, o sistema solicita o código TOTP.

---

### 6. Criação de sessão

Após validação completa:

- gera token UUID
- define expiração da sessão
- registra auditoria

---

## Fluxo de Recuperação de Senha

```text
Solicitação
   ↓
Geração de Token
   ↓
Envio por E-mail
   ↓
Validação do Token
   ↓
Redefinição da Senha
```

---

## Camadas Envolvidas

### AuthController

Recebe as requisições HTTP.

### UserService

Executa regras de negócio.

### UserRepository

Realiza acesso ao banco de dados.

### PostgreSQL

Persistência de dados.
