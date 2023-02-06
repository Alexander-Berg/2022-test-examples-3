package ru.yandex.market.checkout.util.report.generators;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.AbstractDelivery;
import ru.yandex.market.common.report.model.AbstractDeliveryResult;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.ExtraCharge;
import ru.yandex.market.common.report.model.KgtInfo;
import ru.yandex.market.common.report.model.OfferProblem;
import ru.yandex.market.common.report.model.SupplierProcessing;
import ru.yandex.market.common.report.model.TariffInfo;
import ru.yandex.market.common.report.model.TariffStats;

/**
 * @author mmetlov
 */
public abstract class AbstractDeliveryJsonGenerator<T extends AbstractDeliveryResult>
        extends AbstractCheckouterJsonGenerator<ReportGeneratorParameters> {

    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    protected JsonArray getCommonProblemsJson(AbstractDelivery<?> delivery) {
        JsonArray problems = new JsonArray();
        for (String problem : delivery.getCommonProblems()) {
            problems.add(problem);
        }
        return problems;
    }

    protected JsonArray getOfferProblemsJson(AbstractDelivery<?> delivery) {
        JsonArray offerProblems = new JsonArray();
        for (OfferProblem offerProblem : delivery.getOfferProblems()) {
            JsonObject jsonProblem = new JsonObject();
            setJsonPropertyValue(jsonProblem, "wareId", offerProblem.getWareId());

            JsonArray problems = new JsonArray();
            for (String problem : offerProblem.getProblems()) {
                problems.add(problem);
            }
            setJsonPropertyValue(jsonProblem, "problems", problems);

            offerProblems.add(jsonProblem);
        }
        return offerProblems;
    }

    protected JsonArray getResultsJson(AbstractDelivery<T> delivery) {
        JsonArray results = new JsonArray();
        for (T result : delivery.getResults()) {
            JsonObject jsonResult = getResultJson(result);
            results.add(jsonResult);
        }
        return results;
    }

    @NotNull
    protected JsonObject getResultJson(T result) {
        JsonObject jsonResult = new JsonObject();
        setJsonPropertyValue(jsonResult, "weight", result.getWeight());
        setJsonPropertyValue(jsonResult, "dimensions", result.getDimensions());
        setJsonPropertyValue(jsonResult, "largeSize", result.getLargeSize());
        fillDeliveryOffers(jsonResult, result.getOffers());
        return jsonResult;
    }

    protected JsonObject buildJsonActualDeliveryOption(ActualDeliveryOption actualDeliveryOption) {
        JsonObject json = buildJsonDeliveryOption(actualDeliveryOption);

        setJsonPropertyValue(json, "priceForShop.currency", actualDeliveryOption.getPriceForShopCurrency());
        setJsonPropertyValue(json, "priceForShop.value", actualDeliveryOption.getPriceForShop());
        setJsonPropertyValue(json, "leaveAtTheDoor", actualDeliveryOption.getLeaveAtTheDoor());
        if (actualDeliveryOption.getCustomizers() != null && !actualDeliveryOption.getCustomizers().isEmpty()) {
            JsonArray customizers = new JsonArray();
            actualDeliveryOption.getCustomizers().forEach(dc -> {
                JsonObject customizer = new JsonObject();
                setJsonPropertyValue(customizer, "key", dc.getKey());
                setJsonPropertyValue(customizer, "name", dc.getName());
                setJsonPropertyValue(customizer, "type", dc.getType());
                customizers.add(customizer);
            });
            setJsonPropertyValue(json, "customizers", customizers);
        }

        if (actualDeliveryOption.getTimeIntervals() == null) {
            return json;
        }
        JsonArray intervalsJson = new JsonArray();
        actualDeliveryOption.getTimeIntervals()
                .forEach(deliveryTimeInterval -> {
                    JsonObject intervalJson = new JsonObject();
                    if (deliveryTimeInterval.getFrom() != null) {
                        setJsonPropertyValue(
                                intervalJson, "from",
                                deliveryTimeInterval.getFrom().format(TIME_FORMATTER)
                        );
                    }
                    if (deliveryTimeInterval.getTo() != null) {
                        setJsonPropertyValue(
                                intervalJson, "to",
                                deliveryTimeInterval.getTo().format(TIME_FORMATTER)
                        );
                    }
                    if (deliveryTimeInterval.isDefault()) {
                        setJsonPropertyValue(
                                intervalJson, "isDefault",
                                deliveryTimeInterval.isDefault()
                        );
                    }
                    intervalsJson.add(intervalJson);
                });
        setJsonPropertyValue(json, "timeIntervals", intervalsJson);

        if (CollectionUtils.isNotEmpty(actualDeliveryOption.getDisclaimers())) {
            JsonArray disclaimersJson = new JsonArray();
            actualDeliveryOption.getDisclaimers().forEach(disclaimersJson::add);
            setJsonPropertyValue(json, "disclaimers", disclaimersJson);
        }
        if (CollectionUtils.isNotEmpty(actualDeliveryOption.getSupplierProcessings())) {
            JsonArray supplierProcessingsJson = new JsonArray();
            actualDeliveryOption.getSupplierProcessings().stream().map(this::fillSupplierProcessing)
                    .forEach(supplierProcessingsJson::add);
            setJsonPropertyValue(json, "supplierProcessing", supplierProcessingsJson);
        }
        setJsonPropertyValue(json, "isTryingAvailable", actualDeliveryOption.getTryingAvailable());
        fillExtraCharge(json, actualDeliveryOption);
        return json;
    }

    private void fillExtraCharge(JsonObject json, ActualDeliveryOption actualDeliveryOption) {
        ExtraCharge extraCharge = actualDeliveryOption.getExtraCharge();
        if (extraCharge == null) {
            return;
        }
        JsonObject result = new JsonObject();
        setJsonPropertyValue(result, "value", extraCharge.getValue());
        setJsonPropertyValue(result, "unitEconomyValue", extraCharge.getUnitEconomyValue());
        JsonArray jsonElements = new JsonArray();
        extraCharge.getReasonCodes().forEach(jsonElements::add);
        setJsonPropertyValue(result, "reasonCodes", extraCharge.getReasonCodes());
        setJsonPropertyValue(json, "extraCharge", extraCharge);
    }

    private void fillDeliveryOffers(JsonObject jsonResult, List<DeliveryOffer> offers) {
        JsonArray jsonElements = new JsonArray();
        if (CollectionUtils.isNotEmpty(offers)) {
            for (DeliveryOffer offer : offers) {
                JsonObject jsonObject = new JsonObject();
                setJsonPropertyValue(jsonObject, "marketSku", offer.getMarketSku());
                setJsonPropertyValue(jsonObject, "fulfillmentWarehouse",
                        offer.getFulfillmentWarehouseId());
                JsonObject sellerObject = new JsonObject();
                setJsonPropertyValue(sellerObject, "price", offer.getSellerPrice());
                setJsonPropertyValue(sellerObject, "currency", offer.getCurrency());
                setJsonPropertyValue(jsonObject, "seller", sellerObject);
                jsonElements.add(jsonObject);
            }
        }
        setJsonPropertyValue(jsonResult, "offers", jsonElements);
    }

    private JsonObject fillSupplierProcessing(SupplierProcessing processing) {
        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, "warehouseId", processing.getWarehouseId());
        setJsonPropertyValue(jsonObject, "startDateTime",
                DATE_TIME_FORMATTER.format(processing.getStartDateTime()));
        setJsonPropertyValue(jsonObject, "shipmentDateTime",
                DATE_TIME_FORMATTER.format(processing.getShipmentDateTime()));
        if (processing.getShipmentDateTimeBySupplier() != null) {
            setJsonPropertyValue(jsonObject, "shipmentBySupplier",
                    DATE_TIME_FORMATTER.format(processing.getShipmentDateTimeBySupplier()));
        }
        if (processing.getReceptionDateTimeByWarehouse() != null) {
            setJsonPropertyValue(jsonObject, "receptionByWarehouse",
                    DATE_TIME_FORMATTER.format(processing.getReceptionDateTimeByWarehouse()));
        }
        return jsonObject;
    }

    protected JsonObject createTariffStatsJson(TariffStats tariffStats) {
        if (tariffStats == null) {
            return null;
        }

        JsonObject tariffStatsObject = new JsonObject();

        KgtInfo kgtInfo = tariffStats.getKgtInfo();
        if (kgtInfo != null) {
            setJsonPropertyValue(tariffStatsObject, "kgtInfo.text", kgtInfo.getText());

            JsonArray factors = new JsonArray();
            Optional.ofNullable(kgtInfo.getFactors())
                    .orElse(Collections.emptyList())
                    .stream().map(it -> {
                JsonObject factor = new JsonObject();

                setJsonPropertyValue(factor, "type", it.getType().getOriginalName());
                setJsonPropertyValue(factor, "orderValue.value", it.getOrderValue().getValue());
                setJsonPropertyValue(factor, "orderValue.unit", it.getOrderValue().getUnit());

                return factor;
            }).forEach(factors::add);

            setJsonPropertyValue(tariffStatsObject, "kgtInfo.factors", factors);
        }

        TariffInfo tariffInfo = tariffStats.getTariffInfo();
        if (tariffInfo != null) {
            JsonArray factors = new JsonArray();
            Optional.ofNullable(tariffInfo.getFactors())
                    .orElse(Collections.emptyList())
                    .stream().map(it -> {
                JsonObject factor = new JsonObject();

                setJsonPropertyValue(factor, "type", it.getType().getOriginalName());

                return factor;
            }).forEach(factors::add);

            setJsonPropertyValue(tariffStatsObject, "tariffInfo.factors", factors);
        }

        return tariffStatsObject;
    }
}
