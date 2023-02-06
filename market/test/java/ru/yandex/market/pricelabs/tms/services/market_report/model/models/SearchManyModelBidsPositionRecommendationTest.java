package ru.yandex.market.pricelabs.tms.services.market_report.model.models;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.tms.services.market_report.MarketReportException;
import ru.yandex.market.pricelabs.tms.services.market_report.model.JsonErrorResponse;
import ru.yandex.market.pricelabs.tms.services.market_report.model.JsonErrorResponse.Error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class SearchManyModelBidsPositionRecommendationTest {

    @Test
    void testParseError() {
        var withError = Utils.fromJsonResource("tms/services/market_report/model/models/modelbids_error.json",
                SearchManyModelBidsPositionRecommendation.class);

        log.info("Result: {}", withError);

        var expect = new SearchManyModelBidsPositionRecommendation();
        var expectErr = new Error();
        expectErr.setCode("INVALID_USER_CGI");
        expectErr.setMessage("one or more hyperid parameters should be provided");
        expect.setError(expectErr);

        assertEquals(expect, withError);
        assertNotNull(withError);
        assertThrows(MarketReportException.class, () -> JsonErrorResponse.check(withError));

    }
}
