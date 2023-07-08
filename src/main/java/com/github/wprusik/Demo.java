package com.github.wprusik;

import com.github.wprusik.radioscrapper.InternetRadioScrapper;
import com.github.wprusik.radioscrapper.model.RadioCategory;
import com.github.wprusik.radioscrapper.model.RadioStation;
import lombok.SneakyThrows;

import java.io.File;
import java.util.List;

public class Demo {

    private static final String DATA_DIRECTORY = System.getProperty("user.dir") + File.separator + "data";

    public static void main(String... args) {
        fetchTest();
    }

    private static void fetchTest() {
        List<RadioCategory> categories = new InternetRadioScrapper(DATA_DIRECTORY, 300).fetchAll();
        print(categories);
    }

    @SneakyThrows
    private static void readTest() {
        List<RadioCategory> categories = new InternetRadioScrapper(DATA_DIRECTORY).read();
        print(categories);
    }

    private static void print(List<RadioCategory> categories) {
        for (RadioCategory cat : categories) {
            System.out.println("\n------------------\nCategory: " + cat.name());
            System.out.println("Description: " + cat.description());
            System.out.println("Radio stations:");
            List<RadioStation> stations = cat.stations();
            for (int i=0; i < stations.size(); i++) {
                RadioStation station = stations.get(i);
                System.out.println("\t" + (i + 1) + ".\t" + station.name());
                if (station.url() != null) {
                    System.out.println("\t\t" + station.url());
                }
                System.out.println("\t\t" + station.kbps() + " kbps");
                System.out.println("\t\tGenres:\t" + String.join(", ", station.genres()));
                System.out.println("\t\tPlaylist file:\t" + station.playlistFile());
            }
        }
    }

}
