package ru.yandex.autotests.direct.httpclient.util;

import net.minidev.json.JSONValue;
import ru.yandex.autotests.httpclient.lite.core.exceptions.BackEndClientException;

import java.io.IOException;

/**
 * Created by shmykov on 10.12.14.
 */
public class JsonContainer {

    public JsonContainer(Object json) {
        this.json = json;
    }

    public JsonContainer() {

    }

    private Object json;

    public Object getJson() {
        return json;
    }

    public void setJson(Object json) {
        this.json = json;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            JSONValue.writeJSONString(json, sb);
        } catch (IOException e) {
            throw new BackEndClientException("Ошибка при преобразовании json в строку: " + e);
        }
        return sb.toString();
    }
}
