package ru.yandex.javacourse.schedule.manager;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Default managers.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static TaskManager getDefaultFileBacked() {
        return new FileBackedTaskManager(Paths.get(System.getProperty("user.dir"), "data.csv"));
    }

    public static TaskManager getFileBacked(Path filePath) {
        return new FileBackedTaskManager(filePath);
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
