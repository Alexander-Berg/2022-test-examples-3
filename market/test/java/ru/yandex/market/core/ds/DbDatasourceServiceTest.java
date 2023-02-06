package ru.yandex.market.core.ds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.geobase.model.Region;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;

/**
 * Тесты {@link DatasourceService}.
 *
 * @author Vadim Lyalin
 */
@DbUnitDataSet(before = "DbDatasourceServiceTest.before.csv")
class DbDatasourceServiceTest extends FunctionalTest {

    @Autowired
    private DbDatasourceService datasourceService;

    @Test
    void testGetExistedDatasourceIds() {
        final List<Long> ids = new ArrayList<>(Collections.nCopies(2000, 1L));
        ids.add(2L);
        ids.add(3L);

        final List<Long> existedDatasourceIds = datasourceService.getExistedDatasourceIds(ids);
        MatcherAssert.assertThat(existedDatasourceIds, contains(1L, 2L));
    }

    @DisplayName("Проверка магазинов на доступность")
    @Test
    void getEnabledShops_listWithElement_filledMap() {
        Map<Long, Boolean> existedDatasourceIds = datasourceService.getEnabledShops(List.of(23L, 25L, 22L, 24L));
        Assertions.assertEquals(3, existedDatasourceIds.size());
        MatcherAssert.assertThat(existedDatasourceIds,
                allOf(hasEntry(23L, false), hasEntry(25L, true), hasEntry(22L, true)));
    }

    @DisplayName("Получить домашний регион-город доставки")
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Регион задан в параметрах,1,65",
            "Дефолтный регион,2,213"
    })
    @DbUnitDataSet(before = "DbDatasourceServiceTest.localRegion.before.csv")
    void testGetLocalDeliveryRegionOrDefault(String name, long partnerId, long regionId) {
        Region actual = datasourceService.getLocalDeliveryRegionOrDefault(partnerId);
        Assertions.assertEquals(regionId, actual.getId());
    }

    @Test
    void smokeSetDatasourceComment() {
        datasourceService.setDatasourceComment(1, "test comment", 1);
    }
}
