package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskHandler extends BaseHttpHandler {
    private static final Pattern TASK_ROOT_PATTERN = Pattern.compile("^/tasks/?$");
    private static final Pattern TASK_BY_ID_PATTERN = Pattern.compile("^/tasks/(\\d+)$");

    private final List<Route<TaskEndpoint>> routes = List.of(
            new Route<>("GET", TASK_ROOT_PATTERN, TaskEndpoint.GET_ALL_TASKS),
            new Route<>("GET", TASK_BY_ID_PATTERN, TaskEndpoint.GET_TASK_BY_ID),
            new Route<>("POST", TASK_ROOT_PATTERN, TaskEndpoint.POST_TASK),
            new Route<>("DELETE", TASK_BY_ID_PATTERN, TaskEndpoint.DELETE_TASK_BY_ID)
    );

    private enum TaskEndpoint {
        GET_ALL_TASKS,
        GET_TASK_BY_ID,
        POST_TASK,
        DELETE_TASK_BY_ID,
        UNKNOWN
    }

    public TaskHandler(TaskManager taskManager, Gson gson) {
        super(taskManager, gson);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        TaskEndpoint endpoint = resolveEndpoint(exchange);
        try {
            switch (endpoint) {
                case GET_ALL_TASKS -> handleGetAllTasks(exchange);
                case GET_TASK_BY_ID -> handleGetTaskById(exchange);
                case POST_TASK -> handlePostTask(exchange);
                case DELETE_TASK_BY_ID -> handleDeleteTask(exchange);
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

    private TaskEndpoint resolveEndpoint(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        for (Route<TaskEndpoint> route : routes) {
            if (route.method().equalsIgnoreCase(method)) {
                Matcher matcher = route.pattern().matcher(path);
                if (matcher.matches()) {
                    return route.endpoint();
                }
            }
        }
        return TaskEndpoint.UNKNOWN;
    }

    private void handleGetAllTasks(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getTasks();
        String response = gson.toJson(tasks);
        sendText(exchange, response);
    }

    private void handleGetTaskById(HttpExchange exchange) throws IOException {
        int id = extractPathId(exchange, TASK_BY_ID_PATTERN);
        Task task = taskManager.getTask(id);
        if (task != null) {
            String response = gson.toJson(task);
            sendText(exchange, response);
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePostTask(HttpExchange exchange) throws IOException {
        String requestBody = readRequestBody(exchange);
        Task task;
        task = gson.fromJson(requestBody, Task.class);
        if (task == null) {
            throw new JsonSyntaxException("Request body is empty or invalid");
        }
        int taskId = task.getId();
        if (taskId == 0) {
            taskManager.addNewTask(task);
            sendSuccess(exchange);
        } else {
            Task existingTask = taskManager.getTask(taskId);
            if (existingTask == null) {
                sendNotFound(exchange);
                return;
            }
            taskManager.updateTask(task);
            sendSuccess(exchange);
        }
    }

    private void handleDeleteTask(HttpExchange exchange) throws IOException {
        int taskId = extractPathId(exchange, TASK_BY_ID_PATTERN);
        Task task = taskManager.getTask(taskId);
        if (task == null) {
            sendNotFound(exchange);
        } else {
            taskManager.deleteTask(taskId);
            sendSuccess(exchange);
        }
    }
}
