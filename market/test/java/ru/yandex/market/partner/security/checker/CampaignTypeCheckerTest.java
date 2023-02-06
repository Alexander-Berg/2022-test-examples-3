package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * @author stani on 17.07.18.
 */
@DbUnitDataSet(before = "CampaignTypeCheckerTest.before.csv")
class CampaignTypeCheckerTest extends FunctionalTest {

    private final static Long SUPPLIER_CAMPAIGN_ID = 103L;
    private final static Long SHOP_CAMPAIGN_ID = 104L;
    private final static Long UNKNOWN = 404L;

    @Autowired
    CampaignTypeChecker campaignTypeChecker;

    private static Stream<Arguments> data() {
        return Stream.of(
                data(Boolean.TRUE, SUPPLIER_CAMPAIGN_ID, new Authority("test", "SUPPLIER")),
                data(Boolean.TRUE, SHOP_CAMPAIGN_ID, new Authority("test", "SHOP")),
                data(Boolean.FALSE, SUPPLIER_CAMPAIGN_ID, new Authority("test", "SHOP")),
                data(Boolean.FALSE, SHOP_CAMPAIGN_ID, new Authority("test", "SUPPLIER")),
                data(Boolean.TRUE, SUPPLIER_CAMPAIGN_ID, new Authority("test", "-SHOP")),
                data(Boolean.TRUE, SHOP_CAMPAIGN_ID, new Authority("test", "-SUPPLIER")),
                data(Boolean.FALSE, UNKNOWN, new Authority("test", "-SUPPLIER"))
        );
    }

    private static Arguments data(Boolean expected, long shopId, Authority auth) {
        return of(expected, shopId, auth);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testParamCheck(Boolean expected, Long shopId, Authority auth) {
        Assertions.assertEquals(campaignTypeChecker.checkTyped(
                new MockPartnerRequest(0, 0, shopId, shopId), auth), expected);
    }
}
