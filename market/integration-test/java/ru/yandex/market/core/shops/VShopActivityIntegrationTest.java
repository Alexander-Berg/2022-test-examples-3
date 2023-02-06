package ru.yandex.market.core.shops;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.core.config.DevIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тест для shops_web.v_shop_activity_yt_export, проверяет правильность определения активности магазина за условное вчера.
 * Db unit тестом покрыть не получается.
 */
class VShopActivityIntegrationTest extends DevIntegrationTest {

    private static final long CLIENT_ID = 100_000_001L;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.of("Europe/Moscow"));

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void closedCampaignTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_001L;
                long currentCampId = 300_000_001L;
                createDsAndPartner(currentDsId);
                jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID,BILLING_TYPE,START_DATE,CLIENT_ID, END_DATE) " +
                        " values (?, ?, 1, sysdate-3, ?, sysdate - 2)", currentCampId, currentDsId, CLIENT_ID);
                checkNotActiveShop(currentDsId, currentCampId);
            }
        });
    }

    @Test
    void createdAfterDateCampaignTest() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_002L;
                long currentCampId = 300_000_002L;
                createDsAndPartner(currentDsId);
                jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID,BILLING_TYPE,START_DATE,CLIENT_ID) " +
                        " values (?, ?, 1, sysdate, ?)", currentCampId, currentDsId, CLIENT_ID);
                checkNotActiveShop(currentDsId, currentCampId);
            }
        });
    }

    private void checkNotActiveShop(long datasourceId, long campaignId) {
        List<Long> activeShops = jdbcTemplate.queryForList("select datasource_id from shops_web.v_yt_exp_shop_activity where dt = trunc(sysdate -1) and datasource_id = ?", Long.class, datasourceId);

        cleanDb(datasourceId, campaignId);
        assertThat(activeShops).isEmpty();
    }

    private void insertPartnerAndCampaign(long currentDsId,
                                          long currentCampaignId,
                                          int beforeDays) {
        createDsAndPartner(currentDsId);
        jdbcTemplate.update("insert into market_billing.campaign_info (CAMPAIGN_ID,DATASOURCE_ID,BILLING_TYPE,START_DATE,CLIENT_ID) " +
                " values (?,?, 1, sysdate-?, ?)", currentCampaignId, currentDsId, beforeDays, CLIENT_ID);

    }

    @Test
    void testSimpleActiveCampaignWithoutCutoffs() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_003L;
                long currentCampaignId = 300_000_003L;
                long currentCutoffId = 400_000_030L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-5), trunc(sysdate-4))", currentCutoffId, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //gap: a b
    //катоф с 8 до 12 и 16 до 18
    //должен быть активным
    @Test
    void testGapABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_004L;
                long currentCampaignId = 300_000_004L;
                long currentCutoffId = 400_000_040L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 8/24), trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 16/24, trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    private void addCutoff(long currentCutoffId, long currentDsId, int from, int to) {
        jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                " values (?,?, 3,( trunc(sysdate-1)+?/24), trunc(sysdate-1)+?/24)", currentCutoffId, currentDsId, from, to);

    }

    //gap: a b
    //катоф с 1 до 15 и 16 до 23
    //не должен быть активным
    @Test
    void testGapABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_005L;
                long currentCampaignId = 300_000_005L;
                long currentCutoffId = 400_000_050L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-2), trunc(sysdate-1))", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 1/24, trunc(sysdate-1)+15/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 16/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate), trunc(sysdate)+1/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //gap: a b
    //катоф с 23 предыдущего дня от проверяемого до 12 и 16 до 03 следующего
    //должен быть активным
    @Test
    void testGapABActiveFromPrevDay() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_006L;
                long currentCampaignId = 300_000_006L;
                long currentCutoffId = 400_000_060L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)- 1/24), trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 16/24, trunc(sysdate)+3/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //gap: a b
    //катоф с 23 предыдущего дня от проверяемого до 15 и 16 до 03 следующего
    //не должен быть активным
    @Test
    void testGapABNotActiveFromPrevDay() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_007L;
                long currentCampaignId = 300_000_007L;
                long currentCutoffId = 400_000_070L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)- 1/24), trunc(sysdate-1)+15/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 16/24, trunc(sysdate)+3/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //gap: a b
    //катоф с 00 до 10 и 18 до 23
    //должен быть активным
    @Test
    void testOneGapABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {

                long currentDsId = 200_000_008L;
                long currentCampaignId = 300_000_008L;
                long currentCutoffId = 400_000_080L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 8/24), trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 16/24, trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //gap: a b
    //катоф с 1 до 5, c 6 до 13, с 14 до 17, и 18 до 23
    //не должен быть активным
    @Test
    // @Disabled
    void testOneGapABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_009L;
                long currentCampaignId = 300_000_009L;
                long currentCutoffId = 400_000_090L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                addCutoff(currentCutoffId++, currentDsId, 1, 5);
                addCutoff(currentCutoffId++, currentDsId, 6, 13);
                addCutoff(currentCutoffId++, currentDsId, 14, 17);
                addCutoff(currentCutoffId++, currentDsId, 18, 23);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //meets: a b
    //катоф с 1  до 15 и 15 до 23
    //не должен быть активным
    @Test
    void testMeetsABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_011L;
                long currentCampaignId = 300_000_011L;
                long currentCutoffId = 400_000_110L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+15/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 15/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //meets: a b
    //катоф с 1  до 15 и 15 до 19
    //должен быть активным
    @Test
    void testMeetsABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_014L;
                long currentCampaignId = 300_000_014L;
                long currentCutoffId = 400_000_140L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+15/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 15/24, trunc(sysdate-1)+19/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //overlaps: a b
    //катоф с 1  до 17 и 15 до 23
    //не должен быть активным
    @Test
    void testOverlapsABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {

                long currentDsId = 200_000_012L;
                long currentCampaignId = 300_000_012L;
                long currentCutoffId = 400_000_120L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+17/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 15/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //overlaps: a b
    //катоф с 1 до 10 и с 8 до 12, и с 17 до 20 и 18 до 23
    //должен быть активным
    @Test
    void testOverlapsABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {

                long currentDsId = 200_000_013L;
                long currentCampaignId = 300_000_013L;
                long currentCutoffId = 400_000_130L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-10)+ 1/24), trunc(sysdate-2)+23/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 17/24), trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 18/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //finished by: a b
    //катоф с 1 до 10 и с 8 до 10, и с 17 до 24 и 18 до 24
    //должен быть активным
    @Test
    void testFinishedBySeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_015L;
                long currentCampaignId = 300_000_015L;
                long currentCutoffId = 400_000_150L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+10/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 17/24), trunc(sysdate-1)+24/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 18/24, trunc(sysdate-1)+24/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //finished by: a b
    //катоф с 1 до 20 и с 8 до 20, и с 17 до 20
    //должен быть активным
    @Test
    void testFinishedByOneABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_016L;
                long currentCampaignId = 300_000_016L;
                long currentCutoffId = 400_000_160L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 17/24), trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //finished by: a b
    //катоф с 3 до 23 и с 6 до 23, и с 14 до 23
    //не должен быть активным
    @Test
    void testFinishedByOneABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {

                long currentDsId = 200_000_017L;
                long currentCampaignId = 300_000_017L;
                long currentCutoffId = 400_000_170L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 3/24), trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 6/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 14/24), trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //Contains: a b
    //катоф с 2 до 22 и с 6 до 20, и с 4 до 16
    //не должен быть активным
    @Test
    void testContainsSeveralABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_018L;
                long currentCampaignId = 300_000_018L;
                long currentCutoffId = 400_000_180L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 2/24), trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 6/24, trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 4/24), trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //Contains: a b
    //катоф с 2 до 19 и с 6 до 12, и с 4 до 16
    //должен быть активным
    @Test
    void testContainsSeveralABEndActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_019L;
                long currentCampaignId = 300_000_019L;
                long currentCutoffId = 400_000_190L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 2/24), trunc(sysdate-1)+19/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 6/24, trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 4/24), trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //Contains: a b
    //катоф дня до
    //катоф с 5 до 22 и с 6 до 12, и с 4 до 16
    //должен быть активным
    @Test
    void testContainsSeveralABStartActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_020L;
                long currentCampaignId = 300_000_020L;
                long currentCutoffId = 400_000_200L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-2)+ 5/24), trunc(sysdate-2)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 5/24), trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 6/24, trunc(sysdate-1)+12/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 7/24), trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //starts: a b
    //катоф с 22 предыдущего до 10, и с 22 до 8, и с 15 до 23
    //должен быть активным
    @Test
    void testStartsSeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_021L;
                long currentCampaignId = 300_000_021L;
                long currentCutoffId = 400_000_210L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-2)+ 22/24), trunc(sysdate-1)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 15/24), trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 22/24, trunc(sysdate-1)+8/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //starts: a b
    //катоф с 22 предыдущего до 10, и с 22 предыдущего до проверяемого 23
    //не должен быть активным
    @Test
    void testStartsSeveralABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_022L;
                long currentCampaignId = 300_000_022L;
                long currentCutoffId = 400_000_220L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-2)+ 22/24), trunc(sysdate-1)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-2)+ 22/24, trunc(sysdate-1)+23/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //equals: a b
    //закрытый давно катоф
    //катоф с 2 до 22, и с 2 до 22
    //не должен быть активным
    @Test
    void testEqualsSeveralABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_023L;
                long currentCampaignId = 300_000_023L;
                long currentCutoffId = 400_000_230L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 2/24), trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 2/24, trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //equals: a b
    //закрытый давно катоф
    //катоф с 14 до 18, и с 14 до 18
    //должен быть активным
    @Test
    void testEqualsSeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_024L;
                long currentCampaignId = 300_000_024L;
                long currentCutoffId = 400_000_240L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 14/24), trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 14/24, trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //started by: a b
    //закрытый давно катоф
    //катоф с 14 до 18, и с 14 до 16
    //должен быть активным
    @Test
    void testStartedBySeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_025L;
                long currentCampaignId = 300_000_025L;
                long currentCutoffId = 400_000_250L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 14/24), trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 14/24, trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //started by: a b
    //закрытый давно катоф
    //катоф с 14 пердыдущего дня до 18, и с 14 пердыдущего дня до 16
    //должен быть активным
    @Test
    void testStartedByBeforeSeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_027L;
                long currentCampaignId = 300_000_027L;
                long currentCutoffId = 400_000_270L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-2)+ 14/24), trunc(sysdate-1)+18/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-2)+ 14/24, trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //started by: a b
    //закрытый давно катоф
    //катоф с 1 до 2 след дня, и с 1 до 16
    //не должен быть активным
    @Test
    void testStartedBySeveralABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_026L;
                long currentCampaignId = 300_000_026L;
                long currentCutoffId = 400_000_260L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate)+2/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 1/24, trunc(sysdate-1)+16/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //open_cutoff
    //не должен быть активным
    @Test
    void testOpenCutoffABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_028L;
                long currentCampaignId = 300_000_028L;
                long currentCutoffId = 400_000_280L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                        " values (?,?, 3, (trunc(sysdate-20)+ 22/24))", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //open_cutoff из будущего
    // должен быть активным
    @Test
    void testOpenCutoffABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_029L;
                long currentCampaignId = 300_000_029L;
                long currentCutoffId = 400_000_290L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);

                jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                        " values (?,?, 3,( trunc(sysdate)+ 22/24))", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //finishes: a b
    //закрытый давно катоф
    //катоф с 1 до 22, и с 8 до 22
    //не должен быть активным
    @Test
    void testFinishesSeveralABNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_030L;
                long currentCampaignId = 300_000_030L;
                long currentCutoffId = 400_000_300L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //finishes: a b
    //закрытый давно катоф
    //катоф с 1 до 20, и с 8 до 20
    //должен быть активным
    @Test
    void testFinishesSeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_031L;
                long currentCampaignId = 300_000_031L;
                long currentCutoffId = 400_000_310L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 1/24), trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+20/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //finishes: a b
    //закрытый давно катоф
    //катоф с 6 до 22, и с 8 до 22
    //должен быть активным
    @Test
    void testFinishesStartSeveralABActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_032L;
                long currentCampaignId = 300_000_032L;
                long currentCutoffId = 400_000_320L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 6/24), trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3, trunc(sysdate-1)+ 8/24, trunc(sysdate-1)+22/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //Выключается на ночь по расписанию
    //катоф с 20 до 9 утра
    //должен быть активным
    @Test
    void testScheduleCutoffActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_033L;
                long currentCampaignId = 300_000_033L;
                long currentCutoffId = 400_000_330L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-2)+ 20/24), trunc(sysdate-1)+9/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 20/24), trunc(sysdate)+9/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                        " values (?,?, 3, trunc(sysdate)+ 20/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //Выключается несколько раз днем по чуть-чуть
    //должен быть активным
    @Test
    void testCutoffDuringDayActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_034L;
                long currentCampaignId = 300_000_034L;
                long currentCutoffId = 400_000_340L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 6/24), trunc(sysdate-1)+9/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 15/24), trunc(sysdate)+16/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                        " values (?,?, 3, trunc(sysdate)+ 20/24)", currentCutoffId++, currentDsId);
                checkActiveShop(currentDsId, currentCampaignId);
            }
        });
    }

    //Выключается несколько раз днем по чуть-чуть, но с маленькими интервалами
    //не должен быть активным
    @Test
    void testCutoffDuringDayNotActive() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_035L;
                long currentCampaignId = 300_000_035L;
                long currentCutoffId = 400_000_350L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 3);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-20)+ 22/24), trunc(sysdate-19)+10/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 2/24), trunc(sysdate-1)+4/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 5/24), trunc(sysdate)+16/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                        " values (?,?, 3,( trunc(sysdate-1)+ 17/24), trunc(sysdate)+18/24)", currentCutoffId++, currentDsId);
                jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                        " values (?,?, 3, trunc(sysdate)+ 20/24)", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //Реальный случай
    // проверяем, когда у нас есть несколько окрытых катофов давным давно
    // и потом есть закрытие катофа внутри периода открытого
    @Test
    void testRealCaseOpenCutoff() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long currentDsId = 200_000_036L;
                long currentCampaignId = 300_000_036L;
                long currentCutoffId = 400_000_360L;
                insertPartnerAndCampaign(currentDsId,
                        currentCampaignId, 18 * 365);
                addOpenCutoff(1, "2000-12-25 11:32:25", currentCutoffId++, currentDsId);
                addOpenCutoff(2, "2007-04-27 01:11:58", currentCutoffId++, currentDsId);
                addOpenCutoff(3, "2007-08-14 20:44:08", currentCutoffId++, currentDsId);
                addOpenCutoff(4, "2007-11-24 16:26:43", currentCutoffId++, currentDsId);
                addClosedCutoff(5, "2006-04-26 12:10:38", "2007-04-27 01:15:10", currentCutoffId++, currentDsId);
                addClosedCutoff(6, "2007-12-13 16:36:00", "2011-03-21 16:10:11", currentCutoffId++, currentDsId);
                addClosedCutoff(7, "2008-04-09 14:06:24", "2011-03-21 16:10:23", currentCutoffId++, currentDsId);
                addClosedCutoff(8, "2011-02-14 17:21:33", "2011-03-21 16:32:57", currentCutoffId++, currentDsId);
                checkNotActiveShop(currentDsId, currentCampaignId);
            }
        });
    }


    //Проверяем, чтобы при открытом катофе не было записи о магазине в активных
    @Disabled("Тест работает 350-400 секунд, все остальные тесты суммарно укладываются в 100 секунд")
    @Test
    void dontIncludeOpenedCutoff() {

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsIdPast = 200_000_037L;
                long dsIdFuture = 200_000_038L;
                long dsIdToday = 200_000_039L;
                long currentCutoffId = 400_000_370L;

                insertPartnerAndCampaign(dsIdPast, 300_000_037L, 10 * 365);
                insertPartnerAndCampaign(dsIdFuture, 300_000_038L, -1 * 1000/*лет*/ * 365);
                insertPartnerAndCampaign(dsIdToday, 300_000_039L, 0);


                addOpenCutoff(1, "2018-08-25 11:32:25", currentCutoffId++, dsIdPast);
                addOpenCutoff(2, "3007-04-27 01:11:58", currentCutoffId++, dsIdFuture);

                String date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date());
                addOpenCutoff(3, date, currentCutoffId++, dsIdToday);

                List<Integer> disabledShopsCount = jdbcTemplate.queryForList("" +
                                " select distinct vsa.datasource_id from shops_web.v_yt_exp_shop_activity vsa  " +
                                " left join shops_web.open_cutoff oc on oc.datasource_id = vsa.datasource_id " +
                                " where dt = trunc(sysdate -1) " +
                                " and vsa.datasource_id in (?, ?, ?) " +
                                " and oc.from_time <= trunc(sysdate -1) ",
                        new Object[]{dsIdPast, dsIdFuture, dsIdToday},
                        Integer.class);

                cleanDb(200_000_037L, 300_000_037L);
                cleanDb(200_000_038L, 300_000_038L);
                cleanDb(200_000_039L, 300_000_039L);

                assertThat(disabledShopsCount).isEmpty();
            }
        });
    }

    //Проверяем, что когда перевалили на другой день, то предыдущий не будет считаться активным, если не прошло 4 часа
    @Test
    void checkNextDaySwitchActivity() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 200_000_040L;
                long campId = 300_000_040L;
                long currentCutoffId = 400_000_400L;

                insertPartnerAndCampaign(dsId, campId, 5 * 365);

                //e.g. 2018-09-10
                Instant today = Instant.now();
                //e.g. 2018-09-09
                String todayMinus1 = getFormattedDate(today, 1);
                //e.g. 2018-09-03
                String todayMinus6 = getFormattedDate(today, 6);
                //e.g. 2018-08-31
                String todayMinus10 = getFormattedDate(today, 10);
                //e.g. 2018-08-16
                String todayMinus24 = getFormattedDate(today, 24);

                addClosedCutoff(17, todayMinus1 + " 11:38:01", todayMinus1 + " 12:38:02", currentCutoffId++, dsId);
                addClosedCutoff(5, todayMinus10 + " 18:02:10", todayMinus6 + " 22:07:42", currentCutoffId++, dsId);
                addClosedCutoff(5, todayMinus24 + " 08:15:07", todayMinus24 + " 11:49:57", currentCutoffId++, dsId);
                checkDayActivity(dsId, campId);
            }
        });
    }

    //Проверяем, что когда перевалили на другой день, то предыдущий не будет считаться активным, если не прошло 4 часа
    //и у магаза есть открытый катоф потом
    @Test
    void checkNextDaySwitchActivityWithCurrentOpenCutoff() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(@Nonnull TransactionStatus status) {
                long dsId = 200_000_041L;
                long campId = 300_000_041L;
                long currentCutoffId = 400_000_410L;

                insertPartnerAndCampaign(dsId, campId, 5 * 365);

                //e.g. 2018-09-10
                Instant currentDay = Instant.now();
                //e.g. 2018-09-09
                String todayMinus1 = getFormattedDate(currentDay, 1);
                //e.g. 2018-09-03
                String todayMinus6 = getFormattedDate(currentDay, 6);
                //e.g. 2018-08-31
                String todayMinus9 = getFormattedDate(currentDay, 9);
                //e.g. 2018-08-09
                String monthAgo = getFormattedDate(currentDay, 30);
                //e.g. 2018-07-09
                String twoMonthAgo = getFormattedDate(currentDay, 60);

                addOpenCutoff(17, todayMinus1 + " 21:55:50", currentCutoffId++, dsId);
                addClosedCutoff(3, monthAgo + " 01:00:08",
                        todayMinus6 + " 22:12:07", currentCutoffId++, dsId);
                addClosedCutoff(6, twoMonthAgo + " 11:11:03",
                        todayMinus9 + " 13:08:29", currentCutoffId++, dsId);
                checkDayActivity(dsId, campId);
            }
        });
    }

    private void checkDayActivity(long dsId, long campId) {
        //e.g. 2018-09-10
        Instant currentDay = Instant.now();

        //e.g. 2018-09-02
        String todayMinus7 = getFormattedDate(currentDay, 7);
        List<Long> notActiveShop02 = getDsIdShopActivity(dsId, todayMinus7);

        //e.g. 2018-09-03
        String todayMinus6 = getFormattedDate(currentDay, 6);
        List<Long> notActiveShop03 = getDsIdShopActivity(dsId, todayMinus6);

        //e.g. 2018-09-04
        String todayMinus5 = getFormattedDate(currentDay, 5);
        List<Long> activeShop04 = getDsIdShopActivity(dsId, todayMinus5);

        //e.g. 2018-09-09
        String todayMinus1 = getFormattedDate(currentDay, 1);
        List<Long> activeShop09 = getDsIdShopActivity(dsId, todayMinus1);

        cleanDb(dsId, campId);
        assertThat(notActiveShop02).isEmpty();
        assertThat(notActiveShop03).isEmpty();

        assertThat(activeShop04).containsExactly(dsId);
        assertThat(activeShop09).containsExactly(dsId);
    }

    private String getFormattedDate(Instant current, int numberOfDaysBeforeToday) {
        return FORMATTER.format(current.minus(Duration.ofDays(numberOfDaysBeforeToday)));
    }

    private List<Long> getDsIdShopActivity(long dsId, String checkDate) {
        return jdbcTemplate.queryForList("select datasource_id from shops_web.v_yt_exp_shop_activity " +
                "where dt =TO_DATE(?, 'yyyy-mm-dd') and datasource_id = ?", Long.class, checkDate, dsId);
    }

    private void addOpenCutoff(int type, String date, long currentCutoffId, long currentDsId) {
        jdbcTemplate.update("insert into shops_web.open_cutoff (id, datasource_id, type, from_time)" +
                " values (?,?, ?, TO_DATE('" + date + "', 'yyyy-mm-dd hh24:mi:ss') )", currentCutoffId, currentDsId, type);
    }

    private void addClosedCutoff(int type, String dateFrom, String dateTo, long currentCutoffId, long currentDsId) {
        jdbcTemplate.update("insert into shops_web.closed_cutoff (id, datasource_id, type, from_time, to_time )" +
                " values (?,?, ?,TO_DATE('" + dateFrom + "', 'yyyy-mm-dd hh24:mi:ss'),TO_DATE('" + dateTo + "', 'yyyy-mm-dd hh24:mi:ss'))", currentCutoffId++, currentDsId, type);
    }

    private void checkActiveShop(long currentDsId, long currentCampaignId) {
        List<Long> activeShops = jdbcTemplate.queryForList("select datasource_id from shops_web.v_yt_exp_shop_activity  where dt = trunc(sysdate -1) and datasource_id = ?", Long.class, currentDsId);

        cleanDb(currentDsId, currentCampaignId);
        assertThat(activeShops).containsExactly(currentDsId);
    }

    private void createDsAndPartner(long currentDsId) {
        jdbcTemplate.update("insert into shops_web.PARTNER (ID, TYPE) values (" + (currentDsId) + ", 'SHOP')");
        jdbcTemplate.update("insert into shops_web.DATASOURCE (ID,NAME,MANAGER_ID,COMMENTS) values (" + currentDsId + ", 'VShopActivityIntegrationTest', -2, 'For VShopActivityIntegrationTest')");

    }

    private void cleanDb(long datasourceId, long campaignId) {
        jdbcTemplate.update("delete from shops_web.open_cutoff where datasource_id in " + datasourceId);
        jdbcTemplate.update("delete from shops_web.closed_cutoff where datasource_id in " + datasourceId);

        jdbcTemplate.update("delete from market_billing.campaign_info where campaign_id = " + campaignId);
        jdbcTemplate.update("delete from shops_web.datasource where id in " + datasourceId);
        jdbcTemplate.update("delete from shops_web.partner where id in " + datasourceId);
    }
}
