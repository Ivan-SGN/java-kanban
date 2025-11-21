package ru.yandex.javacourse.schedule.api;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import ru.yandex.javacourse.schedule.api.json.GsonConfig;
import ru.yandex.javacourse.schedule.manager.Managers;
import ru.yandex.javacourse.schedule.manager.TaskManager;

import java.io.IOException;
import java.net.http.HttpClient;

public class HttpTaskServerTest {
    private static final int PORT = 8090;
    protected static final String BASE_URL = "http://localhost:" + PORT;
    protected TaskManager taskManager;
    protected HttpTaskServer httpTaskServer;
    protected HttpClient httpClient;
    protected Gson gson;

    @BeforeEach
    void setUp() throws IOException {
        taskManager = Managers.getDefaultInMemory();
        httpTaskServer = new HttpTaskServer(taskManager, PORT);
        httpTaskServer.start();
        httpClient = HttpClient.newHttpClient();
        gson = GsonConfig.createGson();
    }

    @AfterEach
    void tearDown() {
        if (httpTaskServer != null) {
            httpTaskServer.stop();
        }
    }
}