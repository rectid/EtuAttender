package com.rect.etuattender.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rect.etuattender.dto.lesson.LessonDto;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
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
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());

    public EtuApiService(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

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


        } catch (IOException | InterruptedException e) {
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
        List<LessonDto> lessonDtos;
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lessonDtos = Arrays.stream(objectMapper.readValue(response.body(), LessonDto[].class)).toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> src.getLesson().getShortTitle(),Lesson::setShortTitle);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(LessonDto::getId,Lesson::setLessonId);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> user,Lesson::setUser);
        List<Lesson> lessons = modelMapper.map(lessonDtos, new TypeToken<List<Lesson>>() {}.getType());
        return lessons;
    }

    public void check(User user, String lessonId){
        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://digital.etu.ru/attendance/api/schedule/check-in/"+lessonId))
                    .setHeader("Cookie", user.getCookie())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()==200){
                log.info("ok"+user);
            }
            if (response.body().contains("истекло")){
                throw new IOException();
            }
        } catch (IOException | InterruptedException e) {
            log.error("Problem with check "+user.getNick()+", lesson "+lessonId);
        }

    }
}