package ru.yandex.market.checkout.util.report.generators.geo;

import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;

import ru.yandex.market.checkout.util.report.generators.AbstractJsonGenerator;
import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.common.report.model.outlet.SelfDeliveryRule;
import ru.yandex.market.common.report.parser.json.GeoMarketReportJsonParserSettings;

public class GeoFoundOutletInsertGenerator extends AbstractJsonGenerator<GeoGeneratorParameters> {

    private final GeoMarketReportJsonParserSettings settings;

    public GeoFoundOutletInsertGenerator() {
        this(new GeoMarketReportJsonParserSettings());
    }

    public GeoFoundOutletInsertGenerator(GeoMarketReportJsonParserSettings settings) {
        this.settings = settings;
    }

    @Override
    protected String getDefaultJsonFileName() {
        return "/generators/report/geoBlockStub.json";
    }

    @Override
    public JsonObject patch(JsonObject object, GeoGeneratorParameters parameters) {
        if (CollectionUtils.isEmpty(parameters.getOutlets())) {
            return object;
        }

        for (Outlet outlet : parameters.getOutlets()) {
            JsonObject outletJson = generateOutlet(outlet);
            addJsonPropertyValue(object, settings.getSearchResults(), outletJson);
        }

        return object;
    }


    private JsonObject generateOutlet(Outlet outlet) {
        JsonObject jsonObject = loadJson();

        setJsonPropertyValue(jsonObject, settings.getOutletId(), outlet.getId());
        generateSelfDeliveryRule(jsonObject, outlet.getSelfDeliveryRule());

        if (CollectionUtils.isNotEmpty(outlet.getPaymentMethods())) {
            outlet.getPaymentMethods().forEach(paymentMethod -> {
                addJsonPropertyValue(jsonObject, settings.getPaymentMethods(), paymentMethod);
            });
        }

        return jsonObject;
    }

    private void generateSelfDeliveryRule(JsonObject jsonObject, SelfDeliveryRule selfDeliveryRule) {
        if (selfDeliveryRule == null) {
            return;
        }

        if (selfDeliveryRule.getDayFrom() != null) {
            setJsonPropertyValue(jsonObject, settings.getSelfDeliveryRuleDayFrom(), selfDeliveryRule.getDayFrom());
        }

        if (selfDeliveryRule.getDayTo() != null) {
            setJsonPropertyValue(jsonObject, settings.getSelfDeliveryRuleDayTo(), selfDeliveryRule.getDayTo());
        }

        if (selfDeliveryRule.getCost() != null) {
            setJsonPropertyValue(jsonObject, settings.getSelfDeliveryRuleCost(), selfDeliveryRule.getCost());
        }
    }
}
