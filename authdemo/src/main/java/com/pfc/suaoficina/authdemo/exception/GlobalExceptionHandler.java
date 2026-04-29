package com.pfc.suaoficina.authdemo.exception;

// Importa a classe ResponseEntity, que permite construir respostas HTTP com status e corpo personalizados
import org.springframework.http.ResponseEntity;
// Importa a anotação que captura exceções lançadas por qualquer controller da aplicação
import org.springframework.web.bind.annotation.ExceptionHandler;
// Importa a anotação que define esta classe como um interceptador global de exceções para controllers REST
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manipulador global de exceções para todos os controllers REST da aplicação.
 *
 * A anotação @RestControllerAdvice combina @ControllerAdvice (intercepta exceções de controllers)
 * com @ResponseBody (converte o retorno automaticamente para JSON ou texto).
 *
 * Quando qualquer controller lançar uma exceção, o Spring verifica se existe um método
 * nesta classe anotado com @ExceptionHandler para o tipo da exceção. Se existir,
 * esse método é executado e seu retorno é enviado como resposta HTTP.
 *
 * Esta abordagem centraliza o tratamento de erros, evitando blocos try-catch repetidos
 * nos controllers e padronizando as respostas de erro.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Trata qualquer exceção do tipo RuntimeException lançada em qualquer controller.
     * RuntimeException é a superclasse de todas as exceções não verificadas (unchecked),
     * que normalmente representam erros de lógica de negócio ou condições inesperadas.
     *
     * O método verifica a mensagem da exceção para decidir qual status HTTP retornar,
     * permitindo diferentes comportamentos para diferentes tipos de erro sem criar
     * múltiplas classes de exceção personalizadas.
     *
     * @param ex A exceção capturada, contendo a mensagem de erro original
     * @return ResponseEntity contendo o status HTTP e o corpo da resposta (mensagem de erro)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {

        // TRATAMENTO ESPECIAL PARA 2FA
        // Verifica se a mensagem da exceção é exatamente "2FA_REQUIRED"
        // Este é um código de controle usado pelo serviço para indicar que o login
        // foi bem-sucedido na credencial, mas o usuário precisa completar o segundo fator.
        // O frontend deve interceptar este status 401 com esta mensagem específica
        // para solicitar o código de autenticação de dois fatores ao usuário.
        if ("2FA_REQUIRED".equals(ex.getMessage())) {
            // Retorna status HTTP 401 (Unauthorized) com o código especial "2FA_REQUIRED"
            // O status 401 é semanticamente correto porque o usuário não completou
            // todos os fatores de autenticação necessários.
            return ResponseEntity.status(401).body("2FA_REQUIRED");
        }

        // Para qualquer outra RuntimeException que não seja o caso especial acima,
        // retorna status HTTP 400 (Bad Request) com a mensagem da exceção.
        // O status 400 indica que a requisição do cliente foi malformada ou
        // violou alguma regra de negócio (ex: e-mail já cadastrado, senha incorreta).
        return ResponseEntity
                .badRequest()
                .body(ex.getMessage());
    }
}