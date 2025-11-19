package ru.yandex.javacourse.schedule.api.handlers;

import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.api.HttpTaskServerTest;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubtaskHandlerTest extends HttpTaskServerTest {
    private final String SUBTASK_URI = BASE_URL + "/subtasks";

    @Test
    void testGetSubtasksEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Subtask>>() {
        }.getType();
        List<Subtask> subtasks = gson.fromJson(response.body(), listType);
        assertNotNull(subtasks);
        assertEquals(0, subtasks.size());
    }

    @Test
    void testGetSubtasksFilled() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", TaskStatus.IN_PROGRESS, epicId);
        taskManager.addNewSubtask(subtask1);
        taskManager.addNewSubtask(subtask2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Subtask>>() {
        }.getType();
        List<Subtask> subtasks = gson.fromJson(response.body(), listType);
        assertNotNull(subtasks);
        assertEquals(2, subtasks.size());
    }

    @Test
    void testGetSubtaskByIdSuccess() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        int subtaskId = taskManager.addNewSubtask(subtask);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI + "/" + subtaskId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Subtask loadedSubtask = gson.fromJson(response.body(), Subtask.class);
        assertNotNull(loadedSubtask);
        assertEquals(subtaskId, loadedSubtask.getId());
        assertEquals("Subtask 1", loadedSubtask.getName());
        assertEquals("Description 1", loadedSubtask.getDescription());
        assertEquals(TaskStatus.NEW, loadedSubtask.getStatus());
    }

    @Test
    void testGetSubtaskByIdErrors() throws Exception {
        int nonExistingId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI + "/" + nonExistingId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void testPostSubtaskSuccess() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        String subtaskJson = gson.toJson(subtask);
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(subtaskJson))
                .build();

        HttpResponse<String> response = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals(1, taskManager.getSubtasks().size());
        Subtask storedSubtask = taskManager.getSubtasks().getFirst();
        assertEquals("Subtask 1", storedSubtask.getName());
        assertEquals("Description 1", storedSubtask.getDescription());
        assertEquals(TaskStatus.NEW, storedSubtask.getStatus());
        assertEquals(epicId, storedSubtask.getEpicId());
    }

    @Test
    void testPostSubtaskErrors() throws Exception {
        String invalidJson = "{invalid";
        HttpRequest invalidRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> invalidResponse = httpClient.send(invalidRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, invalidResponse.statusCode());

        int nonExistingEpicId = 9999;
        Subtask subtaskWithMissingEpic = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, nonExistingEpicId);
        String subtaskWithMissingEpicJson = gson.toJson(subtaskWithMissingEpic);
        HttpRequest missingEpicRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(subtaskWithMissingEpicJson))
                .build();

        HttpResponse<String> missingEpicResponse = httpClient.send(missingEpicRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, missingEpicResponse.statusCode());
    }

    @Test
    void testPostSubtaskOverlappingError() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        LocalDateTime startTime1 = LocalDateTime.of(2025, 1, 1, 10, 0);
        LocalDateTime startTime2 = LocalDateTime.of(2025, 1, 1, 10, 30);
        Duration duration = Duration.ofMinutes(60);
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, startTime1, duration, epicId);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", TaskStatus.NEW, startTime2, duration, epicId);
        String subtask1Json = gson.toJson(subtask1);
        String subtask2Json = gson.toJson(subtask2);
        HttpRequest firstRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(subtask1Json))
                .build();
        HttpRequest secondRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(subtask2Json))
                .build();

        httpClient.send(firstRequest, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> secondResponse = httpClient.send(secondRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, secondResponse.statusCode());
    }

    @Test
    void testPostSubtaskUpdateSuccess() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        Subtask originalSubtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        int subtaskId = taskManager.addNewSubtask(originalSubtask);
        Subtask updatedSubtask = new Subtask(subtaskId, "Subtask 2", "Description 2", TaskStatus.IN_PROGRESS, epicId);
        String updatedSubtaskJson = gson.toJson(updatedSubtask);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(updatedSubtaskJson))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Subtask storedSubtask = taskManager.getSubtask(subtaskId);
        assertNotNull(storedSubtask);
        assertEquals("Subtask 2", storedSubtask.getName());
        assertEquals("Description 2", storedSubtask.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, storedSubtask.getStatus());
        assertEquals(epicId, storedSubtask.getEpicId());
    }

    @Test
    void testPostSubtaskUpdateErrors() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        int nonExistingSubtaskId = 9999;
        Subtask nonExistingSubtask = new Subtask(nonExistingSubtaskId, "Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        String nonExistingSubtaskJson = gson.toJson(nonExistingSubtask);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI))
                .POST(HttpRequest.BodyPublishers.ofString(nonExistingSubtaskJson))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertNull(taskManager.getSubtask(nonExistingSubtaskId));
    }

    @Test
    void testDeleteSubtaskByIdSuccess() throws Exception {
        int epicId = taskManager.addNewEpic(new Epic("Epic 1", "Description 1"));
        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        int subtaskId = taskManager.addNewSubtask(subtask);
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI + "/" + subtaskId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertNull(taskManager.getSubtask(subtaskId));
    }

    @Test
    void testDeleteSubtaskByIdErrors() throws Exception {
        int nonExistingSubtaskId = 9999;
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(SUBTASK_URI + "/" + nonExistingSubtaskId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }
}