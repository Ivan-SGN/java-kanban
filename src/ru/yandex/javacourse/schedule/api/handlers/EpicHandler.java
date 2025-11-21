package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

public class EpicHandler extends BaseHttpHandler {
    private static final Pattern EPIC_ROOT_PATTERN = Pattern.compile("^/epics/?$");
    private static final Pattern EPIC_BY_ID_PATTERN = Pattern.compile("^/epics/(\\d+)$");
    private static final Pattern EPIC_BY_ID_SUBTASK_PATTERN = Pattern.compile("^/epics/(\\d+)/subtasks/?$");

    private final List<Route<EpicEndpoint>> routes = List.of(
            new Route<>("GET", EPIC_ROOT_PATTERN, EpicEndpoint.GET_ALL_EPICS),
            new Route<>("GET", EPIC_BY_ID_PATTERN, EpicEndpoint.GET_EPIC_BY_ID),
            new Route<>("GET", EPIC_BY_ID_SUBTASK_PATTERN, EpicEndpoint.GET_SUBTASKS_BY_EPIC_ID),
            new Route<>("POST", EPIC_ROOT_PATTERN, EpicEndpoint.POST_EPIC),
            new Route<>("DELETE", EPIC_BY_ID_PATTERN, EpicEndpoint.DELETE_EPIC_BY_ID)
    );

    private enum EpicEndpoint {
        GET_ALL_EPICS,
        GET_EPIC_BY_ID,
        GET_SUBTASKS_BY_EPIC_ID,
        POST_EPIC,
        DELETE_EPIC_BY_ID,
        UNKNOWN
    }

    public EpicHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        EpicEndpoint endpoint = resolveEndpoint(exchange, routes, EpicEndpoint.UNKNOWN);
        try {
            switch (endpoint) {
                case GET_ALL_EPICS -> handleGetAllEpics(exchange);
                case GET_EPIC_BY_ID -> handleGetEpicById(exchange);
                case GET_SUBTASKS_BY_EPIC_ID -> handleGetSubtasksByEpicId(exchange);
                case POST_EPIC -> handlePostEpic(exchange);
                case DELETE_EPIC_BY_ID -> handleDeleteEpic(exchange);
                case UNKNOWN -> sendNotFound(exchange);
            }
        } catch (JsonSyntaxException exception) {
            sendBadRequest(exchange, "Invalid JSON");
        } catch (IllegalArgumentException exception) {
            sendBadRequest(exchange);
        } catch (Exception exception) {
            sendServerError(exchange);
        }
    }

    private void handleGetAllEpics(HttpExchange exchange) throws IOException {
        List<Epic> epics = taskManager.getEpics();
        String response = gson.toJson(epics);
        sendText(exchange, response);
    }

    private void handleGetEpicById(HttpExchange exchange) throws IOException {
        int id = extractPathId(exchange, EPIC_BY_ID_PATTERN);
        Epic epic = taskManager.getEpic(id);
        if (epic != null) {
            String response = gson.toJson(epic);
            sendText(exchange, response);
        } else {
            sendNotFound(exchange);
        }
    }

    private void handleGetSubtasksByEpicId(HttpExchange exchange) throws IOException {
        int id = extractPathId(exchange, EPIC_BY_ID_SUBTASK_PATTERN);
        Epic epic = taskManager.getEpic(id);
        if (epic != null) {
            List<Subtask> subtasks = taskManager.getEpicSubtasks(id);
            String response = gson.toJson(subtasks);
            sendText(exchange, response);
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePostEpic(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        Epic epic;
        epic = gson.fromJson(requestBody, Epic.class);
        if (epic == null) {
            throw new JsonSyntaxException("Request body is empty or invalid");
        }
        int epicId = epic.getId();
        if (epicId == 0) {
            taskManager.addNewEpic(epic);
            sendSuccess(exchange);
        } else {
            Epic existingEpic = taskManager.getEpic(epicId);
            if (existingEpic == null) {
                sendNotFound(exchange);
                return;
            }
            taskManager.updateEpic(epic);
            sendSuccess(exchange);
        }
    }

    private void handleDeleteEpic(HttpExchange exchange) throws IOException {
        int epicId = extractPathId(exchange, EPIC_BY_ID_PATTERN);
        Epic task = taskManager.getEpic(epicId);
        if (task == null) {
            sendNotFound(exchange);
        } else {
            taskManager.deleteEpic(epicId);
            sendSuccess(exchange);
        }
    }
}
