package ru.yandex.market.checkout.carter.report.generator;

import java.util.Collection;
import java.util.Map;

import com.google.gson.JsonObject;

import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters.ReportOffer;
import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;

public class SkuOfferBlockInsertGenerator extends AbstractJsonGenerator<ReportGeneratorParameters> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/files/skuOfferBlock.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        if (parameters.getMskuToOfferMap() == null || parameters.getMskuToOfferMap().isEmpty()) {
            return object;
        }

        for (Map.Entry<Long, Collection<ReportOffer>> mskuToOfferId :
                parameters.getMskuToOfferMap().asMap().entrySet()) {
            Long msku = mskuToOfferId.getKey();
            addJsonPropertyValue(object, "search.results", generateSkuInfo(msku, mskuToOfferId.getValue()));
        }

        setJsonPropertyValue(object, "search.totalWarehouses", parameters.getMskuToOfferMap().values().stream()
                .map(ReportOffer::getWarehouseId)
                .distinct()
                .count()
        );
        return object;
    }

    private JsonObject generateSkuInfo(Long msku, Collection<ReportOffer> reportOffers) {
        JsonObject offer = loadJson();
        setJsonPropertyValue(offer, "id", String.valueOf(msku));

        JsonObject baseItem = offer.getAsJsonObject("offers").getAsJsonArray("items")
                .get(0).getAsJsonObject();
        offer.getAsJsonObject("offers").getAsJsonArray("items").remove(0);
        for (ReportOffer reportOffer : reportOffers) {
            JsonObject jsonObject = deepCopy(baseItem);

            jsonObject.addProperty("wareId", reportOffer.getWareMd5());
            setJsonPropertyValue(jsonObject, "supplier.warehouseId", reportOffer.getWarehouseId());
            setJsonPropertyValue(jsonObject, "sku", String.valueOf(msku));
            offer.getAsJsonObject("offers").getAsJsonArray("items").add(jsonObject);
        }

        return offer;
    }
}
