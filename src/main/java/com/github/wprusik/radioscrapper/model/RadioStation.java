package com.github.wprusik.radioscrapper.model;

import lombok.Builder;

import javax.annotation.Nullable;
import java.util.List;

@Builder(builderClassName = "Builder", toBuilder = true)
public record RadioStation(String name, @Nullable String url, List<String> genres, Integer kbps, String playlistFile) {}
