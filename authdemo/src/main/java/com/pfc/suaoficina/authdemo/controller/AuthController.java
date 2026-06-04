package com.pfc.suaoficina.authdemo.controller;

import com.pfc.suaoficina.authdemo.model.User;
import com.pfc.suaoficina.authdemo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints REST de autenticação, segurança e conformidade LGPD.
 * Atua exclusivamente como camada de entrada HTTP — toda lógica de negócio
 * está delegada ao UserService.
 *
 * REQ 1.7 - Fluxo de autenticação documentado
 * REQ 1.12 - Justificativas técnicas documentadas
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.3 - Uso de salt criptográfico único por usuário
    // REQ 1.4 - Armazenamento correto do hash + salt
    @PostMapping("/register")
    public User register(@RequestParam String email, @RequestParam String password) {
        return userService.register(email, password);
    }

    // Retorna 2FA_REQUIRED se o segundo fator estiver ativo — frontend redireciona para /verify-2fa
    // REQ 1.1 - Uso de hash criptográfico seguro para senhas (Argon2, bcrypt ou PBKDF2)
    // REQ 1.6 - Validação do 2FA após autenticação primária
    // REQ 1.11 - Proteção contra força bruta (rate limit, bloqueio, atraso)
    @PostMapping("/login")
    public User login(@RequestParam String email, @RequestParam String password) {
        return userService.login(email, password);
    }

    // Retorna a chave secreta TOTP para uma possível geração do QR Code no frontend
    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    @PostMapping("/enable-2fa")
    public String enable2FA(@RequestParam String email) {
        return userService.enable2FA(email);
    }

    // REQ 1.5 - Autenticação de dois fatores (2FA) implementada
    // REQ 1.6 - Validação do 2FA após autenticação primária
    // REQ 1.9 - Sessões com tempo de expiração
    @PostMapping("/verify-2fa")
    public User verify2FA(@RequestParam String email, @RequestParam int code) {
        return userService.verify2FA(email, code);
    }

    // REQ 2.1 - Funcionalidade de recuperação de senha implementada
    // REQ 2.2 - Token criptograficamente seguro
    // REQ 2.3 - Token com tempo de expiração
    // REQ 2.6 - Registro de solicitação de recuperação em log
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        return userService.requestPasswordReset(email);
    }

    // REQ 2.4 - Token invalidado após uso
    // REQ 2.5 - Falha correta para token expirado
    // REQ 2.7 - Registro de sucesso/falha do processo
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return "Senha redefinida com sucesso";
    }

    // Token lido do header Authorization: Bearer <token>
    // REQ 1.9 - Sessões com tempo de expiração
    // REQ 1.10 - Invalidação de sessão no logout
    @GetMapping("/validate-session")
    public ResponseEntity<?> validateSession(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "").trim();
        boolean valid = userService.validateSession(token);
        if (!valid) return ResponseEntity.status(401).body("Sessão inválida ou expirada.");
        return ResponseEntity.ok(Map.of("valid", true));
    }

    // REQ 4.4 - Registro explícito de consentimento
    // REQ 4.5 - Consentimento associado à finalidade
    // REQ 4.7 - Registro de data e versão do consentimento
    @PostMapping("/consent")
    public String giveConsent(@RequestParam String email) {
        return userService.giveConsent(email);
    }

    // REQ 4.8 - Funcionalidade de consulta aos dados do titular
    // REQ 4.9 - Funcionalidade de exportação dos dados
    @GetMapping("/export-data")
    public String exportData(@RequestParam String email) {
        return userService.exportData(email);
    }

    // REQ 4.10 - Funcionalidade de exclusão dos dados pessoais
    @DeleteMapping("/delete-account")
    public String deleteAccount(@RequestParam String email) {
        return userService.deleteAccount(email);
    }

    // REQ 4.6 - Possibilidade de revogação do consentimento
    @PostMapping("/revoke-consent")
    public String revokeConsent(@RequestParam String email) {
        return userService.revokeConsent(email);
    }
}