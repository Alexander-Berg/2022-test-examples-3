package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/MarketModelBidBusinessControllerTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/MarketModelBidBusinessControllerTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class MarketModelBidBusinessControllerTest extends AbstractVendorPartnerFunctionalTest {

    private final Clock clock;

    @Autowired
    public MarketModelBidBusinessControllerTest(Clock clock) {
        this.clock = clock;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBusinessControllerTest/testGetBusinesses/before.csv",
            dataSource = "vendorDataSource"
    )
    void testGetBusinesses() {
        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, 6, 25, 0, 0, 0)));

        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/businesses?uid=100500");
        String expected = getStringResource("/testGetBusinesses/expected.json");

        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBusinessControllerTest/testPutBusinesses/before.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBusinessControllerTest/testPutBusinesses/after.csv",
            dataSource = "vendorDataSource"
    )
    void testPutBusinesses() {

        Mockito.when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2021, 6, 25, 0, 0, 0)));

        String body = getStringResource("/testPutBusinesses/request.json");
        FunctionalTestHelper.put(
                baseUrl + "/vendors/101/modelbids/businesses?uid=100500",
                body
        );

    }
}
