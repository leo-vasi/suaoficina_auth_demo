package com.pfc.suaoficina.authdemo.controller;

// Importa a entidade User para ser usada como retorno nos métodos de registro, login e verificação 2FA
import com.pfc.suaoficina.authdemo.model.User;
// Importa o serviço que contém a lógica de negócio para autenticação e gerenciamento de usuários
import com.pfc.suaoficina.authdemo.service.UserService;
// Importa anotações do Spring para definir este componente como um controller REST
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST responsável por expor os endpoints de autenticação da aplicação.
 *
 * A anotação @RestController indica que esta classe é um controller onde cada método
 * retorna diretamente o objeto que será convertido para JSON/XML (diferente de @Controller,
 * que geralmente retorna nomes de views).
 *
 * A anotação @RequestMapping("/auth") define que todos os endpoints deste controller
 * terão o prefixo "/auth". Por exemplo, o método register será acessível em "/auth/register".
 *
 * Este controller atua como uma camada de apresentação (interface HTTP), apenas
 * recebendo requisições, extraindo parâmetros, delegando a execução para o UserService,
 * e retornando os resultados. Nenhuma lógica de negócio deve existir aqui.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    // Dependência do serviço de usuário, responsável pela lógica de negócio
    private final UserService userService;

    /**
     * Construtor para injeção de dependência via construtor.
     * O Spring automaticamente fornece uma instância de UserService ao criar este controller.
     * Este é o padrão recomendado para injeção de dependência, pois facilita testes
     * e garante que a dependência seja obrigatória (final) e imutável.
     *
     * @param userService Serviço de usuário que contém os métodos de registro, login, 2FA, etc.
     */
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Endpoint para registro de novo usuário.
     * POST /auth/register?email=usuario@exemplo.com&password=senha123
     *
     * @param email Endereço de e-mail do usuário (será usado como nome de usuário)
     * @param password Senha em texto puro (o serviço deve criptografar antes de salvar)
     * @return O objeto User recém-criado e salvo no banco de dados (sem a senha, devido ao @JsonIgnore)
     */
    @PostMapping("/register")
    public User register(@RequestParam String email, @RequestParam String password) {
        return userService.register(email, password);
    }

    /**
     * Endpoint para login do usuário.
     * POST /auth/login?email=usuario@exemplo.com&password=senha123
     *
     * Comportamento esperado:
     * - Se o usuário não tem 2FA ativado, retorna os dados do usuário logado.
     * - Se o usuário tem 2FA ativado, o serviço lança uma exceção com mensagem "2FA_REQUIRED",
     *   que será capturada pelo GlobalExceptionHandler e retornará status 401 com o código especial.
     *   O frontend deve então chamar o endpoint /verify-2fa para completar o login.
     *
     * @param email E-mail do usuário
     * @param password Senha do usuário
     * @return O objeto User logado (dados básicos, sem campos sensíveis)
     */
    @PostMapping("/login")
    public User login(@RequestParam String email, @RequestParam String password) {
        return userService.login(email, password);
    }

    /**
     * Endpoint para ativar a autenticação de dois fatores (2FA) para um usuário.
     * POST /auth/enable-2fa?email=usuario@exemplo.com
     *
     * @param email E-mail do usuário que deseja ativar o 2FA
     * @return Uma string contendo a URI de configuração (ex: otpauth://...)
     *         que deve ser convertida em QR Code para o usuário escanear no aplicativo
     *         autenticador (Google Authenticator, Microsoft Authenticator, etc.)
     */
    @PostMapping("/enable-2fa")
    public String enable2FA(@RequestParam String email) {
        return userService.enable2FA(email);
    }

    /**
     * Endpoint para verificar o código de autenticação de dois fatores.
     * POST /auth/verify-2fa?email=usuario@exemplo.com&code=123456
     *
     * Este endpoint é chamado após o login bem-sucedido, quando o usuário tem 2FA ativado.
     * O código de 6 dígitos gerado pelo aplicativo autenticador deve ser validado.
     *
     * @param email E-mail do usuário
     * @param code Código numérico de 6 dígitos gerado pelo aplicativo autenticador
     * @return O objeto User logado (após validação bem-sucedida do 2FA)
     */
    @PostMapping("/verify-2fa")
    public User verify2FA(@RequestParam String email, @RequestParam int code) {
        return userService.verify2FA(email, code);
    }

    /**
     * Endpoint para solicitar redefinição de senha (esqueci minha senha).
     * POST /auth/forgot-password?email=usuario@exemplo.com
     *
     * @param email E-mail do usuário que perdeu a senha
     * @return Mensagem de confirmação informando que um e-mail com o token foi enviado
     *         (ou que o e-mail foi encontrado, sem revelar informações sensíveis)
     */
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email) {
        return userService.requestPasswordReset(email);
    }

    /**
     * Endpoint para redefinir a senha usando o token recebido por e-mail.
     * POST /auth/reset-password?token=token_recebido&newPassword=nova_senha123
     *
     * @param token Token único e temporário enviado ao e-mail do usuário
     * @param newPassword Nova senha que substituirá a antiga (deve ser armazenada criptografada)
     * @return Mensagem de confirmação de sucesso
     */
    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        userService.resetPassword(token, newPassword);
        return "Senha redefinida com sucesso";
    }

    /**
     * Endpoint para validar se um token de sessão ainda é válido.
     * GET /auth/validate-session?token=token_da_sessao
     *
     * Útil para o frontend verificar periodicamente se a sessão do usuário ainda está ativa
     * sem precisar fazer uma requisição que exija dados sensíveis. Por exemplo, pode ser
     * chamado a cada minuto para estender automaticamente a sessão ou redirecionar para login.
     *
     * @param token Token de sessão obtido durante o login
     * @return true se o token existe e não expirou, false caso contrário
     */
    @GetMapping("/validate-session")
    public boolean validateSession(@RequestParam String token) {
        return userService.validateSession(token);
    }

    @PostMapping("/consent")
    public String giveConsent(@RequestParam String email) {
        return userService.giveConsent(email);
    }

    @GetMapping("/export-data")
    public String exportData(@RequestParam String email) {
        return userService.exportData(email);
    }

    @DeleteMapping("/delete-account")
    public String deleteAccount(@RequestParam String email) {
        return userService.deleteAccount(email);
    }

    @PostMapping("/revoke-consent")
    public String revokeConsent(@RequestParam String email) {
        return userService.revokeConsent(email);
    }
}