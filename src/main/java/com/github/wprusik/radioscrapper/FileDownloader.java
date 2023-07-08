package com.github.wprusik.radioscrapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.htmlunit.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

@RequiredArgsConstructor
class FileDownloader {

    private final WebClient webClient;

    @SuppressWarnings("SameParameterValue")
    @SneakyThrows
    Optional<File> download(String fileUrl, String extension) {
        URL url = new URL(fileUrl);
        File targetFile = Files.createTempFile("ir_", "." + extension).toFile();
        targetFile.deleteOnExit();

        try (InputStream is = webClient.getPage(url).getWebResponse().getContentAsStream();
             OutputStream os = new FileOutputStream(targetFile)) {

            IOUtils.copy(is, os);
        }
        if (targetFile.length() == 0) {
            targetFile.delete();
            return Optional.empty();
        }
        return Optional.of(targetFile);
    }
}
