package ru.yandex.market.checkout.util.report;

import java.util.List;
import java.util.Map;

import ru.yandex.market.checkout.util.report.generators.ReportGenerator;
import ru.yandex.market.common.report.model.MarketReportPlace;

public class CommonReportResponseGenerator<T> extends AbstractReportResponseGenerator<T> {

    public CommonReportResponseGenerator() {
        super();
    }

    public void setGeneratorsMap(Map<MarketReportPlace, List<ReportGenerator<T>>> generatorsMap) {
        this.generatorsMap = generatorsMap;
    }
}
