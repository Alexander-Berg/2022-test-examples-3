package ru.yandex.market.shop;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Функциональный тест на логику работы представления SHOPS_WEB.V_SHOPS_ALIVE.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "shopsAliveTest.before.csv")
class ShopsAliveTest extends FunctionalTest {

    private static final Collection<Long> SHOP_IDS = ImmutableList.of(100L, 200L, 300L, 1603468075L, 1603468508L);

    private static final String GET_ALIVE_SHOPS_QUERY = "" +
            "SELECT DATASOURCE_ID " +
            "FROM SHOPS_WEB.V_SHOPS_ALIVE " +
            "WHERE DATASOURCE_ID IN (:ds_ids)";

    private static final RowMapper<Long> SHOPS_ROW_MAPPER = (rs, i) -> rs.getLong("datasource_id");

    private static final MapSqlParameterSource PARAMETER_SOURCE = new MapSqlParameterSource("ds_ids", SHOP_IDS);

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Магазин жив, если:
     * <ol>
     * <li>Не имеет CPC отключений</li>
     * <li>Имеет запись для фичи {@code FeatureType.MARKETPLACE_SELF_DELIVERY} и не имеет отключений по ней </li>
     * </ol>
     */
    @Test
    @DbUnitDataSet
    void noCutoffs() {
        List<Long> aliveShops = getAliveShops();
        MatcherAssert.assertThat(
                aliveShops,
                Matchers.containsInAnyOrder(100L, 200L, 300L)
        );
    }

    /**
     * Магазин жив, если:
     * <ul>
     * <li>Все отключения по CPA моложе 30 дней</li>
     * <li>Все отключения по CPC моложе 30 дней</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "aliveShops.csv"
    )
    void aliveShops() {
        List<Long> aliveShops = getAliveShops();
        MatcherAssert.assertThat(
                aliveShops,
                Matchers.containsInAnyOrder(100L, 200L, 300L, 1603468075L)
        );
    }

    /**
     * Магазин мертв, если все условия выполняются:
     * <ul>
     * <li>Имеет отключение от CPA и CPC старше 30 дней</li>
     * <li>Имеет отключение для фичи {@code FeatureType.MARKETPLACE_SELF_DELIVERY} старше 30 дней или не имеет
     * записи для фичи</li>
     * </ul>
     */
    @Test
    @DbUnitDataSet(
            before = "deadShop.csv"
    )
    void deadShops() {
        List<Long> aliveShops = getAliveShops();
        MatcherAssert.assertThat(
                aliveShops,
                Matchers.empty()
        );
    }

    private List<Long> getAliveShops() {
        return namedParameterJdbcTemplate.query(
                GET_ALIVE_SHOPS_QUERY,
                PARAMETER_SOURCE,
                SHOPS_ROW_MAPPER);
    }

}
