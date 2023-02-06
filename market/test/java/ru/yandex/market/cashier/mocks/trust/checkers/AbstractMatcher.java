package ru.yandex.market.cashier.mocks.trust.checkers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

abstract class AbstractMatcher extends CustomMatcher<JsonElement> {

    AbstractMatcher(String description) {
        super(description);
    }

    @Override
    public boolean matches(Object item) {
        if (!(item instanceof JsonElement)) {
            return false;
        }
        if (!((JsonElement) item).isJsonObject()) {
            return false;
        }
        JsonObject object = ((JsonElement) item).getAsJsonObject();

        return matchObjectFields(object);
    }

    protected abstract boolean matchObjectFields(JsonObject object);

    protected static boolean matchValue(JsonObject object, String name, Object expectedValue) {
        if (expectedValue == null) {
            return !object.has(name);
        }

        if (expectedValue instanceof Matcher) {
            return ((Matcher) expectedValue).matches(object.get(name).getAsString());
        }

        return expectedValue.toString().equals(object.get(name).getAsString());
    }

    protected static boolean matchValue(JsonObject object, String name, BigDecimal expectedValue) {
        if (expectedValue == null) {
            return !object.has(name);
        }
        if (!object.get(name).isJsonPrimitive()) {
            return false;
        }

        //ожидаем что BigDecimal поля должны сериализоваться как строки
        // https://wiki.yandex-team.ru/TRUST/Payments/API/Conventions/#ispolzuemyetipydannyx
        if (!object.get(name).getAsJsonPrimitive().isString()) {
            return false;
        }

        if (expectedValue instanceof Matcher) {
            return ((Matcher) expectedValue).matches(object.get(name).getAsString());
        }

        return expectedValue.compareTo(new BigDecimal(object.get(name).getAsString())) == 0;
    }

    protected static boolean matchDateValue(JsonObject object, String name, Date expectedValue) {
        if (expectedValue == null) {
            return !object.has(name);
        }

        BigDecimal milliseconds = new BigDecimal(expectedValue.getTime());
        BigDecimal seconds = milliseconds.divide(new BigDecimal(TimeUnit.SECONDS.toMillis(1)), 3, RoundingMode.HALF_DOWN);
        //сверяем с точностью до секунда, т.к. в нашем ресте ходят данные с точностью до нее.
        return seconds.longValue() == new BigDecimal(object.get(name).getAsString()).longValue();
    }

    protected static boolean matchPropertiesSize(JsonObject object, Object... expectedFieldValues) {
        long count = Arrays.stream(expectedFieldValues).filter(Objects::nonNull).count();
        return object.size() == count;
    }


}
