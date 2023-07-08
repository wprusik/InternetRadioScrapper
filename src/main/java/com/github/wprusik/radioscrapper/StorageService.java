package com.github.wprusik.radioscrapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wprusik.radioscrapper.model.RadioCategory;
import com.github.wprusik.radioscrapper.model.RadioStation;
import lombok.SneakyThrows;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

class StorageService {

    private static final String CONFIG_FILENAME = "data.json";

    private final String baseDirectory;
    private final String playlistDirectory;
    private final MessageDigest md;
    private final Map<String, String> fileHashes;

    StorageService(String baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.playlistDirectory = baseDirectory + File.separator + "m3u";
        this.md = createMessageDigest();
        this.fileHashes = getFileHashes();
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
        // if file is already in playlist directory, just return
        if (file.getAbsolutePath().startsWith(playlistDirectory)) {
            saveHash(file);
            return station;
        }
        // if there is existing playlist with the same file hash, return it
        String hash = getFileHash(file);
        String samePlaylistPath = fileHashes.get(hash);
        if (samePlaylistPath != null) {
            return station.toBuilder().playlistFile(samePlaylistPath).build();
        }
        // move file to playlist directory
        File targetFile = new File(playlistDirectory + File.separator + file.getName());
        if (!file.renameTo(targetFile)) {
            throw new IllegalStateException();
        }
        saveHash(targetFile);
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

    private Map<String, String> getFileHashes() {
        File file = new File(this.playlistDirectory);
        if (file.exists()) {
            File[] children = file.listFiles();
            if (children != null) {
                return Arrays.stream(children).collect(Collectors.toMap(File::getAbsolutePath, this::getFileHash));
            }
            return null;
        }
        return new HashMap<>();
    }

    private String getFileHash(File file) {
        try (InputStream is = new FileInputStream(file)) {
            md.update(is.readAllBytes());
            byte[] digest = md.digest();
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void saveHash(File file) {
        fileHashes.put(getFileHash(file), file.getAbsolutePath());
    }

    private MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
