package com.github.wprusik.radioscrapper.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.wprusik.radioscrapper.utils.UUIDGenerator;
import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderClassName = "Builder", toBuilder = true)
public record RadioStation(UUID uuid, String name, @Nullable String url, List<String> genres, Integer kbps, String playlistFile) {

    @JsonCreator
    public RadioStation(@JsonProperty("name") String name,
                        @JsonProperty("url") @Nullable String url,
                        @JsonProperty("genres") List<String> genres,
                        @JsonProperty("kbps") Integer kbps,
                        @JsonProperty("playlistFile") String playlistFile) {

        this(UUIDGenerator.create(name, url, kbps), name, url, genres, kbps, playlistFile);
    }

}
