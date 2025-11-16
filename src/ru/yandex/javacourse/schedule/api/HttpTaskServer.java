package ru.yandex.javacourse.schedule.api;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import ru.yandex.javacourse.schedule.api.handlers.TaskHandler;
import ru.yandex.javacourse.schedule.api.json.GsonConfig;
import ru.yandex.javacourse.schedule.manager.Managers;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {
    private static final int PORT = 8080;

    private final TaskManager taskManager;
    private final Gson gson;
    private final HttpServer httpServer;

    public HttpTaskServer(TaskManager taskManager) throws IOException {
        this.taskManager = taskManager;
        this.gson = (GsonConfig.createGson());
        this.httpServer = HttpServer.create(new InetSocketAddress(PORT), 0);
        httpServer.createContext("/tasks", new TaskHandler(this.taskManager, gson));
    }

    public void start (){
        httpServer.start();
        System.out.println("Server starts "
                + "IP: " + httpServer.getAddress().getAddress() + " "
                + "Port :" + httpServer.getAddress().getPort());
    }

    public static void main(String[] args) {
        try {
            HttpTaskServer httpTaskServer = new HttpTaskServer(Managers.getDefaultFileBacked());
            httpTaskServer.start();
        } catch (IOException e) {
            System.out.println("Failed to start HTTP server: " + e.getMessage());
        }
    }
}
