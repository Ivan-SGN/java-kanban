package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryHandler extends BaseHttpHandler {
    private static final Pattern HISTORY_ROOT_PATTERN = Pattern.compile("^/history/?$");
    private final TaskManager taskManager;
    private final Gson gson;

    private record Route(String method, Pattern pattern, HistoryEndpoint endpoint) {
    }

    private final List<Route> routes = List.of(
            new Route("GET", HISTORY_ROOT_PATTERN, HistoryEndpoint.GET_HISTORY)
    );

    private enum HistoryEndpoint {
        GET_HISTORY,
        UNKNOWN
    }

    public HistoryHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HistoryEndpoint endpoint = resolveEndpoint(exchange);
        try {
            switch (endpoint) {
                case GET_HISTORY -> handleGetHistory(exchange);
                case UNKNOWN -> sendNotFound(exchange);
            }
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    private HistoryEndpoint resolveEndpoint(HttpExchange exchange) {
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
        return HistoryEndpoint.UNKNOWN;
    }

    private void handleGetHistory(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getHistory();
        String response = gson.toJson(tasks);
        sendText(exchange, response);
    }
}