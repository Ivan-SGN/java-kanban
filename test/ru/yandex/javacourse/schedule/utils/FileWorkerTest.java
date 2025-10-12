package ru.yandex.javacourse.schedule.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileWorkerTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteAndReadEmptyFile() throws IOException {
        Path file = tempDir.resolve("empty.csv");
        FileWorker.writeAllLines(file, List.of());
        List<String> lines = FileWorker.readAllLines(file);
        assertEquals(0, lines.size(), "File should be empty after writing an empty list");
    }

    @Test
    void testWriteAndReadLines() throws IOException {
        Path file = tempDir.resolve("data.csv");
        List<String> expected = List.of("first line", "second line", "third line");
        FileWorker.writeAllLines(file, expected);
        List<String> actual = FileWorker.readAllLines(file);
        assertEquals(expected, actual, "File content should match written lines");
    }

    @Test
    void testOverwritesOldContent() throws IOException {
        Path file = tempDir.resolve("overwrite.csv");
        FileWorker.writeAllLines(file, List.of("old data"));
        FileWorker.writeAllLines(file, List.of("new data"));
        List<String> actual = Files.readAllLines(file);
        assertEquals(List.of("new data"), actual, "Old content should be overwritten");
    }
}