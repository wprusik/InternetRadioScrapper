package com.github.wprusik.radioscrapper;

import com.github.wprusik.radioscrapper.model.RadioCategory;
import com.github.wprusik.radioscrapper.model.RadioStation;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.htmlunit.WebClient;
import org.htmlunit.html.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RequiredArgsConstructor
class RadioCategoryExtractor {

    private final WebClient webClient;
    private final String baseUrl;
    private final int delayBetweenDownloads;

    @SneakyThrows
    RadioCategory getRadioCategory(String name, String uri) {
        HtmlPage page = webClient.getPage(baseUrl + uri);
        String description = extractDescription(page);
        List<HtmlPage> pages = getPages(page);
        List<RadioStation> stations = pages.stream()
                .map(this::extractRadioStations)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return new RadioCategory(name, description, stations);
    }

    private String extractDescription(HtmlPage page) {
        HtmlElement container = page.getBody().getElementsByAttribute("div", "class", "panel panel-default").stream()
                .filter(this::isAboutSection)
                .findAny()
                .orElseThrow(IllegalStateException::new);
        List<HtmlElement> elements = container.getElementsByAttribute("p", "class", "lead");
        if (elements.size() != 1) {
            throw new IllegalStateException();
        }
        return elements.get(0).getTextContent();
    }

    private List<HtmlPage> getPages(HtmlPage page) throws IOException {
        List<HtmlElement> pagination = page.getBody().getElementsByAttribute("ul", "class", "pagination");
        if (pagination.isEmpty()) {
            throw new IllegalStateException("Pagination element not found");
        }
        HtmlUnorderedList ul = (HtmlUnorderedList) pagination.get(0);
        Map<Integer, URL> pageLinks = extractPageLinks(ul);

        List<HtmlPage> result = new ArrayList<>();
        for (Map.Entry<Integer, URL> link : pageLinks.entrySet()) {
            HtmlPage htmlpage = webClient.getPage(link.getValue());
            result.add(htmlpage);
        }
        return result;
    }

    private Map<Integer, URL> extractPageLinks(HtmlUnorderedList ul) {
        return StreamSupport.stream(ul.getChildElements().spliterator(), false)
                .filter(el -> el instanceof HtmlListItem)
                .filter(el -> el.getAttribute("class").isEmpty())
                .map(this::extractAnchor)
                .collect(Collectors.toMap(a -> Integer.parseInt(a.getTextContent()), a -> createURL(a.getAttribute("href"))));
    }

    private HtmlAnchor extractAnchor(DomElement el) {
        for (DomElement child : el.getChildElements()) {
            if (child instanceof HtmlAnchor) {
                return (HtmlAnchor) child;
            }
        }
        throw new IllegalStateException("Anchor not found");
    }

    private boolean isAboutSection(HtmlElement divPanel) {
        return divPanel.getElementsByAttribute("h2", "class", "panel-title").stream()
                .map(DomNode::getTextContent)
                .anyMatch(t -> t.trim().startsWith("About"));
    }

    @SneakyThrows
    private List<RadioStation> extractRadioStations(HtmlPage page) {
        HtmlTableBody tbody = getTableBody(page);
        List<HtmlTableRow> rows = extractRows(tbody);
        List<RadioStation> result = new ArrayList<>();
        RadioStationExtractor radioStationExtractor = new RadioStationExtractor(webClient, baseUrl);
        for (HtmlTableRow row : rows) {
            radioStationExtractor.extractRadioInfo(row).ifPresent(result::add);
            if (delayBetweenDownloads > 0) {
                Thread.sleep(delayBetweenDownloads);
            }
        }
        return result;
    }


    private HtmlTableBody getTableBody(HtmlPage page) {
        HtmlElement table = page.getBody().getElementsByAttribute("table", "class", "table table-striped").get(0);
        return (HtmlTableBody) table.getChildElements().iterator().next();
    }

    private List<HtmlTableRow> extractRows(HtmlTableBody tbody) {
        return StreamSupport.stream(tbody.getChildElements().spliterator(), false)
                .filter(el -> el instanceof HtmlTableRow)
                .filter(r -> !r.getAttribute("id").startsWith("play_nohtml"))
                .map(HtmlTableRow.class::cast).toList();
    }

    private URL createURL(String relativePath) {
        try {
            return new URL(baseUrl + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
