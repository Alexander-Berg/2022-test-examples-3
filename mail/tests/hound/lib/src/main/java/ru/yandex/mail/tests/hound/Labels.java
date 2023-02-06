package ru.yandex.mail.tests.hound;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;
import ru.yandex.mail.tests.hound.generated.LabelSymbol;
import ru.yandex.mail.tests.hound.generated.Lid;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Labels {
    private String respAsString;

    private String getStringFromJson(String path) {
        try {
            return JsonPath.from(respAsString).getString(path);
        } catch (JsonPathException e) {
            throw new AssertionError("Не удалось распарсить JSON", e);
        }
    }

    List<Map<String, String>> getListFromJson(String path) {
        try {
            return JsonPath.from(respAsString).getList(path);
        } catch (com.jayway.restassured.path.json.exception.JsonPathException e) {
            throw new AssertionError("Не удалось распарсить JSON", e);
        }
    }

    public Labels(String responseAsString) {
        this.respAsString = responseAsString;
    }

    public static Labels labels(Response response) {
        return new Labels(response.asString());
    }

    public List<String> userLids() {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());

        return lids
                .entrySet()
                .stream()
                .filter((entry -> entry
                        .getValue()
                        .getIsUser()))
                .map((entry -> entry.getKey()))
                .collect(Collectors.toList());
    }

    public Map<String, Lid> labels() {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        return new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
    }

    public Long countByName(String name) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getName()
                                .equals(name))
                .findFirst();
        return lidEntry.isPresent() ? lidEntry.get().getValue().getMessagesCount() : null;
    }

    public Optional<Long> countByLid(String lid) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getKey()
                                .equals(lid))
                .findFirst();
        return lidEntry.isPresent() ? Optional.of(lidEntry.get().getValue().getMessagesCount()) : Optional.empty();
    }

    public String lidBySymbol(LabelSymbol symbol) {
        return lidByTitle(symbol.toString());
    }

    public String nameBySymbol(LabelSymbol symbol) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getName()
                                .equals(symbol.toString()))
                .findFirst();
        return lidEntry.isPresent() ? lidEntry.get().getValue().getName() : null;
    }

    public String lidByTitle(String lidTitle) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getSymbolicName()
                                .getTitle()
                                .equals(lidTitle))
                .findFirst();
        return lidEntry.isPresent() ? lidEntry.get().getKey() : null;
    }

    public String lidByName(String lidName) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry -> entry
                                .getValue()
                                .getName()
                                .equals(lidName))
                .findFirst();
        return lidEntry.isPresent() ? lidEntry.get().getKey() : null;
    }

    public String lidByNameAndType(String lidName, String typeName) {
        JsonParser parser = new JsonParser();
        JsonElement lidsFragment = parser.parse(respAsString).getAsJsonObject().get("labels");
        Map<String, Lid> lids = new Gson().fromJson(lidsFragment, new TypeToken<Map<String, Lid>>(){}.getType());
        Optional<Map.Entry<String, Lid>> lidEntry = lids
                .entrySet()
                .stream()
                .filter
                        (entry ->
                                entry.getValue().getName().equals(lidName) &&
                                        entry.getValue().getType().getTitle().equals(typeName)
                        )
                .findFirst();
        return lidEntry.isPresent() ? lidEntry.get().getKey() : null;
    }
}
