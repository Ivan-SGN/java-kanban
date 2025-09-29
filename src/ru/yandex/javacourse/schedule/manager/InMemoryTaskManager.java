package ru.yandex.javacourse.schedule.manager;

import static ru.yandex.javacourse.schedule.tasks.TaskStatus.IN_PROGRESS;
import static ru.yandex.javacourse.schedule.tasks.TaskStatus.NEW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.javacourse.schedule.tasks.Epic;
import ru.yandex.javacourse.schedule.tasks.Subtask;
import ru.yandex.javacourse.schedule.tasks.Task;
import ru.yandex.javacourse.schedule.tasks.TaskStatus;

public class InMemoryTaskManager implements TaskManager {

	private final Map<Integer, Task> tasks = new HashMap<>();
	private final Map<Integer, Epic> epics = new HashMap<>();
	private final Map<Integer, Subtask> subtasks = new HashMap<>();
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
		ArrayList<Subtask> tasks = new ArrayList<>();
		Epic epic = epics.get(epicId);
		if (epic == null) {
			return null;
		}
		for (int id : epic.getSubtaskIds()) {
			tasks.add(subtasks.get(id));
		}
		return tasks;
	}

	@Override
	public Task getTask(int id) {
		final Task task = tasks.get(id);
		historyManager.addTask(task);
		return task;
	}

	@Override
	public Subtask getSubtask(int id) {
		final Subtask subtask = subtasks.get(id);
		historyManager.addTask(subtask);
		return subtask;
	}

	@Override
	public Epic getEpic(int id) {
		final Epic epic = epics.get(id);
		historyManager.addTask(epic);
		return epic;
	}

	@Override
	public int addNewTask(Task task) {
        final int id = assignOrValidateId(task.getId());
        task.setId(id);
        tasks.put(id, task);
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
        if (epic == null) {
            return null;
        }
        final int id = assignOrValidateId(subtask.getId());
        subtask.setId(id);
        subtasks.put(id, subtask);
        subtask.markAsManaged();
        epic.addSubtaskId(id);
        updateEpicStatus(epicId);
        return id;
    }

	@Override
	public void updateTask(Task task) {
        final int id = task.getId();
        if (!tasks.containsKey(id)) {
            return;
        }
        tasks.put(id, task);
        task.markAsManaged();
	}

	@Override
	public void updateEpic(Epic epic) {
        final int id = epic.getId();
        if (!epics.containsKey(id)) {
            return;
        }
        epics.put(id, epic);
        epic.markAsManaged();
	}

	@Override
	public void updateSubtask(Subtask subtask) {
        final int id = subtask.getId();
        final Subtask saved = subtasks.get(id);
        if (saved == null) {
            return;
        }
        final int oldEpicId = saved.getEpicId();
        final int newEpicId = subtask.getEpicId();
        final Epic newEpic = epics.get(newEpicId);
        if (newEpic == null) {
            return;
        }
        subtasks.put(id, subtask);
        subtask.markAsManaged();
        if (oldEpicId != newEpicId) {
            final Epic oldEpic = epics.get(oldEpicId);
            if (oldEpic != null) {
                oldEpic.removeSubtask(id);
                updateEpicStatus(oldEpicId);
            }
            newEpic.addSubtaskId(id);
        }
        updateEpicStatus(newEpicId);
	}

	@Override
	public void deleteTask(int id) {
		tasks.remove(id);
        historyManager.remove(id);
	}

	@Override
	public void deleteEpic(int id) {
		final Epic epic = epics.remove(id);
        if (epic == null){
            return;
        }
        historyManager.remove(id);
        for (Integer subtaskId : epic.getSubtaskIds()) {
			subtasks.remove(subtaskId);
            historyManager.remove(subtaskId);
        }
	}

	@Override
	public void deleteSubtask(int id) {
		Subtask subtask = subtasks.remove(id);
		if (subtask == null) {
			return;
		}
        historyManager.remove(id);
        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null){
		    epic.removeSubtask(id);
		    updateEpicStatus(epic.getId());
        }
	}

	@Override
	public void deleteTasks() {
		for (Task task : tasks.values()){
            historyManager.remove(task.getId());
        }
        tasks.clear();
	}

	@Override
	public void deleteSubtasks() {
		for (Epic epic : epics.values()) {
			epic.cleanSubtaskIds();
			updateEpicStatus(epic.getId());
		}
        for (Subtask subtask : subtasks.values()){
            historyManager.remove(subtask.getId());
        }
		subtasks.clear();
	}

	@Override
	public void deleteEpics() {
        for (Epic epic : epics.values()){
            historyManager.remove(epic.getId());
        }
        epics.clear();
        for (Subtask subtask : subtasks.values()){
            historyManager.remove(subtask.getId());
        }
		subtasks.clear();
	}

	@Override
	public List<Task> getHistory() {
		return historyManager.getHistory();
	}

    private void updateEpicStatus(int epicId) {
        final Epic oldEpic = epics.get(epicId);
        if (oldEpic == null) {
            return;
        }
        final TaskStatus newStatus = computeEpicStatus(oldEpic.getSubtaskIds());
        if (oldEpic.getStatus() == newStatus) {
            return;
        }
        final Epic newEpic = new Epic(oldEpic.getId(), oldEpic.getName(), oldEpic.getDescription());
        for (int sid : oldEpic.getSubtaskIds()) {
            newEpic.addSubtaskId(sid);
        }
        newEpic.setStatus(newStatus);
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
}
