package com.rect.etuattender.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@PropertySource("application.properties")
public class BotConfig {
    @Value("${bot.name}")
    String name;

    @Value("${bot.token}")
    String token;

    @Value("${bot.owner}")
    long owner;
}
