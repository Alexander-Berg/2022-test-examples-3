package ru.yandex.market.core.supplier.alive;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.db.LongRowMapper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

/**
 * Тесты для вьюхи {@code shops_web.v_suppliers_alive}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class SupplierAliveViewTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @ParameterizedTest(name = "{0}")
    @DisplayName("Живость FF поставщика по вьюхе v_suppliers_alive")
    @MethodSource("testFfData")
    @DbUnitDataSet(before = "SupplierAliveViewTest/csv/testFf.before.csv")
    void testFf(String name, long partnerId, List<Long> aliveFeedIds) {
        checkAlive(partnerId, aliveFeedIds);
    }

    private static Stream<Arguments> testFfData() {
        return Stream.of(
                Arguments.of(
                        "ФФ. Фича MARKETPLACE активна. Катоффов нет => жив",
                        1001,
                        List.of(1301L, 1302L, 1303L, 1304L)
                ),
                Arguments.of(
                        "ФФ. Фича MARKETPLACE не активна. Катофф открыт 59 дней назад => жив",
                        1002,
                        List.of(2301L)
                ),
                Arguments.of(
                        "ФФ. Фича MARKETPLACE не активна. Катофф открыт 61 день назад => мертв",
                        1003,
                        List.of()
                ),
                Arguments.of(
                        "ФФ. Фича MARKETPLACE активна. Открыто два катоффа на фичу. Жив. В результате нет дублей",
                        1103,
                        List.of(1313L)
                ),
                Arguments.of(
                        "ФФ. Поставщик мертв, но есть в белом списке => жив",
                        1104,
                        List.of()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Живость CD поставщика по вьюхе v_suppliers_alive")
    @MethodSource("testCdData")
    @DbUnitDataSet(before = {
            "SupplierAliveViewTest/csv/testFf.before.csv",
            "SupplierAliveViewTest/csv/testCd.before.csv"
    })
    void testCd(String name, long partnerId, List<Long> aliveFeedIds) {
        checkAlive(partnerId, aliveFeedIds);
    }

    private static Stream<Arguments> testCdData() {
        return Stream.of(
                Arguments.of(
                        "КД. Фича MARKETPLACE активна. Фича CROSSDOCK активна => жив",
                        1004,
                        List.of(4301L, 4302L)
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE активна. Фича CROSSDOCK не активна. Катофф CROSSDOCK открыт давно => КД мертв, ФФ жив",
                        1005,
                        List.of(5301L)
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE не активна. Фича CROSSDOCK активна. Катофф MARKETPLACE открыт давно => мертв",
                        1006,
                        List.of()
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE не активна. Фича CROSSDOCK не активна. Оба катоффа открыты давно => мертв",
                        1007,
                        List.of()
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Живость DS поставщика по вьюхе v_suppliers_alive")
    @MethodSource("testDsData")
    @DbUnitDataSet(before = "SupplierAliveViewTest/csv/testDs.before.csv")
    void testDs(String name, long partnerId, List<Long> aliveFeedIds) {
        checkAlive(partnerId, aliveFeedIds);
    }

    private static Stream<Arguments> testDsData() {
        return Stream.of(
                Arguments.of(
                        "ДШ. Фича MARKETPLACE активна. Фича DROPSHIP активна => жив",
                        1004,
                        List.of(4302L)
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE активна. Фича DROPSHIP не активна. Катофф DROPSHIP открыт давно => мертв",
                        1005,
                        List.of()
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE не активна. Фича DROPSHIP активна. Катофф MARKETPLACE открыт давно => мертв",
                        1006,
                        List.of()
                ),
                Arguments.of(
                        "КД. Фича MARKETPLACE не активна. Фича DROPSHIP не активна. Оба катоффа открыты давно => мертв",
                        1007,
                        List.of()
                )
        );
    }

    private void checkAlive(long partnerId, List<Long> aliveFeedIds) {
        MapSqlParameterSource params = new MapSqlParameterSource("partnerId", partnerId);
        List<Long> actualFeedIds = namedParameterJdbcTemplate.query(
                "select feed_id from shops_web.v_suppliers_alive where supplier_id = :partnerId",
                params, new LongRowMapper());

        Assertions.assertThat(actualFeedIds)
                .containsExactlyInAnyOrderElementsOf(aliveFeedIds);
    }
}
