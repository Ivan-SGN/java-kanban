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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpicHandlerTest extends HttpTaskServerTest {
    private static final String EPIC_URI = BASE_URL + "/epics";

    @Test
    void testGetEpicsEmpty() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Epic>>() {
        }.getType();
        List<Epic> epics = gson.fromJson(response.body(), listType);
        assertNotNull(epics);
        assertEquals(0, epics.size());
    }

    @Test
    void testGetEpicsFilled() throws Exception {
        Epic epic1 = new Epic("Epic 1", "Description 1");
        Epic epic2 = new Epic("Epic 2", "Description 2");
        taskManager.addNewEpic(epic1);
        taskManager.addNewEpic(epic2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Epic>>() {
        }.getType();
        List<Epic> epics = gson.fromJson(response.body(), listType);
        assertNotNull(epics);
        assertEquals(2, epics.size());
    }

    @Test
    void testGetEpicByIdSuccess() throws Exception {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addNewEpic(epic);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + epicId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Epic loadedEpic = gson.fromJson(response.body(), Epic.class);
        assertNotNull(loadedEpic);
        assertEquals(epicId, loadedEpic.getId());
        assertEquals("Epic 1", loadedEpic.getName());
        assertEquals("Description 1", loadedEpic.getDescription());
    }

    @Test
    void testGetEpicByIdErrors() throws Exception {
        int nonExistingId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + nonExistingId))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void testGetEpicSubtasksSuccess() throws Exception {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addNewEpic(epic);
        Subtask subtask1 = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        Subtask subtask2 = new Subtask("Subtask 2", "Description 2", TaskStatus.IN_PROGRESS, epicId);
        int subtaskId1 = taskManager.addNewSubtask(subtask1);
        int subtaskId2 = taskManager.addNewSubtask(subtask2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + epicId + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        Type listType = new TypeToken<List<Subtask>>() {
        }.getType();
        List<Subtask> subtasks = gson.fromJson(response.body(), listType);
        assertNotNull(subtasks);
        assertEquals(2, subtasks.size());
        assertTrue(subtasks.stream().anyMatch(s -> s.getId() == subtaskId1));
        assertTrue(subtasks.stream().anyMatch(s -> s.getId() == subtaskId2));
    }

    @Test
    void testGetEpicSubtasksErrors() throws Exception {
        int nonExistingEpicId = 9999;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + nonExistingEpicId + "/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    void testPostEpicSuccess() throws Exception {
        Epic epic = new Epic("Epic 1", "Description 1");
        String epicJson = gson.toJson(epic);
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .build();

        HttpResponse<String> response = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals(1, taskManager.getEpics().size());
        Epic storedEpic = taskManager.getEpics().getFirst();
        assertEquals("Epic 1", storedEpic.getName());
        assertEquals("Description 1", storedEpic.getDescription());
    }

    @Test
    void testPostEpicErrors() throws Exception {
        String invalidJson = "{invalid";
        HttpRequest invalidRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
                .build();

        HttpResponse<String> invalidResponse = httpClient.send(invalidRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, invalidResponse.statusCode());
    }

    @Test
    void testPostEpicUpdateSuccess() throws Exception {
        Epic originalEpic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addNewEpic(originalEpic);
        Epic updatedEpic = new Epic(epicId, "Epic 2", "Description 2");
        String updatedEpicJson = gson.toJson(updatedEpic);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .POST(HttpRequest.BodyPublishers.ofString(updatedEpicJson))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        Epic storedEpic = taskManager.getEpic(epicId);
        assertNotNull(storedEpic);
        assertEquals("Epic 2", storedEpic.getName());
        assertEquals("Description 2", storedEpic.getDescription());
    }

    @Test
    void testPostEpicUpdateErrors() throws Exception {
        int nonExistingEpicId = 9999;
        Epic nonExistingEpic = new Epic(nonExistingEpicId, "Epic 1", "Description 1");
        String nonExistingEpicJson = gson.toJson(nonExistingEpic);
        HttpRequest updateRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI))
                .POST(HttpRequest.BodyPublishers.ofString(nonExistingEpicJson))
                .build();

        HttpResponse<String> response = httpClient.send(updateRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
        assertNull(taskManager.getEpic(nonExistingEpicId));
    }

    @Test
    void testDeleteEpicByIdSuccess() throws Exception {
        Epic epic = new Epic("Epic 1", "Description 1");
        int epicId = taskManager.addNewEpic(epic);
        Subtask subtask = new Subtask("Subtask 1", "Description 1", TaskStatus.NEW, epicId);
        int subtaskId = taskManager.addNewSubtask(subtask);
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + epicId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertNull(taskManager.getEpic(epicId));
        assertNull(taskManager.getSubtask(subtaskId));
    }

    @Test
    void testDeleteEpicByIdErrors() throws Exception {
        int nonExistingEpicId = 9999;
        HttpRequest deleteRequest = HttpRequest.newBuilder()
                .uri(URI.create(EPIC_URI + "/" + nonExistingEpicId))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }
}