package ru.yandex.market.checkout.util.report.generators;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.PickupOption;
import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.InstallmentsInfo;
import ru.yandex.market.common.report.model.json.credit.YandexBnplInfo;
import ru.yandex.market.common.report.model.specs.UsedParam;

/**
 * @author mmetlov
 */
public abstract class AbstractCheckouterJsonGenerator<T> extends AbstractJsonGenerator<T> {

    private static final DateTimeFormatter ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    protected void fillLocalDeliveryOptionsParameters(JsonObject offer,
                                                      FoundOffer item,
                                                      ReportGeneratorParameters parameters) {

        List<LocalDeliveryOption> options = Optional.ofNullable(parameters.getLocalDeliveryOptions())
                .map(m -> m.get(item.getFeedOfferId()))
                .orElse(Collections.emptyList());
        if (CollectionUtils.isNotEmpty(options)) {
            fillLocalDeliveryOptions(offer, options);
        }

        List<PickupOption> postOptions = Optional.ofNullable(parameters.getPostOptions())
                .map(m -> m.get(item.getFeedOfferId()))
                .orElse(Collections.emptyList());

        if (CollectionUtils.isNotEmpty(postOptions)) {
            fillLocalPostOptions(offer, postOptions);
        }

        List<PickupOption> pickupOptions = Optional.ofNullable(parameters.getPickupOptions())
                .map(m -> m.get(item.getFeedOfferId()))
                .orElse(Collections.emptyList());

        if (CollectionUtils.isNotEmpty(pickupOptions)) {
            fillLocalPickupOptions(offer, pickupOptions);
        }
    }

    protected void fillLocalDeliveryOptions(JsonObject offer, Collection<LocalDeliveryOption> options) {
        JsonArray jsonElements = new JsonArray();
        options.stream().map(this::buildJsonDeliveryOption).forEach(jsonElements::add);
        setJsonPropertyValue(offer, "delivery.options", jsonElements);
    }

    protected void fillLocalPostOptions(JsonObject offer, Collection<PickupOption> postOptions) {
        fillLocalPostOptions(offer, postOptions, "delivery.postOptions");
    }

    protected void fillLocalPickupOptions(JsonObject offer, Collection<PickupOption> postOptions) {
        fillLocalPostOptions(offer, postOptions, "delivery.pickupOptions");
    }

    protected void fillLocalPostOptions(JsonObject offer, Collection<PickupOption> postOptions,
                                        String propertyFullPath) {
        JsonArray jsonElements = new JsonArray();
        postOptions.stream().map(this::buildJsonDeliveryOption).forEach(jsonElements::add);
        setJsonPropertyValue(offer, propertyFullPath, jsonElements);
    }

    @Nonnull
    protected JsonObject buildJsonDeliveryOption(LocalDeliveryOption o) {
        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, "price.currency", o.getCurrency());
        setJsonPropertyValue(jsonObject, "price.value", o.getCost());
        setJsonPropertyValue(jsonObject, "price.valueWithoutVAT", o.getPriceWithoutVat());
        setJsonPropertyValue(jsonObject, "supplierDiscount.value", o.getSupplierDiscount());
        setJsonPropertyValue(jsonObject, "supplierPrice.value", o.getSupplierPrice());
        setJsonPropertyValue(jsonObject, "dayFrom", o.getDayFrom());
        setJsonPropertyValue(jsonObject, "dayTo", o.getDayTo());
        setJsonPropertyValue(jsonObject, "isDefault", false);
        setJsonPropertyValue(jsonObject, "serviceId", o.getDeliveryServiceId());
        setJsonPropertyValue(jsonObject, "paymentMethods", o.getPaymentMethods());
        setJsonPropertyValue(jsonObject, "partnerType", o.getPartnerType());
        setJsonPropertyValue(jsonObject, "shipmentDay", o.getShipmentDay());
        if (o.getShipmentDate() != null) {
            setJsonPropertyValue(jsonObject, "shipmentDate", o.getShipmentDate().toString());
        }
        setJsonPropertyValue(jsonObject, "tariffId", o.getTariffId());
        if (o.getPackagingTime() != null) {
            setJsonPropertyValue(jsonObject, "packagingTime", o.getPackagingTime().toString());
        }
        if (o.getSupplierShipmentDateTime() != null) {
            setJsonPropertyValue(jsonObject, "supplierShipmentDateTime", o.getSupplierShipmentDateTime().toString());
        }
        if (o.getShipmentDateTimeBySupplier() != null) {
            setJsonPropertyValue(jsonObject, "shipmentBySupplier",
                    ZONED_DATE_TIME_FORMATTER.format(o.getShipmentDateTimeBySupplier()));
        }
        if (o.getReceptionDateTimeByWarehouse() != null) {
            setJsonPropertyValue(jsonObject, "receptionByWarehouse",
                    ZONED_DATE_TIME_FORMATTER.format(o.getReceptionDateTimeByWarehouse()));
        }
        if (Boolean.TRUE.equals(o.isExpress())) {
            setJsonPropertyValue(jsonObject, "isExpress", true);
        }
        if (Boolean.TRUE.equals(o.isOnDemand())) {
            setJsonPropertyValue(jsonObject, "isOnDemand", true);
        }
        if (o.getDiscount() != null) {
            setJsonPropertyValue(jsonObject, "discount.oldMin.value", o.getDiscount().getOldPrice());
            setJsonPropertyValue(jsonObject, "discount.discountType", o.getDiscount().getDiscountType().getValue());
        }
        if (o.isMarketBranded() != null) {
            setJsonPropertyValue(jsonObject, "isMarketBranded", o.isMarketBranded());
        }
        if (o.getMarketPartner() != null) {
            setJsonPropertyValue(jsonObject, "isMarketPartner", o.getMarketPartner());
        }
        if (o.getMarketPostTerm() != null) {
            setJsonPropertyValue(jsonObject, "isMarketPostTerm", o.getMarketPostTerm());
        }
        if (Boolean.TRUE.equals(o.isDeferredCourier())) {
            setJsonPropertyValue(jsonObject, "isDeferredCourier", true);
        }
        if (Boolean.TRUE.equals(o.isExternalLogistics())) {
            setJsonPropertyValue(jsonObject, "isExternalLogistics", true);
        }

