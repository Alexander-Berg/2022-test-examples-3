package ru.yandex.market.checkout.util.report.generators;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.util.report.ReportGeneratorParameters;
import ru.yandex.market.common.report.model.json.credit.BnplDenial;
import ru.yandex.market.common.report.model.json.credit.CreditInfo;
import ru.yandex.market.common.report.model.json.credit.CreditOffer;

public class CreditInfoJsonGenerator extends AbstractCheckouterJsonGenerator<ReportGeneratorParameters> {

    private final String defaultJsonFileName;

    public CreditInfoJsonGenerator(String defaultJsonFileName) {
        this.defaultJsonFileName = defaultJsonFileName;
    }

    public CreditInfoJsonGenerator() {
        this("/generators/report/creditInfo.json");
    }

    @Override
    protected String getDefaultJsonFileName() {
        return defaultJsonFileName;
    }

    @Override
    public JsonObject patch(JsonObject object, ReportGeneratorParameters parameters) {
        JsonObject json = loadJson();

        CreditInfo creditInfo = parameters.getCreditInfo();
        if (creditInfo == null) {
            return json;
        }

        if (creditInfo.getGlobalRestrictions() != null) {
            setJsonPropertyValue(
                    json,
                    "results.globalRestrictions.minPrice.value",
                    creditInfo.getGlobalRestrictions().getMinPrice()
            );
        }

        if (creditInfo.getCreditInfoResult() != null) {
            setJsonPropertyValue(
                    json,
                    "results.creditInfo.monthlyPayment.value",
                    creditInfo.getCreditInfoResult().getMonthlyPayment()
            );
        }

        if (creditInfo.getCreditDenial() != null) {
            setJsonPropertyValue(
                    json,
                    "results.creditDenial.reason",
                    creditInfo.getCreditDenial().getReason()
            );

            List<String> blackList = creditInfo.getCreditDenial().getBlackListOffers();
            if (CollectionUtils.isNonEmpty(blackList)) {
                JsonArray jsonElements = new JsonArray();
                blackList.forEach(jsonElements::add);
                setJsonPropertyValue(
                        json,
                        "results.creditDenial.blacklistOffers",
                        jsonElements
                );
            }
        }

        if (CollectionUtils.isNonEmpty(creditInfo.getCreditOffers())) {
            JsonArray offers = new JsonArray();
            for (CreditOffer creditOffer : creditInfo.getCreditOffers()) {
                JsonObject offer = new JsonObject();
                offer.addProperty("entity", creditOffer.getEntity());
                offer.addProperty("hid", creditOffer.getHid());
                offer.addProperty("wareId", creditOffer.getWareId());
                JsonObject yandexBnplInfo = new JsonObject();
                yandexBnplInfo.addProperty("enabled", creditOffer.getYandexBnplInfo().isEnabled());
                BnplDenial bnplDenial = creditOffer.getYandexBnplInfo().getBnplDenial();
                if (bnplDenial != null) {
                    JsonObject bnplDenialJson = new JsonObject();
                    bnplDenialJson.addProperty("reason", bnplDenial.getReason().name());
                    bnplDenialJson.addProperty("threshold", bnplDenial.getThreshold());
                    yandexBnplInfo.add("bnplDenial", bnplDenialJson);
                }
                offer.add("yandexBnplInfo", yandexBnplInfo);
                offers.add(offer);
            }
            setJsonPropertyValue(json, "results.offers", offers);
        }

        return json;
    }
}
