package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class PrioritizedHandler extends BaseHttpHandler {
    private static final Pattern PRIORITIZED_ROOT_PATTERN = Pattern.compile("^/prioritized/?$");

    private final List<Route<PrioritizedEndpoint>> routes = List.of(
            new Route<>("GET", PRIORITIZED_ROOT_PATTERN, PrioritizedEndpoint.GET_PRIORITIZED)
    );

    private enum PrioritizedEndpoint {
        GET_PRIORITIZED,
        UNKNOWN
    }

    public PrioritizedHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        PrioritizedEndpoint endpoint = resolveEndpoint(exchange, routes, PrioritizedEndpoint.UNKNOWN);
        try {
            switch (endpoint) {
                case GET_PRIORITIZED -> handleGetPrioritized(exchange);
                case UNKNOWN -> sendNotFound(exchange);
            }
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    private void handleGetPrioritized(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getPrioritizedTasks();
        String response = gson.toJson(tasks);
        sendText(exchange, response);
    }
}