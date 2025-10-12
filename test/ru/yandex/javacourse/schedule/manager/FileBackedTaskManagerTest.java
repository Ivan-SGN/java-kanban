package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @TempDir
    static Path tempDir;

    Path file;
    TaskManager manager;

    @BeforeEach
    void initManager() throws IOException {
        file = Files.createTempFile(tempDir, "test-storage", ".csv");
        manager = new FileBackedTaskManager(file);
    }

    @Test
    void testLoadFromEmptyFile() {
        assertTrue(manager.getTasks().isEmpty(), "tasks should be empty for a fresh file");
        assertTrue(manager.getEpics().isEmpty(), "epics should be empty for a fresh file");
        assertTrue(manager.getSubtasks().isEmpty(), "subtasks should be empty for a fresh file");
    }

    @Test
    void testSaveAndLoadTaskFromFile() {
        Task task = new Task("Task 1", "Task description", TaskStatus.NEW);
        int taskId = manager.addNewTask(task);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Task loaded = reloaded.getTask(taskId);
        assertNotNull(loaded, "loaded task should be found by id");
        assertEquals("Task 1", loaded.getName(), "task name should persist");
        assertEquals("Task description", loaded.getDescription(), "task description should persist");
        assertEquals(TaskStatus.NEW, loaded.getStatus(), "task status should persist");
    }

    @Test
    void testUpdateTaskFileBacked() {
        int taskId = manager.addNewTask(new Task("Task 1", "Task description", TaskStatus.NEW));
        Task updated = new Task(manager.getTask(taskId));
        updated.setName("Task 1 (updated)");
        updated.setDescription("Task description (updated)");
        updated.setStatus(TaskStatus.DONE);
        manager.updateTask(updated);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Task loaded = reloaded.getTask(taskId);
        assertNotNull(loaded, "updated task must exist after reload");
        assertEquals("Task 1 (updated)", loaded.getName(), "name should persist after update");
        assertEquals("Task description (updated)", loaded.getDescription(), "description should persist after update");
        assertEquals(TaskStatus.DONE, loaded.getStatus(), "status should persist after update");
    }

    @Test
    void testDeleteTaskFileBacked() {
        int taskId = manager.addNewTask(new Task("Task 1", "Task description", TaskStatus.NEW));
        manager.deleteTask(taskId);

        TaskManager reloaded = new FileBackedTaskManager(file);
        assertNull(reloaded.getTask(taskId), "task should be absent after deletion and reload");
    }

    @Test
    void testSaveAndLoadEpicFromFile() {
        Epic epic = new Epic("Epic 1", "Epic description");
        int epicId = manager.addNewEpic(epic);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Epic loaded = reloaded.getEpic(epicId);
        assertNotNull(loaded, "loaded epic should be found by id");
        assertEquals("Epic 1", loaded.getName(), "epic name should persist");
        assertEquals("Epic description", loaded.getDescription(), "epic description should persist");
    }

    @Test
    void testUpdateEpicFileBacked() {
        int epicId = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        Epic updated = new Epic(manager.getEpic(epicId));
        updated.setName("Epic 1 (updated)");
        updated.setDescription("Epic description (updated)");
        manager.updateEpic(updated);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Epic loaded = reloaded.getEpic(epicId);
        assertNotNull(loaded, "updated task must exist after reload");
        assertEquals("Epic 1 (updated)", loaded.getName(), "name should persist after update");
        assertEquals("Epic description (updated)", loaded.getDescription(), "description should persist after update");
    }

    @Test
    void testDeleteEpicFileBacked() {
        int epicId = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        Integer subId1 = manager.addNewSubtask(new Subtask("Subtask 1", "Subtask description", TaskStatus.NEW, epicId));
        Integer subId2 = manager.addNewSubtask(new Subtask("Subtask 2", "Subtask description", TaskStatus.NEW, epicId));
        manager.deleteEpic(epicId);
        TaskManager reloaded = new FileBackedTaskManager(file);
        assertNull(reloaded.getEpic(epicId), "epic should be absent after deletion and reload");
        assertNull(reloaded.getSubtask(subId1), "linked subtask should be removed with epic");
        assertNull(reloaded.getSubtask(subId2), "linked subtask should be removed with epic");
    }

    @Test
    void testSaveAndLoadSubtaskFromFile() {
        int epicId = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        Subtask sub = new Subtask("Subtask 1", "Subtask description", TaskStatus.NEW, epicId);
        Integer subId = manager.addNewSubtask(sub);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Subtask loaded = reloaded.getSubtask(subId);
        Epic loadedEpic = reloaded.getEpic(epicId);
        assertNotNull(loaded, "loaded subtask should be found by id");
        assertEquals("Subtask 1", loaded.getName(), "subtask name should persist");
        assertEquals("Subtask description", loaded.getDescription(), "subtask description should persist");
        assertEquals(TaskStatus.NEW, loaded.getStatus(), "subtask status should persist");
        assertEquals(epicId, loaded.getEpicId(), "subtask epicId should persist");
        assertNotNull(loadedEpic, "linked epic should be present after reload");
    }

    @Test
    void testUpdateSubtaskFileBacked() {
        int epicId = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        int subtaskId = manager.addNewSubtask(new Subtask("Subtask 1", "Subtask description", TaskStatus.NEW, epicId));
        Subtask updated = new Subtask(manager.getSubtask(subtaskId));
        updated.setName("Subtask 1 (updated)");
        updated.setDescription("Subtask description (updated)");
        updated.setStatus(TaskStatus.DONE);
        manager.updateSubtask(updated);
        TaskManager reloaded = new FileBackedTaskManager(file);
        Subtask loaded = reloaded.getSubtask(subtaskId);
        assertNotNull(loaded, "updated subtask must exist after reload");
        assertEquals("Subtask 1 (updated)", loaded.getName(), "name should persist after update");
        assertEquals("Subtask description (updated)", loaded.getDescription(), "description should persist after update");
        assertEquals(TaskStatus.DONE, loaded.getStatus(), "status should persist after update");
        assertEquals(epicId, loaded.getEpicId(), "epicId should persist after update");
    }

    @Test
    void testDeleteSubtaskFileBacked() {
        int epicId = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        Integer subId = manager.addNewSubtask(new Subtask("Subtask 1", "Subtask description", TaskStatus.NEW, epicId));
        manager.deleteSubtask(subId);
        TaskManager reloaded = new FileBackedTaskManager(file);
        assertNull(reloaded.getSubtask(subId), "subtask should be absent after deletion and reload");
        Epic reloadedEpic = reloaded.getEpic(epicId);
        assertNotNull(reloadedEpic, "epic should still exist after subtask deletion");
        assertFalse(reloadedEpic.getSubtaskIds().contains(subId), "epic must not keep deleted subtask id");
    }

    @Test
    void testGeneratorIdOrderAfterReload() {
        int task = manager.addNewTask(new Task(1, "Task 1", "Description 1", TaskStatus.NEW));
        TaskManager reloaded = new FileBackedTaskManager(file);
        int newTaskId = reloaded.addNewTask(new Task("Task 2", "Description 2", TaskStatus.DONE));
        assertEquals(2, newTaskId, "id generator should be consistent after reload");
    }

    @Test
    void testSortingByIdBeforeSaving() throws IOException {
        int epicId1 = manager.addNewEpic(new Epic("Epic 1", "Epic description"));
        Integer subtaskId1 = manager.addNewSubtask(
                new Subtask("Subtask 1", "Subtask description", TaskStatus.NEW, epicId1));
        int taskId1 = manager.addNewTask(new Task("Task 1", "Task description", TaskStatus.NEW));
        int taskId2 = manager.addNewTask(new Task("Task 2", "Task description", TaskStatus.NEW));
        int epicId2 = manager.addNewEpic(new Epic("Epic 2", "Epic description"));
        Integer subtaskId2 = manager.addNewSubtask(
                new Subtask("Subtask 2", "Subtask description", TaskStatus.NEW, epicId2));
        List<Integer> expectedIds = List.of(epicId1, subtaskId1, taskId1, taskId2, epicId2, subtaskId2);

        TaskManager reloaded = new FileBackedTaskManager(file);
        List<Integer> idsAfterReload = new ArrayList<>();
        for (Task task : reloaded.getTasks()) {
            idsAfterReload.add(task.getId());
        }
        for (Epic epic : reloaded.getEpics()) {
            idsAfterReload.add(epic.getId());
        }
        for (Subtask subtask : reloaded.getSubtasks()) {
            idsAfterReload.add(subtask.getId());
        }

        assertEquals(new HashSet<>(expectedIds), new HashSet<>(idsAfterReload),
                "After reload manager should contain exactly all saved ids, " +
                        "regardless of paste order or Task types"
        );
    }
}