package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.tasks.Task;

import java.util.List;

public interface HistoryManager {
    List<Task> getHistory();

    void addTask(Task task);

    void remove(int id);
}
