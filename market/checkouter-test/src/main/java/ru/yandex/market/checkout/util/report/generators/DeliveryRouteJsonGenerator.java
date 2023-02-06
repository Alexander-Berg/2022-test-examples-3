package ru.yandex.market.checkout.util.report.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.DeliveryRoute;
import ru.yandex.market.common.report.model.DeliveryRouteResult;
import ru.yandex.market.common.report.model.ExtraChargeParameters;

/**
 * @author mmetlov
 */
public class DeliveryRouteJsonGenerator extends AbstractDeliveryJsonGenerator<DeliveryRouteResult> {


    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/deliveryRoute.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        JsonObject res = loadJson();
        DeliveryRoute deliveryRoute = parameters.getDeliveryRoute();
        if (deliveryRoute == null) {
            return res;
        }

        if (deliveryRoute.getOfferProblems() != null) {
            JsonArray problems = getOfferProblemsJson(deliveryRoute);
            res.getAsJsonObject("search").add("offerProblems", problems);
        }

        if (deliveryRoute.getCommonProblems() != null) {
            JsonArray problems = getCommonProblemsJson(deliveryRoute);
            res.getAsJsonObject("search").add("commonProblems", problems);
        }

        if (deliveryRoute.getExtraChargeParameters() != null) {
            JsonObject extraChargeParameters = getExtraChargeParametersJson(deliveryRoute.getExtraChargeParameters());
            res.getAsJsonObject("search").add("extraChargeParameters", extraChargeParameters);
        }

        JsonArray results = getResultsJson(deliveryRoute);
        res.getAsJsonObject("search").add("results", results);

        return res;
    }

    private JsonObject getExtraChargeParametersJson(ExtraChargeParameters parameters) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("maxCharge", parameters.getMaxCharge());
        jsonObject.addProperty("chargeQuant", parameters.getChargeQuant());
        jsonObject.addProperty("vatMultiplier", parameters.getVatMultiplier());
        jsonObject.addProperty("minChargeOfGmv", parameters.getMinChargeOfGmv());
        jsonObject.addProperty("minCharge", parameters.getMinCharge());
        return jsonObject;
    }

    @Override
    protected @NotNull JsonObject getResultJson(DeliveryRouteResult result) {
        JsonObject jsonResult = super.getResultJson(result);
        setJsonPropertyValue(jsonResult, "delivery.option", buildJsonActualDeliveryOption(result.getOption()));
        setJsonPropertyValue(jsonResult, "delivery.option.outletIds", result.getOption().getOutletIds());
        setJsonPropertyValue(jsonResult, "delivery.route", gson.fromJson(result.getRoute(), JsonObject.class));
        setJsonPropertyValue(jsonResult, "delivery.tariffStats", createTariffStatsJson(result.getTariffStats()));
        return jsonResult;
    }
}
