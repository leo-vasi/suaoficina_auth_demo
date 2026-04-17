package com.pfc.suaoficina.authdemo.repository;

// Importa a entidade User que este repositório irá manipular
import com.pfc.suaoficina.authdemo.model.User;
// Importa a interface base do Spring Data JPA que fornece métodos prontos como save, findById, delete, etc.
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Interface responsável por acessar e manipular os dados da entidade User no banco de dados.
 * Ao estender JpaRepository<User, Long>, esta interface herda automaticamente métodos comuns
 * como salvar, buscar por ID, listar todos, deletar, etc., sem necessidade de implementação manual (DAO).
 *
 * O primeiro tipo genérico (User) é a entidade gerenciada.
 * O segundo tipo genérico (Long) é o tipo da chave primária da entidade.
 *
 * O Spring Data JPA gera automaticamente a implementação desta interface em tempo de execução.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca um usuário no banco de dados pelo seu endereço de e-mail.
     * O Spring Data JPA interpreta o nome do método "findByEmail" e gera automaticamente a consulta SQL.
     *
     * O retorno é um Optional, que pode conter o usuário se encontrado ou estar vazio se não existir.
     * O uso de Optional evita o tratamento de valores nulos (NullPointerException) e força quem chama
     * este método a lidar explicitamente com a possibilidade de o usuário não ser encontrado.
     *
     * @param email O endereço de e-mail do usuário a ser buscado (único no sistema)
     * @return Optional contendo o usuário se encontrado, ou Optional vazio caso contrário
     */
    Optional<User> findByEmail(String email);
}