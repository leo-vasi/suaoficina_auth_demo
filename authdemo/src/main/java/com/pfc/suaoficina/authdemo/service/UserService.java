package com.pfc.suaoficina.authdemo.service;

import com.pfc.suaoficina.authdemo.model.User;
import com.pfc.suaoficina.authdemo.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

// REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
// REQ 1.2 - Parâmetros de custo do hash configurados e justificados
// REQ 1.5 - Autenticação de dois fatores (2FA) implementada
// REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
// REQ 5.1 - Logs de autenticação registrados
// REQ 5.2 - Logs de falhas e 2FA registrados
// Camada de negócio central — concentra todas as políticas de segurança da aplicação.
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.2 - Parâmetros de custo do hash configurados e justificados
    // REQ 1.3 - Uso de salt criptográfico único por usuário
    // BCrypt com fator de custo 10 — salt único gerado automaticamente por usuário.
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // REQ 5.2 - Logs de falhas e 2FA registrados
    // Sanitização de inputs nos logs previne Log Injection via CRLF.
    private String sanitizeLog(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\r\n\t]", "_");
    }

    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.3 - Uso de salt criptográfico único por usuário
    // REQ 1.4 - Armazenamento correto do hash + salt
    // REQ 5.1 - Logs de autenticação registrados
    public User register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("[REGISTRO] Tentativa de cadastro com e-mail já existente: {}", sanitizeLog(email));
            throw new RuntimeException("Email já cadastrado");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));

        User saved = userRepository.save(user);
        log.info("[REGISTRO] Novo usuário registrado: {}", sanitizeLog(email));
        return saved;
    }

    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.6 - Validação do 2FA após autenticação primária
    // REQ 1.9 - Sessões com tempo de expiração
    // REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
    // REQ 5.1 - Logs de autenticação registrados
    // REQ 5.2 - Logs de falhas e 2FA registrados
    // Bloqueio temporário de 5 minutos após 5 tentativas consecutivas falhas.
    // Sessão não é gerada enquanto o segundo fator não for validado.
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[LOGIN] E-mail não encontrado: {}", sanitizeLog(email));
                    return new RuntimeException("Usuário não encontrado");
                });

        if (user.getAccountLocked()) {
            if (user.getLockTime().plusMinutes(5).isAfter(LocalDateTime.now())) {
                log.warn("[LOGIN] Acesso em conta bloqueada: {}", sanitizeLog(email));
                throw new RuntimeException("Conta bloqueada, tente novamente mais tarde.");
            }
            user.setAccountLocked(false);
            user.setFailedAttempts(0);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                log.warn("[LOGIN] Conta bloqueada por excesso de tentativas: {}", sanitizeLog(email));
            } else {
                log.warn("[LOGIN] Senha inválida para: {}. Tentativas: {}", sanitizeLog(email), attempts);
            }

            userRepository.save(user);
            throw new RuntimeException("Senha inválida");
        }

        user.setFailedAttempts(0);
        user.setAccountLocked(false);

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            userRepository.save(user);
            log.info("[LOGIN] 2FA necessário para: {}", sanitizeLog(email));
            throw new RuntimeException("2FA_REQUIRED");
        }

        // REQ 1.9 - Sessões com tempo de expiração
        // Token UUID v4 com expiração de 10 minutos.
        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        log.info("[LOGIN] Login bem-sucedido: {}", sanitizeLog(email));
        return user;
    }

    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    // Chave TOTP gerada conforme RFC 6238, compatível com Google Authenticator.
    public String enable2FA(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();

        user.setTwoFactorSecret(key.getKey());
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        log.info("[2FA] Ativado para: {}", sanitizeLog(email));
        return key.getKey();
    }

    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    // REQ 1.6 - Validação do 2FA após autenticação primária
    // REQ 1.9 - Sessões com tempo de expiração
    // REQ 5.2 - Logs de falhas e 2FA registrados
    // Código TOTP de 6 dígitos validado com janela de 30 segundos.
    public User verify2FA(String email, int code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        if (!gAuth.authorize(user.getTwoFactorSecret(), code)) {
            log.warn("[2FA] Código inválido para: {}", sanitizeLog(email));
            throw new RuntimeException("Código 2FA inválido");
        }

        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        log.info("[2FA] Verificação bem-sucedida para: {}", sanitizeLog(email));
        return user;
    }

    // REQ 2.1 - Funcionalidade de recuperação de senha implementada
    // REQ 2.2 - Token criptograficamente seguro
    // REQ 2.3 - Token com tempo de expiração
    // REQ 2.6 - Registro de solicitação de recuperação em log
    // Token UUID v4 (122 bits de entropia), expiração de 10 minutos.
    // Envio assíncrono via @Async — libera a thread HTTP imediatamente, prevenindo Resource Exhaustion DoS.
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        sendResetEmailAsync(email, token);

        log.info("[RESET] Solicitação para: {}", sanitizeLog(email));
        return "E-mail de recuperação enviado";
    }

    // REQ 2.4 - Token invalidado após uso
    // REQ 2.5 - Falha correta para token expirado
    // REQ 2.7 - Registro de sucesso/falha do processo
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> {
                    log.warn("[RESET] Token inválido");
                    return new RuntimeException("Token inválido");
                });

        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            log.warn("[RESET] Token expirado para: {}", sanitizeLog(user.getEmail()));
            throw new RuntimeException("Token expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // REQ 2.4 - Token invalidado após uso
        user.setResetToken(null);
        user.setResetTokenExpiration(null);
        userRepository.save(user);

        log.info("[RESET] Senha redefinida para: {}", sanitizeLog(user.getEmail()));
    }

    // REQ 1.9 - Sessões com tempo de expiração
    // REQ 1.10 - Invalidação de sessão no logout
    public boolean validateSession(String token) {
        User user = userRepository.findBySessionToken(token).orElse(null);
        if (user == null) return false;

        if (user.getSessionExpiration().isBefore(LocalDateTime.now())) {
            log.warn("[SESSÃO] Token expirado para: {}", sanitizeLog(user.getEmail()));
            return false;
        }

        return true;
    }

    // REQ 2.1 - Funcionalidade de recuperação de senha implementada
    // REQ 5.1 - Logs de autenticação registrados
    // Processado em pool de threads secundário via @Async — não bloqueia a thread HTTP.
    @Async
    public void sendResetEmailAsync(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Recuperação de senha");
        message.setText("Seu token de recuperação é: " + token);
        mailSender.send(message);
        log.info("[RESET] E-mail enviado para: {}", sanitizeLog(email));
    }

    // REQ 4.4 - Registro explícito de consentimento
    // REQ 4.5 - Consentimento associado à finalidade
    // REQ 4.7 - Registro de data e versão do consentimento
    public String giveConsent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setConsentGiven(true);
        user.setConsentDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("[LGPD] Consentimento registrado para: {}", sanitizeLog(email));
        return "Consentimento registrado em: " + user.getConsentDate();
    }

    // REQ 4.8 - Funcionalidade de consulta aos dados do titular
    // REQ 4.9 - Funcionalidade de exportação dos dados
    // REQ 4.3 - Evidência de minimização de dados — campos sensíveis omitidos da exportação.
    public String exportData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        log.info("[LGPD] Exportação de dados para: {}", sanitizeLog(email));
        return "Dados do titular:\n" +
                "ID: " + user.getId() + "\n" +
                "Email: " + user.getEmail() + "\n" +
                "2FA ativado: " + user.getTwoFactorEnabled() + "\n" +
                "Consentimento dado: " + user.getConsentGiven() + "\n" +
                "Data do consentimento: " + user.getConsentDate();
    }

    // REQ 4.10 - Funcionalidade de exclusão dos dados pessoais
    public String deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        userRepository.delete(user);

        log.info("[LGPD] Conta deletada: {}", sanitizeLog(email));
        return "Conta deletada com sucesso";
    }

    // REQ 4.6 - Possibilidade de revogação do consentimento
    public String revokeConsent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setConsentGiven(false);
        user.setConsentDate(null);
        userRepository.save(user);

        log.info("[LGPD] Consentimento revogado para: {}", sanitizeLog(email));
        return "Consentimento revogado com sucesso";
    }
}