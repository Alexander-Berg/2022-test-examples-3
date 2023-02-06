package ru.yandex.market.crm.core.test.utils.report;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.core.domain.Color;
import ru.yandex.market.crm.core.test.utils.report.Offer.Delivery;
import ru.yandex.market.crm.core.test.utils.report.Offer.Delivery.Region;
import ru.yandex.market.crm.core.test.utils.report.Offer.OfferPrices;
import ru.yandex.market.crm.core.test.utils.report.Offer.OfferPrices.Discount;
import ru.yandex.market.crm.core.test.utils.report.Offer.Promo;
import ru.yandex.market.crm.core.test.utils.report.ReportModel.ModelPrices;
import ru.yandex.market.crm.json.serialization.JsonSerializer;
import ru.yandex.market.mcrm.http.HttpEnvironment;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;

import static ru.yandex.market.mcrm.http.HttpRequest.get;

/**
 * @author apershukov
 */
@Component
public class ReportTestHelper {

    private static Picture picture() {
        Image image = new Image();
        image.setUrl("http://ya.static.ru/image");
        image.setWidth(768);
        image.setHeight(768);
        image.setContainerHeight(768);
        image.setContainerWidth(768);

        Picture picture = new Picture();
        picture.setOriginal(image);
        return picture;
    }

    public static ReportModel model(long modelId) {
        ReportModel model = new ReportModel();
        model.setId(String.valueOf(modelId));
        model.setOffers(new ModelOffers());
        model.setPictures(List.of(picture()));
        return model;
    }

    public static Offer offer(String offerId) {
        Offer offer = new Offer();
        offer.setPrices(new OfferPrices());
        offer.setId(offerId);
        offer.setPictures(List.of(picture()));
        offer.setDelivery(
                new Delivery()
                        .setShopPriorityRegion(new Region(213))
                        .setInStock(true)
        );
        return offer;
    }

    public static Offer offer() {
        return offer(UUID.randomUUID().toString());
    }

    public static ModelPrices modelPrices(double price) {
        String priceStr = String.valueOf(price);
        return new ModelPrices()
                .setMin(priceStr)
                .setMax(priceStr)
                .setAvg(priceStr)
                .setCurrency("RUR");
    }

    public static OfferPrices offerPrices(double price) {
        return new OfferPrices()
                .setCurrency("RUR")
                .setValue(String.valueOf(price))
                .setDiscount(
                        new Discount()
                                .setPercent(0)
                                .setOldMin(String.valueOf(price))
                );
    }

    public static Promo promo(String type, String description) {
        return new Promo()
                .setKey(UUID.randomUUID().toString())
                .setType(type)
                .setDescription(description);
    }

    private static HttpRequest httpRequest(Color color, long regionId, String place) {
        return get("http://int-report.vs.market.yandex.net:17151/yandsearch")
            .param("rgb", color.name())
            .param("place", place)
            .param("rids", String.valueOf(regionId));
    }

    private final JsonSerializer jsonSerializer;
    private final HttpEnvironment httpEnvironment;

    public ReportTestHelper(JsonSerializer jsonSerializer, HttpEnvironment httpEnvironment) {
        this.jsonSerializer = jsonSerializer;
        this.httpEnvironment = httpEnvironment;
    }

    public void prepareModel(Color color, long regionId, ReportModel model) {
        httpEnvironment.when(
                httpRequest(color, regionId, "modelinfo")
                        .param("hyperid", model.getId())
        )
        .then(
                ResponseBuilder.newBuilder()
                        .body(responseBody(model))
                        .build()
        );
    }

    public void prepareNotExistingModel(Color color, long regionId, long modelId) {
        httpEnvironment.when(
                httpRequest(color, regionId, "modelinfo")
                        .param("hyperid", String.valueOf(modelId))
        )
        .then(
                ResponseBuilder.newBuilder()
                        .body(responseBody())
                        .build()
        );
    }

    public void prepareModelOffers(Color color, long regionId, long modelId, Offer... offers) {
        httpEnvironment.when(
                httpRequest(color, regionId, "productoffers")
                        .param("hyperid", String.valueOf(modelId))
        )
        .then(
                ResponseBuilder.newBuilder()
                        .body(responseBody(offers))
                        .build()
        );
    }

    public void prepareOffers(Color color, long regionId, Offer... offers) {
        String offerIds = Stream.of(offers)
                .map(ReportEntity::getId)
                .collect(Collectors.joining(","));

        httpEnvironment.when(
                httpRequest(color, regionId, "offerinfo")
                        .param("offerid", offerIds)
                        .param("regset", "2")
        )
                .then(
                        ResponseBuilder.newBuilder()
                                .body(responseBody(offers))
                                .build()
                );
    }

    private String responseBody(ReportEntity... items) {
        List<JSONObject> jsonObjects = Stream.of(items)
                .map(jsonSerializer::writeObjectAsString)
                .map(str -> {
                    try {
                        return new JSONObject(str);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        try {
            return new JSONObject()
                    // Не упрощать! В актуальной версии json какой-то странный баг из-за которого
                    // значение result сериализуется как строка
                    .put("search", new JSONObject().put("results", new JSONArray(jsonObjects)))
                    .toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
