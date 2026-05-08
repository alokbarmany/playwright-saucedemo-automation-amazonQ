package com.saucedemo.dataprovider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.saucedemo.utils.ConfigReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonDataReader {

    private static final ThreadLocal<List<Map<String, String>>> allDataTL    = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>>       currentRowTL = new ThreadLocal<>();

    public static void loadTestData(String dataFilePath) {
        String env = System.getProperty("env", ConfigReader.get("default.env"));
        String resolvedPath = dataFilePath.replace("{env}", env);

        try (InputStream input = JsonDataReader.class.getClassLoader().getResourceAsStream(resolvedPath)) {
            if (input == null) throw new RuntimeException("Data file not found: " + resolvedPath);

            JsonArray jsonArray = new Gson().fromJson(new InputStreamReader(input), JsonArray.class);
            List<Map<String, String>> allRows = new ArrayList<>();
            for (JsonElement element : jsonArray) {
                allRows.add(flattenJson(element.getAsJsonObject()));
            }
            allDataTL.set(allRows);
            currentRowTL.set(allRows.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test data: " + resolvedPath, e);
        }
    }

    public static List<Map<String, String>> getAllTestData() {
        return allDataTL.get();
    }

    public static void setCurrentRow(Map<String, String> row) {
        currentRowTL.set(row);
    }

    public static Map<String, String> getTestData() {
        return currentRowTL.get();
    }

    public static String get(String key) {
        Map<String, String> data = currentRowTL.get();
        return data != null ? data.get(key) : null;
    }

    public static String resolvePlaceholder(String text) {
        if (text == null || !text.contains("${")) return text;
        Map<String, String> data = currentRowTL.get();
        if (data == null) return text;
        String resolved = text;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            resolved = resolved.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }

    public static void clearTestData() {
        allDataTL.remove();
        currentRowTL.remove();
    }

    public static int getRowCount(String dataFilePath) {
        String env = System.getProperty("env", ConfigReader.get("default.env"));
        String resolvedPath = dataFilePath.replace("{env}", env);
        try (InputStream input = JsonDataReader.class.getClassLoader().getResourceAsStream(resolvedPath)) {
            if (input == null) return 1;
            JsonArray jsonArray = new Gson().fromJson(new InputStreamReader(input), JsonArray.class);
            return jsonArray.size();
        } catch (Exception e) {
            return 1;
        }
    }

    private static Map<String, String> flattenJson(JsonObject jsonObject) {
        Map<String, String> flatMap = new HashMap<>();
        flattenJsonRecursive("", jsonObject, flatMap);
        return flatMap;
    }

    private static void flattenJsonRecursive(String prefix, JsonObject jsonObject, Map<String, String> flatMap) {
        for (String key : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value.isJsonObject()) {
                flattenJsonRecursive(fullKey, value.getAsJsonObject(), flatMap);
            } else if (value.isJsonPrimitive()) {
                flatMap.put(fullKey, value.getAsString());
            }
        }
    }
}
