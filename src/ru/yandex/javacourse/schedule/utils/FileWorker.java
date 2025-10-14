package ru.yandex.javacourse.schedule.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileWorker {
    public static void writeAllLines(Path path, List<String> lines) throws IOException {
        try (BufferedWriter fileWriter = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (String line : lines) {
                fileWriter.write(line);
                fileWriter.newLine();
            }
        }
    }

    public static List<String> readAllLines(Path path) throws IOException {
        List<String> result = new ArrayList<>();
        String line;
        try (BufferedReader fileReader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            while ((line = fileReader.readLine()) != null) {
                result.add(line);
            }
            return result;
        }
    }
}
