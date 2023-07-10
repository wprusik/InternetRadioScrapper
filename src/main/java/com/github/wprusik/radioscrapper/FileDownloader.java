package com.github.wprusik.radioscrapper;

import com.github.wprusik.radioscrapper.exception.TooManyErrorsException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.Page;
import org.htmlunit.WebClient;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@RequiredArgsConstructor
class FileDownloader {

    private static final int FAIL_LIMIT = 50;

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

    private Optional<Page> tryToConnect(URL url) {
        try {
            URL finalURL = url;
            Page page = callWithTimeout(() -> webClient.getPage(finalURL));   // due to HtmlUnit bug
            return Optional.of(page);
        } catch (FailingHttpStatusCodeException e) {
            handleFail(e, url);
            if (e.getStatusCode() == 400 && url.toString().contains("http:")) {
                url = createURL(url.toString().replace("http:", "https:"));
                return tryToConnect(url);
            }
        } catch (Exception e) {
            handleFail(e, url);
        }
        return Optional.empty();
    }

    private <T> T callWithTimeout(Callable<T> callable) throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future<T> future = executor.submit(callable);
        executor.shutdown();
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Throwable e) {
            future.cancel(true);
            throw e;
        }
    }

    private void handleFail(Exception e, URL url) throws TooManyErrorsException {
        log.warn("Unable to download file from URL " + url, e);
        if (++failedCount > FAIL_LIMIT) {
            log.error("The number of errors exceeded the allowable limit of " + FAIL_LIMIT);
            throw new TooManyErrorsException(e);
        }
    }

    private URL createURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
