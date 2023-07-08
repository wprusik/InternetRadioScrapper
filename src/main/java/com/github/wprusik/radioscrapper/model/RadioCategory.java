package com.github.wprusik.radioscrapper.model;

import lombok.Builder;

import java.util.List;

@Builder(builderClassName = "Builder", toBuilder = true)
public record RadioCategory(String name, String description, List<RadioStation> stations) {}
