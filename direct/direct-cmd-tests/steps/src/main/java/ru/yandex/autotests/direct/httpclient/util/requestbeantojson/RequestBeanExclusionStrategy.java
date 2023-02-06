package ru.yandex.autotests.direct.httpclient.util.requestbeantojson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import ru.yandex.autotests.direct.httpclient.util.jsonParser.JsonPath;

import java.util.Objects;

/**
 * Created by shmykov on 16.04.15.
 */
public class RequestBeanExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
        return fieldAttributes.getAnnotation(JsonPath.class) == null || Objects.equals(fieldAttributes.getAnnotation(JsonPath.class).requestPath(), "");
    }

    @Override
    public boolean shouldSkipClass(Class<?> aClass) {
        return false;
    }
}
