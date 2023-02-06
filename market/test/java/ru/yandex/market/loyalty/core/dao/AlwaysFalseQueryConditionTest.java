package ru.yandex.market.loyalty.core.dao;

import java.util.Collections;

import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.query.Filter;
import ru.yandex.market.loyalty.core.dao.query.Query;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.loyalty.core.dao.coin.CoinDao.COIN_PROPS_TABLE;
import static ru.yandex.market.loyalty.core.dao.query.Filter.and;
import static ru.yandex.market.loyalty.core.dao.query.Filter.not;
import static ru.yandex.market.loyalty.core.dao.query.Filter.or;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public class AlwaysFalseQueryConditionTest extends MarketLoyaltyCoreMockedDbTestBase {
    //Рассматриваю на примере coinDao

    private JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

    public static final CoinDao.DiscountTable DISCOUNT_TABLE = new CoinDao.DiscountTable();
    private static final Query COUNT_DISCOUNTS_BY_PROMO = new Query(
            "SELECT cp.promo_id, COUNT(*) AS cnt" +
                    "  FROM discount AS d" +
                    "       JOIN coin_props AS cp ON d.coin_props_id = cp.id" +
                    " WHERE " + Query.FILTER_PLACEHOLDER +
                    " GROUP BY cp.promo_id");

    private void query(Filter filter) {
        COUNT_DISCOUNTS_BY_PROMO
                .where(filter)
                .query(jdbcTemplate, (rs, i) -> Pair.of(rs.getLong("promo_id"), rs.getLong("cnt")));
    }

    private void nTimeUse(int n) {
        verify(jdbcTemplate, (times(n))).query(anyString(), (Object[]) any(), (RowMapper<Object>) any());
    }


    @Test
    public void shouldNotExecuteQueryIfOnlyOneFalseCondition() {
        Filter filter = COIN_PROPS_TABLE.promoId.in(Collections.emptyList());
        query(filter);
        nTimeUse(0);
    }

    @Test
    public void shouldNotExecuteQueryWithOneFalseConditionUseAnd() {
        Filter filter = COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .and(DISCOUNT_TABLE.uid.eqTo(1L))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(0);
        filter = and(DISCOUNT_TABLE.uid.eqTo(1L),
                DISCOUNT_TABLE.coinPropsId.eqTo(2L),
                COIN_PROPS_TABLE.promoId.in(Collections.emptyList()),
                DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(0);
    }

    @Test
    public void shouldExecuteQueryWithOneFalseConditionUseOr1() {
        Filter filter = COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .or(DISCOUNT_TABLE.uid.eqTo(1L))
                .or(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .or(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(1);
        filter = or(COIN_PROPS_TABLE.promoId
                        .in(Collections.emptyList()),
                DISCOUNT_TABLE.uid.eqTo(1L),
                DISCOUNT_TABLE.coinPropsId.eqTo(2L),
                DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(2);
    }

    @Test
    public void shouldExecuteQueryWithNotCondition() {
        Filter filter = not(COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList()));
        query(filter);
        nTimeUse(1);
        filter = not(COIN_PROPS_TABLE.promoId.in(Collections.emptyList())
                .or(DISCOUNT_TABLE.uid.eqTo(1L))
                .or(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .or(DISCOUNT_TABLE.id.eqTo(3L)));
        query(filter);
        nTimeUse(2);
        filter = not(COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .and(DISCOUNT_TABLE.uid.eqTo(1L))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L)));
        query(filter);
        nTimeUse(3);
    }

    @Test
    public void shouldExecuteQueryWithMixCondition() {
        Filter filter = COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .and(DISCOUNT_TABLE.uid.eqTo(1L))
                .or(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(1);

        filter = COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .or(DISCOUNT_TABLE.uid.eqTo(1L))
                .or(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(2);

        filter = COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .and(DISCOUNT_TABLE.uid.eqTo(1L))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .or(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(3);

        filter = not(COIN_PROPS_TABLE.promoId
                .in(Collections.emptyList())
                .and(DISCOUNT_TABLE.uid.eqTo(1L)))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(4);
    }

    @Test
    public void shouldExecuteQueryWithNestedCondition() {
        Filter filter = and(COIN_PROPS_TABLE.promoId
                        .in(Collections.emptyList()),
                DISCOUNT_TABLE.uid.eqTo(1L),
                or(DISCOUNT_TABLE.certificateToken.eqTo("ctoken"),
                        (DISCOUNT_TABLE.activationToken.eqTo("atoken"))))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L))
                .and(COIN_PROPS_TABLE.promoId.in(Collections.emptyList()));
        query(filter);
        nTimeUse(0);

        filter = and(COIN_PROPS_TABLE.promoId
                        .in(Collections.emptyList()),
                DISCOUNT_TABLE.uid.eqTo(1L),
                DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .or(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(1);

        filter = or(COIN_PROPS_TABLE.promoId
                        .in(Collections.emptyList()),
                DISCOUNT_TABLE.uid.eqTo(1L))
                .and(DISCOUNT_TABLE.coinPropsId.eqTo(2L))
                .and(DISCOUNT_TABLE.id.eqTo(3L));
        query(filter);
        nTimeUse(2);
    }
}
