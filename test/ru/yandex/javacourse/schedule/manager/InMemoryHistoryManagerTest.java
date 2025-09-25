package ru.yandex.javacourse.schedule.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class InMemoryHistoryManagerTest {

    HistoryManager historyManager;

    @BeforeEach
    public void initHistoryManager(){
        historyManager = Managers.getDefaultHistory();
    }

    private Task newTask (int id){
        return new Task(id, "Task " + id, "Testing task% " + id, TaskStatus.NEW);
    }

    @Test
    public void testAddNullTask() {
        Task task = null;
        historyManager.addTask(task);
        assertTrue(historyManager.getHistory().isEmpty(), "nullable Task " +
                "should not change history");
    }

    @Test
    public void testTaskOrder(){
        List<Task> expectedTaskList = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            expectedTaskList.addLast(newTask(i));
        }
        for (int i = 0; i < 2; i++) {
            historyManager.addTask(expectedTaskList.get(i));
        }
        assertEquals(expectedTaskList, historyManager.getHistory(), "Task order should be as linked list");
    }

    @Test
    public void testDuplicateTask(){
        List<Task> expectedTaskList = new LinkedList<>();
        for (int i = 0; i < 2; i++) {
            expectedTaskList.addLast(newTask(i));
        }
        for (int i = 0; i < 2; i++) {
            historyManager.addTask(expectedTaskList.get(i));
            historyManager.addTask(expectedTaskList.get(i));
        }
        assertEquals(expectedTaskList, historyManager.getHistory(), "Task order should be as linked list");
    }

    @Test
    public void testRemoveTask(){
        List<Task> expectedTaskList = new LinkedList<>();
        final int taskCount = 10;
        for (int i = 0; i < taskCount + 1; i++) {
            expectedTaskList.addLast(newTask(i));
        }
        for (int i = 0; i < taskCount + 1; i++) {
            historyManager.addTask(expectedTaskList.get(i));
        }
        expectedTaskList.removeFirst();
        historyManager.remove(0);
        assertEquals(expectedTaskList, historyManager.getHistory(), "Firs element in history " +
                "should be removed from head");
        expectedTaskList.removeLast();
        historyManager.remove(taskCount);
        assertEquals(expectedTaskList, historyManager.getHistory(), "Last element in history " +
                "should be removed from tail");
        Task removeTask = expectedTaskList.get(taskCount/2);
        expectedTaskList.remove(removeTask);
        historyManager.remove(removeTask.getId());
        assertEquals(expectedTaskList, historyManager.getHistory(), "Middle element in history " +
                "should be removed from middle");
    }

    @Test
    public void testHistoricVersions(){
        Task task = new Task("Test 1", "Testiong task 1", TaskStatus.NEW);
        historyManager.addTask(task);
        assertEquals(1, historyManager.getHistory().size(), "historic task should be added");
        task.setStatus(TaskStatus.IN_PROGRESS);
        historyManager.addTask(task);
        assertEquals(1, historyManager.getHistory().size(), "Duplicate historic task " +
                "should be removed");
    }

    @Test
    public void testHistoricVersionsByPointer(){
        Task task = new Task("Test 1", "Testiong task 1", TaskStatus.NEW);
        historyManager.addTask(task);
        assertEquals(task.getStatus(), historyManager.getHistory().get(0).getStatus(), "historic task " +
                "should be stored");
        task.setStatus(TaskStatus.IN_PROGRESS);
        historyManager.addTask(task);
        assertEquals(TaskStatus.IN_PROGRESS, historyManager.getHistory().get(0).getStatus(), "history should " +
                "store latest state if task added twice");
    }

}
