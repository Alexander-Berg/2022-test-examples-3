package ru.yandex.market.shopadminstub.services.report;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.common.report.model.outlet.Outlet;
import ru.yandex.market.common.report.model.outlet.SelfDeliveryRule;
import ru.yandex.market.common.report.parser.json.GeoMarketReportJsonParser;
import ru.yandex.market.common.report.parser.json.GeoMarketReportJsonParserSettings;
import ru.yandex.market.common.report.parser.json.GeoMarketReportStreamingJsonParser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class GeoReportServiceParserTest {

    private GeoMarketReportJsonParser geoParser;
    private GeoMarketReportStreamingJsonParser geoStreamingParser;

    @BeforeEach
    public void setUp() {
        geoParser = new GeoMarketReportJsonParser(new GeoMarketReportJsonParserSettings());
        geoStreamingParser = new GeoMarketReportStreamingJsonParser();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/report/non_empty_geo.json", "/report/empty_geo.json"})
    public void testParsersHaveSameOutput(String jsonPath) throws IOException {
        var inputJsonInputStream = GeoReportServiceParserTest.class.getResourceAsStream(jsonPath);
        var outlets1 = geoParser.parse(inputJsonInputStream);
        var outletMap1 = outlets1.stream()
                .collect(Collectors.toMap(Outlet::getId, Function.identity()));
        inputJsonInputStream = GeoReportServiceParserTest.class.getResourceAsStream(jsonPath);
        var outlets2 = geoStreamingParser.parse(inputJsonInputStream);
        var outletMap2 = outlets2.stream()
                .collect(Collectors.toMap(Outlet::getId, Function.identity()));
        assertThat(outlets1, hasSize(outlets2.size()));
        outletMap1.forEach((key, value) -> {
            assertThat(outletMap2.containsKey(key), is(true));
            assertThat(outletEquals(value, outletMap2.get(key)), is(true));
        });
    }

    private static boolean outletEquals(Outlet a, Outlet b) {
        return a.getId().equals(b.getId())
                && CollectionUtils.isEqualCollection(a.getPaymentMethods(), b.getPaymentMethods())
                && selfDeliveryRuleEquals(a.getSelfDeliveryRule(), b.getSelfDeliveryRule());
    }

    private static boolean selfDeliveryRuleEquals(SelfDeliveryRule a, SelfDeliveryRule b) {
        return a.getDayTo().equals(a.getDayTo())
                && a.getDayFrom().equals(a.getDayFrom())
                && a.getCost().equals(b.getCost());
    }
}
