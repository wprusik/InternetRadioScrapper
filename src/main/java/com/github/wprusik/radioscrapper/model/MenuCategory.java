package com.github.wprusik.radioscrapper.model;

import java.util.List;

public record MenuCategory(String name, List<RadioCategory> subcategories) {}
