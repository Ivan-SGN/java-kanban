package ru.yandex.javacourse.schedule.manager;

import ru.yandex.javacourse.schedule.exceptions.NotFoundException;
import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.IN_PROGRESS;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

public class InMemoryTaskManager implements TaskManager {

    private final Map<Integer, Task> tasks = new HashMap<>();
    private final Map<Integer, Epic> epics = new HashMap<>();
    private final Map<Integer, Subtask> subtasks = new HashMap<>();
    private final Set<Task> prioritizedTasks = new TreeSet<>(
            Comparator
                    .comparing(Task::getStartTime)
                    .thenComparingInt(Task::getId));
    private int generatorId = 0;
    private final HistoryManager historyManager = Managers.getDefaultHistory();

    private int assignOrValidateId(int requestedId) {
        if (requestedId == 0) {
            return ++generatorId;
        }
        if (requestedId <= generatorId) {
            throw new IllegalArgumentException(
                    "Predefined id must be greater than current sequence (generatorId=" + generatorId + "): " + requestedId);
        }
        if (tasks.containsKey(requestedId) || epics.containsKey(requestedId) || subtasks.containsKey(requestedId)) {
            throw new IllegalArgumentException("Id already exists: " + requestedId);
        }
        generatorId = requestedId;
        return requestedId;
    }

    @Override
    public ArrayList<Task> getTasks() {
        return new ArrayList<>(this.tasks.values());
    }

    public ArrayList<Task> getPrioritizedTasks() {
        return new ArrayList<>(this.prioritizedTasks);
    }

