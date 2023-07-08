package com.github.wprusik.radioscrapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.htmlunit.Page;
import org.htmlunit.WebClient;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

@RequiredArgsConstructor
class FileDownloader {

    private final WebClient webClient;

    @SneakyThrows
    @SuppressWarnings({"SameParameterValue", "ResultOfMethodCallIgnored"})
    Optional<File> download(String fileUrl, String extension) {
        URL url = new URL(fileUrl);
        File targetFile = Files.createTempFile("ir_", "." + extension).toFile();
        targetFile.deleteOnExit();

        Page page = webClient.getPage(url);
        saveToFile(page, targetFile);

        if (targetFile.length() == 0) {
            targetFile.delete();
            return Optional.empty();
        }
        return Optional.of(targetFile);
    }

    private void saveToFile(Page page, File targetFile) {
        try (InputStream is = page.getWebResponse().getContentAsStream();
             OutputStream os = new FileOutputStream(targetFile)) {

            IOUtils.copy(is, os);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
