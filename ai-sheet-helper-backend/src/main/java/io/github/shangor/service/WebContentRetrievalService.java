package io.github.shangor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
@Slf4j
public class WebContentRetrievalService {
    /**
     * You can define as many parameters as you want.
     */
    @Tool(description = "Get HTML or JSON data from a given URL")
    public String fetchDataFromUrl(String url) {
        log.info("Tooling request: fetchData from `{}`", url);
        try (var httpclient = HttpClient.newHttpClient()) {
            var resp = httpclient.send(HttpRequest.newBuilder().GET().uri(URI.create(url)).build(), HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() >=200 && resp.statusCode() < 300) {
                return resp.body();
            } else {
                return "Error: %d - %s".formatted(resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            return "Error: %s".formatted(e.getMessage());
        }
    }
}
