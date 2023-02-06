package ru.yandex.market.core.shops;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.config.DevIntegrationTest;
import ru.yandex.market.mbi.util.db.DbUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Для вьюхи shops_web.v_yt_exp_shop_agency_history.
 * Проверяем правильность привязки магазина к агентству.
 */
@Disabled
class VShopAgencyHistoryIntegrationTest extends DevIntegrationTest {
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Europe/Moscow");
    private static final Instant DEFAULT_FROM_DATE = LocalDate.of(2018, Month.AUGUST, 14)
            .atStartOfDay(DEFAULT_ZONE_ID).toInstant();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(DEFAULT_ZONE_ID);


    @Autowired
    private TransactionTemplate transactionTemplate;


    /**
     * простой случай магаз с привязкой одного агентства всегда, до истории
     */
    @Test
    void oneShopOneAgencyTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 210_000_001L;
                long campId = 310_000_001L;
                long clientId = 410_000_001L;
                long agencyId = 510_000_001L;
                createDs(dsId);
                createCampaign(dsId, campId, clientId, "2018-05-10");
                createAgency(agencyId, clientId);
                createAgencyHistory(dsId, campId, agencyId, clientId, "2018-08-14");

                ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(DEFAULT_ZONE_ID);
                //e.g. 2018-09-01
                Instant firstDayOfMonth = today.withDayOfMonth(1).toInstant();
                //e.g. 2018-09-02
                Instant secondDayOfMonth = today.withDayOfMonth(2).toInstant();

                DsAgency dsAgency_1_9 = getDsAgency(dsId, FORMATTER.format(firstDayOfMonth));
                DsAgency dsAgency_2_9 = getDsAgency(dsId, FORMATTER.format(secondDayOfMonth));

                clean(dsId, campId, agencyId);

                DsAgency expected19 = new DsAgency(dsId, campId, agencyId, firstDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected19, dsAgency_1_9);

