package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseHttpHandler implements HttpHandler {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    protected final TaskManager taskManager;
    protected final Gson gson;

    protected record Route<E>(String method, Pattern pattern, E endpoint) {
    }

    protected BaseHttpHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

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

    public void sendNotFound(HttpExchange httpExchange, String message) throws IOException {
        sendText(httpExchange, message, 404);
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

    protected <E> E resolveEndpoint(HttpExchange httpExchange, List<Route<E>> routes, E defaultEndpoint) {
        String method = httpExchange.getRequestMethod();
        String path = httpExchange.getRequestURI().getPath();
        for (Route<E> route : routes) {
            if (route.method().equalsIgnoreCase(method)) {
                Matcher matcher = route.pattern().matcher(path);
                if (matcher.matches()) {
                    return route.endpoint();
                }
            }
        }
        return defaultEndpoint;
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