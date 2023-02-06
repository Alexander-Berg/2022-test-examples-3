package ru.yandex.market.checkout.util.report;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.market.checkout.util.report.generators.ActualDeliveryJsonGenerator;
import ru.yandex.market.checkout.util.report.generators.CreditInfoJsonGenerator;
import ru.yandex.market.checkout.util.report.generators.DeliveryRouteJsonGenerator;
import ru.yandex.market.checkout.util.report.generators.ModelInfoPlaceGenerator;
import ru.yandex.market.checkout.util.report.generators.OfferBlockInsertGenerator;
import ru.yandex.market.checkout.util.report.generators.ReportGenerator;
import ru.yandex.market.checkout.util.report.generators.ShopInfoPlaceGenerator;
import ru.yandex.market.checkout.util.report.generators.offerinfo.OfferInfoPlaceStubGenerator;
import ru.yandex.market.common.report.model.MarketReportPlace;

/**
 * @author Nikolai Iusiumbeli
 * date: 10/07/2017
 */
public class ReportResponseGenerator extends AbstractReportResponseGenerator<ReportGeneratorParameters> {

    public ReportResponseGenerator() {
        generatorsMap.put(MarketReportPlace.OFFER_INFO, offerInfoGenerators());
        generatorsMap.put(MarketReportPlace.SHOP_INFO, shopInfoGenerators());
        generatorsMap.put(MarketReportPlace.MODEL_INFO, modelInfoGenerators());
        generatorsMap.put(MarketReportPlace.ACTUAL_DELIVERY, actualDeliveryJsonGenerators());
        generatorsMap.put(MarketReportPlace.DELIVERY_ROUTE, deliveryRouteJsonGenerators());
        generatorsMap.put(MarketReportPlace.CREDIT_INFO, creditInfoJsonGenerators());
    }

    private List<ReportGenerator<ReportGeneratorParameters>> offerInfoGenerators() {
        return Arrays.asList(
                new OfferInfoPlaceStubGenerator(),
                new OfferBlockInsertGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> shopInfoGenerators() {
        return Collections.singletonList(
                new ShopInfoPlaceGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> modelInfoGenerators() {
        return Collections.singletonList(
                new ModelInfoPlaceGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> actualDeliveryJsonGenerators() {
        return Collections.singletonList(
                new ActualDeliveryJsonGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> deliveryRouteJsonGenerators() {
        return Collections.singletonList(
                new DeliveryRouteJsonGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> creditInfoJsonGenerators() {
        return Collections.singletonList(
                new CreditInfoJsonGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> creditInfoWithoutOffersJsonGenerators() {
        return Collections.singletonList(
                new CreditInfoJsonGenerator("/generators/report/creditInfo_without_offers.json")
        );
    }

    public void defaultCreditInfo() {
        generatorsMap.put(MarketReportPlace.CREDIT_INFO, creditInfoJsonGenerators());
    }

    public void creditInfoWithoutOffers() {
        generatorsMap.put(MarketReportPlace.CREDIT_INFO, creditInfoWithoutOffersJsonGenerators());
    }

    public void creditInfo(String path) {
        generatorsMap.put(MarketReportPlace.CREDIT_INFO, Collections.singletonList(
                new CreditInfoJsonGenerator(path)
        ));
    }
}
