package ru.yandex.market.checkout.util.report.generators;

import com.google.gson.JsonObject;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;

/**
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public class ShopInfoPlaceGenerator extends AbstractJsonGenerator<ReportGeneratorParameters> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/shopInfoStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        JsonObject shopInfoJson = loadJson();
        setJsonPropertyValue(shopInfoJson, "id", parameters.getOrder().getShopId());
        setJsonPropertyValue(shopInfoJson, "deliveryCurrency", parameters.getDeliveryCurrency());
        setJsonPropertyValue(shopInfoJson, "ignoreStocks", parameters.isIgnoreStocks());
        setJsonPropertyValue(shopInfoJson, "isCrossborder", parameters.isCrossborder());
        addJsonPropertyValue(object, "results", shopInfoJson);
        return object;
    }
}

