package com.pfc.suaoficina.authdemo.repository;

// Importa a entidade User que este repositório irá manipular
import com.pfc.suaoficina.authdemo.model.User;
// Importa a interface base do Spring Data JPA que fornece métodos prontos como save, findById, delete, etc.
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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


    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
    Optional<User> findBySessionToken(String sessionToken);
}

