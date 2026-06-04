package com.pfc.suaoficina.authdemo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pfc.suaoficina.authdemo.util.AesEncryptor;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // REQ 4.1 - Listagem completa dos dados pessoais coletados
    // REQ 4.2 - Associação de cada dado a uma finalidade
    // E-mail usado exclusivamente como identificador de autenticação
    @Column(unique = true, nullable = false)
    private String email;

    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.4 - Armazenamento correto do hash + salt
    // REQ 4.3 - Evidência de minimização de dados — @JsonIgnore impede exposição em respostas JSON
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    private Boolean twoFactorEnabled = false;

    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    // REQ 3.4 - Dados sensíveis criptografados em repouso
    // REQ 3.5 - Uso de algoritmo criptográfico adequado (ex.: AES)
    // REQ 4.3 - Evidência de minimização de dados — @JsonIgnore impede exposição em respostas JSON
    @JsonIgnore
    @Convert(converter = AesEncryptor.class)
    private String twoFactorSecret;

    // REQ 2.2 - Token criptograficamente seguro
    // REQ 3.4 - Dados sensíveis criptografados em repouso
    // REQ 3.5 - Uso de algoritmo criptográfico adequado (ex.: AES)
    // REQ 4.3 - Evidência de minimização de dados — @JsonIgnore impede exposição em respostas JSON
    @JsonIgnore
    @Convert(converter = AesEncryptor.class)
    private String resetToken;

    // REQ 2.3 - Token com tempo de expiração
    @JsonIgnore
    private LocalDateTime resetTokenExpiration;

    // REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
    // REQ 4.3 - Evidência de minimização de dados — @JsonIgnore impede exposição em respostas JSON
    @JsonIgnore
    private Integer failedAttempts = 0;

    // REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
    private Boolean accountLocked = false;

    // REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
    // Registra o momento do bloqueio para cálculo de desbloqueio automático
    @JsonIgnore
    private LocalDateTime lockTime;

    // REQ 1.9 - Sessões com tempo de expiração
    private String sessionToken;

    // REQ 1.9 - Sessões com tempo de expiração
    @JsonIgnore
    private LocalDateTime sessionExpiration;

    // REQ 4.4 - Registro explícito de consentimento
    // REQ 4.5 - Consentimento associado à finalidade
    private Boolean consentGiven = false;

    // REQ 4.7 - Registro de data e versão do consentimento
    private LocalDateTime consentDate;
}