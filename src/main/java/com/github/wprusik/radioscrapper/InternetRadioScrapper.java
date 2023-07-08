package com.github.wprusik.radioscrapper;

import com.github.wprusik.radioscrapper.model.MenuCategory;
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
    private final int delayBetweenDownloadsMillis;

    /**
     * @param baseDirectory - if specified, it saves whole configuration in given directory
     */
    public InternetRadioScrapper(@Nullable String baseDirectory) {
        this(baseDirectory, 0);
    }

    /**
     * @param baseDirectory - if specified, it saves whole configuration in given directory
     * @param delayBetweenDownloadsMillis - defines delay between single radio station downloads in milliseconds
     */
    public InternetRadioScrapper(@Nullable String baseDirectory, int delayBetweenDownloadsMillis) {
        this.browserVersion = BrowserVersion.FIREFOX;
        this.baseDirectory = baseDirectory;
        this.delayBetweenDownloadsMillis = delayBetweenDownloadsMillis;
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
            clearBaseDirectory();
        }
        try (WebClient webClient = new WebClient(browserVersion)) {
            MenuCategory category = new MenuExtractor(webClient, BASE_URL, baseDirectory, delayBetweenDownloadsMillis).getListenCategory();
            return category.subcategories();
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

    private void clearBaseDirectory() {
        if (baseDirectory != null) {
            StorageService storageService = new StorageService(baseDirectory);
            storageService.clearBaseDirectory();
        }
    }
}
