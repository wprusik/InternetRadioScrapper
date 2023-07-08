package com.github.wprusik.radioscrapper;

import com.github.wprusik.radioscrapper.model.RadioStation;
import org.apache.commons.lang3.StringUtils;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class RadioStationExtractor {

    private final String baseUrl;
    private final FileDownloader fileDownloader;
    private final Pattern kbpsPattern = Pattern.compile("(?<number>\\d+)[ ](Kbps)");

    public RadioStationExtractor(WebClient webClient, String baseUrl) {
        this.baseUrl = baseUrl;
        this.fileDownloader = new FileDownloader(webClient);
    }

    Optional<RadioStation> extractRadioInfo(HtmlTableRow row) {
        RadioStation.Builder builder = RadioStation.builder();

        for (DomElement el : row.getChildElements()) {
            if (el instanceof HtmlTableDataCell) {
                processTableCell((HtmlTableDataCell) el, builder);
            }
        }
        RadioStation station = builder.build();
        if (isComplete(station)) {
            return Optional.of(station);
        }
        return Optional.empty();
    }

    private void processTableCell(HtmlTableDataCell cell, RadioStation.Builder builder) {
        if (isPlayerSection(cell)) {
            fetchPlaylist(cell).ifPresent(builder::playlistFile);
        } else if (isMiddleSection(cell)) {
            extractRadioName(cell).ifPresent(builder::name);
            extractRadioUrl(cell).ifPresent(builder::url);
            extractGenres(cell).ifPresent(builder::genres);
        } else if (isRightSection(cell)) {
            extractKbps(cell).ifPresent(builder::kbps);
        }
    }

    private Optional<String> fetchPlaylist(HtmlTableDataCell cell) {
        String url = findM3uURL(cell);
        return fileDownloader.download(url, "m3u")
                .map(File::getAbsolutePath);
    }

    private String findM3uURL(HtmlTableDataCell cell) {
        return cell.getElementsByAttribute("a", "title", "M3U Playlist File").stream()
                .filter(el -> el instanceof HtmlAnchor)
                .map(el -> el.getAttribute("href"))
                .filter(StringUtils::isNotBlank)
                .map(el -> baseUrl + el)
                .findAny().orElseThrow();
    }

    private Optional<String> extractRadioName(HtmlTableDataCell cell) {
        return StreamSupport.stream(cell.getChildElements().spliterator(), false)
                .filter(el -> el instanceof HtmlHeading4)
                .map(DomNode::getTextContent)
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .findAny();
    }

    private Optional<String> extractRadioUrl(HtmlTableDataCell cell) {
        return StreamSupport.stream(cell.getChildren().spliterator(), false)
                .map(el -> el.getTextContent().trim())
                .filter(t -> t.startsWith("https://"))
                .findAny();
    }

    private Optional<List<String>> extractGenres(HtmlTableDataCell cell) {
        AtomicBoolean genresBegan = new AtomicBoolean();

        List<String> genres = StreamSupport.stream(cell.getChildren().spliterator(), false)
                .map(el -> el.getTextContent().trim())
                .peek(text -> {
                    if (text.startsWith("Genres:")) {
                        genresBegan.set(true);
                    }
                })
                .filter(text -> genresBegan.get())
                .map(text -> text.replace("Genres: ", ""))
                .map(text -> text.replace("\n", "").replace("\t", ""))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        return Optional.of(genres);
    }

    private Optional<Integer> extractKbps(HtmlTableDataCell cell) {
        return StreamSupport.stream(cell.getChildren().spliterator(), false)
                .map(DomNode::getTextContent)
                .map(text -> text.replace("\t", "").replace("\n", "").trim())
                .filter(text -> text.contains(" Kbps"))
                .findAny()
                .map(kbpsPattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group("number"))
                .map(Integer::parseInt);
    }

    private boolean isPlayerSection(HtmlTableDataCell cell) {
        return cell.getAttribute("id").startsWith("play_");
    }

    private boolean isMiddleSection(HtmlTableDataCell cell) {
        for (DomElement el : cell.getChildElements()) {
            if (el instanceof HtmlHeading4 && el.getAttribute("class").contains("text-danger")) {
                return true;
            }
        }
        return false;
    }

    private boolean isRightSection(HtmlTableDataCell cell) {
        return cell.getAttribute("class").contains("text-right");
    }

    private boolean isComplete(RadioStation station) {
        return StringUtils.isNotBlank(station.name())
                && station.genres() != null
                && station.kbps() != null
                && station.playlistFile() != null;
    }
}
