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

import static org.junit.jupiter.api.Assertions.*;

class TaskHandlerTest extends HttpTaskServerTest {
    private final String TASK_URI = BASE_URL + "/tasks";

    @Test
    void testGetTasksEmpty() throws Exception {
        HttpRequest emptyRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .GET()
                .build();

        HttpResponse<String> emptyResponse = httpClient.send(emptyRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, emptyResponse.statusCode(), "GET /tasks must return 200 for empty storage");
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> emptyTasks = gson.fromJson(emptyResponse.body(), listType);
        assertNotNull(emptyTasks, "response body must not be null");
        assertEquals(0, emptyTasks.size(), "tasks list must be empty");
    }

    @Test
    void testGetTasksFilled() throws Exception {
        Task firstTask = new Task("Task 1", "Description 1", TaskStatus.NEW);
        Task secondTask = new Task("Task 2", "Description 2", TaskStatus.IN_PROGRESS);
        taskManager.addNewTask(firstTask);
        taskManager.addNewTask(secondTask);

        HttpRequest filledRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .GET()
                .build();

        HttpResponse<String> filledResponse = httpClient.send(filledRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, filledResponse.statusCode(), "GET /tasks must return 200 for non-empty storage");
        Type listType = new TypeToken<List<Task>>() {
        }.getType();
        List<Task> filledTasks = gson.fromJson(filledResponse.body(), listType);
        assertNotNull(filledTasks, "response body must not be null");
        assertEquals(2, filledTasks.size(), "tasks list must contain two tasks");
    }

    @Test
    void testGetTaskByIdSuccess() throws Exception {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW);
        int taskId = taskManager.addNewTask(task);

        HttpRequest existingRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI + "/" + taskId))
                .GET()
                .build();

        HttpResponse<String> existingResponse = httpClient.send(existingRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, existingResponse.statusCode(), "GET /tasks/{id} must return 200 for existing task");
        Task loadedTask = gson.fromJson(existingResponse.body(), Task.class);
        assertNotNull(loadedTask, "task must be returned in response body");
        assertEquals(taskId, loadedTask.getId(), "returned task id must match requested id");
    }

    @Test
    void testGetTaskByIdErrors() throws Exception {
        int nonExistingId = -1;

        HttpRequest missingRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI + "/" + nonExistingId))
                .GET()
                .build();
        HttpResponse<String> missingResponse = httpClient.send(missingRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, missingResponse.statusCode(), "GET /tasks/{id} must return 404 for missing task");
    }

    @Test
    void testPostTasksSuccess() throws Exception {
        Task newTask = new Task("Task 1", "Description 1", TaskStatus.NEW);
        String newTaskJson = gson.toJson(newTask);

        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(newTaskJson))
                .build();
        HttpResponse<String> response = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "POST /tasks must return 201 on create");
        assertEquals(1, taskManager.getTasks().size(), "one task must be stored after create");
        Task storedTask = taskManager.getTasks().getFirst();
        assertEquals("Task 1", storedTask.getName());
        assertEquals("Description 1", storedTask.getDescription());
        assertEquals(TaskStatus.NEW, storedTask.getStatus());
    }

    @Test
    void testPostTasksErrors() throws Exception {
        String invalidJson = "{invalid";

        HttpRequest invalidRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();
        HttpResponse<String> invalidResponse = httpClient.send(invalidRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, invalidResponse.statusCode(), "POST /tasks must return 400 for invalid JSON");
    }

    @Test
    void testPostTasksOverlappingError() throws Exception {
        LocalDateTime startTime1 = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime startTime2 = LocalDateTime.of(2025, 1, 1, 10, 30);
        Duration duration = Duration.ofMinutes(60);
        Task firstOverlapping = new Task("Task 1", "Description 1", TaskStatus.NEW, startTime1, duration);
        Task secondOverlapping = new Task("Task 2", "Description 2", TaskStatus.NEW, startTime2, duration);
        String firstOverlappingJson = gson.toJson(firstOverlapping);
        String secondOverlappingJson = gson.toJson(secondOverlapping);

        HttpRequest firstOverlapRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(firstOverlappingJson))
                .build();
        httpClient.send(firstOverlapRequest, HttpResponse.BodyHandlers.ofString());
        HttpRequest secondOverlapRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(secondOverlappingJson))
                .build();
        HttpResponse<String> secondOverlapResponse = httpClient.send(secondOverlapRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, secondOverlapResponse.statusCode(), "POST /tasks must return 406 for overlapping tasks");
    }

    @Test
    void testPostTaskUpdateSuccess() throws Exception {
        Task originalTask = new Task("Task 1", "Description 1", TaskStatus.NEW);
        int taskId = taskManager.addNewTask(originalTask);
        Task updatedTask = new Task(taskId, "Task 2", "Description 2", TaskStatus.IN_PROGRESS);
        String updatedTaskJson = gson.toJson(updatedTask);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(updatedTaskJson))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "POST /tasks must return 201 on update");
        Task storedTask = taskManager.getTask(taskId);
        assertNotNull(storedTask, "task must exist after update");
        assertEquals("Task 2", storedTask.getName(), "task name must be updated");
        assertEquals("Description 2", storedTask.getDescription(), "task description must be updated");
        assertEquals(TaskStatus.IN_PROGRESS, storedTask.getStatus(), "task status must be updated");
    }

    @Test
    void testPostTaskUpdateErrors() throws Exception {
        int nonExistingId = 9999;
        Task nonExistingTask = new Task(nonExistingId, "Task 1", "Description 1", TaskStatus.NEW);
        String nonExistingTaskJson = gson.toJson(nonExistingTask);

        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(nonExistingTaskJson))
                .build();
        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode(), "POST /tasks must return 404 when updating missing task");
        assertNull(taskManager.getTask(nonExistingId), "missing task must not appear in manager after failed update");
    }

    @Test
    void testDeleteTaskByIdSuccess() throws Exception {
        Task task = new Task("Task 1", "Description 1", TaskStatus.NEW);
        int taskId = taskManager.addNewTask(task);

        HttpRequest deleteExistingRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI + "/" + taskId))
                .DELETE()
                .build();
        HttpResponse<String> deleteExistingResponse = httpClient.send(deleteExistingRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, deleteExistingResponse.statusCode(), "DELETE /tasks/{id} must return 201 on success");
        assertNull(taskManager.getTask(taskId), "task must be removed from manager");
    }

    @Test
    void testDeleteTaskByIdErrors() throws Exception {
        int nonExistingId = 9999;

        HttpRequest deleteMissingRequest = HttpRequest.newBuilder()
                .uri(URI.create(TASK_URI + "/" + nonExistingId))
                .DELETE()
                .build();
        HttpResponse<String> deleteMissingResponse = httpClient.send(deleteMissingRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, deleteMissingResponse.statusCode(), "DELETE /tasks/{id} must return 404 for missing task");
    }
}
