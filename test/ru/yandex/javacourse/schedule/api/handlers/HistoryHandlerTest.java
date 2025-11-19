package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.api.HttpTaskServerTest;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HistoryHandlerTest extends HttpTaskServerTest {
    private final String HISTORY_URI = BASE_URL + "/history";

    @Test
    void testGetHistoryEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HISTORY_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> history = gson.fromJson(response.body(), listType);
        assertNotNull(history);
        assertEquals(0, history.size());
    }

    @Test
    void testGetHistoryFilled() throws Exception {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 10, 0);
        Duration duration = Duration.ofMinutes(30);
        Task task1 = new Task("Task 1", "Description 1", TaskStatus.NEW, start, duration);
        Task task2 = new Task("Task 2", "Description 2", TaskStatus.IN_PROGRESS, start.plusHours(1), duration);
        int id1 = taskManager.addNewTask(task1);
        int id2 = taskManager.addNewTask(task2);
        taskManager.getTask(id1);
        taskManager.getTask(id2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HISTORY_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> history = gson.fromJson(response.body(), listType);
        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals("Task 1", history.get(0).getName());
        assertEquals("Task 2", history.get(1).getName());
    }
}