package com.rect.etuattender.repository;

import com.rect.etuattender.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findUserByNick(String nick);

    Optional<User> findById(long id);
}
