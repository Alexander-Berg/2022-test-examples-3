package ru.yandex.market.checkout.carter.report;

import java.util.Arrays;
import java.util.List;

import ru.yandex.market.checkout.carter.report.generator.ConsolidateBlockInsertGenerator;
import ru.yandex.market.checkout.carter.report.generator.ConsolidateStubGenerator;
import ru.yandex.market.checkout.carter.report.generator.SkuOfferBlockInsertGenerator;
import ru.yandex.market.checkout.carter.report.generator.SkuOffersStubGenerator;
import ru.yandex.market.checkout.util.report.AbstractReportResponseGenerator;
import ru.yandex.market.checkout.util.report.generators.ReportGenerator;
import ru.yandex.market.common.report.model.MarketReportPlace;

public class CarterReportResponseGenerator extends AbstractReportResponseGenerator<ReportGeneratorParameters> {

    public CarterReportResponseGenerator() {
        generatorsMap.put(MarketReportPlace.SKU_OFFERS, skuOfferGenerators());
        generatorsMap.put(MarketReportPlace.CONSOLIDATE, consolidateGenerators());
    }

    private List<ReportGenerator<ReportGeneratorParameters>> consolidateGenerators() {
        return Arrays.asList(
                new ConsolidateStubGenerator(),
                new ConsolidateBlockInsertGenerator()
        );
    }

    private List<ReportGenerator<ReportGeneratorParameters>> skuOfferGenerators() {
        return Arrays.asList(
                new SkuOffersStubGenerator(),
                new SkuOfferBlockInsertGenerator()
        );
    }
}
