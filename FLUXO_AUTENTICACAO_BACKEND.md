# Fluxo de Autenticação do Backend

Este documento complementa o README principal, detalhando o fluxo interno de autenticação, validação e resposta ao frontend, incluindo práticas de segurança e alinhamento com LGPD.

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
Resposta ao Frontend
```

---

## Etapas executadas

### 1. Recebimento da requisição

O endpoint:

```http
POST /auth/login
```

recebe:

- e-mail
- senha

O AuthController é responsável por:

- validar estrutura da requisição
- encaminhar para o UserService

---

### 2. Busca do usuário

O UserService consulta o repositório:

```java
findByEmail()
```

Caso o usuário não exista:

- retorna erro controlado
- não informa se o e-mail é válido (prevenção contra enumeração)

---

### 3. Validação de bloqueio

O sistema verifica:

- número de tentativas inválidas
- status de bloqueio temporário

Se bloqueado:

- interrompe o fluxo
- registra tentativa
- retorna status de conta bloqueada

---

### 4. Validação de senha

A senha informada:

- é comparada com o hash armazenado (BCrypt)
- nunca é armazenada ou retornada em texto plano

Em caso de falha:

- incrementa contador de tentativas
- retorna erro genérico

---

### 5. Verificação de autenticação em dois fatores

Se o 2FA estiver habilitado:

- o login não é finalizado
- o sistema retorna indicação de necessidade de código TOTP

---

### 6. Validação do código 2FA

Quando aplicável:

- o código TOTP é validado
- falhas são tratadas com limite de tentativas

---

### 7. Geração de sessão

Após validação completa:

- gera token de sessão (UUID ou equivalente)
- define tempo de expiração
- registra evento de auditoria

---

### 8. Resposta ao frontend

O sistema retorna respostas padronizadas:

```text
Sucesso:
   - token de autenticação
   - tempo de expiração
   - dados mínimos do usuário

2FA necessário:
   - flag indicando necessidade de validação adicional

Erro:
   - mensagem genérica
   - sem exposição de dados sensíveis
```

---

## Fluxo de Recuperação de Senha

```text
Solicitação (Frontend)
   ↓
AuthController
   ↓
UserService
   ↓
Geração de Token
   ↓
Envio por E-mail
   ↓
Validação do Token
   ↓
Redefinição da Senha
   ↓
Resposta ao Frontend
```

---

## Etapas executadas

### 1. Solicitação de recuperação

Endpoint:

```http
POST /auth/forgot-password
```

Recebe:

- e-mail

Resposta:

- sempre genérica (não confirma existência do usuário)

---

### 2. Geração de token

O UserService:

- gera token temporário e único
- define tempo de expiração
- associa ao usuário

---

### 3. Envio por e-mail

O sistema:

- envia link contendo token
- não expõe informações sensíveis

---

### 4. Validação do token

Endpoint:

```http
POST /auth/reset-password
```

Valida:

- integridade do token
- expiração
- associação com usuário

---

### 5. Redefinição da senha

O sistema:

- aplica hash BCrypt
- invalida tokens anteriores
- registra auditoria

---

### 6. Resposta ao frontend

- confirma alteração sem expor dados
- não retorna senha ou informações sensíveis

---

## Camadas Envolvidas

### AuthController

Responsável por:

- receber requisições HTTP
- validar entrada
- retornar respostas padronizadas

---

### UserService

Responsável por:

- regras de negócio
- validação de autenticação
- controle de segurança

---

### UserRepository

Responsável por:

- acesso aos dados persistidos

---

### PostgreSQL

Responsável por:

- armazenamento seguro dos dados

---

## Considerações de Segurança

- uso de hash BCrypt para senhas
- proteção contra brute force (bloqueio por tentativas)
- autenticação em dois fatores (2FA)
- tokens com expiração
- respostas genéricas para evitar enumeração de usuários
- auditoria de eventos de autenticação

---

## Considerações de LGPD

- minimização de dados retornados ao frontend
- não armazenamento de dados sensíveis em texto plano
- controle de acesso baseado em sessão
- expiração de tokens e invalidação de sessões
- rastreabilidade por auditoria
- comunicação segura via HTTPS

---

## Integração com o Frontend

O backend fornece respostas estruturadas para permitir decisões no frontend:

```text
Sucesso:
   → frontend armazena token e redireciona

2FA necessário:
   → frontend solicita código adicional

Erro:
   → frontend exibe mensagem ao usuário
```

---

## Fluxo Completo

```text
Frontend
   ↓
AuthController
   ↓
UserService
   ↓
UserRepository
   ↓
PostgreSQL
   ↓
UserService
   ↓
AuthController
   ↓
Frontend
```
