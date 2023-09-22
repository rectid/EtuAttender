package com.rect.etuattender.service;

import org.springframework.stereotype.Component;

@Component
public class CheckPoolService {

    private final UserService userService;
    private final EtuApiService etuApiService;

    public CheckPoolService(UserService userService, EtuApiService etuApiService) {
        this.userService = userService;
        this.etuApiService = etuApiService;
    }

    public void check(){

    }
}
