package com.rect.etuattender.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rect.etuattender.model.lesson.Lesson;
import com.rect.etuattender.model.user.User;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Component
@Slf4j
public class EtuApiService {
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public EtuApiService(UserService userService) {
        this.userService = userService;
    }

    @SneakyThrows
    public String auth(User user, String[] lk) {
        try {

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize?response_type=code&redirect_uri=https%3A%2F%2Fdigital.etu.ru%2Fattendance%2Fapi%2Fauth%2Fredirect&client_id=29"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            List<String> loginPostCookies = response.headers().allValues("Set-Cookie");
            String xsrfToken = loginPostCookies.get(0).split(";")[0];
            String lkSessionToken = loginPostCookies.get(1).split(";")[0];
            String allLoginCookie = xsrfToken + "; " + lkSessionToken;


            Document document = Jsoup.parse(response.body());
            Elements elements = document.getElementsByAttributeValue("name", "_token");
            String token = elements.attr("value");


            client = HttpClient.newBuilder().build();

            String loginRequestFields = "_token=" + token + "&email=" + lk[0] + "&password=" + lk[1];

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/login"))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setHeader("Cookie", allLoginCookie)
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequestFields))
                    .build();


            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            loginPostCookies = response.headers().allValues("Set-Cookie");
            xsrfToken = loginPostCookies.get(0).split(";")[0];
            lkSessionToken = loginPostCookies.get(1).split(";")[0];
            allLoginCookie = xsrfToken + "; " + lkSessionToken;


            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize?client_id=29&redirect_uri=https%3A%2F%2Fdigital.etu.ru%2Fattendance%2Fapi%2Fauth%2Fredirect&response_type=code"))
                    .setHeader("Cookie", allLoginCookie)
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            loginPostCookies = response.headers().allValues("Set-Cookie");
            xsrfToken = loginPostCookies.get(0).split(";")[0];
            lkSessionToken = loginPostCookies.get(1).split(";")[0];
            allLoginCookie = xsrfToken + "; " + lkSessionToken;

            document = Jsoup.parse(response.body());
            if (document.hasSameValue("Неверный логин или пароль")) {
                return "lk_error";
            }
            elements = document.getElementsByAttributeValue("name", "_token");
            token = elements.attr("value");
            elements = document.getElementsByAttributeValue("name", "auth_token");
            String authToken = elements.attr("value");

            String oauthRequestFields = "_token=" + token + "&state=&client_id=29" + "&auth_token=" + authToken;
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize"))
                    .setHeader("Cookie", allLoginCookie)
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(oauthRequestFields))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String cookieRedirect = response.headers().firstValue("Location").get();

            request = HttpRequest.newBuilder()
                    .uri(URI.create(cookieRedirect))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String fullCookie = response.headers().firstValue("Set-Cookie").get();

            String[] dividedCookie = fullCookie.split(";");
            String cookie = dividedCookie[0];
            String expires = dividedCookie[2].split("=")[1];

            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss O"))
                    .toFormatter(Locale.ENGLISH);
            LocalDateTime localDateTime = LocalDateTime.parse(expires, formatter);


            if (cookie == null) {
                return "server_error";
            }

            user.setCookie(cookie);
            user.setCookieLifetime(localDateTime);
            userService.saveUser(user);

            return "ok";


        } catch (IOException e) {
            log.error("Auth error!" + user.getId());
            return "server_error";
        }
    }

    public List<Lesson> getLessons(User user) {
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://digital.etu.ru/attendance/api/schedule/check-in"))
                .setHeader("Cookie", user.getCookie())
                .GET()
                .build();
        List<Lesson> lessons;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lessons = Arrays.stream(objectMapper.readValue(response.body(), Lesson[].class)).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return lessons;
    }
}
