package ru.yandex.javacourse.schedule.manager;

/**
 * Default managers.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class Managers {
    public static TaskManager getDefault() {
        return new FileBackedTaskManager("/Users/ivanmakarov/dev/java-kanban/storage.csv");
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
