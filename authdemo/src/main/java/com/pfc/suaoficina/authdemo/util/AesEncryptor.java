// Pacote de utilitários da aplicação
package com.pfc.suaoficina.authdemo.util;

// Importações do JPA para conversão de atributos
import jakarta.persistence.AttributeConverter;  // Interface para converter entre Java e banco
import jakarta.persistence.Converter;           // Marca a classe como conversor JPA

// Importações do Spring
import org.springframework.beans.factory.annotation.Value;  // Injeta valores do application.properties
import org.springframework.stereotype.Component;            // Torna a classe um Bean Spring

// Bibliotecas de criptografia
import javax.crypto.Cipher;                    // Classe principal de criptografia
import javax.crypto.spec.SecretKeySpec;        // Especificação da chave secreta
import java.util.Base64;                       // Codificação Base64 para salvar no banco

// @Converter - Registra no JPA/Hibernate como conversor de atributos
// @Component - Permite injeção de dependências (@Value funciona por causa disso)
@Converter
@Component
public class AesEncryptor implements AttributeConverter<String, String> {
    // AttributeConverter<String, String> significa:
    // - Converte de String (Java) para String (banco de dados)
    // - Útil para campos como senhas, documentos, dados sensíveis

    private static String SECRET;  // Chave secreta estática (compartilhada entre instâncias)

    // Injeta a chave secreta do arquivo application.properties
    // Exemplo: encryption.secret=minhasenhasecreta123
    @Value("${encryption.secret}")
    public void setSecret(String secret) {
        // Garante exatamente 16 bytes (128 bits) para AES-128
        // String.format("%-16s", secret) - Preenche com espaços até 16 caracteres
        // .substring(0, 16) - Corta se for maior que 16
        // Isso evita erro de tamanho de chave inválido
        SECRET = String.format("%-16s", secret).substring(0, 16);
    }

    // CONVERTE PARA O BANCO DE DADOS (Salvando/Atualizando)
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;  // Não criptografa valores nulos
        
        try {
            // 1. Cria a chave AES a partir do SECRET
            //    SECRET.getBytes() - converte string em bytes
            //    "AES" - algoritmo de criptografia
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");
            
            // 2. Configura o cifrador
            //    "AES/ECB/PKCS5Padding" - algoritmo/modo/preenchimento
            //    ECB: Electronic Codebook (modo básico, menos seguro que CBC)
            //    PKCS5Padding: preenche blocos para tamanho correto
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            
            // 3. Inicializa em modo de CRIPTOGRAFIA com a chave
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            // 4. Criptografa os bytes do texto original
            //    E codifica em Base64 para salvar como texto no banco
            return Base64.getEncoder().encodeToString(
                cipher.doFinal(attribute.getBytes())
            );
        } catch (Exception e) {
            // Se falhar, lança exceção com contexto claro
            throw new RuntimeException("Erro ao criptografar campo", e);
        }
    }

    // CONVERTE DO BANCO DE DADOS (Lendo do banco)
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;  // Não descriptografa valores nulos
        
        try {
            // 1. Cria a mesma chave usada na criptografia
            SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(), "AES");
            
            // 2. Configura o cifrador (mesma configuração)
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            
            // 3. Inicializa em modo de DESCRIPTOGRAFIA
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            // 4. Decodifica de Base64 e descriptografa
            //    Converte bytes de volta para String
            return new String(
                cipher.doFinal(Base64.getDecoder().decode(dbData))
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao descriptografar campo", e);
        }
    }
}
