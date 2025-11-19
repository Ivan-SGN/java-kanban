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

class PrioritizedHandlerTest extends HttpTaskServerTest {
    private static final String PRIORITIZED_URI = BASE_URL + "/prioritized";

    @Test
    void testGetPrioritizedEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PRIORITIZED_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> tasks = gson.fromJson(response.body(), listType);
        assertNotNull(tasks);
        assertEquals(0, tasks.size());
    }

    @Test
    void testGetPrioritizedFilled() throws Exception {
        LocalDateTime t1 = LocalDateTime.of(2025, 1, 1, 9, 0);
        LocalDateTime t2 = LocalDateTime.of(2025, 1, 1, 11, 0);
        LocalDateTime t3 = LocalDateTime.of(2025, 1, 1, 13, 0);
        Task task1 = new Task("Task 1", "Description 1", TaskStatus.NEW, t1, Duration.ofMinutes(30));
        Task task2 = new Task("Task 2", "Description 2", TaskStatus.NEW, t2, Duration.ofMinutes(30));
        Task task3 = new Task("Task 3", "Description 3", TaskStatus.NEW, t3, Duration.ofMinutes(30));
        taskManager.addNewTask(task2);
        taskManager.addNewTask(task3);
        taskManager.addNewTask(task1);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PRIORITIZED_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> tasks = gson.fromJson(response.body(), listType);
        assertNotNull(tasks);
        assertEquals(3, tasks.size());
        assertEquals("Task 1", tasks.get(0).getName());
        assertEquals("Task 2", tasks.get(1).getName());
        assertEquals("Task 3", tasks.get(2).getName());
    }
}