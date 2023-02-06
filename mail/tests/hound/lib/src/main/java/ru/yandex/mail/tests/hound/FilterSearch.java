package ru.yandex.mail.tests.hound;

import com.jayway.restassured.path.json.JsonPath;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;


public class FilterSearch {
    private String responseAsString;

    private String getStringFromJson(String path) {
        try {
            return JsonPath.from(responseAsString).getString(path);
        } catch (JsonPathException e) {
            throw new AssertionError("Не удалось распарсить JSON", e);
        }
    }

    List<Map<String, String>> getListFromJson(String path) {
        try {
            return JsonPath.from(responseAsString).getList(path);
        } catch (com.jayway.restassured.path.json.exception.JsonPathException e) {
            throw new AssertionError("Не удалось распарсить JSON", e);
        }
    }

    public FilterSearch(String responseAsString) {
        this.responseAsString = responseAsString;
    }

    public static FilterSearch filterSearch(Response response) {
        return new FilterSearch(response.asString());
    }

    public String displayNameFrom() {
        return getStringFromJson("envelopes[0].from[0].displayName");
    }

    public String domainTo() {
        return getStringFromJson("envelopes[0].to[0].domain");
    }

    public String subject() {
        return getStringFromJson("envelopes[0].subject");
    }

    public Map<String, String> from() {
        return getListFromJson("envelopes[0].from").get(0);
    }

    public List<Map<String, String>> to() {
        return getListFromJson("envelopes[0].to");
    }

    public List<Map<String, String>> cc() {
        return getListFromJson("envelopes[0].cc");
    }

    public List<Map<String, String>> bcc() {
        return getListFromJson("envelopes[0].bcc");
    }

    public String firstline() {
        return getStringFromJson("envelopes[0].firstline");
    }
}
