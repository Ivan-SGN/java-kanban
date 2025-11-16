package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.yandex.javacourse.schedule.manager.TaskManager;
import ru.yandex.javacourse.schedule.tasks.Task;

import java.io.IOException;
import java.util.List;

public class TaskHandler extends BaseHttpHandler {
    TaskManager taskManager;
    Gson gson;

    private enum TaskEndpoint {
        GET_ALL_Tasks,
        UNKNOWN
    }

    public TaskHandler(TaskManager taskManager, Gson gson) {
        this.taskManager = taskManager;
        this.gson = gson;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        TaskEndpoint endpoint = resolveEndpoint(exchange);
        switch (endpoint){
            case GET_ALL_Tasks -> handleGetAllTasks(exchange);
            case UNKNOWN -> sendNotFound(exchange);
        }
    }

    private TaskEndpoint resolveEndpoint(HttpExchange exchange) {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        if ("GET".equalsIgnoreCase(method) && "/tasks".equalsIgnoreCase(path)){
            return TaskEndpoint.GET_ALL_Tasks;
        }
        return TaskEndpoint.UNKNOWN;
    }

    private void handleGetAllTasks(HttpExchange exchange) throws IOException {
        List<Task> tasks = taskManager.getTasks();
        String response = gson.toJson(tasks);
        sendText(exchange, response);
    }
}
