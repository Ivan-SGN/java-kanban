package ru.yandex.javacourse.schedule.utils;

import ru.yandex.javacourse.schedule.tasks.*;

import java.util.HashMap;
import java.util.Map;

public class CsvSerializer {
    private static final String CSV_DELIMITER = ",";
    private static final String CSV_NULL_SYMBOL = "";
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

    public static String buildHeader() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Column column : Column.values()) {
            if (!stringBuilder.isEmpty()) {
                stringBuilder.append(CSV_DELIMITER);
            }
            stringBuilder.append(column.key);
        }
        return stringBuilder.toString();
    }

    public static String taskToString(Task task) {
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

    public static Task stringToTask(String line) {
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