        setJsonPropertyValue(jsonObject, "isEstimated", o.getEstimated());

        if (o.getExtraCharge() != null) {
            setJsonPropertyValue(jsonObject, "extraCharge", o.getExtraCharge());
        }

        return jsonObject;
    }

    protected void fillInternalSpecs(JsonObject offer, FoundOffer foundOffer) {
        if (foundOffer.getSpecs() != null &&
                !foundOffer.getSpecs().getInternal().isEmpty()) {
            JsonArray jsonElements = new JsonArray();
            foundOffer.getSpecs().getInternal().forEach(it -> {
                JsonObject jsonObject = new JsonObject();
                setJsonPropertyValue(jsonObject, "type", it.getType());
                setJsonPropertyValue(jsonObject, "value", it.getValue());
                setJsonPropertyValue(jsonObject, "usedParams", it.getUsedParams().isEmpty()
                        ? Collections.emptyList()
                        : createSpecUsedParams(it.getUsedParams()));
                jsonElements.add(jsonObject);
            });
            setJsonPropertyValue(offer, "specs.internal", jsonElements);
        }
    }

    private JsonArray createSpecUsedParams(List<UsedParam> paramList) {
        JsonArray jsonElements = new JsonArray();

        for (UsedParam param : paramList) {
            JsonObject jsonObject = new JsonObject();
            setJsonPropertyValue(jsonObject, "name", param.getName());
            jsonElements.add(jsonObject);
        }
        return jsonElements;
    }

    protected void fillDeliveryMethods(JsonObject offer, ReportGeneratorParameters parameters) {
        if (parameters.getDeliveryMethods() == null) {
            return;
        }

        JsonArray deliveryMethods = new JsonArray();
        parameters.getDeliveryMethods().stream()
                .map(dm -> {
                    JsonObject jsonObject = new JsonObject();
                    setJsonPropertyValue(jsonObject, "serviceId", dm.getServiceId());
                    setJsonPropertyValue(jsonObject, "isMarketBranded", dm.getMarketBranded());
                    return jsonObject;
                }).forEach(deliveryMethods::add);
        setJsonPropertyValue(offer, "delivery.availableServices", deliveryMethods);
    }

    protected void fillServices(JsonObject offer, FoundOffer foundOffer) {
        if (foundOffer.getServices() == null) {
            return;
        }

        JsonArray services = new JsonArray();
        foundOffer.getServices().stream()
                .map(offerService -> {
                    JsonObject jsonObject = new JsonObject();
                    setJsonPropertyValue(jsonObject, "service_id", offerService.getServiceId());
                    setJsonPropertyValue(jsonObject, "title", offerService.getTitle());
                    setJsonPropertyValue(jsonObject, "description", offerService.getDescription());
                    setJsonPropertyValue(jsonObject, "price.value", offerService.getPrice());
                    setJsonPropertyValue(jsonObject, "price.currency", offerService.getCurrency());
                    setJsonPropertyValue(jsonObject, "ya_service_id", offerService.getYaServiceId());
                    return jsonObject;
                }).forEach(services::add);
        setJsonPropertyValue(offer, "services", services);
    }

    protected void fillBnplInfo(JsonObject offer, FoundOffer givenOffer) {
        if (givenOffer.getYandexBnplInfo() == null) {
            return;
        }
        var bnpl = givenOffer.getYandexBnplInfo();
        JsonObject bnplObj = new JsonObject();
        bnplObj.addProperty("enabled", bnpl.isEnabled());
        BnplDenial bnplDenial = bnpl.getBnplDenial();
        if (bnplDenial != null) {
            JsonObject bnplDenialJson = new JsonObject();
            bnplDenialJson.addProperty("reason", bnplDenial.getReason().name());
            bnplDenialJson.addProperty("threshold", bnplDenial.getThreshold());
            bnplObj.add("bnplDenial", bnplDenialJson);
        }
        setJsonPropertyValue(offer, "yandexBnplInfo", bnplObj);
    }

    protected void fillInstallmentsInfo(JsonObject offer, FoundOffer givenOffer) {
        if (CollectionUtils.isEmpty(givenOffer.getInstallmentsInfoSet())) {
            return;
        }
        JsonObject installmentsInfo = new JsonObject();
        JsonArray installments = new JsonArray();
        installmentsInfo.addProperty("bnplAvailable", Optional.ofNullable(givenOffer.getYandexBnplInfo())
                .map(YandexBnplInfo::isEnabled)
                .orElse(false));
        for (InstallmentsInfo info : givenOffer.getInstallmentsInfoSet()) {
            JsonObject installmentInfo = new JsonObject();
            installmentInfo.addProperty("term", info.getTerm());
            if (info.getMonthlyPayment() != null) {
                JsonObject payment = new JsonObject();
                payment.addProperty("currency", info.getMonthlyPayment().getCurrency());
                payment.addProperty("value", info.getMonthlyPayment().getValue());
                installmentInfo.add("monthlyPayment", payment);
            }
            installments.add(installmentInfo);
        }
        installmentsInfo.add("terms", installments);
        offer.add("installmentInfo", installmentsInfo);
    }
}
