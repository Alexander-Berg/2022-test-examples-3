package ru.yandex.market.admin.service.dao;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.SerializableID;
import ru.yandex.market.admin.ui.model.shop.UIDatasource;
import ru.yandex.market.admin.ui.service.SortOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

/**
 * Тесты для {@link DatasourceUIDao}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class DatasourceUIDaoTest extends FunctionalTest {

    private static final Date CREATE_DATE = DateTimes.asDate(2020, 1, 1);

    private static final UIDatasource SHOP_1001 = datasource(1001, "business", "shop1001",
            "shop1001domain", CREATE_DATE,
            "cmnt1001", -2, 2001, 4001L);

    private static final UIDatasource SHOP_1002 = datasource(1002, "business", "shop1002",
            "shop1002domain", CREATE_DATE,
            null, 1, 2002, 4001L);

    @Autowired
    private DatasourceUIDao datasourceUIDao;

    @ParameterizedTest
    @MethodSource("testSearchData")
    @DisplayName("Проверка поиска")
    @DbUnitDataSet(before = "DatasourceUIDaoTest.before.csv")
    void testSearch(String name, String searchString, long managerId, SerializableID sortByField,
                    SortOrder sortOrder, final int fromIndex, final int toIndex,
                    List<UIDatasource> expected) {
        List<UIDatasource> actual = datasourceUIDao.searchShops(searchString, managerId,
                sortByField, sortOrder, fromIndex, toIndex);

        Assertions.assertThat(actual)
                .containsExactlyInAnyOrderElementsOf(expected);
    }

    private static Stream<Arguments> testSearchData() {
        return Stream.of(
                Arguments.of(
                        "Поиск по id магазина",
                        "1001",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по id кампании",
                        "2001",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по названию магазина",
                        "shop1001",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по внешнему названию магазина",
                        "shop1001name",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по имени контакта",
                        "name1",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по названию магазина. Подходят оба",
                        "shop10",
                        0,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1001, SHOP_1002)
                ),
                Arguments.of(
                        "Поиск по названию магазина. Подходят оба. Первая страница. Сортировка по id",
                        "shop10",
                        0,
                        UIDatasource.ID,
                        SortOrder.ASC,
                        0,
                        0,
                        List.of(SHOP_1001)
                ),
                Arguments.of(
                        "Поиск по названию магазина. Подходят оба. Вторая страница. Сортировка по id",
                        "shop10",
                        0,
                        UIDatasource.ID,
                        SortOrder.ASC,
                        1,
                        1,
                        List.of(SHOP_1002)
                ),
                Arguments.of(
                        "Поиск по названию магазина. Подходят оба. Первая страница. Сортировка по id. Обратный порядок",
                        "shop10",
                        0,
                        UIDatasource.ID,
                        SortOrder.DESC,
                        0,
                        0,
                        List.of(SHOP_1002)
                ),
                Arguments.of(
                        "Фильтрация по менеджеру",
                        "shop10",
                        1,
                        null,
                        null,
                        -1,
                        -1,
                        List.of(SHOP_1002)
                )
        );
    }

    @SuppressWarnings("checkstyle:parameterNumber")
    private static UIDatasource datasource(long id, String name,
                                           String internalName, String domain,
                                           Date createDate, String comments,
                                           long managerId, long campaignId,
                                           long businessId) {
        UIDatasource result = new UIDatasource();
        result.setField(UIDatasource.ID, id);
        result.setField(UIDatasource.SHOP_NAME, name);
        result.setField(UIDatasource.INTERNAL_NAME, internalName);
        result.setField(UIDatasource.DOMAIN, domain);
        result.setField(UIDatasource.CREATE_DATE, createDate);
        result.setField(UIDatasource.COMMENTS, comments);
        result.setField(UIDatasource.MANAGER_ID, managerId);
        result.setField(UIDatasource.CAMPAIGN_ID, campaignId);
        result.setField(UIDatasource.BUSINESS_ID, businessId);
        return result;
    }
}
