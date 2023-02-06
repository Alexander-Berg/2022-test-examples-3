package ru.yandex.market.checkout.util.report.generators;

import java.util.Collection;
import java.util.Collections;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.AddressPreset;
import ru.yandex.market.common.report.model.DeliveryTypeDistribution;
import ru.yandex.market.common.report.model.ExtraChargeParameters;
import ru.yandex.market.common.report.model.OutletDeliveryTimeInterval;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.common.report.model.PresetParcel;
import ru.yandex.market.common.report.model.TryingAvailable;

/**
 * @author mmetlov
 */
public class ActualDeliveryJsonGenerator extends AbstractDeliveryJsonGenerator<ActualDeliveryResult> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/actualDelivery.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        JsonObject res = loadJson();
        ActualDelivery actualDelivery = parameters.getActualDelivery();
        if (actualDelivery == null) {
            return res;
        }
        setJsonPropertyValue(res, "search.freeDeliveryRemainder.value", actualDelivery.getFreeDeliveryRemainder());
        setJsonPropertyValue(res, "search.freeDeliveryThreshold.value", actualDelivery.getFreeDeliveryThreshold());
        setJsonPropertyValue(res, "search.offersTotalPrice.value", actualDelivery.getOffersTotalPrice());
        setJsonPropertyValue(res, "search.betterWithPlus", actualDelivery.getBetterWithPlus());
        setJsonPropertyValue(res, "search.cheaperDeliveryRemainder.value",
                actualDelivery.getCheaperDeliveryRemainder());
        setJsonPropertyValue(res, "search.cheaperDeliveryThreshold.value",
                actualDelivery.getCheaperDeliveryThreshold());

        if (actualDelivery.getOfferProblems() != null) {
            JsonArray problems = getOfferProblemsJson(actualDelivery);
            res.getAsJsonObject("search").add("offerProblems", problems);
        }

        if (actualDelivery.getCommonProblems() != null) {
            JsonArray problems = getCommonProblemsJson(actualDelivery);
            res.getAsJsonObject("search").add("commonProblems", problems);
        }

        if (actualDelivery.getAddressPresets() != null) {
            JsonArray presets = getPresetsJson(actualDelivery);
            res.getAsJsonObject("search").add("addressPresets", presets);
        }

        if (actualDelivery.getExtraChargeParameters() != null) {
            JsonObject extraChargeParameters = getExtraChargeParametersJson(actualDelivery.getExtraChargeParameters());
            res.getAsJsonObject("search").add("extraChargeParameters", extraChargeParameters);
        }


        JsonArray results = getResultsJson(actualDelivery);
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
    protected @NotNull JsonObject getResultJson(ActualDeliveryResult result) {
        JsonObject jsonResult = super.getResultJson(result);
        fillActualDeliveryOptions(jsonResult, result.getDelivery());
        fillPickupOptions(jsonResult, result.getPickup(), "delivery.pickupOptions");
        fillPickupOptions(jsonResult, result.getPost(), "delivery.postOptions");
        setJsonPropertyValue(jsonResult, "bucketsAll",
                createDeliveryTypeDistributionJson(result.getBucketAll()));
        setJsonPropertyValue(jsonResult, "bucketsActive",
                createDeliveryTypeDistributionJson(result.getBucketActive()));
        setJsonPropertyValue(jsonResult, "carriersAll",
                createDeliveryTypeDistributionJson(result.getCarrierAll()));
        setJsonPropertyValue(jsonResult, "carriersActive",
                createDeliveryTypeDistributionJson(result.getCarrierActive()));
        setJsonPropertyValue(jsonResult, "delivery.nearestOutlet", createNearestOutlet());
        setJsonPropertyValue(jsonResult, "parcelInfo", result.getParcelInfo());
        setJsonPropertyValue(jsonResult, "delivery.tariffStats", createTariffStatsJson(result.getTariffStats()));
        fillAvailableDeliveryMethods(jsonResult, result);
        return jsonResult;
    }

    protected void fillAvailableDeliveryMethods(JsonObject result, ActualDeliveryResult actualDeliveryResult) {
        if (actualDeliveryResult.getAvailableDeliveryMethods() == null) {
            return;
        }

        JsonArray availableDeliveryMethods = new JsonArray();
        actualDeliveryResult.getAvailableDeliveryMethods().forEach(availableDeliveryMethods::add);

        setJsonPropertyValue(result, "delivery.availableDeliveryMethods", availableDeliveryMethods);
    }

    private JsonObject createNearestOutlet() {
        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, "id", 12345L);
        setJsonPropertyValue(jsonObject, "gpsCoord.longitude", "12.1");
        setJsonPropertyValue(jsonObject, "gpsCoord.latitude", "24.2");
        return jsonObject;
    }

    private void fillActualDeliveryOptions(JsonObject jsonResult, Collection<ActualDeliveryOption> options) {
        JsonArray jsonElements = new JsonArray();
        options.stream()
                .map(this::buildJsonActualDeliveryOption)
                .forEach(jsonElements::add);
        setJsonPropertyValue(jsonResult, "delivery.options", jsonElements);
    }

    private JsonObject createDeliveryTypeDistributionJson(DeliveryTypeDistribution distribution) {
        JsonObject jsonObject = new JsonObject();
        if (distribution == null) {
            distribution = new DeliveryTypeDistribution();
            distribution.setPost(Collections.emptyList());
            distribution.setCourier(Collections.emptyList());
            distribution.setPickup(Collections.emptyList());
        }
        setJsonPropertyValue(jsonObject, "courier", distribution.getCourier());
        setJsonPropertyValue(jsonObject, "pickup", distribution.getPickup());
        setJsonPropertyValue(jsonObject, "post", distribution.getPost());
        return jsonObject;
    }

    private void fillPickupOptions(
            JsonObject actualDeliveryResultJson, Collection<PickupOption> pickupOptions, String propertyFullPath) {
        if (pickupOptions == null) {
            return;
        }
        JsonArray jsonOptions = new JsonArray();
        pickupOptions.stream()
                .map(option -> {
                    JsonObject jsonOption = buildJsonDeliveryOption(option);
                    setJsonPropertyValue(jsonOption, "outletIds", option.getOutletIds());
                    setJsonPropertyValue(jsonOption, "postCodes", option.getPostCodes());
                    setJsonPropertyValue(jsonOption, "outletTimeIntervals",
                            getOutletTimeIntervalsJson(option.getOutletTimeIntervals()));
                    return jsonOption;
                })
                .forEach(jsonOptions::add);
        setJsonPropertyValue(actualDeliveryResultJson, propertyFullPath, jsonOptions);
    }

    private JsonArray getOutletTimeIntervalsJson(Collection<OutletDeliveryTimeInterval> timeIntervals) {
        JsonArray intervalsJson = new JsonArray();
        if (timeIntervals == null) {
            return intervalsJson;
        }

        timeIntervals
                .forEach(interval -> {
                    JsonObject intervalJson = new JsonObject();
                    setJsonPropertyValue(
                            intervalJson, "outletId",
                            interval.getOutletId()
                    );
                    if (interval.getFrom() != null) {
                        setJsonPropertyValue(
                                intervalJson, "from",
                                interval.getFrom().format(TIME_FORMATTER)
                        );
                    }
                    if (interval.getTo() != null) {
                        setJsonPropertyValue(
                                intervalJson, "to",
                                interval.getTo().format(TIME_FORMATTER)
                        );
                    }
                    intervalsJson.add(intervalJson);
                });
        return intervalsJson;
    }

    private JsonArray getPresetsJson(ActualDelivery actualDelivery) {
        JsonArray presets = new JsonArray();
        for (AddressPreset addressPreset : actualDelivery.getAddressPresets()) {
            JsonObject preset = new JsonObject();
            preset.addProperty("id", addressPreset.getId());
            preset.addProperty("type", addressPreset.getType());
            preset.addProperty("outletId", addressPreset.getOutletId());
            preset.addProperty("rid", addressPreset.getRid());
            JsonObject coord = new JsonObject();
            coord.addProperty("lat", 1.1);
            coord.addProperty("lon", 2.7);
            preset.add("coord", coord);
            if (addressPreset.getParcels() != null) {
                JsonArray parcels = new JsonArray();
                for (PresetParcel presetParcel : addressPreset.getParcels()) {
                    JsonObject parcel = new JsonObject();
                    parcel.addProperty("parcelIndex", presetParcel.getParcelIndex());
                    parcel.addProperty("deliveryAvailable", presetParcel.getDeliveryAvailable().name());
                    parcel.addProperty("tryingAvailable",
                            (Boolean.TRUE.equals(presetParcel.getTryingAvailable())
                                    ? TryingAvailable.TRYAIBLE
                                    : TryingAvailable.UNTRYAIBLE).getValue()
                    );
                    parcels.add(parcel);
                }
                preset.add("parcels", parcels);
            }
            presets.add(preset);
        }
        return presets;
    }
}
