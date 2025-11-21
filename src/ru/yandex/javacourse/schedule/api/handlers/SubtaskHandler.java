package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Subtask;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class SubtaskHandler extends BaseHttpHandler {
    private static final Pattern SUBTASK_ROOT_PATTERN = Pattern.compile("^/subtasks/?$");
    private static final Pattern SUBTASK_BY_ID_PATTERN = Pattern.compile("^/subtasks/(\\d+)$");

    private final List<Route<SubtaskEndpoint>> routes = List.of(
            new Route<>("GET", SUBTASK_ROOT_PATTERN, SubtaskEndpoint.GET_ALL_SUBTASKS),
            new Route<>("GET", SUBTASK_BY_ID_PATTERN, SubtaskEndpoint.GET_SUBTASK_BY_ID),
            new Route<>("POST", SUBTASK_ROOT_PATTERN, SubtaskEndpoint.POST_SUBTASK),
            new Route<>("DELETE", SUBTASK_BY_ID_PATTERN, SubtaskEndpoint.DELETE_SUBTASK_BY_ID)
    );

    private enum SubtaskEndpoint {
        GET_ALL_SUBTASKS,
        GET_SUBTASK_BY_ID,
        POST_SUBTASK,
        DELETE_SUBTASK_BY_ID,
        UNKNOWN
    }

    public SubtaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SubtaskEndpoint endpoint = resolveEndpoint(exchange, routes, SubtaskEndpoint.UNKNOWN);
        try {
            switch (endpoint) {
                case GET_ALL_SUBTASKS -> handleGetAllSubtasks(exchange);
                case GET_SUBTASK_BY_ID -> handleGetSubtaskById(exchange);
                case POST_SUBTASK -> handlePostSubtask(exchange);
                case DELETE_SUBTASK_BY_ID -> handleDeleteSubtask(exchange);
                case UNKNOWN -> sendNotFound(exchange);
            }
        } catch (JsonSyntaxException exception) {
            sendBadRequest(exchange, "Invalid JSON");
        } catch (IllegalArgumentException exception) {
            sendHasInteractions(exchange);
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    private void handleGetAllSubtasks(HttpExchange exchange) throws IOException {
        List<Subtask> subtasks = taskManager.getSubtasks();
        String response = gson.toJson(subtasks);
        sendText(exchange, response);
    }

    private void handleGetSubtaskById(HttpExchange exchange) throws IOException {
        int id = extractPathId(exchange, SUBTASK_BY_ID_PATTERN);
        Subtask subtask = taskManager.getSubtask(id);
        if (subtask != null) {
            String response = gson.toJson(subtask);
            sendText(exchange, response);
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePostSubtask(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        Subtask subtask;
        subtask = gson.fromJson(requestBody, Subtask.class);
        int subtaskId = subtask.getId();
        if (subtaskId == 0) {
            Integer createdSubtaskId = taskManager.addNewSubtask(subtask);
            if (createdSubtaskId == null) {
                sendNotFound(exchange);
            } else {
                sendSuccess(exchange);
            }
        } else {
            Subtask existingSubtask = taskManager.getSubtask(subtaskId);
            if (existingSubtask == null) {
                sendNotFound(exchange);
            }
            taskManager.updateSubtask(subtask);
            sendSuccess(exchange);
        }
    }

    private void handleDeleteSubtask(HttpExchange exchange) throws IOException {
        int subtaskId = extractPathId(exchange, SUBTASK_BY_ID_PATTERN);
        Subtask task = taskManager.getSubtask(subtaskId);
        if (task == null) {
            sendNotFound(exchange);
        } else {
            taskManager.deleteSubtask(subtaskId);
            sendSuccess(exchange);
        }
    }
}
