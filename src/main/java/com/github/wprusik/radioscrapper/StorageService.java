package com.github.wprusik.radioscrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wprusik.radioscrapper.model.RadioCategory;
import com.github.wprusik.radioscrapper.model.RadioStation;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class StorageService {

    private static final String CONFIG_FILENAME = "data.json";

    private final String baseDirectory;
    private final String playlistDirectory;

    StorageService(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.playlistDirectory = baseDirectory + File.separator + "m3u";
    }

    @SneakyThrows
    void save(List<RadioCategory> radioCategories) {
        createDirectoryIfNotExists(baseDirectory);
        File configFile = createFileIfNotExists(baseDirectory + File.separator + CONFIG_FILENAME);
        new ObjectMapper().writeValue(configFile, radioCategories);
    }

    List<RadioCategory> load() {
        File configFile = new File(baseDirectory + File.separator + CONFIG_FILENAME);
        if (configFile.exists()) {
            TypeReference<List<RadioCategory>> type = new TypeReference<>() {};
            try {
                List<RadioCategory> categories = new ObjectMapper().readValue(configFile, type);
                return categories != null ? new ArrayList<>(categories) : new ArrayList<>();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return new ArrayList<>();
    }

    RadioCategory storePlaylists(RadioCategory category) {
        createDirectoryIfNotExists(playlistDirectory);
        List<RadioStation> storedStations = category.stations().stream().map(this::storeRadioStation).toList();
        return category.toBuilder().stations(storedStations).build();
    }

    private RadioStation storeRadioStation(RadioStation station) {
        File file = new File(station.playlistFile());
        if (file.getAbsolutePath().startsWith(playlistDirectory)) {
            return station;
        }
        File targetFile = new File(playlistDirectory + File.separator + file.getName());
        if (!file.renameTo(targetFile)) {
            throw new IllegalStateException();
        }
        return station.toBuilder().playlistFile(targetFile.getAbsolutePath()).build();
    }

    private File createFileIfNotExists(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        try {
            return Files.createFile(Path.of(path)).toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createDirectoryIfNotExists(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                Files.createDirectories(Path.of(path));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean deleteBaseDirectory() {
        File dir = new File(baseDirectory);
        if (!dir.isDirectory()) {
            throw new IllegalStateException();
        }
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                return Arrays.stream(files).allMatch(this::deleteFile) && dir.delete();
            }
        }
        return false;
    }

    private boolean deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                Arrays.stream(files).forEach(this::deleteFile);
            }
        }
        return file.delete();
    }
}
