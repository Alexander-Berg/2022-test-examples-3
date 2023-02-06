package ru.yandex.calendar.logic.staff;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.misc.io.http.UrlUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;


public class TestStaffHttpProvider implements StaffHttpProvider {


    private String readJson(String path) throws IOException {
        try (final LineNumberReader src =
                     new LineNumberReader(new InputStreamReader(new FileInputStream(path)))) {
            String line;
            StringBuilder text = new StringBuilder();
            while ((line = src.readLine()) != null) {
                text.append(line).append("\n");
            }
            return text.toString();
        }
    }

    @Override
    public String doRequest(String url, int limit, int page, String query, String fields) throws IOException {
        return doRequest(url, Cf.map("_page", String.valueOf(page),
                "_query", query, "_fields", fields, "_limit", String.valueOf(limit)));
    }


    @Override
    public String doRequest(String url, MapF<String, String> params) throws IOException {
        for (String key : params.keySet()) {
            url = UrlUtils.addParameter(url, key, params.getO(key).get());
        }
        if (url.contains("telegram_accounts")) {
            return "{\"limit\": 50, \"result\": [{\"telegram_accounts\": [{\"value\": \"Nikita_Andreev1\", \"value_lower\": \"nikita_andreev1\", \"private\": false, \"id\": 161764}]}], \"page\": 1}";
        }
        if (url.contains("/v3/persons") && !url.contains("is_deleted=true")) {
            return readJson("src/test/resources/ru/yandex/calendar/logic/staff/person.json");
        }
        if (url.contains("/v3/groups") && !url.contains("is_deleted=true")) {
            return readJson("src/test/resources/ru/yandex/calendar/logic/staff/groups.json");
        }
        if (url.contains("/v3/offices") && !url.contains("is_deleted=true")) {
            return readJson("src/test/resources/ru/yandex/calendar/logic/staff/offices.json");
        }
        if (url.contains("/v3/rooms") && !url.contains("is_deleted=true")) {
            return readJson("src/test/resources/ru/yandex/calendar/logic/staff/rooms.json");
        }
        return "{\"result\":[],\"total\":0}";
    }
}
