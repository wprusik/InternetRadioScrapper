package com.github.wprusik.radioscrapper.utils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UUIDGenerator {

    public static UUID create(Object... args) {
        StringBuilder str = new StringBuilder();
        for (Object arg : args) {
            str.append(arg);
        }
        return UUID.nameUUIDFromBytes(str.toString().getBytes(StandardCharsets.UTF_8));
    }
}
