package com.rect.etuattender.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rect.etuattender.dto.lesson.LessonDto;
import com.rect.etuattender.model.Lesson;
import com.rect.etuattender.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

@Component
@Slf4j
public class EtuApiService {
    private final UserService userService;
    private final ModelMapper modelMapper;
    private final LessonService lessonService;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).registerModule(new JavaTimeModule());

    public EtuApiService(UserService userService, ModelMapper modelMapper, LessonService lessonService) {
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.lessonService = lessonService;
    }

    private String extractCookie(HttpResponse<String> response) {
        List<String> loginPostCookies = response.headers().allValues("Set-Cookie");
        String xsrfToken = loginPostCookies.get(0).split(";")[0];
        String lkSessionToken = loginPostCookies.get(1).split(";")[0];
        return xsrfToken + "; " + lkSessionToken;
    }

    private String extractHtmlElement(HttpResponse<String> response, String value) {
        Document document = Jsoup.parse(response.body());
        Elements elements = document.getElementsByAttributeValue("name", value);
        return elements.attr("value");
    }

    private HttpResponse<String> sendRequest(HttpClient client, HttpRequest request, User user) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info(user.getId() + " => " + response.uri() + " == " + response.statusCode());
            return response;
        } catch (IOException | InterruptedException e) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            log.error(e.getMessage());
            return sendRequest(client, request, user);
        }
    }

    public String auth(User user, String[] lk) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/login"))
                    .GET()
                    .build();

            HttpResponse<String> response = null;
            response = sendRequest(client, request, user);

            String loginRequestFields = "_token=" + extractHtmlElement(response, "_token") + "&email=" + lk[0] + "&password=" + lk[1];

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/login"))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setHeader("Cookie", extractCookie(response))
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequestFields))
                    .build();

            response = sendRequest(client, request, user);

            if (response.statusCode() == 419) {
                return "lk_error";
            }

            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);

            String trueLoginLkCookie = extractCookie(response);
            String reportToForLoginLk = RegExUtils.removeAll(response.headers().map().get("location").getFirst(), "https:\\/\\/id.etu.ru\\/");

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/initialize"))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            HashMap<String, String> newApiToken = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            HashMap<String, Object> newApiRequest = new HashMap<>() {{
                put("email", lk[0]);
                put("password", lk[1]);
                put("remember", false);
                put("_token", newApiToken.get("token"));
            }};

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/portal/api/account"))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/auth/login"))
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Cookie", extractCookie(response))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newApiRequest)))
                    .build();

            response = sendRequest(client, request, user);
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/portal/oauth/" + reportToForLoginLk))
                    .setHeader("Accept", "application/json")
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            HashMap<String, String> newApiToOldRedirect = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            request = HttpRequest.newBuilder()
                    .uri(URI.create(newApiToOldRedirect.get("redirect")))
                    .setHeader("Cookie", trueLoginLkCookie)
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize?client_id=29&redirect_uri=https%3A%2F%2Fdigital.etu.ru%2Fattendance%2Fapi%2Fauth%2Fredirect&response_type=code"))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
            String oauthRequestFields = "_token="
                    + extractHtmlElement(response, "_token")
                    + "&state=&client_id=29" + "&auth_token="
                    + extractHtmlElement(response, "auth_token");

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize"))
                    .setHeader("Cookie", extractCookie(response))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(oauthRequestFields))
                    .build();

            response = sendRequest(client, request, user);
            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().firstValue("location").get()))
                    .GET()
                    .build();

            response = sendRequest(client, request, user);
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
            user.setLessons(getLessons(user));
            user.setAutoCheck(true);
            user.setState(User.State.IN_LESSONS_MENU);
            userService.saveUser(user);

            return "ok";


        } catch (IOException e) {
            log.error(e.getMessage());
            log.error("Auth error!|" + user.getId());
            return "server_error";
        }
    }

    public List<Lesson> getLessons(User user) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://digital.etu.ru/attendance/api/schedule/check-in"))
                .setHeader("Cookie", user.getCookie())
                .GET()
                .build();
        List<LessonDto> lessonDtos;
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            HttpResponse<String> response = sendRequest(client, request, user);
            lessonDtos = Arrays.stream(objectMapper.readValue(response.body(), LessonDto[].class)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> src.getLesson().getShortTitle(), Lesson::setShortTitle);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(LessonDto::getId, Lesson::setLessonId);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> user, Lesson::setUser);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> user, Lesson::setUser);
        modelMapper.typeMap(LessonDto.class, Lesson.class).addMapping(src -> {
            if (src.getTeachers() != null) {
                return src.getTeachers().getFirst().getSurname();
            }
            return null;
        }, Lesson::setTeacher);


        return modelMapper.map(lessonDtos, new TypeToken<List<Lesson>>() {
        }.getType());
    }


    public void check(User user, Lesson lesson) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://digital.etu.ru/attendance/api/schedule/check-in/" + lesson.getLessonId()))
                .setHeader("Cookie", user.getCookie())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()) {
            sendRequest(client, request, user);
            lessonService.checkLesson(lesson);
        }

    }
}