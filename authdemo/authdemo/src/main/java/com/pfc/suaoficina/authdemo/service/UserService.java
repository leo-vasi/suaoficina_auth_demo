package com.pfc.suaoficina.authdemo.service;

import com.pfc.suaoficina.authdemo.model.User;
import com.pfc.suaoficina.authdemo.repository.UserRepository;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public User register(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("[REGISTRO] Tentativa de cadastro com e-mail já existente: {}", email);
            throw new RuntimeException("Email já cadastrado");
        }

        User user = new User();
        user.setEmail(email);
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        User saved = userRepository.save(user);
        log.info("[REGISTRO] Novo usuário registrado: {}", email);
        return saved;
    }

    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[LOGIN] Tentativa de login com e-mail não encontrado: {}", email);
                    return new RuntimeException("Usuário não encontrado");
                });

        if (user.getAccountLocked()) {
            if (user.getLockTime().plusMinutes(5).isAfter(LocalDateTime.now())) {
                log.warn("[LOGIN] Tentativa de acesso em conta bloqueada: {}", email);
                throw new RuntimeException("Conta bloqueada, tente novamente mais tarde.");
            } else {
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
            }
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now());
                log.warn("[LOGIN] Conta bloqueada por excesso de tentativas: {}", email);
            } else {
                log.warn("[LOGIN] Senha inválida para: {}. Tentativas: {}", email, attempts);
            }

            userRepository.save(user);
            throw new RuntimeException("Senha inválida");
        }

        user.setFailedAttempts(0);
        user.setAccountLocked(false);

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            userRepository.save(user);
            log.info("[LOGIN] 2FA necessário para: {}", email);
            throw new RuntimeException("2FA_REQUIRED");
        }

        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        log.info("[LOGIN] Login bem-sucedido: {}", email);
        return user;
    }

    public String enable2FA(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        GoogleAuthenticatorKey key = gAuth.createCredentials();

        user.setTwoFactorSecret(key.getKey());
        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        log.info("[2FA] 2FA ativado para: {}", email);
        return key.getKey();
    }

    public User verify2FA(String email, int code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), code);

        if (!isValid) {
            log.warn("[2FA] Código 2FA inválido para: {}", email);
            throw new RuntimeException("Código 2FA inválido");
        }

        String sessionToken = UUID.randomUUID().toString();
        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        log.info("[2FA] Verificação bem-sucedida para: {}", email);
        return user;
    }

    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        sendResetEmail(email, token);
        log.info("[RESET] Solicitação de recuperação de senha para: {}", email);
        return "E-mail de recuperação enviado";
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("[RESET] Tentativa de reset com token inválido");
                    return new RuntimeException("Token inválido");
                });

        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            log.warn("[RESET] Token expirado para: {}", user.getEmail());
            throw new RuntimeException("Token expirado");
        }

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);
        user.setResetToken(null);
        user.setResetTokenExpiration(null);
        userRepository.save(user);

        log.info("[RESET] Senha redefinida com sucesso para: {}", user.getEmail());
    }

    public boolean validateSession(String token) {
        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getSessionToken()))
                .findFirst()
                .orElse(null);

        if (user == null) return false;

        if (user.getSessionExpiration().isBefore(LocalDateTime.now())) {
            log.warn("[SESSÃO] Token expirado para: {}", user.getEmail());
            return false;
        }

        return true;
    }

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Recuperação de senha");
        message.setText("Seu token de recuperação é: " + token);
        mailSender.send(message);
    }

    public String giveConsent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        user.setConsentGiven(true);
        user.setConsentDate(LocalDateTime.now());
        userRepository.save(user);

        log.info("[LGPD] Consentimento registrado para: {}", email);
        return "Consentimento registrado em: " + user.getConsentDate();
    }

    public String exportData(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        log.info("[LGPD] Exportação de dados solicitada para: {}", email);
        return "Dados do titular:\n" +
                "ID: " + user.getId() + "\n" +
                "Email: " + user.getEmail() + "\n" +
                "2FA ativado: " + user.getTwoFactorEnabled() + "\n" +
                "Consentimento dado: " + user.getConsentGiven() + "\n" +
                "Data do consentimento: " + user.getConsentDate();
    }

    public String deleteAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        userRepository.delete(user);

        log.info("[LGPD] Conta deletada: {}", email);
        return "Conta deletada com sucesso";
    }

    public String revokeConsent(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        user.setConsentGiven(false);
        user.setConsentDate(null);
        userRepository.save(user);

        log.info("[LGPD] Consentimento revogado para: {}", email);
        return "Consentimento revogado com sucesso";
    }
}