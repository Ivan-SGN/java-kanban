package ru.yandex.javacourse.schedule.manager;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Managers {
    public static InMemoryTaskManager getDefaultInMemory() {
        return new InMemoryTaskManager();
    }

    public static FileBackedTaskManager getDefaultFileBacked() {
        return new FileBackedTaskManager(Paths.get(System.getProperty("user.dir"), "dataNewFormat.csv"));
    }

    public static FileBackedTaskManager getFileBacked(Path filePath) {
        return new FileBackedTaskManager(filePath);
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}
