package com.pfc.suaoficina.authdemo.service;

// Importa a entidade User para manipulação dos dados do usuário
import com.pfc.suaoficina.authdemo.model.User;
// Importa o repositório para acesso ao banco de dados
import com.pfc.suaoficina.authdemo.repository.UserRepository;
// Importa as classes da biblioteca GoogleAuthenticator para geração e validação de códigos 2FA (TOTP)
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
// Importa anotação para injeção de dependência automática
import org.springframework.beans.factory.annotation.Autowired;
// Importa classes para envio de e-mails simples (texto puro)
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
// Importa o codificador de senhas BCrypt para criptografia segura (hash com salt)
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Importa anotação para marcar esta classe como um serviço gerenciado pelo Spring
import org.springframework.stereotype.Service;

// Importa classes para manipulação de data/hora e geração de tokens UUID
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Camada de serviço responsável por toda a lógica de negócio relacionada a usuários,
 * autenticação, 2FA, recuperação de senha e gerenciamento de sessão.
 *
 * A anotação @Service indica ao Spring que esta classe é um componente de serviço,
 * permitindo injeção de dependência e transações (embora transações explícitas
 * não estejam configuradas neste exemplo).
 *
 * Este serviço contém as regras de negócio como:
 * - Validação de credenciais
 * - Controle de tentativas de login e bloqueio de conta
 * - Geração e validação de tokens de sessão
 * - Integração com autenticação de dois fatores (Google Authenticator)
 * - Envio de e-mails para recuperação de senha
 */
@Service
public class UserService {

    // Repositório para operações de banco de dados na tabela de usuários
    private final UserRepository userRepository;

    // Codificador de senhas BCrypt - transforma senhas em hashes irreversíveis
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * Construtor para injeção de dependência.
     * O Spring fornece automaticamente o UserRepository.
     * O BCryptPasswordEncoder é instanciado manualmente aqui.
     *
     * @param userRepository Repositório de usuários injetado pelo Spring
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // BCryptPasswordEncoder é um algoritmo de hash específico para senhas,
        // que adiciona automaticamente um "salt" (valor aleatório) para evitar
        // ataques com tabelas pré-computadas (rainbow tables).
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Registra um novo usuário no sistema.
     *
     * Fluxo:
     * 1. Verifica se o e-mail já está cadastrado
     * 2. Cria um novo objeto User
     * 3. Aplica hash na senha fornecida (nunca armazena senha em texto puro)
     * 4. Salva no banco de dados
     *
     * @param email E-mail do novo usuário (único no sistema)
     * @param password Senha em texto puro que será codificada antes de armazenar
     * @return O usuário recém-criado e persistido (com ID gerado)
     * @throws RuntimeException Se o e-mail já estiver cadastrado
     */
    public User register(String email, String password) {

        // Verifica se já existe um usuário com este e-mail no banco de dados
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email já cadastrado");
        }

        // Cria uma nova instância da entidade User
        User user = new User();
        user.setEmail(email);

        // HASH GERADO AQUI
        // Converte a senha em texto puro para um hash BCrypt antes de armazenar
        // Este hash é irreversível: não é possível obter a senha original a partir dele
        String hashedPassword = passwordEncoder.encode(password);
        user.setPassword(hashedPassword);

