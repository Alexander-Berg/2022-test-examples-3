package ru.yandex.market.checkout.util.report.generators;

import com.google.gson.JsonObject;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;

public class ModelInfoPlaceGenerator extends AbstractJsonGenerator<ReportGeneratorParameters> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/modelInfoStub.json";
    }


    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        return loadJson();
    }
}
