package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

@DbUnitDataSet(before = "HasSingleOwnWarehouse.before.csv")
public class HasSingleOwnWarehouseCheckerTest extends FunctionalTest {
    private static final int USER_ID = 123;
    private static final String CHECKER_NAME = "HAS_SINGLE_OWN_WAREHOUSE";

    @Autowired
    private HasSingleOwnWarehouseChecker hasSingleOwnWarehouseChecker;

    @ParameterizedTest
    @MethodSource("testCheckerData")
    void testChecker(final String name, final PartnerId partnerId, final boolean expected) {
        final MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, partnerId);
        final Authority authority = new Authority(CHECKER_NAME, "");
        final boolean actual = hasSingleOwnWarehouseChecker.checkTyped(data, authority);
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                Arguments.of(
                        "НЕ дропшип и НЕ ДБС = false",
                        PartnerId.datasourceId(774),
                        false
                ),
                Arguments.of(
                        "ДБС - один склад",
                        PartnerId.datasourceId(775),
                        true
                ),
                Arguments.of(
                        "Дропшип - 1 склад",
                        PartnerId.datasourceId(664),
                        true
                ),
                Arguments.of(
                        "Дропшип - 2 склада",
                        PartnerId.datasourceId(665),
                        false
                ),
                Arguments.of(
                        "Неизвестный тип партнера",
                        PartnerId.datasourceId(884),
                        false
                )
        );
    }
}
