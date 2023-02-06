package ru.yandex.autotests.direct.httpclient.util.requestbeantojson;

import com.google.gson.FieldNamingStrategy;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.lang.reflect.Field;

/**
 * Created by shmykov on 16.04.15.
 */
public class RequestBeanCustomNamingStrategy implements FieldNamingStrategy {

    @Override
    public String translateName(Field field) {
        if (field.isAnnotationPresent(JsonPath.class) && !field.getAnnotation(JsonPath.class).requestPath().equals("")) {
            JsonPath path = field.getAnnotation(JsonPath.class);
            return path.requestPath();
        } else {
            return null;
        }
    }
}