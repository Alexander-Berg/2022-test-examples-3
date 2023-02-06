package ru.yandex.market.checkout.carter.report.generator;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;

import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters;
import ru.yandex.market.checkout.carter.report.ReportGeneratorParameters.ReportOffer;
import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;

public class ConsolidateBlockInsertGenerator extends AbstractJsonGenerator<ReportGeneratorParameters> {

    @Override
    protected String getDefaultJsonFileName() {
        return "/files/skuOfferBlock.json";
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        if (parameters.getMskuToOfferMap() == null || parameters.getMskuToOfferMap().isEmpty()) {
            return object;
        }

        Multimap<Long, ReportOffer> responseMskuToOffers = parameters.getMskuToOfferResponseMap().isEmpty() ?
                parameters.getMskuToOfferMap() : parameters.getMskuToOfferResponseMap();

        for (Map.Entry<Long, Collection<ReportOffer>> mskuToOfferId : responseMskuToOffers.asMap().entrySet()) {
            Long msku = mskuToOfferId.getKey();
            addJsonPropertyValue(object, "search.results", generateSkuInfo(msku, mskuToOfferId.getValue()));
        }

        setJsonPropertyValue(object, "search.totalWarehouses", responseMskuToOffers.values()
                .stream()
                .map(ReportOffer::getWarehouseId)
                .distinct()
                .count()
        );
        setJsonPropertyValue(object, "search.replaced", true);
        return object;
    }

    private JsonObject generateSkuInfo(Long msku, Collection<ReportOffer> reportOffers) {
        JsonObject offer = loadJson();
        setJsonPropertyValue(offer, "id", String.valueOf(msku));
        JsonObject baseItem = offer.getAsJsonObject("offers").getAsJsonArray("items").get(0).getAsJsonObject();
        offer.getAsJsonObject("offers").getAsJsonArray("items").remove(0);
        for (ReportOffer reportOffer : reportOffers) {
            JsonObject offerItem = deepCopy(baseItem);
            offerItem.addProperty("wareId", reportOffer.getWareMd5());
            if (reportOffer.getPrice() != null) {
                setJsonPropertyValue(offerItem, "prices.value", reportOffer.getPrice().toString());
                setJsonPropertyValue(offerItem, "prices.currency", "RUR");
                setJsonPropertyValue(offerItem, "prices.rawValue", reportOffer.getPrice().toString());
            }
            setJsonPropertyValue(offerItem, "supplier.warehouseId", reportOffer.getWarehouseId());
            setJsonPropertyValue(offerItem, "sku", String.valueOf(msku));
            offer.getAsJsonObject("offers").getAsJsonArray("items").add(offerItem);
        }
        return offer;
    }
}
