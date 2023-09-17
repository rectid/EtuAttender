package com.rect.etuattender.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rect.etuattender.model.lesson.Lesson;
import com.rect.etuattender.model.user.User;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Slf4j
public class EtuApiService {

    private final OkHttpClient client = new OkHttpClient();
    private final UserService userService;
    private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public EtuApiService(UserService userService) {
        this.userService = userService;
    }

    public String auth(User user, String[] lk){
        try {
            Request request = new Request.Builder()
                    .url("https://digital.etu.ru/attendance/api/auth")
                    .get()
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();

            List<String> loginPostCookies = response.headers("Set-Cookie");
            String xsrfToken = loginPostCookies.get(0).split(";")[0];
            String lkSessionToken = loginPostCookies.get(1).split(";")[0];
            String allLoginCookie = xsrfToken+";"+lkSessionToken;


            Document document = Jsoup.parse(response.body().string());
            Elements elements = document.getElementsByAttributeValue("name","_token");
            String token = elements.attr("value");

            RequestBody body = RequestBody.create(MediaType.get("application/x-www-form-urlencoded"), "_token="+token+"&email="+lk[0]+"&password="+lk[1]);
            request = new Request.Builder()
                    .url("https://lk.etu.ru/login")
                    .addHeader("Content-Type","application/x-www-form-urlencoded")
                    .addHeader("Cookie",allLoginCookie)
                    .post(body)
                    .build();

            call = client.newCall(request);
            response = call.execute();

            request = new Request.Builder()
                    .url("https://digital.etu.ru/attendance/api/auth")
                    .addHeader("Cookie",allLoginCookie)
                    .get()
                    .build();

            call = client.newCall(request);
            response = call.execute();

            document = Jsoup.parse(response.body().string());
            if (document.hasSameValue("Неверный логин или пароль")){
                return "lk_error";
            }
            elements = document.getElementsByAttributeValue("name","_token");
            token = elements.attr("value");
            elements = document.getElementsByAttributeValue("name","auth_token");
            String authToken = elements.attr("value");

            body = RequestBody.create(MediaType.get("application/x-www-form-urlencoded"), "_token="+token+"state=&client_id=29&auth_token="+authToken);
            request = new Request.Builder()
                    .url("https://lk.etu.ru/login")
                    .post(body)
                    .build();

            call = client.newCall(request);
            response = call.execute();

            String getCookieURL = response.headers().values("Location").get(0).toString();

            request = new Request.Builder()
                    .url(getCookieURL)
                    .get()
                    .build();

            call = client.newCall(request);
            response = call.execute();

            String fullCookie = response.headers().values("Set-Cookie").get(0).toString();

            String[] dividedCookie = fullCookie.split(";");
            String cookie = dividedCookie[0];
            String expires = dividedCookie[2].split("=")[1];

            SimpleDateFormat dateFormat = new SimpleDateFormat("E, dd MMM yyyy hh:mm:ss z");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date parsedDate = dateFormat.parse(expires);
            Timestamp expiresTimestamp = new java.sql.Timestamp(parsedDate.getTime());

            if (cookie==null){
                return "server_error";
            }

            user.setCookie(cookie);
            user.setTimestamp(expiresTimestamp);
            userService.saveUser(user);

            return "ok";


        } catch (IOException | ParseException e) {
            log.error("Auth error!"+user.getId());
            return "server_error";
        }
    }

    public List<Lesson> getLessons(User user){
            Request request = new Request.Builder()
                    .url("https://lk.etu.ru/login")
                    .addHeader("Cookie",user.getCookie())
                    .get()
                    .build();

            Call call = client.newCall(request);
        try {
            Response response = call.execute();
            List<Lesson> lessons = Arrays.stream(objectMapper.readValue(response.body().string(),Lesson[].class)).toList();
            return lessons;
        } catch (IOException e) {
            return null;
        }
    }
}
