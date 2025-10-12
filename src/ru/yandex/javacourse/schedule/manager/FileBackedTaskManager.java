package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.exceptions.ManagerSaveException;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.utils.CsvSerializer;
import ru.yandex.javacourse.schedule.utils.FileWorker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private final Path path;

    public FileBackedTaskManager(Path filePath) {
        this.path = filePath;
        try {
            if (Files.notExists(path)) {
                Files.createFile(this.path);
            } else {
                loadFromFile();
            }
        } catch (IOException e) {
            throw new ManagerSaveException("Error due initialization file: " +
                    this.path.getFileName(), e);
        }
    }

    @Override
    public int addNewTask(Task task) {
        int result = super.addNewTask(task);
        save();
        return result;
    }

    @Override
    public int addNewEpic(Epic epic) {
        int result = super.addNewEpic(epic);
        save();
        return result;
    }

    @Override
    public Integer addNewSubtask(Subtask subtask) {
        Integer result = super.addNewSubtask(subtask);
        if (result != null) {
            save();
        }
        return result;
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
        save();
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
        save();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
        save();
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
        save();
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
        save();
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
        save();
    }

    @Override
    public void deleteTasks() {
        super.deleteTasks();
        save();
    }

    @Override
    public void deleteSubtasks() {
        super.deleteSubtasks();
        save();
    }

    @Override
    public void deleteEpics() {
        super.deleteEpics();
        save();
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        lines.add(CsvSerializer.buildHeader());
        List<Task> allTasks = new ArrayList<>();
        allTasks.addAll(getTasks());
        allTasks.addAll(getEpics());
        allTasks.addAll(getSubtasks());
        allTasks.sort(Comparator.comparingInt(Task::getId));
        for (Task task : allTasks) {
            lines.add(CsvSerializer.taskToString(task));
        }
        try {
            FileWorker.writeAllLines(path, lines);
        } catch (IOException e) {
            throw new ManagerSaveException("Error due writing to file: " + path.getFileName(), e);
        }
    }

    private void loadFromFile() {
        List<String> lines;
        try {
            lines = FileWorker.readAllLines(path);
        } catch (IOException e) {
            throw new ManagerSaveException("Error due reading from file: " +
                    path.getFileName(), e);
        }
        if (lines.isEmpty()) {
            return;
        }
        String expectedHeader = CsvSerializer.buildHeader();
        String actualHeader = lines.getFirst();
        if (!actualHeader.equals(expectedHeader)) {
            throw new ManagerSaveException("CSV header mismatch: expected '" + expectedHeader +
                    "', found '" + actualHeader + "'");
        }
        int startLine = 1;
        for (int i = startLine; i < lines.size(); i++) {
            Task task = CsvSerializer.stringToTask(lines.get(i));
            if (task instanceof Subtask) {
                super.addNewSubtask((Subtask) task);
            } else if (task instanceof Epic) {
                super.addNewEpic((Epic) task);
            } else {
                super.addNewTask(task);
            }
        }
    }
}

