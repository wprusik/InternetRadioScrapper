package com.github.wprusik.radioscrapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;

import java.io.*;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;

@RequiredArgsConstructor
class FileDownloader {

    private final WebClient webClient;

    private int failedCount = 0;

    @SneakyThrows
    @SuppressWarnings({"SameParameterValue", "ResultOfMethodCallIgnored"})
    Optional<File> download(String fileUrl, String extension) {
        URL url = new URL(fileUrl);
        File targetFile = Files.createTempFile("ir_", "." + extension).toFile();
        targetFile.deleteOnExit();

        tryToConnect(url).ifPresent(page -> saveToFile(page, targetFile));

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

    private Optional<Page> tryToConnect(URL url) throws IOException {
        try {
            return Optional.of(webClient.getPage(url));
        } catch (ConnectException e) {
            printWarning(url, e);
            if (++failedCount > 10) {
                throw new UncheckedIOException(e);
            }
        } catch (FailingHttpStatusCodeException e) {
            printWarning(url, e);
            if (++failedCount > 10) {
                throw new RuntimeException(e.getMessage(), e);
            }
            if (e.getStatusCode() == 400 && url.toString().contains("http:")) {
                url = createURL(url.toString().replace("http:", "https:"));
                return tryToConnect(url);
            }
        }
        return Optional.empty();
    }

    private void printWarning(URL url, Throwable e) {
        System.out.printf("\n\u001B[31mWARNING: UNABLE TO DOWNLOAD FILE FROM URL: '%s'\nException: %s\u001B[0m\n\n", url, e.getMessage());
    }

    private URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