        // Salva o usuário no banco de dados. O método save retorna a entidade persistida,
        // que agora contém o ID gerado pelo banco de dados.
        return userRepository.save(user);
    }

    /**
     * Realiza o login do usuário com validação de credenciais, controle de bloqueio
     * e suporte a autenticação de dois fatores.
     *
     * Fluxo completo:
     * 1. Busca o usuário pelo e-mail
     * 2. Verifica se a conta está bloqueada (após 5 tentativas falhas)
     * 3. Valida a senha usando BCrypt
     * 4. Se a senha estiver errada, incrementa contador e bloqueia se necessário
     * 5. Se a senha estiver correta, reseta tentativas de falha
     * 6. Se 2FA estiver ativado, lança exceção especial para frontend solicitar o código
     * 7. Se 2FA não estiver ativado, gera token de sessão e retorna usuário
     *
     * @param email E-mail do usuário
     * @param password Senha em texto puro (será comparada com o hash armazenado)
     * @return O usuário autenticado (com token de sessão gerado)
     * @throws RuntimeException Se usuário não encontrado, senha inválida, conta bloqueada, ou 2FA necessário
     */
    public User login(String email, String password) {

        // Busca o usuário pelo e-mail. Se não existir, lança exceção imediatamente.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verifica se está bloqueado
        // accountLocked = true indica que a conta foi bloqueada por excesso de tentativas
        if (user.getAccountLocked()) {
            // Verifica se o tempo de bloqueio de 5 minutos ainda não expirou
            // lockTime é o momento em que a conta foi bloqueada
            if (user.getLockTime().plusMinutes(5).isAfter(LocalDateTime.now())) {
                throw new RuntimeException("Conta bloqueada, tente novamente mais tarde.");
            } else {
                // desbloqueia automaticamente após os 5 minutos
                // Reseta o status de bloqueio e as tentativas de falha
                user.setAccountLocked(false);
                user.setFailedAttempts(0);
            }
        }

        // Valida senha usando BCrypt.matches()
        // O método matches compara a senha em texto puro com o hash armazenado
        // Ele aplica o mesmo algoritmo e salt para verificar se há correspondência
        if (!passwordEncoder.matches(password, user.getPassword())) {

            // Incrementa o contador de tentativas falhas
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);

            // Se atingiu 5 tentativas falhas, bloqueia a conta
            // O bloqueio é uma medida de segurança contra ataques de força bruta
            if (attempts >= 5) {
                user.setAccountLocked(true);
                user.setLockTime(LocalDateTime.now()); // Registra o momento do bloqueio
            }

            // Salva as alterações (contador de tentativas e possível bloqueio)
            userRepository.save(user);

            throw new RuntimeException("Senha inválida");
        }

        // se o login for correto, reseta tentativas
        // Login bem-sucedido limpa o histórico de falhas
        user.setFailedAttempts(0);
        user.setAccountLocked(false);

        // Verifica se tá com o 2fa habilitado, se estiver, ele solicita, caso não ele segue o fluxo normal de login
        // Se twoFactorEnabled = true, o login não está completo ainda
        // O frontend deve capturar a exceção "2FA_REQUIRED" e chamar /verify-2fa
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            userRepository.save(user);
            throw new RuntimeException("2FA_REQUIRED");
        }

        // gera token de sessão
        // UUID.randomUUID() gera um identificador único universal praticamente impossível de adivinhar
        // Este token será enviado ao frontend e usado em requisições subsequentes para identificar o usuário
        String sessionToken = UUID.randomUUID().toString();

        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10)); // Sessão válida por 10 minutos
        userRepository.save(user);

        return user;
    }

    /**
     * Ativa a autenticação de dois fatores (2FA) para um usuário.
     *
     * O 2FA adiciona uma camada extra de segurança: além da senha, o usuário precisa
     * digitar um código de 6 dígitos gerado por um aplicativo autenticador (Google Authenticator, etc.)
     *
     * Fluxo:
     * 1. Busca o usuário pelo e-mail
     * 2. Gera uma chave secreta única usando a biblioteca GoogleAuthenticator
     * 3. Armazena a chave secreta no perfil do usuário
     * 4. Marca twoFactorEnabled = true
     * 5. Retorna a chave secreta para o frontend (que deve gerar um QR Code)
     *
     * @param email E-mail do usuário que deseja ativar o 2FA
     * @return Chave secreta no formato texto (ex: "JBSWY3DPEHPK3PXP")
     *         O frontend deve construir a URI otpauth://totp/... a partir desta chave
     * @throws RuntimeException Se o usuário não for encontrado
     */
    public String enable2FA(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Cria uma instância do gerador Google Authenticator
        GoogleAuthenticator gAuth = new GoogleAuthenticator();
        // createCredentials() gera uma nova chave secreta aleatória
        // Esta chave é o segredo compartilhado entre o servidor e o aplicativo do usuário
        GoogleAuthenticatorKey key = gAuth.createCredentials();

        // Armazena a chave secreta (em texto puro) no banco de dados
        user.setTwoFactorSecret(key.getKey());
        user.setTwoFactorEnabled(true); // Marca 2FA como ativado

        userRepository.save(user);

        // Retorna a chave secreta. O frontend deve:
        // 1. Construir a URI: "otpauth://totp/email?secret=chave&issuer=SuaOficina"
        // 2. Gerar um QR Code a partir desta URI
        // 3. Exibir o QR Code para o usuário escanear no app autenticador
        return key.getKey();
    }

    /**
     * Verifica o código de autenticação de dois fatores fornecido pelo usuário.
     *
     * Este método é chamado após o login bem-sucedido (senha correta) quando
     * o usuário tem 2FA ativado. Ele valida o código de 6 dígitos gerado
     * pelo aplicativo autenticador.
     *
     * Fluxo:
     * 1. Busca o usuário pelo e-mail
     * 2. Valida o código usando a chave secreta armazenada
     * 3. Se válido, gera um token de sessão e o retorna
     * 4. Se inválido, lança exceção
     *
     * @param email E-mail do usuário
     * @param code Código numérico de 6 dígitos (ex: 123456) fornecido pelo usuário
     * @return O usuário autenticado com token de sessão gerado
     * @throws RuntimeException Se usuário não encontrado ou código 2FA inválido
     */
    public User verify2FA(String email, int code) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        // authorize() verifica se o código fornecido corresponde ao esperado
        // baseado na chave secreta do usuário e no horário atual
        // O algoritmo TOTP (Time-based One-Time Password) usa o horário como fator,
        // então o código muda a cada 30 segundos automaticamente
        boolean isValid = gAuth.authorize(user.getTwoFactorSecret(), code);

        if (!isValid) {
            throw new RuntimeException("Código 2FA inválido");
        }

        //GERA SESSÃO AQUI
        // Após validar o 2FA, completa o processo de login gerando a sessão
        String sessionToken = UUID.randomUUID().toString();

        user.setSessionToken(sessionToken);
        user.setSessionExpiration(LocalDateTime.now().plusMinutes(10));

        userRepository.save(user);

        return user;
    }

    /**
     * Solicita redefinição de senha para um usuário (fluxo "esqueci minha senha").
     *
     * Fluxo:
     * 1. Busca o usuário pelo e-mail
     * 2. Gera um token único e temporário (UUID)
     * 3. Armazena o token e sua data de expiração (10 minutos)
     * 4. Envia o token por e-mail para o usuário
     *
     * @param email E-mail do usuário que perdeu a senha
     * @return Mensagem de confirmação (não revela se o e-mail existe por segurança)
     * @throws RuntimeException Se o usuário não for encontrado
     */
    public String requestPasswordReset(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Gera um token aleatório e único. UUID versão 4 é baseado em números aleatórios,
        // oferecendo 122 bits de entropia, suficiente para ser considerado imprevisível.
        String token = UUID.randomUUID().toString();

        user.setResetToken(token);
        user.setResetTokenExpiration(LocalDateTime.now().plusMinutes(10)); // Token expira em 10 minutos

        userRepository.save(user);

        // Envia o token para o e-mail do usuário
        sendResetEmail(email, token);
        return "E-mail de recuperação enviado"; // Envio por email
    }

    /**
     * Redefine a senha do usuário após validação do token de recuperação.
     *
     * Fluxo:
     * 1. Busca o usuário que possui o token fornecido (varredura linear)
     * 2. Verifica se o token não expirou
     * 3. Codifica a nova senha com BCrypt
     * 4. Atualiza a senha e limpa o token (uso único)
     *
     * @param token Token de recuperação enviado por e-mail
     * @param newPassword Nova senha em texto puro (será codificada antes de armazenar)
     * @throws RuntimeException Se token inválido ou expirado
     *
     * OBSERVAÇÃO DE PERFORMANCE: O método atual usa findAll() + stream() para buscar pelo token.
     * Em produção com muitos usuários, isso pode ser ineficiente. O ideal seria adicionar
     * um método no repositório como findByResetToken(String token) para busca indexada.
     */
    public void resetPassword(String token, String newPassword) {

        // Busca o usuário que tenha este token de reset
        // Este método percorre todos os usuários da tabela, o que pode ser lento
        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getResetToken()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        // Verifica se o token ainda está dentro do prazo de validade (10 minutos)
        if (user.getResetTokenExpiration().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expirado");
        }

        // Codifica a nova senha antes de armazenar
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(hashedPassword);

        // limpa token para que não possa ser reutilizado
        // Tokens de recuperação devem ser de uso único por segurança
        user.setResetToken(null);
        user.setResetTokenExpiration(null);

        userRepository.save(user);
    }

    /**
     * Valida se um token de sessão é ativo e não expirou.
     *
     * @param token Token de sessão obtido durante o login ou verificação 2FA
     * @return true se o token existe no banco e a expiração ainda não ocorreu,
     *         false caso contrário (token inexistente ou expirado)
     *
     * OBSERVAÇÃO DE PERFORMANCE: Similar ao resetPassword, este método varre todos
     * os usuários para encontrar o token. Para produção, implementar:
     * UserRepository.findBySessionToken(String token)
     */
    public boolean validateSession(String token) {

        // Busca o usuário que possui este token de sessão
        User user = userRepository.findAll()
                .stream()
                .filter(u -> token.equals(u.getSessionToken()))
                .findFirst()
                .orElse(null);

        if (user == null) return false;

        // Verifica se a data/hora atual já ultrapassou a expiração da sessão
        if (user.getSessionExpiration().isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    // ========== Configuração de e-mail ==========

    /**
     * Componente do Spring para envio de e-mails.
     * Configurado no application.properties (spring.mail.*)
     * A anotação @Autowired injeta automaticamente a implementação fornecida pelo Spring Boot.
     */
    @Autowired
    private JavaMailSender mailSender;

    /**
     * Envia um e-mail simples (texto puro) com o token de recuperação de senha.
     *
     * @param email Endereço de e-mail do destinatário
     * @param token Token de recuperação a ser enviado no corpo do e-mail
     *
     * OBSERVAÇÃO DE SEGURANÇA: Em produção, o token não deveria ser enviado em texto puro
     * no corpo do e-mail. O ideal seria enviar um link único (URL) que o usuário clica,
     * contendo o token como parâmetro, direcionando para uma página segura da aplicação.
     */
    public void sendResetEmail(String email, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);                           // Destinatário
        message.setSubject("Recuperação de senha");     // Assunto do e-mail
        // Corpo do e-mail - em produção, seria mais apropriado um HTML com instruções
        message.setText("Seu token de recuperação é: " + token);

        // Envia o e-mail usando o servidor SMTP configurado
        mailSender.send(message);
    }
}