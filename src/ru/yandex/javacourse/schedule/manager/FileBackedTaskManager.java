package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.exceptions.ManagerSaveException;
import ru.yandex.javacourse.schedule.tasks.*;

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
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_NULL_SYMBOL = "";
    private static final String CSV_COLUMNS = buildHeader();
    private final Path path;

    private enum Column {
        ID("id"),
        TYPE("type"),
        NAME("name"),
        STATUS("status"),
        DESCRIPTION("description"),
        EPIC("epic");

        final String key;

        Column(String key) {
            this.key = key;
        }
    }

    private static String buildHeader() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Column c : Column.values()) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(CSV_DELIMITER);
            }
            stringBuilder.append(c.key);
        }
        return stringBuilder.toString();
    }

    public FileBackedTaskManager(String filePath) {
        this.path = Paths.get(filePath);
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
        int result = super.addNewSubtask(subtask);
        save();
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
        lines.add(CSV_COLUMNS);
        for (Task task : getTasks()) {
            lines.add(taskTostring(task));
        }
        for (Epic epic : getEpics()) {
            lines.add(taskTostring(epic));
        }
        for (Subtask subtask : getSubtasks()) {
            lines.add(taskTostring(subtask));
        }
        writeAllLines(lines);
    }

    private void loadFromFile() {
        List<String> lines = readAllLines();
        if (lines.isEmpty()) {
            return;
        }
        int headerRawIndex = 0;
        if (lines.getFirst().equals(CSV_COLUMNS)) {
            headerRawIndex = 1;
        }
        for (int i = headerRawIndex; i < lines.size(); i++) {
            Task task = stringToTask(lines.get(i));
            if (task instanceof Subtask) {
                super.addNewSubtask((Subtask) task);
            } else if (task instanceof Epic) {
                super.addNewEpic((Epic) task);
            } else {
                super.addNewTask(task);
            }
        }
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
            throw new ManagerSaveException("Error due writing to file: " +
                    this.path.getFileName(), e);
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
            throw new ManagerSaveException("Error due reading from file: " +
                    this.path.getFileName(), e);
        }
    }

    private static String taskTostring(Task task) {
        TaskType type;
        String epic = CSV_NULL_SYMBOL;
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

    private static Task stringToTask(String line) {
        String[] data = line.split(CSV_DELIMITER, -1);
        int id = Integer.parseInt(data[Column.ID.ordinal()]);
        TaskType type = TaskType.valueOf(data[Column.TYPE.ordinal()]);
        String name = data[Column.NAME.ordinal()];
        String statusStr = data[Column.STATUS.ordinal()];
        String description = data[Column.DESCRIPTION.ordinal()];
        TaskStatus status = TaskStatus.valueOf(statusStr);
        switch (type) {
            case TASK:
                return new Task(id, name, description, status);
            case EPIC:
                return new Epic(id, name, description);
            case SUBTASK:
                int epicId = Integer.parseInt(data[Column.EPIC.ordinal()]);
                return new Subtask(id, name, description, status, epicId);
            default:
                throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }
}

