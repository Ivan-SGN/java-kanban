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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileBackedTaskManager extends InMemoryTaskManager {
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_NULL_SYMBOL = "";
    private final Path path;
    private static final Map<Column, Integer> CSV_COLUMNS = new HashMap<>();

    static {
        CSV_COLUMNS.put(Column.ID, 0);
        CSV_COLUMNS.put(Column.TYPE, 1);
        CSV_COLUMNS.put(Column.NAME, 2);
        CSV_COLUMNS.put(Column.STATUS, 3);
        CSV_COLUMNS.put(Column.DESCRIPTION, 4);
        CSV_COLUMNS.put(Column.EPIC, 5);
    }

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
        for (Column column : Column.values()) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(CSV_DELIMITER);
            }
            stringBuilder.append(column.key);
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
        lines.add(buildHeader());
        for (Task task : getTasks()) {
            lines.add(taskToString(task));
        }
        for (Epic epic : getEpics()) {
            lines.add(taskToString(epic));
        }
        for (Subtask subtask : getSubtasks()) {
            lines.add(taskToString(subtask));
        }
        writeAllLines(lines);
    }

    private void loadFromFile() {
        List<String> lines = readAllLines();
        if (lines.isEmpty()) {
            return;
        }
        int start = lines.getFirst().equals(buildHeader()) ? 1 : 0;
        List<Subtask> tempSubtasks = new ArrayList<>();
        for (int i = start; i < lines.size(); i++) {
            Task task = stringToTask(lines.get(i));
            if (task instanceof Subtask) {
                tempSubtasks.add((Subtask) task);
            } else if (task instanceof Epic) {
                super.addNewEpic((Epic) task);
            } else {
                super.addNewTask(task);
            }
        }
        for (Subtask subtask : tempSubtasks) {
            super.addNewSubtask(subtask);
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

    private static String taskToString(Task task) {
        String[] row = new String[CSV_COLUMNS.size()];
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
        row[CSV_COLUMNS.get(Column.ID)] = String.valueOf(task.getId());
        row[CSV_COLUMNS.get(Column.TYPE)] = type.name();
        row[CSV_COLUMNS.get(Column.NAME)] = task.getName();
        row[CSV_COLUMNS.get(Column.STATUS)] = task.getStatus().name();
        row[CSV_COLUMNS.get(Column.DESCRIPTION)] = task.getDescription();
        row[CSV_COLUMNS.get(Column.EPIC)] = epic;
        return String.join(CSV_DELIMITER, row);
    }

    private static Task stringToTask(String line) {
        String[] data = line.split(CSV_DELIMITER, -1);
        int id = Integer.parseInt(data[CSV_COLUMNS.get(Column.ID)]);
        TaskType type = TaskType.valueOf(data[CSV_COLUMNS.get(Column.TYPE)]);
        String name = data[CSV_COLUMNS.get(Column.NAME)];
        String statusStr = data[CSV_COLUMNS.get(Column.STATUS)];
        String description = data[CSV_COLUMNS.get(Column.DESCRIPTION)];
        TaskStatus status = TaskStatus.valueOf(statusStr);
        switch (type) {
            case TASK:
                return new Task(id, name, description, status);
            case EPIC:
                return new Epic(id, name, description);
            case SUBTASK:
                int epicId = Integer.parseInt(data[CSV_COLUMNS.get(Column.EPIC)]);
                return new Subtask(id, name, description, status, epicId);
            default:
                throw new IllegalArgumentException("Unknown task type: " + type);
        }
    }
}

