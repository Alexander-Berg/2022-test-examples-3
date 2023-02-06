package ru.yandex.market.checkout.util.report.generators.offerinfo;


import com.google.gson.JsonObject;
import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;

/**
 * Просто вытаскивает заготовку из файла
 *
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public class OfferInfoPlaceStubGenerator<T> extends AbstractJsonGenerator<T> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/offerInfoPlaceStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, T parameters) {
        return loadJson();
    }
}
