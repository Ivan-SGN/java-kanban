package ru.yandex.javacourse.schedule.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.*;

class CsvSerializerTest {
    @Test
    void testHeaderOrderBuilding() {
        String expectedHeader = "id,type,name,status,description,epic";
        String actualHeader = CsvSerializer.buildHeader();
        Assertions.assertEquals(expectedHeader, actualHeader,
                "Header must match declared column order: " + expectedHeader);
    }

    @Test
    void testTaskToCsv() {
        Task task = new Task(1, "Task 1", "Desc 1", TaskStatus.NEW);
        Epic epic = new Epic(2, "Epic 2", "Desc 2");
        Subtask subtask = new Subtask(3, "Sub 3", "Desc 3", TaskStatus.DONE, 2);

        String taskCsv = CsvSerializer.taskToString(task);
        String epicCsv = CsvSerializer.taskToString(epic);
        String subCsv = CsvSerializer.taskToString(subtask);

        Assertions.assertEquals("1," + TaskType.TASK.name() + ",Task 1,NEW,Desc 1,",
                taskCsv, "Task serialization is incorrect");
        Assertions.assertEquals("2," + TaskType.EPIC.name() + ",Epic 2,NEW,Desc 2,",
                epicCsv, "Epic serialization is incorrect");
        Assertions.assertEquals("3," + TaskType.SUBTASK.name() + ",Sub 3,DONE,Desc 3,2",
                subCsv, "Subtask serialization is incorrect");
    }

    @Test
    void testCsvToTask() {
        Task task = CsvSerializer.stringToTask("1," + TaskType.TASK.name() + ",Task 1,IN_PROGRESS,Desc 1,");
        Task epic = CsvSerializer.stringToTask("2," + TaskType.EPIC.name() + ",Epic 2,NEW,Desc 2,");
        Task subtask = CsvSerializer.stringToTask("3," + TaskType.SUBTASK.name() + ",Sub 3,DONE,Desc 3,2");

        Assertions.assertEquals(1, task.getId(), "Task id must be parsed");
        Assertions.assertEquals("Task 1", task.getName(), "Task name must be parsed");
        Assertions.assertEquals("Desc 1", task.getDescription(), "Task description must be parsed");
        Assertions.assertEquals(TaskStatus.IN_PROGRESS, task.getStatus(), "Task status must be parsed");
        Assertions.assertEquals(Task.class, task.getClass(), "Task type must be Task");

        Assertions.assertEquals(2, epic.getId(), "Epic id must be parsed");
        Assertions.assertEquals("Epic 2", epic.getName(), "Epic name must be parsed");
        Assertions.assertEquals("Desc 2", epic.getDescription(), "Epic description must be parsed");
        Assertions.assertEquals(TaskStatus.NEW, epic.getStatus(), "Epic status must be parsed");
        Assertions.assertEquals(Epic.class, epic.getClass(), "Epic type must be Epic");

        Assertions.assertEquals(3, subtask.getId(), "Subtask id must be parsed");
        Assertions.assertEquals("Sub 3", subtask.getName(), "Subtask name must be parsed");
        Assertions.assertEquals("Desc 3", subtask.getDescription(), "Subtask description must be parsed");
        Assertions.assertEquals(TaskStatus.DONE, subtask.getStatus(), "Subtask status must be parsed");
        Assertions.assertEquals(Subtask.class, subtask.getClass(), "Subtask type must be Subtask");
        Assertions.assertEquals(2, ((Subtask) subtask).getEpicId(), "Subtask epicId must be parsed");
    }
}