    @Override
    public ArrayList<Subtask> getSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public ArrayList<Epic> getEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public ArrayList<Subtask> getEpicSubtasks(int epicId) {
        Epic epic = epics.get(epicId);
        ensureFoundOrThrow(epic);
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Task getTask(int id) {
        final Task task = tasks.get(id);
        ensureFoundOrThrow(task);
        historyManager.addTask(task);
        return task;
    }

    @Override
    public Subtask getSubtask(int id) {
        final Subtask subtask = subtasks.get(id);
        ensureFoundOrThrow(subtask);
        historyManager.addTask(subtask);
        return subtask;
    }

    @Override
    public Epic getEpic(int id) {
        final Epic epic = epics.get(id);
        ensureFoundOrThrow(epic);
        historyManager.addTask(epic);
        return epic;
    }

    @Override
    public int addNewTask(Task task) {
        final int id = assignOrValidateId(task.getId());
        if (isTaskCrossOther(task)) {
            throw new IllegalArgumentException("Task time crosses existing task");
        }
        task.setId(id);
        tasks.put(id, task);
        addToPrioritizedTasks(task);
        task.markAsManaged();
        return id;
    }

    @Override
    public int addNewEpic(Epic epic) {
        final int id = assignOrValidateId(epic.getId());
        epic.setId(id);
        epics.put(id, epic);
        epic.markAsManaged();
        return id;
    }

    @Override
    public Integer addNewSubtask(Subtask subtask) {
        final int epicId = subtask.getEpicId();
        final Epic epic = epics.get(epicId);
        ensureFoundOrThrow(epic);
        final int id = assignOrValidateId(subtask.getId());
        if (isTaskCrossOther(subtask)) {
            throw new IllegalArgumentException("Task time crosses existing task");
        }
        subtask.setId(id);
        subtasks.put(id, subtask);
        addToPrioritizedTasks(subtask);
        subtask.markAsManaged();
        epic.addSubtaskId(id);
        updateEpic(epicId);
        return id;
    }

    @Override
    public void updateTask(Task task) {
        final int id = task.getId();
        final Task old = tasks.get(id);
        ensureFoundOrThrow(old);
        if (isTaskCrossOther(task)) {
            throw new IllegalArgumentException("Task time crosses existing task");
        }
        prioritizedTasks.remove(old);
        tasks.put(id, task);
        addToPrioritizedTasks(task);
        task.markAsManaged();
    }

    @Override
    public void updateEpic(Epic epic) {
        final int id = epic.getId();
        if (!epics.containsKey(id)) {
            throw new NotFoundException("epic not found");
        }
        epics.put(id, epic);
        epic.markAsManaged();
    }

    @Override
    public void updateSubtask(Subtask subtask) {
        final int id = subtask.getId();
        final Subtask saved = subtasks.get(id);
        ensureFoundOrThrow(saved);
        final int oldEpicId = saved.getEpicId();
        final int newEpicId = subtask.getEpicId();
        final Epic newEpic = epics.get(newEpicId);
        ensureFoundOrThrow(newEpic);
        if (isTaskCrossOther(subtask)) {
            throw new IllegalArgumentException("Task time crosses existing task");
        }
        prioritizedTasks.remove(saved);
        subtasks.put(id, subtask);
        addToPrioritizedTasks(subtask);
        subtask.markAsManaged();
        if (oldEpicId != newEpicId) {
            final Epic oldEpic = epics.get(oldEpicId);
            if (oldEpic != null) {
                oldEpic.removeSubtask(id);
                updateEpic(oldEpicId);
            }
            newEpic.addSubtaskId(id);
        }
        updateEpic(newEpicId);
    }

    @Override
    public void deleteTask(int id) {
        final Task task = tasks.remove(id);
        ensureFoundOrThrow(task);
        prioritizedTasks.remove(tasks.remove(id));
        historyManager.remove(id);
    }

    @Override
    public void deleteEpic(int id) {
        final Epic epic = epics.remove(id);
        ensureFoundOrThrow(epic);
        historyManager.remove(id);
        epic.getSubtaskIds().forEach(subtaskId -> {
            Subtask removed = subtasks.remove(subtaskId);
            prioritizedTasks.remove(removed);
            historyManager.remove(subtaskId);
        });
    }

    @Override
    public void deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        ensureFoundOrThrow(subtask);
        prioritizedTasks.remove(subtask);
        historyManager.remove(id);
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.removeSubtask(id);
            updateEpic(epic.getId());
        }
    }

    @Override
    public void deleteTasks() {
        tasks.values().forEach(task -> {
            prioritizedTasks.remove(task);
            historyManager.remove(task.getId());
        });
        tasks.clear();
    }

    @Override
    public void deleteSubtasks() {
        epics.values().forEach(epic -> {
            epic.cleanSubtaskIds();
            updateEpic(epic.getId());
        });
        subtasks.values().forEach(subtask -> {
            historyManager.remove(subtask.getId());
            prioritizedTasks.remove(subtask);
        });
        subtasks.clear();
    }

    @Override
    public void deleteEpics() {
        epics.values().forEach(epic -> historyManager.remove(epic.getId()));
        epics.clear();
        subtasks.values().forEach(subtask -> {
            historyManager.remove(subtask.getId());
            prioritizedTasks.remove(subtask);
        });
        subtasks.clear();
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    private void updateEpic(int epicId) {
        final Epic oldEpic = epics.get(epicId);
        if (oldEpic == null) {
            return;
        }
        final Epic newEpic = new Epic(oldEpic.getId(), oldEpic.getName(), oldEpic.getDescription());
        oldEpic.getSubtaskIds().forEach(newEpic::addSubtaskId);
        newEpic.setStatus(computeEpicStatus(oldEpic.getSubtaskIds()));
        newEpic.setDuration(computeEpicDuration(oldEpic));
        newEpic.setStartTime(computeEpicStartTime(oldEpic));
        newEpic.setEndTime(computeEpicEndTime(oldEpic));
        epics.put(epicId, newEpic);
        newEpic.markAsManaged();
    }

    private TaskStatus computeEpicStatus(List<Integer> subtaskIds) {
        if (subtaskIds == null || subtaskIds.isEmpty()) {
            return NEW;
        }
        TaskStatus status = null;
        for (int sid : subtaskIds) {
            final Subtask s = subtasks.get(sid);
            if (s == null) {
                continue;
            }
            if (status == null) {
                status = s.getStatus();
                continue;
            }
            if (status != s.getStatus() || status == IN_PROGRESS) {
                return IN_PROGRESS;
            }
        }
        return status == null ? NEW : status;
    }

    private LocalDateTime computeEpicStartTime(Epic epic) {
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .map(Subtask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private Duration computeEpicDuration(Epic epic) {
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .map(Subtask::getDuration)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private LocalDateTime computeEpicEndTime(Epic epic) {
        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .map(Subtask::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    private void addToPrioritizedTasks(Task task) {
        if (task.getStartTime() != null) {
            prioritizedTasks.add(task);
        }
    }

    private boolean isTaskCrossOther(Task task) {
        return prioritizedTasks.stream().anyMatch(current -> areTasksCrossing(current, task));
    }

    private boolean areTasksCrossing(Task first, Task second) {
        if (first.getStartTime() == null || first.getDuration().isZero()
                || second.getStartTime() == null || second.getDuration().isZero()) {
            return false;
        }
        LocalDateTime firstStart = first.getStartTime();
        LocalDateTime firstEnd = first.getEndTime();
        LocalDateTime secondStart = second.getStartTime();
        LocalDateTime secondEnd = second.getEndTime();
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private void ensureFoundOrThrow(Task task) {
        if (task == null) {
            throw new NotFoundException("epic not found");
        }
    }
}
