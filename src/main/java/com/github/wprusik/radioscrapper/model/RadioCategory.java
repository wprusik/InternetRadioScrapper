package com.github.wprusik.radioscrapper.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.wprusik.radioscrapper.utils.UUIDGenerator;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(builderClassName = "Builder", toBuilder = true)
public record RadioCategory(UUID uuid, String name, String description, List<RadioStation> stations) {

    @JsonCreator
    public RadioCategory(@JsonProperty("name") String name,
                         @JsonProperty("description") String description,
                         @JsonProperty("stations") List<RadioStation> stations) {

        this(UUIDGenerator.create(name, description), name, description, stations);
    }
}
