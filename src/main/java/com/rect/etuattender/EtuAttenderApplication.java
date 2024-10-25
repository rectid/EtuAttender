package com.rect.etuattender;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.ModelMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class EtuAttenderApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtuAttenderApplication.class, args);
    }

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }

    @Bean
    public JavaTimeModule javaTimeModule(){
        return new JavaTimeModule();
    }


}
