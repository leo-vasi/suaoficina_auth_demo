package com.pfc.suaoficina.authdemo.config;

// Importa as anotações do Spring para definir beans e classes de configuração
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Importa classes para configurar o CORS (Cross-Origin Resource Sharing)
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração de CORS (Cross-Origin Resource Sharing) para a aplicação.
 * CORS é um mecanismo de segurança do navegador que controla quais domínios (origens)
 * podem fazer requisições a esta API. Sem a configuração adequada, requisições vindas
 * de um frontend hospedado em um domínio diferente seriam bloqueadas pelo navegador.
 *
 * A anotação @Configuration indica que esta classe contém configurações do Spring.
 */
@Configuration
public class CorsConfig {

    /**
     * Cria e configura um bean do tipo WebMvcConfigurer, que permite personalizar
     * o comportamento do Spring MVC, incluindo as regras de CORS.
     *
     * O método é anotado com @Bean, informando ao Spring que o objeto retornado
     * deve ser gerenciado pelo contêiner de inversão de controle (IoC).
     *
     * ATENÇÃO: A configuração atual libera totalmente o acesso, o que é adequado
     * para desenvolvimento ou testes, mas pode representar risco de segurança
     * em produção, pois qualquer site pode fazer requisições a esta API.
     *
     * @return Um objeto WebMvcConfigurer com as regras de CORS definidas
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Define quais rotas, origens, métodos HTTP e cabeçalhos são permitidos.
             * @param registry Objeto usado para registrar as configurações de CORS
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // addMapping("/**"): aplica as regras a todas as rotas da aplicação
                // .allowedOrigins("*"): permite requisições vindas de qualquer domínio
                // .allowedMethods("*"): permite todos os métodos HTTP (GET, POST, PUT, DELETE, etc.)
                // .allowedHeaders("*"): permite todos os cabeçalhos HTTP
                registry.addMapping("/**")
                        .allowedOrigins("*") // libera todas as origens
                        .allowedMethods("*")
                        .allowedHeaders("*");
            }
        };
    }
}