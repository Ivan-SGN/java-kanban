package ru.yandex.javacourse.schedule.tasks;

import java.util.Objects;

public class Task {
    protected int id;
    protected String name;
    protected TaskStatus status;
    protected String description;
    private boolean managed = false;

    public Task(int id, String name, String description, TaskStatus status) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(String name, String description, TaskStatus status) {
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public Task(Task other) {
        this.id = other.id;
        this.name = other.name;
        this.description = other.description;
        this.status = other.status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        ensureMutable();
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        ensureMutable();
        this.name = name;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        ensureMutable();
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        ensureMutable();
        this.description = description;
    }

    public boolean isManaged() {
        return managed;
    }

    public void markAsManaged() {
        this.managed = true;
    }

    private void ensureMutable() {
        if (managed) {
            throw new IllegalStateException("Task is managed; fields are immutable outside manager");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id == task.id;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
