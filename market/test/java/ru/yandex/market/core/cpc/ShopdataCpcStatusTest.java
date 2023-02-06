package ru.yandex.market.core.cpc;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Тест на логику расчета параметра CPC в выгрузке shops.dat
 *
 * @author fbokovikov
 */
class ShopdataCpcStatusTest extends FunctionalTest {

    private static final String SHOP_CPC_STATUSES_QUERY =
            "select datasource_id, cpc from shops_web.v_shopdata_cpc_status";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DbUnitDataSet(before = "ShopdataCpcStatusTest.before.csv")
    void testCpcStatusValue() {
        List<Pair<Long, String>> shopCpcStatuses = jdbcTemplate.query(SHOP_CPC_STATUSES_QUERY,
                (rs, rowNum) -> {
                    long shopId = rs.getLong("datasource_id");
                    String cpc = rs.getString("cpc");
                    return Pair.of(shopId, cpc);
                });

        //проверяем, что CutoffType.PARTNER_SCHEDULE, а также CutoffType.FINANCE и CutoffType.QUALITY_PINGER
        //не влияет на выгрзуку CPC=REAL
        assertThat(shopCpcStatuses, hasItem(Pair.of(1L, "REAL")));

        //наличие CPC_PARTNER приводит к выгрузке CPC=NO
        assertThat(shopCpcStatuses, hasItem(Pair.of(2L, "NO")));

        //наличие CutoffType.PARTNER_SCHEDULE открытого ранее 8 часов назад не влияет на выгрзуку CPC=REAL
        assertThat(shopCpcStatuses, hasItem(Pair.of(3L, "REAL")));

        //наличие отключения, отличного от CutoffType.FINANCE и CutoffType.QUALITY_PINGER открытого ранее 8 часов назад
        //приводит к выгрузке CPC=SBX
        assertThat(shopCpcStatuses, hasItem(Pair.of(4L, "SBX")));

        //магазин без отключения выгружается с CPC=REAL
        assertThat(shopCpcStatuses, hasItem(Pair.of(5L, "REAL")));

        //CutoffType.FINANCE открытый ранее 72 часов назад приводит к выгрузке CPC=SBX
        assertThat(shopCpcStatuses, hasItem(Pair.of(6L, "SBX")));

        //CutoffType.FINANCE открытый позднее 72 часов назад не приводит к покиданию индекса
        assertThat(shopCpcStatuses, hasItem(Pair.of(7L, "REAL")));

        //CutoffType.QMANAGER_OTHER, открытый ранее 48 часов назад, приводит к выгрузке CPC=SBX
        assertThat(shopCpcStatuses, hasItem(Pair.of(8L, "SBX")));

        //CutoffType.QMANAGER_OTHER + CutoffType.FORTESTING,
        // открытые позднее 48 часов назад, не приводят к покиданию индекса
        assertThat(shopCpcStatuses, hasItem(Pair.of(9L, "REAL")));

        //CutoffType.QUALITY_PINGER, открытый ранее 48 часов назад, приводит к выгрузке CPC=SBX
        assertThat(shopCpcStatuses, hasItem(Pair.of(10L, "SBX")));

        //CutoffType.QUALITY_PINGER, открытый позднее 48 часов назад, не приводит к покиданию индекса
        assertThat(shopCpcStatuses, hasItem(Pair.of(11L, "REAL")));

        //CutoffType.FORTESTING, открытый позднее 48 часов назад, не приводит к покиданию индекса
        assertThat(shopCpcStatuses, hasItem(Pair.of(12L, "REAL")));

        //CutoffType.FORTESTING, открытый позднее 48 часов назад, не пускает в индекс магазин-новичок
        assertThat(shopCpcStatuses, hasItem(Pair.of(13L, "SBX")));

        //CutoffType.YAMANAGER, открытый менее 15 минут назад, не приводит к покиданию из индекса
        assertThat(shopCpcStatuses, hasItem(Pair.of(14L, "REAL")));

        //CutoffType.YAMANAGER, открытый более 15 минут назад, не пускает в индекс
        assertThat(shopCpcStatuses, hasItem(Pair.of(15L, "SBX")));

        //в выгрузке нет лишних записей
        assertEquals(15, shopCpcStatuses.size());
    }

}
