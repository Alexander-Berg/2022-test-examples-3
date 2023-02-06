package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.servant.DataSourceable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.of;

@DbUnitDataSet(before = "ShopWorkflowNewbieCheckerTest.before.csv")
class ShopWorkflowNewbieCheckerTest extends FunctionalTest {

    private final static Long NEWBIE_SUPPLIER_ID = 3L;
    private final static Long NEWBIE_SHOP_ID = 4L;
    private final static Long SUPPLIER_ID = 5L;
    private final static Long SHOP_ID = 6L;
    private final static Long UNKNOWN = 404L;

    private static final Authority DEFAULT_AUTHORITY = new Authority();

    @Autowired
    ShopWorkflowNewbieChecker shopWorkflowNewbieChecker;

    private static Stream<Arguments> data() {
        return Stream.of(
                data(Boolean.TRUE, NEWBIE_SUPPLIER_ID),
                data(Boolean.FALSE, SUPPLIER_ID),
                data(Boolean.TRUE, NEWBIE_SHOP_ID),
                data(Boolean.FALSE, SHOP_ID),
                data(Boolean.FALSE, UNKNOWN)
        );
    }

    private static Arguments data(Boolean expected, long shopId) {
        return of(expected, shopId);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testParamCheck(Boolean expected, Long shopId) {
        final DataSourceable request = new MockPartnerRequest(0, 0, shopId, shopId);
        final boolean actual = shopWorkflowNewbieChecker.checkTyped(request, DEFAULT_AUTHORITY);

        assertThat(actual, equalTo(expected));
    }

}
