package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.servant.DataSourceable;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;


@DbUnitDataSet(before = "VirtualShopCheckerTest.before.csv")
class VirtualShopCheckerTest extends FunctionalTest {

    private VirtualShopChecker virtualShopChecker;

    @Autowired
    private ParamService paramService;

    private static Stream<Arguments> args() {
        return Stream.of(
                Arguments.of(true, new MockPartnerRequest(-1, -1, PartnerId.datasourceId(1000L))),
                Arguments.of(false, new MockPartnerRequest(-1, -1, PartnerId.datasourceId(1001L))),
                Arguments.of(false, new MockPartnerRequest(-1, -1, PartnerId.datasourceId(-1)))
        );

    }

    @BeforeEach
    void init() {
        virtualShopChecker = new VirtualShopChecker(paramService, true);
    }

    @ParameterizedTest
    @MethodSource("args")
    void testCheckTyped(boolean expectedCheckResult, DataSourceable dataSourceable) {
        assertEquals(expectedCheckResult, virtualShopChecker.checkTyped(dataSourceable, null));
    }
}
