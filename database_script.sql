CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,

    -- Autenticação em dois fatores (2FA)
    two_factor_enabled BOOLEAN DEFAULT FALSE,
    two_factor_secret VARCHAR(255),

    -- Recuperação de senha
    reset_token VARCHAR(255),
    reset_token_expiration TIMESTAMP,

    -- Proteção contra brute force
    failed_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT FALSE,
    lock_time TIMESTAMP,

    -- Controle de sessão
    session_token VARCHAR(255),
    session_expiration TIMESTAMP,

    -- Consentimento LGPD
    consent_given BOOLEAN DEFAULT FALSE,
    consent_date TIMESTAMP
);
