package ru.yandex.javacourse.schedule.api.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseHttpHandler implements HttpHandler {
    private final Charset CHARSET = StandardCharsets.UTF_8;

    public void sendText(HttpExchange httpExchange, String text) throws IOException {
        sendText(httpExchange, text, 200);
    }

    public void sendSuccess(HttpExchange httpExchange) throws IOException {
        sendText(httpExchange, "Success", 201);
    }

    public void sendBadRequest(HttpExchange httpExchange) throws IOException {
        sendText(httpExchange, "Bad Request", 400);
    }

    public void sendBadRequest(HttpExchange httpExchange, String message) throws IOException {
        sendText(httpExchange, message, 400);
    }

    public void sendNotFound(HttpExchange httpExchange) throws IOException {
        sendText(httpExchange, "Not Found", 404);
    }

    public void sendHasInteractions(HttpExchange httpExchange) throws IOException {
        sendText(httpExchange, "Task has time intersection", 406);
    }

    protected void sendServerError(HttpExchange httpExchange) throws IOException {
        sendText(httpExchange, "Internal server error", 500);
    }

    protected String readRequestBody(HttpExchange httpExchange) throws IOException {
        try (InputStream inputStream = httpExchange.getRequestBody()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendText(HttpExchange httpExchange, String text, int statusCode) throws IOException {
        byte[] responseBytes = text.getBytes(CHARSET);
        httpExchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        httpExchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            outputStream.write(responseBytes);
        }
    }

    protected Integer extractPathId(HttpExchange httpExchange, Pattern pathPattern) {
        String requestPath = httpExchange.getRequestURI().getPath();
        Matcher matcher = pathPattern.matcher(requestPath);
        if (!matcher.matches()) {
            return null;
        }
        String idText = matcher.group(1);
        try {
            return Integer.parseInt(idText);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}