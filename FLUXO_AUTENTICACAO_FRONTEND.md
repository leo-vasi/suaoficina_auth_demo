# Fluxo de Autenticação do Frontend

Este documento complementa o README principal, detalhando como o frontend interage com o backend durante o fluxo de autenticação e validação, mantendo consistência com as camadas: AuthController, UserService e UserRepository.

---

## Fluxo de Login

```text
Frontend (Interface)
   ↓
AuthController (requisição HTTP)
   ↓
UserService (regras de negócio)
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
   ↓
Tratamento da Resposta (Frontend)
```

---

## Etapas executadas

### 1. Recebimento da entrada do usuário (Frontend)

A interface coleta:

- e-mail
- senha

O frontend NÃO realiza autenticação, apenas prepara os dados para envio.

---

### 2. Validação de campos (Frontend)

Antes de acionar o AuthController, o frontend valida:

- campos obrigatórios
- formato do e-mail

Caso inválido, o fluxo é interrompido no frontend.

---

### 3. Envio da requisição ao AuthController

O frontend envia:

```http
POST /auth/login
```

Corpo:

```json
{
  "email": "usuario@email.com",
  "password": "123456"
}
```

O frontend NÃO manipula hash, criptografia ou regras sensíveis.

---

### 4. Delegação para o UserService (Backend)

Após envio:

- o AuthController recebe a requisição
- delega para o UserService
- o frontend entra em estado de carregamento

---

### 5. Validação de Credenciais (Backend)

O UserService:

- consulta o UserRepository (findByEmail)
- compara senha com hash BCrypt

O frontend apenas aguarda a resposta.

---

### 6. Verificação de Bloqueio (Backend)

O UserService verifica:

- tentativas inválidas
- bloqueio temporário

Resposta possível para o frontend:

- conta bloqueada

---

### 7. Validação de autenticação em dois fatores (Backend)

Se 2FA estiver habilitado:

- o backend NÃO finaliza a autenticação
- retorna status indicando necessidade de TOTP

---

### 8. Tratamento de resposta de 2FA (Frontend)

O frontend:

- redireciona para tela de código
- coleta o código TOTP
- envia nova requisição ao AuthController

---

### 9. Geração de sessão (Backend)

Após validação completa:

- geração de token UUID
- definição de expiração
- registro de auditoria

---

### 10. Recebimento da resposta (Frontend)

O frontend recebe:

- token de sessão
- dados mínimos do usuário (quando aplicável)

---

### 11. Persistência de sessão (Frontend)

O frontend:

- armazena o token
- NÃO armazena dados sensíveis
- prepara uso do token em requisições futuras

Header padrão:

```http
Authorization: Bearer <token>
```

---

### 12. Redirecionamento

Após autenticação:

- usuário é direcionado para área protegida

---

## Fluxo de Recuperação de Senha

```text
Frontend (Solicitação)
   ↓
AuthController
   ↓
UserService
   ↓
Geração de Token
   ↓
Envio por E-mail
   ↓
Frontend (acesso ao link)
   ↓
Validação do Token
   ↓
Redefinição da Senha
```

---

## Etapas executadas

### 1. Solicitação (Frontend)

Usuário informa e-mail:

```http
POST /auth/forgot-password
```

---

### 2. Geração de Token (Backend)

O UserService:

- gera token temporário
- associa ao usuário

---

### 3. Envio por e-mail (Backend)

O sistema envia link de redefinição.

O frontend não participa desta etapa.

---

### 4. Acesso ao link (Frontend)

Usuário acessa rota com token:

- frontend captura token da URL

---

### 5. Validação do Token (Backend)

Frontend envia:

```http
POST /auth/reset-password
```

com:

- token
- nova senha

---

### 6. Redefinição da senha (Backend)

O UserService:

- valida token
- atualiza senha (com hash BCrypt)

---

## Camadas Envolvidas

### Frontend (Interface)

Responsável por:

- coleta de dados
- validação básica
- envio de requisições
- tratamento de respostas

---

### AuthController

Responsável por:

- receber requisições HTTP
- expor endpoints de autenticação

---

### UserService

Responsável por:

- regras de negócio
- validação de credenciais
- controle de segurança

---

### UserRepository

Responsável por:

- acesso ao banco de dados

---

### PostgreSQL

Responsável por:

- persistência de dados

---

## Considerações de Segurança e LGPD

### No Frontend

- não armazenar senha ou dados sensíveis
- armazenar apenas o token de sessão
- evitar exposição de dados pessoais desnecessários
- utilizar HTTPS em todas as requisições

---

### No Backend (refletido no comportamento do frontend)

- uso de hash BCrypt para senhas
- controle de tentativas e bloqueio
- autenticação em dois fatores (2FA)
- tokens com expiração
- auditoria de sessões

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
