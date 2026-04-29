package com.pfc.suaoficina.authdemo.model;

// Importa a anotação JsonIgnore do Jackson para evitar que campos sensíveis sejam serializados em respostas JSON
import com.fasterxml.jackson.annotation.JsonIgnore;
// Importa as anotações de persistência da JPA (Jakarta Persistence) para mapear a classe como entidade de banco de dados
import com.pfc.suaoficina.authdemo.util.AesEncryptor;
import jakarta.persistence.*;
// Importa a anotação Lombok Data, que gera automaticamente getters, setters, equals, hashCode e toString
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Representa um usuário do sistema, armazenando dados de autenticação, segurança e controle de acesso.
 * A anotação @Entity indica que esta classe será mapeada para uma tabela no banco de dados.
 * A anotação @Table define o nome da tabela como "users".
 * A anotação @Data do Lombok gera automaticamente os métodos getters, setters e utilitários.
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * Identificador único do usuário no banco de dados.
     * @Id marca este campo como chave primária.
     * @GeneratedValue com strategy IDENTITY faz com que o banco de dados gere automaticamente o valor (auto-incremento).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Endereço de e-mail do usuário, usado como nome de usuário para login.
     * unique = true garante que não haverá dois usuários com o mesmo e-mail.
     * nullable = false impede valores nulos no banco de dados.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * Senha do usuário armazenada de forma criptografada (hash) no banco de dados.
     * @JsonIgnore impede que este campo seja enviado em respostas JSON, evitando exposição acidental.
     * nullable = false garante que a senha seja obrigatória.
     */
    @Column(nullable = false)
    @JsonIgnore
    private String password;

    // ========== Configurações de autenticação de dois fatores (2FA) ==========

    /**
     * Indica se o usuário já ativou a autenticação de dois fatores.
     * Valor padrão é false (desativado). O valor é armazenado como 0/1 no banco de dados.
     */
    private Boolean twoFactorEnabled = false;

    /**
     * Chave secreta usada para gerar e validar códigos de autenticação de dois fatores (ex: TOTP).
     * @JsonIgnore protege este valor sensível, impedindo que apareça em respostas JSON.
     */
    @JsonIgnore
    @Convert(converter = AesEncryptor.class)
    private String twoFactorSecret;

    // ========== Recuperação de senha ==========

    /**
     * Token gerado aleatoriamente quando o usuário solicita redefinição de senha.
     * Enviado por e-mail para verificação de identidade.
     */
    @JsonIgnore
    @Convert(converter = AesEncryptor.class)
    private String resetToken;

    /**
     * Data e hora de expiração do token de redefinição de senha.
     * Após este horário, o token não é mais aceito por questões de segurança.
     */
    @JsonIgnore
    private LocalDateTime resetTokenExpiration;

    // ========== Controle de tentativas de login e bloqueio de conta ==========

    /**
     * Número de tentativas de login com senha incorreta desde o último login bem-sucedido.
     * Usado para detectar possíveis ataques de força bruta.
     */
    @JsonIgnore
    private Integer failedAttempts = 0;

    /**
     * Indica se a conta do usuário está temporariamente bloqueada.
     * O bloqueio ocorre após um número excessivo de tentativas de login com falha.
     */
    private Boolean accountLocked = false;

    /**
     * Momento em que a conta foi bloqueada. Utilizado para calcular a duração do bloqueio
     * e desbloquear automaticamente após um período determinado.
     */
    private LocalDateTime lockTime;

    // ========== Gerenciamento de sessão ==========

    /**
     * Token único gerado no login bem-sucedido, usado para manter o usuário autenticado
     * sem necessidade de enviar credenciais a cada requisição.
     */
    private String sessionToken;

    /**
     * Data e hora de expiração da sessão atual. Após este momento, o sessionToken
     * deixa de ser válido e o usuário precisa se autenticar novamente.
     */
    @JsonIgnore
    private LocalDateTime sessionExpiration;


    private Boolean consentGiven = false;

    private LocalDateTime consentDate;
}
