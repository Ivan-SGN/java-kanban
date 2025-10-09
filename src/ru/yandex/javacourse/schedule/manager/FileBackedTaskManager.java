package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String CSV_COLUMNS = "id,type,name,status,description,epic";
    private final Path path;

    public FileBackedTaskManager(String filePath) {
        this.path = Paths.get(filePath);
        try {
            if (Files.notExists(path)) {
                Files.createFile(this.path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int addNewTask(Task task) {
        return super.addNewTask(task);
    }

    @Override
    public int addNewEpic(Epic epic) {
        return super.addNewEpic(epic);
    }

    @Override
    public Integer addNewSubtask(Subtask subtask) {
        return super.addNewSubtask(subtask);
    }

    @Override
    public void updateTask(Task task) {
        super.updateTask(task);
    }

    @Override
    public void updateEpic(Epic epic) {
        super.updateEpic(epic);
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        super.updateSubtask(subtask);
    }

    @Override
    public void deleteTask(int id) {
        super.deleteTask(id);
    }

    @Override
    public void deleteEpic(int id) {
        super.deleteEpic(id);
    }

    @Override
    public void deleteSubtask(int id) {
        super.deleteSubtask(id);
    }

    @Override
    public void deleteTasks() {
        super.deleteTasks();
    }

    @Override
    public void deleteSubtasks() {
        super.deleteSubtasks();
    }

    @Override
    public void deleteEpics() {
        super.deleteEpics();
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        lines.add(CSV_COLUMNS);
        for (Task task : getTasks()) {
            lines.add(taskToCsv(task));
        }
        for (Epic epic : getEpics()) {
            lines.add(taskToCsv(epic));
        }
        for (Subtask subtask : getSubtasks()) {
            lines.add(taskToCsv(subtask));
        }
        writeAllLines(lines);
    }

    private void writeAllLines(List<String> lines) {
        try (BufferedWriter fileWriter = Files.newBufferedWriter(
                path,
                CHARSET,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            for (String line : lines) {
                fileWriter.write(line);
                fileWriter.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> readAllLines() {
        List<String> result = new ArrayList<>();
        String line;
        try (BufferedReader fileReader = Files.newBufferedReader(path, CHARSET)) {
            while ((line = fileReader.readLine()) != null) {
                result.add(line);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String taskToCsv(Task task) {
        TaskType type;
        String epic = "";
        if (task instanceof Subtask) {
            type = TaskType.SUBTASK;
            epic = String.valueOf(((Subtask) task).getEpicId());
        } else if (task instanceof Epic) {
            type = TaskType.EPIC;
        } else {
            type = TaskType.TASK;
        }
        return String.join(",",
                String.valueOf(task.getId()),
                type.toString(),
                task.getName(),
                task.getStatus().name(),
                task.getDescription(),
                epic
        );
    }
}

