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

    public String auth(User user, String[] lk) {
        try (HttpClient client = HttpClient.newBuilder().build()){

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/login"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String loginRequestFields = "_token=" + extractHtmlElement(response, "_token") + "&email=" + lk[0] + "&password=" + lk[1];

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/login"))
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .setHeader("Cookie", extractCookie(response))
                    .POST(HttpRequest.BodyPublishers.ofString(loginRequestFields))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 419) {
                return "lk_error";
            }

            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            String trueLoginLkCookie = extractCookie(response);
            String reportToForLoginLk = RegExUtils.removeAll(response.headers().map().get("location").getFirst(), "https:\\/\\/id.etu.ru\\/");

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/initialize"))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());
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

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/auth/login"))
                    .setHeader("Content-Type", "application/json")
                    .setHeader("Cookie", extractCookie(response))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(newApiRequest)))
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.id.etu.ru/portal/oauth/" + reportToForLoginLk))
                    .setHeader("Accept", "application/json")
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            HashMap<String, String> newApiToOldRedirect = objectMapper.readValue(response.body(), new TypeReference<>() {
            });

            request = HttpRequest.newBuilder()
                    .uri(URI.create(newApiToOldRedirect.get("redirect")))
                    .setHeader("Cookie", trueLoginLkCookie)
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().map().get("location").getFirst()))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create("https://lk.etu.ru/oauth/authorize?client_id=29&redirect_uri=https%3A%2F%2Fdigital.etu.ru%2Fattendance%2Fapi%2Fauth%2Fredirect&response_type=code"))
                    .setHeader("Cookie", extractCookie(response))
                    .GET()
                    .build();

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

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

            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            request = HttpRequest.newBuilder()
                    .uri(URI.create(response.headers().firstValue("location").get()))
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
            user.setAutoCheck(true);
            user.setState(User.State.IN_LESSONS_MENU);
            user.setLessons(getLessons(user));
            userService.saveUser(user);

            return "ok";


        } catch (IOException | InterruptedException e) {
            log.error("Auth error!" + user.getId());
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
        try (HttpClient client = HttpClient.newBuilder().build()){
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lessonDtos = Arrays.stream(objectMapper.readValue(response.body(), LessonDto[].class)).toList();
        } catch (IOException | InterruptedException e) {
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


    public boolean check(User user, Lesson lesson) {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://digital.etu.ru/attendance/api/schedule/check-in/" + lesson.getLessonId()))
                .setHeader("Cookie", user.getCookie())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try (HttpClient client = HttpClient.newBuilder().build()){
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Checked " + user.getNick() + ", lesson " + lesson.getShortTitle() + ", response: " + response.body());
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Problem with check " + user.getNick() + ", lesson " + lesson.getShortTitle());
            return false;
        }

    }
}