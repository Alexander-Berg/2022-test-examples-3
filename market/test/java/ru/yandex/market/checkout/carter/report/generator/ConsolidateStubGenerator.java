package ru.yandex.market.checkout.carter.report.generator;

import com.google.gson.JsonObject;

import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;

public class ConsolidateStubGenerator extends AbstractJsonGenerator<ReportGeneratorParameters> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/files/consolidateStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        return object;
    }
}
