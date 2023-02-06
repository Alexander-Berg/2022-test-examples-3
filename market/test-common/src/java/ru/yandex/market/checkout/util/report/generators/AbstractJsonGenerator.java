package ru.yandex.market.checkout.util.report.generators;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import ru.yandex.market.checkout.findbugs.SuppressFBWarnings;

/**
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public abstract class AbstractJsonGenerator<T> implements ReportGenerator<T> {

    private String jsonFileName;

    protected final Gson gson = new GsonBuilder().create();

    protected abstract String getDefaultJsonFileName();

    protected JsonObject getJsonObject(JsonObject json, String propertyFullPath) {
        String[] properties = propertyFullPath.split("\\.");
        return findJsonObjectRecursive(json, properties, 0, properties.length - 1);
    }

    protected String stringOf(@Nullable LocalDateTime localDateTime, @Nonnull DateTimeFormatter dateTimeFormatter) {
        return localDateTime == null ? null : localDateTime.format(dateTimeFormatter);
    }

    protected <T> void setJsonPropertyValue(JsonObject json, String propertyFullPath, T o) {
        if (o == null) {
            return;
        }
        String[] properties = propertyFullPath.split("\\.");
        JsonObject jsonObject = findJsonObjectRecursive(json, properties, 0, properties.length - 2);
        String propertyName = properties[properties.length - 1];
        jsonObject.remove(propertyName);
        jsonObject.add(propertyName, gson.toJsonTree(o));
    }

    protected <T> void addJsonPropertyValue(JsonObject json, String propertyFullPath, T o) {
        String[] properties = propertyFullPath.split("\\.");
        JsonObject jsonObject = findJsonObjectRecursive(json, properties, 0, properties.length - 2);
        String propertyName = properties[properties.length - 1];
        JsonElement jsonElement = jsonObject.get(propertyName);
        if (jsonElement == null) {
            jsonElement = new JsonArray();
            jsonObject.add(propertyName, jsonElement);
        }
        jsonElement.getAsJsonArray().add(gson.toJsonTree(o));
    }

    protected <T> void removePropertyValue(JsonObject json, String propertyFullPath) {
        String[] properties = propertyFullPath.split("\\.");
        JsonObject jsonObject = findJsonObjectRecursive(json, properties, 0, properties.length - 2);
        String propertyName = properties[properties.length - 1];
        jsonObject.remove(propertyName);
    }


    protected JsonObject findJsonObjectRecursive(JsonObject parentJsonObject, String[] properties,
                                                 int current, int max) {
        if (current > max) {
            return parentJsonObject;
        }
        String property = properties[current];
        JsonObject newJsonObject;
        if (parentJsonObject.get(property) != null) {
            newJsonObject = parentJsonObject.get(property).getAsJsonObject();
        } else {
            newJsonObject = new JsonObject();
            parentJsonObject.add(property, newJsonObject);
        }
        return findJsonObjectRecursive(newJsonObject, properties, current + 1, max);
    }

    @Nonnull
    public static JsonObject deepCopy(@Nonnull JsonObject jsonObject) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            result.add(entry.getKey(), deepCopy(entry.getValue()));
        }
        return result;
    }

    @Nonnull
    public static JsonArray deepCopy(@Nonnull JsonArray jsonArray) {
        JsonArray result = new JsonArray();
        for (JsonElement e : jsonArray) {
            result.add(deepCopy(e));
        }
        return result;
    }

    @Nonnull
    public static JsonElement deepCopy(@Nonnull JsonElement jsonElement) {
        if (jsonElement.isJsonPrimitive() || jsonElement.isJsonNull()) {
            return jsonElement;       // these are immutables anyway
        } else if (jsonElement.isJsonObject()) {
            return deepCopy(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            return deepCopy(jsonElement.getAsJsonArray());
        } else {
            throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
        }
    }

    @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING")
    protected JsonObject loadJson(String filename) {
        InputStream resourceAsStream = AbstractJsonGenerator.class.getResourceAsStream(filename);
        return gson.fromJson(new InputStreamReader(resourceAsStream), JsonObject.class);
    }

    protected JsonObject loadJson() {
        return loadJson(jsonFileName == null ? getDefaultJsonFileName() : jsonFileName);
    }

    public String getJsonFileName() {
        return jsonFileName;
    }

    public void setJsonFileName(String jsonFileName) {
        this.jsonFileName = jsonFileName;
    }
}
