package ru.yandex.market.admin.service.remote;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.testing.UIShopTestingTypeFilter;
import ru.yandex.market.admin.ui.model.testing.UITestedShop;
import ru.yandex.market.admin.ui.service.SortOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "RemoteDatasourceTestingUIServiceTest/shopTesting.before.csv")
class RemoteDatasourceTestingUIServiceTest extends FunctionalTest {

    @Autowired
    private RemoteDatasourceTestingUIService tested;

    private static Stream<Arguments> getShopTesting() {
        return Stream.of(
                Arguments.of("Для CPC магазина", 2L),
                Arguments.of("Для DSBS магазина", 1L)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getShopTesting")
    void testGetShopTesting(String testName, long shopId) {
        assertThat(tested.getShopInTesting(shopId)).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getShopTesting")
    void testGetShopsTesting(String testName, long shopId) {
        assertThat(tested.getShopsInTesting(
                UIShopTestingTypeFilter.NEW,
                new long[]{shopId},
                UITestedShop.DURATION,
                SortOrder.ASC,
                0, 1
        )).isNotNull();
    }

    @Test
    @DbUnitDataSet(after = "RemoteDatasourceTestingUIServiceTest/clearShopFatalCancelled.after.csv")
    void clearShopFatalCancelled() {
        long shopId = 3L;
        tested.clearShopFatalCancelled(shopId);
        var testedShop = tested.getShopsInTesting(
                null,
                new long[]{shopId},
                null,
                null,
                0, 1
        );
        assertThat(testedShop).hasSize(1);
    }
}
