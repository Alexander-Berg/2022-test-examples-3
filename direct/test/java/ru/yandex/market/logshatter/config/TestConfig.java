package ru.yandex.market.logshatter.config;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 02/11/2017
 */
public class TestConfig {
    private String config;
    private String input;
    @SerializedName("expected")
    private List<JsonObject> expectedValues;

    public String getConfig() {
        return config;
    }

    public String getInput() {
        return input;
    }

    public List<JsonObject> getExpectedResults() {
        return expectedValues;
    }

}
