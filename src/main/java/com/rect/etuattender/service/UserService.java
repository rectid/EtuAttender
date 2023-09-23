package com.rect.etuattender.service;

import com.rect.etuattender.model.User;
import com.rect.etuattender.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public Optional<User> getUser(long id){
        Optional<User> user = userRepository.findById(Math.toIntExact(id));
        return user;
    }

    public void saveUser(User user){
        userRepository.save(user);
    }

    public Optional<User> getUserByNick(String nick){
        Optional<User> user = userRepository.findUserByNick(nick);
        return user;
    }

    public ArrayList<User> getAll(){
        return (ArrayList<User>) userRepository.findAll();
    }


    }
