package com.studyconnect.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class JsonUtils {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    public JsonUtils() {
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> targetClass) {
        return GSON.fromJson(json, targetClass);
    }
}
