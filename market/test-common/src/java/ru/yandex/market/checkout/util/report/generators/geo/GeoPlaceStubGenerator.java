package ru.yandex.market.checkout.util.report.generators.geo;

import com.google.gson.JsonObject;
import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;

public class GeoPlaceStubGenerator<T> extends AbstractJsonGenerator<T> {
    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/geoPlaceStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, T parameters) {
        return loadJson();
    }
}