                DsAgency expected29 = new DsAgency(dsId, campId, agencyId, secondDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected29, dsAgency_2_9);
            }
        });
    }

    /**
     * простой случай магаз с привязкой одного агентства всегда, но создан после начала записи истории
     */
    @Test
    void oneShopOneAgencyCreationTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 210_000_002L;
                long campId = 310_000_002L;
                long clientId = 410_000_002L;
                long agencyId = 510_000_002L;
                createDs(dsId);

                ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(DEFAULT_ZONE_ID);
                //e.g. 2018-09-01
                Instant firstDayOfMonth = today.withDayOfMonth(1).toInstant();
                //e.g. 2018-09-02
                Instant secondDayOfMonth = today.withDayOfMonth(2).toInstant();
                //e.g. 2018-08-30
                Instant previousMonth = today.minusDays(32).toInstant();

                createCampaign(dsId, campId, clientId, FORMATTER.format(firstDayOfMonth));
                createAgency(agencyId, clientId);
                createAgencyHistory(dsId, campId, agencyId, clientId, FORMATTER.format(firstDayOfMonth));

                DsAgency dsAgency30_08 = getDsAgency(dsId, FORMATTER.format(previousMonth));
                DsAgency dsAgency29 = getDsAgency(dsId, FORMATTER.format(secondDayOfMonth));

                clean(dsId, campId, agencyId);
                //ничего не должно быть до создания кампании
                assertNull(dsAgency30_08);

                //запись с датой создания
                DsAgency expected29 = new DsAgency(dsId, campId, agencyId,
                        secondDayOfMonth,
                        firstDayOfMonth
                );
                assertEquals(expected29, dsAgency29);
            }
        });
    }

    /**
     * магаз создан до начала записи истории, поменяли агентство после записи
     */
    @Test
    void oneShopTwoAgencyCreationTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 210_000_003L;
                long campId = 310_000_003L;
                long campId2 = 310_000_004L;
                long clientId = 410_000_003L;
                long clientId2 = 410_000_004L;
                long agencyId = 510_000_003L;
                long agencyId2 = 510_000_004L;
                createDs(dsId);

                ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(DEFAULT_ZONE_ID);
                //e.g. 2018-09-02
                Instant secondDayOfMonth = today.withDayOfMonth(2).toInstant();
                //e.g. 2018-09-03
                Instant thirdDayOfMonth = today.withDayOfMonth(3).toInstant();
                String thirdDayOfMonthStr = FORMATTER.format(thirdDayOfMonth);
                //e.g. 2018-09-04
                Instant fourthDayOfMonth = today.withDayOfMonth(4).toInstant();

                //вставляем кампанию с датой закрытия
                jdbcTemplate.update("insert into market_billing.campaign_info (campaign_id,datasource_id,billing_type,start_date, end_date, client_id) " +
                        " values (?,?, 1, TO_DATE('2018-08-01', 'yyyy-mm-dd'), TO_DATE(?, 'yyyy-mm-dd'), ?)", campId, dsId, thirdDayOfMonthStr, clientId);
                createAgency(agencyId, clientId);
                createAgencyHistory(dsId, campId, agencyId, clientId, "2018-08-14");


                createCampaign(dsId, campId2, clientId2, thirdDayOfMonthStr);
                createAgency(agencyId2, clientId2);
                createAgencyHistory(dsId, campId2, agencyId2, clientId2, thirdDayOfMonthStr);

                DsAgency dsAgency02_09 = getDsAgency(dsId, FORMATTER.format(secondDayOfMonth));
                DsAgency dsAgency03_09 = getDsAgency(dsId, thirdDayOfMonthStr);
                DsAgency dsAgency04_09 = getDsAgency(dsId, FORMATTER.format(fourthDayOfMonth));
                cleanCampAgency(campId, agencyId);
                cleanCampAgency(campId2, agencyId2);
                cleanDS(dsId);

                //первая кампания и агентство
                DsAgency expected02_09 = new DsAgency(dsId, campId, agencyId, secondDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected02_09, dsAgency02_09);

                //все еще первая кампания и агентство, хотя агентство днем поменяли
                DsAgency expected03_09 = new DsAgency(dsId, campId, agencyId, thirdDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected03_09, dsAgency03_09);

                //вторая кампания и агентство
                DsAgency expected04_09 = new DsAgency(dsId, campId2, agencyId2, fourthDayOfMonth, thirdDayOfMonth);
                assertEquals(expected04_09, dsAgency04_09);
            }
        });
    }

    /**
     * магаз создан до начала записи истории, закрыли кампанию
     */
    @Test
    void oneShopOneAgencyClosedCampaignTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 210_000_005L;
                long campId = 310_000_005L;
                long clientId = 410_000_005L;
                long agencyId = 510_000_005L;
                createDs(dsId);

                ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(DEFAULT_ZONE_ID);
                //e.g. 2018-09-02
                Instant secondDayOfMonth = today.withDayOfMonth(2).toInstant();
                //e.g. 2018-09-03
                Instant thirdDayOfMonth = today.withDayOfMonth(3).toInstant();
                String thirdDayOfMonthStr = FORMATTER.format(thirdDayOfMonth);
                //e.g. 2018-09-04
                Instant fourthDayOfMonth = today.withDayOfMonth(4).toInstant();

                //вставляем кампанию с датой закрытия
                jdbcTemplate.update("insert into market_billing.campaign_info (campaign_id,datasource_id,billing_type,start_date, end_date, client_id) " +
                        " values (?,?, 1, TO_DATE('2018-08-01', 'yyyy-mm-dd'), TO_DATE(?, 'yyyy-mm-dd'), ?)", campId, dsId, thirdDayOfMonthStr, clientId);
                createAgency(agencyId, clientId);
                createAgencyHistory(dsId, campId, agencyId, clientId, "2018-08-14");

                DsAgency dsAgency02_09 = getDsAgency(dsId, FORMATTER.format(secondDayOfMonth));
                DsAgency dsAgency03_09 = getDsAgency(dsId, thirdDayOfMonthStr);
                DsAgency dsAgency04_09 = getDsAgency(dsId, FORMATTER.format(fourthDayOfMonth));
                cleanCampAgency(campId, agencyId);
                cleanDS(dsId);

                //первая кампания и агентство
                DsAgency expected02_09 = new DsAgency(dsId, campId, agencyId,
                        secondDayOfMonth
                        , DEFAULT_FROM_DATE);
                assertEquals(expected02_09, dsAgency02_09);


                //все еще первая кампания и агентство, хотя агентство днем поменяли
                DsAgency expected03_09 = new DsAgency(dsId, campId, agencyId,
                        thirdDayOfMonth
                        , DEFAULT_FROM_DATE);
                assertEquals(expected03_09, dsAgency03_09);

                //после закрытия не должно быть записи
                assertNull(dsAgency04_09);
            }
        });
    }


    /**
     * магаз создан до начала записи истории, поменяли агентство после записи между большая щель
     */
    @Test
    void oneShopTwoAgencyClosedBetweenTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 210_000_006L;
                long campId = 310_000_006L;
                long campId2 = 310_000_007L;
                long clientId = 410_000_006L;
                long clientId2 = 410_000_007L;
                long agencyId = 510_000_006L;
                long agencyId2 = 510_000_007L;
                createDs(dsId);

                ZonedDateTime today = LocalDate.now().atStartOfDay().atZone(DEFAULT_ZONE_ID);
                //e.g. 2018-09-02
                Instant secondDayOfMonth = today.withDayOfMonth(2).toInstant();
                //e.g. 2018-09-03
                Instant thirdDayOfMonth = today.withDayOfMonth(3).toInstant();
                String thirdDayOfMonthStr = FORMATTER.format(thirdDayOfMonth);
                //e.g. 2018-09-04
                Instant fourthDayOfMonth = today.withDayOfMonth(4).toInstant();
                //e.g. 2018-09-05
                Instant fifthDayOfMonth = today.withDayOfMonth(5).toInstant();
                String fifthDayOfMonthStr = FORMATTER.format(fifthDayOfMonth);
                //e.g. 2018-09-06
                Instant sixthDayOfMonth = today.withDayOfMonth(6).toInstant();

                //вставляем кампанию с датой закрытия
                jdbcTemplate.update("insert into market_billing.campaign_info (campaign_id,datasource_id,billing_type,start_date, end_date, client_id) " +
                        " values (?,?, 1, TO_DATE('2018-08-01', 'yyyy-mm-dd'), TO_DATE(?, 'yyyy-mm-dd'), ?)", campId, dsId, thirdDayOfMonthStr, clientId);
                createAgency(agencyId, clientId);
                createAgencyHistory(dsId, campId, agencyId, clientId, "2018-08-14");


                createCampaign(dsId, campId2, clientId2, fifthDayOfMonthStr);
                createAgency(agencyId2, clientId2);
                createAgencyHistory(dsId, campId2, agencyId2, clientId2, fifthDayOfMonthStr);

                DsAgency dsAgency02_09 = getDsAgency(dsId, FORMATTER.format(secondDayOfMonth));
                DsAgency dsAgency03_09 = getDsAgency(dsId, thirdDayOfMonthStr);
                DsAgency dsAgency04_09 = getDsAgency(dsId, FORMATTER.format(fourthDayOfMonth));
                DsAgency dsAgency06_09 = getDsAgency(dsId, FORMATTER.format(sixthDayOfMonth));
                cleanCampAgency(campId, agencyId);
                cleanCampAgency(campId2, agencyId2);
                cleanDS(dsId);

                //первая кампания и агентство
                DsAgency expected02_09 = new DsAgency(dsId, campId, agencyId, secondDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected02_09, dsAgency02_09);


                //все еще первая кампания и агентство, хотя агентство днем поменяли
                DsAgency expected03_09 = new DsAgency(dsId, campId, agencyId, thirdDayOfMonth, DEFAULT_FROM_DATE);
                assertEquals(expected03_09, dsAgency03_09);

                //после закрытия не должно быть записи
                assertNull(dsAgency04_09);

                //вторая кампания и агентство
                DsAgency expected06_09 = new DsAgency(dsId, campId2, agencyId2, sixthDayOfMonth, fifthDayOfMonth);
                assertEquals(expected06_09, dsAgency06_09);
            }
        });
    }


    @Nullable
    private DsAgency getDsAgency(long dsId, String dateCheck) {
        try {
            return jdbcTemplate.queryForObject("" +
                            "select dt, from_time, shop_id, campaign_id, agency_id" +
                            " from shops_web.v_yt_exp_shop_agency_history" +
                            " where dt = TO_DATE('" + dateCheck + "', 'yyyy-mm-dd')" +
                            " and shop_id = ?" +
                            " offset 0 rows fetch next 1 rows only",
                    (rs, rowNum) ->
                            new DsAgency(
                                    (rs.getLong("shop_id")),
                                    (rs.getLong("campaign_id")),
                                    (rs.getLong("agency_id")),
                                    DbUtil.getInstantColumn(rs, "dt"),
                                    DbUtil.getInstantColumn(rs, "from_time")
                            )
                    , dsId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private void createDs(long dsId) {
        jdbcTemplate.update("insert into shops_web.partner (id, type) values (?, 'SHOP')", dsId);
        jdbcTemplate.update("insert into shops_web.datasource (id,name,manager_id,comments) values (?, 'VShopAgencyHistoryIntegrationTest', -2, 'For VShopAgencyHistoryIntegrationTest')", dsId);
    }

    private void createCampaign(long dsId, long campaignId, long clientId, String dateFrom) {
        jdbcTemplate.update("insert into market_billing.campaign_info (campaign_id,datasource_id,billing_type,start_date,client_id) " +
                " values (?,?, 1, TO_DATE('" + dateFrom + "', 'yyyy-mm-dd'), ?)", campaignId, dsId, clientId);
    }

    private void createAgency(long agencyId, long clientId) {
        jdbcTemplate.update("insert into market_billing.agency (id, name, email, manager_id) " +
                "  values (?, 'VShopAgencyHistoryIntegrationTest', 'test@test.ru', -2)", agencyId);
        jdbcTemplate.update("insert into market_billing.agency_clients (agency_client_id, client_id) " +
                "values (?, ?) ", agencyId, clientId);
    }

    private void createAgencyHistory(long dsId, long campId, long agencyId, long clientId, String dateFrom) {
        jdbcTemplate.update(
                "insert " +
                        "  into shops_web.shop_agency_history (" +
                        "shop_id , " +
                        "campaign_id , " +
                        "shop_client_id, " +
                        "agency_id, from_time " +
                        "  ) values (?, ?, ?, ?, TO_DATE('" + dateFrom + "', 'yyyy-mm-dd'))", dsId, campId, clientId, agencyId);
    }


    private void clean(long datasourceId, long campaignId, long agencyId) {
        cleanCampAgency(campaignId, agencyId);
        cleanDS(datasourceId);
    }

    private void cleanDS(long datasourceId) {
        jdbcTemplate.update("delete from shops_web.datasource where id = " + datasourceId);
        jdbcTemplate.update("delete from shops_web.partner where id = " + datasourceId);
    }

    private void cleanCampAgency(long campaignId, long agencyId) {
        jdbcTemplate.update("delete from shops_web.shop_agency_history where campaign_id = " + campaignId);
        jdbcTemplate.update("delete from market_billing.campaign_info where campaign_id = " + campaignId);

        jdbcTemplate.update("delete from market_billing.agency_clients where agency_client_id = " + agencyId);
        jdbcTemplate.update("delete from market_billing.agency where id = " + agencyId);

    }

    private class DsAgency {
        long datasourceId;
        long campaignId;
        long agencyId;
        Instant dt;
        Instant fromTime;

        DsAgency(long datasourceId, long campaignId, long agencyId, Instant dt, Instant fromTime) {
            this.datasourceId = datasourceId;
            this.campaignId = campaignId;
            this.agencyId = agencyId;
            this.dt = dt;
            this.fromTime = fromTime;
        }

        @Override
        public String toString() {
            return "DsAgency{" +
                    "datasourceId=" + datasourceId +
                    ", campaignId=" + campaignId +
                    ", agencyId=" + agencyId +
                    ", dt=" + dt +
                    ", fromTime=" + fromTime +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DsAgency dsAgency = (DsAgency) o;
            return datasourceId == dsAgency.datasourceId &&
                    campaignId == dsAgency.campaignId &&
                    agencyId == dsAgency.agencyId &&
                    Objects.equals(dt, dsAgency.dt) &&
                    Objects.equals(fromTime, dsAgency.fromTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(datasourceId, campaignId, agencyId, dt, fromTime);
        }
    }
}
