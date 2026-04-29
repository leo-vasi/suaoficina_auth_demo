package com.pfc.suaoficina.authdemo.config;

// Importa as anotações do Spring para definir beans e classes de configuração
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Importa as classes do Spring Security para configurar filtros de segurança HTTP
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuração de segurança da aplicação usando Spring Security.
 * Esta classe define quais requisições HTTP são permitidas ou bloqueadas,
 * além de configurar mecanismos como CORS, CSRF e autenticação.
 *
 * A anotação @Configuration indica que esta classe contém configurações do Spring.
 *
 * ATENÇÃO: A configuração atual desativa todas as proteções e permite acesso
 * irrestrito a todas as rotas. Isto é adequado apenas para desenvolvimento
 * inicial ou protótipos. Em produção, deve-se implementar autenticação e
 * autorização adequadas.
 */
@Configuration
public class SecurityConfig {

    /**
     * Define a cadeia de filtros de segurança que será aplicada às requisições HTTP.
     * O Spring Security usa uma série de filtros (filters) que interceptam cada requisição
     * antes de chegar aos controllers. Este método configura esses filtros.
     *
     * O objeto HttpSecurity permite configurar, de forma fluente (encadeada), as regras de:
     * - CORS: compartilhamento de recursos entre origens diferentes
     * - CSRF: proteção contra ataques de falsificação de requisição entre sites
     * - Autorização: quem pode acessar quais rotas
     * - Autenticação: como o usuário prova sua identidade
     *
     * @param http Objeto de configuração que permite definir as regras de segurança
     * @return Um SecurityFilterChain contendo todas as configurações aplicadas
     * @throws Exception Pode lançar exceções durante a configuração (ex: regras conflitantes)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilita o suporte a CORS usando a configuração definida em CorsConfig
                // O método cors() recebe um lambda vazio apenas para ativar o mecanismo,
                // pois as regras já foram configuradas separadamente na classe CorsConfig.
                .cors(cors -> {})

                // Desativa a proteção CSRF (Cross-Site Request Forgery).
                // CSRF protege contra ataques onde um site malicioso envia requisições
                // não autorizadas em nome de um usuário autenticado. Desativar é comum
                // em APIs REST stateless (sem sessão) ou durante desenvolvimento.
                .csrf(csrf -> csrf.disable())

                // Configura as regras de autorização para as requisições HTTP.
                .authorizeHttpRequests(auth -> auth
                        // anyRequest().permitAll(): permite que qualquer requisição,
                        // para qualquer rota, seja acessada sem autenticação.
                        // Nenhuma verificação de login ou permissão é aplicada.
                        .anyRequest().permitAll()
                );

        // Constrói e retorna o objeto SecurityFilterChain com todas as configurações acima
        return http.build();
    }
}