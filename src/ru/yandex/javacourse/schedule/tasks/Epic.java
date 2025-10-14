package ru.yandex.javacourse.schedule.tasks;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

public class Epic extends Task {
    protected ArrayList<Integer> subtaskIds = new ArrayList<>();

    public Epic(int id, String name, String description) {
        super(id, name, description, NEW);
    }

    public Epic(String name, String description) {
        super(name, description, NEW);
    }

    @Override
    public TaskType getType() {
        return TaskType.EPIC;
    }

    public void addSubtaskId(int id) {
        if (this.id == id) {
            return;
        }
        if (!subtaskIds.contains(id)) {
            subtaskIds.add(id);
        }
    }

    public Epic(Epic other) {
        super(other);
        this.subtaskIds = new ArrayList<>(other.subtaskIds);
    }

    public List<Integer> getSubtaskIds() {
        return subtaskIds;
    }

    public void cleanSubtaskIds() {
        subtaskIds.clear();
    }

    public void removeSubtask(int id) {
        subtaskIds.remove(Integer.valueOf(id));
    }

    @Override
    public String toString() {
        return "Epic{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", description='" + description + '\'' +
                ", subtaskIds=" + subtaskIds +
                '}';
    }
}
