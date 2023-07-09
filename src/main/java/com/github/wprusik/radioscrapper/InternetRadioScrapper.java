package com.github.wprusik.radioscrapper;

import com.github.wprusik.radioscrapper.model.RadioCategory;
import lombok.RequiredArgsConstructor;
import org.htmlunit.BrowserVersion;
import org.htmlunit.WebClient;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A scrapper for <a href="http://internet-radio.com">internet-radio.com</a> website
 */
@RequiredArgsConstructor
public class InternetRadioScrapper {

    private final static String BASE_URL = "https://www.internet-radio.com";

    private final BrowserVersion browserVersion;
    private final String baseDirectory;

    /**
     * @param baseDirectory - if specified, it saves whole configuration in given directory
     */
    public InternetRadioScrapper(@Nullable String baseDirectory) {
        this.browserVersion = BrowserVersion.BEST_SUPPORTED;
        this.baseDirectory = baseDirectory;
    }

    /**
     * Fetches all radio categories, omitting saved configuration
     */
    public List<RadioCategory> fetchAll() {
        return fetchAll(false);
    }

    /**
     * Returns all radio categories
     * @param redownload - if false, it fetches only radio stations that don't exist in current configuration
     */
    public List<RadioCategory> fetchAll(boolean redownload) {
        if (redownload) {
            clearWorkspace();
        }
        try (WebClient webClient = createWebClient()) {
            return new BaseExtractor(webClient, BASE_URL, baseDirectory).getAllRadioCategories();
        }
    }

    /**
     * Reads configuration only, without connecting to network
     */
    public List<RadioCategory> read() {
        if (baseDirectory == null) {
            throw new IllegalArgumentException("Unable to read configuration - base directory not specified");
        }
        StorageService storageService = new StorageService(baseDirectory);
        return storageService.load();
    }

    private void clearWorkspace() {
        if (baseDirectory != null) {
            StorageService storageService = new StorageService(baseDirectory);
            storageService.deleteBaseDirectory();
        }
    }

    private WebClient createWebClient() {
        WebClient client = new WebClient(browserVersion);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setTimeout(5000);
        return client;
    }
}
