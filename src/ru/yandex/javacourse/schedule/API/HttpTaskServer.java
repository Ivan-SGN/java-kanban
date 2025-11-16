package ru.yandex.javacourse.schedule.API;

import com.sun.net.httpserver.HttpServer;
import ru.yandex.javacourse.schedule.manager.Managers;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    private static final int PORT = 8080;

    private final HttpServer httpServer;
    private final TaskManager taskManager;

    public HttpTaskServer(TaskManager taskManager) throws IOException {
        this.taskManager = taskManager;
        this.httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.createContext("/tasks", new TaskHandler());
    }

    public void start (){
        httpServer.start();
        System.out.println("Server starts "
                + "IP : " + httpServer.getAddress().getAddress()
                + "Port :" + httpServer.getAddress().getPort());
    }

    public static void main(String[] args) {
        try {
            TaskManager taskManager = Managers.getDefaultFileBacked();
            HttpTaskServer httpTaskServer = new HttpTaskServer(taskManager);
            httpTaskServer.start();
        } catch (IOException e) {
            System.out.println("Failed to start HTTP server: " + e.getMessage());

        }
    }
}
