package ru.yandex.javacourse.schedule.manager;

import java.util.*;

import ru.yandex.javacourse.schedule.tasks.Task;

/**
 * In memory history manager.
 *
 * @author Vladimir Ivanov (ivanov.vladimir.l@gmail.com)
 */
public class InMemoryHistoryManager implements HistoryManager {
    private Node<Task> head;
    private Node<Task> tail;
    private final Map<Integer, Node<Task>> history = new HashMap<>();

    private static final class Node <T>{
        private final T data;
        private Node<T> next;
        private Node<T> prev;

        public Node(T data){
            this.data = data;
        }
    }

    private void linkLast(Task task) {
        Node<Task> newNode = new Node<>(task);
        if (head == null){
            head = newNode;
        }
        else {
            tail.next = newNode;
            newNode.prev = tail;
        }
        tail = newNode;
    }

    private ArrayList<Task> getTasks(){
        ArrayList<Task> result = new ArrayList<>(history.size());
        Node<Task> current = head;
        while (current != null){
            result.add(current.data);
            current = current.next;
        }
        return result;
    }

    private void removeNode(Node<Task> node){
        if (node == null){
            return;
        }
        Node<Task> prev = node.prev;
        Node<Task> next = node.next;
        if (prev != null) {
            prev.next = next;
        } else {
            head = next;
        }
        if (next != null) {
            next.prev = prev;
        } else {
            tail = prev;
        }
        node.prev = null;
        node.next = null;
    }

    @Override
	public List<Task> getHistory() {
		return getTasks();
	}

	@Override
	public void addTask(Task task) {
        if (task == null){
            return;
        }
        Node<Task> duplicate = history.remove(task.getId());
        if (duplicate != null) {
            removeNode(duplicate);
        }
        linkLast(task);
        history.put(task.getId(), tail);
    }

    @Override
    public void remove(int id) {
        Node<Task> node = history.remove(id);
        if(node != null){
            removeNode(node);
        }
    }
}
