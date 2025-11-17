package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrioritizedHandler extends BaseHttpHandler {
    private static final Pattern PRIORITIZED_ROOT_PATTERN = Pattern.compile("^/prioritized/?$");
    TaskManager taskManager;
    Gson gson;

    private record Route(String method, Pattern pattern, PrioritizedEndpoint endpoint) {
    }

    private final List<Route> routes = List.of(
            new Route("GET", PRIORITIZED_ROOT_PATTERN, PrioritizedEndpoint.GET_PRIORITIZED)
    );

    private enum PrioritizedEndpoint {
        GET_PRIORITIZED,
        UNKNOWN
    }

    public PrioritizedHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        PrioritizedEndpoint endpoint = resolveEndpoint(exchange);
        try {
            switch (endpoint) {
                case GET_PRIORITIZED -> handleGetPrioritized(exchange);
                case UNKNOWN -> sendNotFound(exchange);
            }
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    private PrioritizedEndpoint resolveEndpoint(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        for (Route route : routes) {
            if (route.method.equalsIgnoreCase(method)) {
                Matcher matcher = route.pattern.matcher(path);
                if (matcher.matches()) {
                    return route.endpoint;
                }
            }
        }
        return PrioritizedEndpoint.UNKNOWN;
    }

    private void handleGetPrioritized(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getPrioritizedTasks();
        String response = gson.toJson(tasks);
        sendText(exchange, response);
    }
}