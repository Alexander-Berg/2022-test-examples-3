package ru.yandex.market.checkout.util.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.checkout.util.report.generators.ReportGenerator;
import ru.yandex.market.common.report.model.MarketReportPlace;

public class AbstractReportResponseGenerator<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractReportResponseGenerator.class);
    protected Map<MarketReportPlace, List<ReportGenerator<T>>> generatorsMap = new LinkedHashMap<>();

    public String generate(MarketReportPlace place, T parameters) {
        List<ReportGenerator<T>> reportGenerators = generatorsMap.get(place);

        if (CollectionUtils.isEmpty(reportGenerators)) {
            return null;
        }

        JsonObject object = new JsonObject();
        for (ReportGenerator<T> reportGenerator : reportGenerators) {
            object = reportGenerator.patch(object, parameters);
        }
        log.debug("Report response {}", object.toString());
        return object.toString();
    }
}
