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

@DbUnitDataSet(before = "shopFeedExistsCheckerTest.csv")
public class ShopFeedExistsCheckerTest extends FunctionalTest {

    @Autowired
    private ShopFeedExistsChecker shopFeedExistsChecker;

    private static final long SHOP_WITH_FEED_CAMPAIGN_ID = 101001;
    private static final long SHOP_WITHOUT_FEED_CAMPAIGN_ID = 101002;
    private static final long SUPPLIER_CAMPAIGN_ID = 101003;

    private static Stream<Arguments> data() {
        return Stream.of(
                data(Boolean.TRUE, SHOP_WITH_FEED_CAMPAIGN_ID, new Authority()),
                data(Boolean.FALSE, SHOP_WITHOUT_FEED_CAMPAIGN_ID, new Authority()),
                data(Boolean.FALSE, SUPPLIER_CAMPAIGN_ID, new Authority())
        );
    }

    private static Arguments data(Boolean expected, long shopId, Authority auth) {
        return of(expected, shopId, auth);
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(Boolean expected, Long shopId, Authority auth) {
        Assertions.assertEquals(shopFeedExistsChecker.checkTyped(
                new MockPartnerRequest(0, 0, shopId, shopId), auth), expected);
    }
}
