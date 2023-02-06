package ru.yandex.market.checkout.util.report.generators;

import com.google.gson.JsonObject;

/**
 * @author Nikolai Iusiumbeli
 *         date: 10/07/2017
 */
public interface ReportGenerator<T> {
    JsonObject patch(JsonObject object, T parameters);
}
