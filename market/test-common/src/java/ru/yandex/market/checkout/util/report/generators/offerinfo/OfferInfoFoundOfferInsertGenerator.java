package ru.yandex.market.checkout.util.report.generators.offerinfo;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;
import ru.yandex.market.common.report.model.FoundOffer;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.MarkupData;
import ru.yandex.market.common.report.model.OfferSeller;
import ru.yandex.market.common.report.parser.json.OfferInfoMarketReportJsonParserSettings;

public class OfferInfoFoundOfferInsertGenerator extends AbstractJsonGenerator<OfferInfoGeneratorParameters> {

    private final OfferInfoMarketReportJsonParserSettings settings;

    public OfferInfoFoundOfferInsertGenerator() {
        this(new OfferInfoMarketReportJsonParserSettings());
    }

    public OfferInfoFoundOfferInsertGenerator(OfferInfoMarketReportJsonParserSettings settings) {
        this.settings = settings;
    }

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/offerBlockStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, OfferInfoGeneratorParameters parameters) {
        return patchInternal(object, parameters);
    }

    private JsonObject patchInternal(JsonObject object, OfferInfoGeneratorParameters parameters) {
        List<FoundOffer> foundOffers = parameters.getFoundOffers();
        if (CollectionUtils.isEmpty(foundOffers)) {
            return object;
        }

        for (FoundOffer foundOffer : foundOffers) {
            JsonObject offerJson = generateOfferInfo(parameters.getShopId(), foundOffer);
            addJsonPropertyValue(object, "search.results", offerJson);
        }

        return object;
    }

    private JsonObject generateOfferInfo(long shopId, FoundOffer offer) {
        JsonObject jsonObject = loadJson();

        setJsonPropertyValue(jsonObject, settings.getShopId(), shopId);
        // Основано на используемых полях в ReportDataFetcher.
        setJsonPropertyValue(jsonObject, settings.getFeedId(), offer.getFeedId());
        setJsonPropertyValue(jsonObject, settings.getOfferId(), offer.getShopOfferId());
        setJsonPropertyValue(jsonObject, settings.getSellerCurrency(), offer.getShopCurrency());
        setJsonPropertyValue(jsonObject, settings.getInStock(), offer.getOnStock());
        setJsonPropertyValue(jsonObject, settings.getDelivery(), offer.getDelivery());
        setJsonPropertyValue(jsonObject, settings.getHasPickup(), offer.getSelfDelivery());
        setJsonPropertyValue(jsonObject, settings.getGlobal(), offer.isGlobal());
        setJsonPropertyValue(jsonObject, settings.getSellerPrice(), offer.getShopPrice());
        setJsonPropertyValue(jsonObject, settings.getWareMd5(), offer.getWareMd5());
        setJsonPropertyValue(jsonObject, settings.getShopSku(), offer.getShopSku());
        setJsonPropertyValue(jsonObject, settings.getSku(), offer.getSku());
        setJsonPropertyValue(jsonObject, settings.getSupplierId(), offer.getSupplierId());
        setJsonPropertyValue(jsonObject, settings.getCpa(), offer.getCpa());
        setJsonPropertyValue(jsonObject, settings.getIsDownloadable(), offer.isDownloadable());
        setJsonPropertyValue(jsonObject, settings.getSubscriptionName(), offer.getSubscriptionName());
        setJsonPropertyValue(jsonObject, settings.getVendorId(), offer.getVendorId());
        setJsonPropertyValue(jsonObject, settings.getOfferSeller(), generateOfferSellerJson(offer.getOfferSeller()));
        removePropertyValue(jsonObject, settings.getDeliveryOptions());
        if (CollectionUtils.isNotEmpty(offer.getLocalDelivery())) {
            for (LocalDeliveryOption ldo : offer.getLocalDelivery()) {
                addJsonPropertyValue(jsonObject, settings.getDeliveryOptions(), generateDeliveryOption(ldo));
            }
        }
        setJsonPropertyValue(jsonObject, settings.getRawParams(), offer.getRawParams());

        return jsonObject;
    }

    private JsonObject generateOfferSellerJson(OfferSeller offerSeller) {
        if (offerSeller == null) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, settings.getOfferSellerComment(), offerSeller.getComment());
        setJsonPropertyValue(jsonObject, settings.getOfferSellerCurrency(), offerSeller.getCurrency());
        setJsonPropertyValue(jsonObject, settings.getOfferSellerPrice(), offerSeller.getPrice());
        setJsonPropertyValue(jsonObject, settings.getOfferSellerMarkupData(),
                generateMarkupData(offerSeller.getMarkupData()));
        return jsonObject;
    }

    private JsonObject generateMarkupData(MarkupData markupData) {
        JsonObject jsonObject = new JsonObject();
        setJsonPropertyValue(jsonObject, settings.getOfferSellerMarkupDataPartnerPrice(), markupData.getPartnerPrice());
        setJsonPropertyValue(jsonObject, settings.getOfferSellerMarkupDataUpdateTime(), markupData.getUpdateTime());
        JsonObject coefficientsJson = new JsonObject();
        markupData.getCoefficients().forEach((k, v) -> setJsonPropertyValue(coefficientsJson, k, v));
        setJsonPropertyValue(jsonObject, settings.getOfferSellerPrice(), coefficientsJson);
        return jsonObject;
    }

    private JsonObject generateDeliveryOption(LocalDeliveryOption localDeliveryOption) {
        JsonObject jsonObject = new JsonObject();

        setJsonPropertyValue(jsonObject, settings.getDeliveryOptionPriceValue(), localDeliveryOption.getCost());
        setJsonPropertyValue(jsonObject, settings.getDeliveryOptionPriceCurrency(), localDeliveryOption.getCurrency());
        if (localDeliveryOption.getDayFrom() == null || localDeliveryOption.getDayFrom() != -1) {
            setJsonPropertyValue(jsonObject, settings.getDeliveryOptionDayFrom(), localDeliveryOption.getDayFrom());
        }
        setJsonPropertyValue(jsonObject, settings.getDeliveryOptionPartnerType(), localDeliveryOption.getPartnerType());
        setJsonPropertyValue(jsonObject, settings.getDeliveryOptionDayTo(), localDeliveryOption.getDayTo());

        if (localDeliveryOption.getOrderBefore() != null) {
            setJsonPropertyValue(jsonObject, settings.getDeliveryOptionOrderBefore(),
                    localDeliveryOption.getOrderBefore());
        }

        if (CollectionUtils.isNotEmpty(localDeliveryOption.getPaymentMethods())) {
            JsonArray jsonArray = new JsonArray();
            localDeliveryOption.getPaymentMethods().forEach(jsonArray::add);

            // TODO: check me
            setJsonPropertyValue(jsonObject, settings.getDeliveryOptionPaymentMethods(), jsonArray);
        }

        return jsonObject;
    }
}
