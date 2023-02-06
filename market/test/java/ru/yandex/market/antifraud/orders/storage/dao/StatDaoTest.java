package ru.yandex.market.antifraud.orders.storage.dao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.stat.ItemPeriodicCountStat;
import ru.yandex.market.antifraud.orders.storage.entity.stat.PeriodStatValues;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static java.math.RoundingMode.UP;
import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class StatDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private StatDao statDao;

    @Before
    public void init() {
        statDao = new StatDao(jdbcTemplate);
    }

    @Test
    public void getStatForItem() {
        ItemPeriodicCountStat stat1 = randomPeriodicCountStat();
        ItemPeriodicCountStat stat2 = stat1.toBuilder().msku(null).build();
        ItemPeriodicCountStat stat3 = stat2.toBuilder().modelId(null).build();

        ItemPeriodicCountStat stat5 = randomPeriodicCountStat();
        ItemPeriodicCountStat stat6 = stat5.toBuilder().msku(null).build();
        ItemPeriodicCountStat stat7 = stat6.toBuilder().modelId(null).build();

        stat1 = statDao.saveItemPeriodicCountStat(stat1);
        stat2 = statDao.saveItemPeriodicCountStat(stat2);
        stat3 = statDao.saveItemPeriodicCountStat(stat3);
        stat5 = statDao.saveItemPeriodicCountStat(stat5);
        stat6 = statDao.saveItemPeriodicCountStat(stat6);
        stat7 = statDao.saveItemPeriodicCountStat(stat7);

        List<ItemPeriodicCountStat> stats1 = statDao.getStatsForItem(stat1.toItemParams());
        List<ItemPeriodicCountStat> stats2 = statDao.getStatsForItem(stat5.toItemParams());
        assertThat(stats1).containsExactlyInAnyOrder(stat1, stat2, stat3);
        assertThat(stats2).containsExactlyInAnyOrder(stat5, stat6, stat7);
    }

    @Test
    public void getStatForItems() {
        ItemPeriodicCountStat stat1 = randomPeriodicCountStat();
        ItemPeriodicCountStat stat2 = stat1.toBuilder().msku(null).build();
        ItemPeriodicCountStat stat3 = stat2.toBuilder().modelId(null).build();

        ItemPeriodicCountStat stat5 = randomPeriodicCountStat();
        ItemPeriodicCountStat stat6 = stat5.toBuilder().msku(null).build();
        ItemPeriodicCountStat stat7 = stat6.toBuilder().modelId(null).build();

        ItemPeriodicCountStat stat9 = randomPeriodicCountStat();
        ItemPeriodicCountStat stat10 = stat9.toBuilder().msku(null).build();
        ItemPeriodicCountStat stat11 = stat10.toBuilder().modelId(null).build();

        stat1 = statDao.saveItemPeriodicCountStat(stat1);
        stat2 = statDao.saveItemPeriodicCountStat(stat2);
        stat3 = statDao.saveItemPeriodicCountStat(stat3);
        stat5 = statDao.saveItemPeriodicCountStat(stat5);
        stat6 = statDao.saveItemPeriodicCountStat(stat6);
        stat7 = statDao.saveItemPeriodicCountStat(stat7);
        stat9 = statDao.saveItemPeriodicCountStat(stat9);
        stat10 = statDao.saveItemPeriodicCountStat(stat10);
        stat11 = statDao.saveItemPeriodicCountStat(stat11);

        List<ItemPeriodicCountStat> stats = statDao.getStatsForItems(List.of(stat1.toItemParams(), stat5.toItemParams()));
        assertThat(stats).containsExactlyInAnyOrder(stat1, stat2, stat3, stat5, stat6, stat7);
    }

    @Test
    public void checkUserSigma() {
        ItemPeriodicCountStat stat1 = ItemPeriodicCountStat.builder()
                .categoryId(-91)
                .modelId(-93L)
                .msku(-94L)
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(3).setScale(2, UP))
                        .countAvgUser(BigDecimal.valueOf(3).setScale(2, UP))
                        .countSigmaGlue(BigDecimal.valueOf(3).setScale(2, UP))
                        .build())
                .build();
        stat1 = statDao.saveItemPeriodicCountStat(stat1);

        List<ItemPeriodicCountStat> stats = statDao.getStatsForItems(List.of(stat1.toItemParams()));
        assertThat(stats).containsExactlyInAnyOrder(stat1.toBuilder()
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(3).setScale(2, UP))
                        .countAvgUser(BigDecimal.valueOf(3).setScale(2, UP))
                        .countSigmaGlue(BigDecimal.valueOf(3).setScale(2, UP))
                        .countSigmaUser(BigDecimal.valueOf(3).setScale(2, UP))
                        .build())
                .periodStat_7d(PeriodStatValues.builder().build())
                .periodStat_30d(PeriodStatValues.builder().build())
                .build()

        );
    }

    private ItemPeriodicCountStat randomPeriodicCountStat() {
        Random random = new Random();
        return ItemPeriodicCountStat.builder()
                .categoryId(random.nextInt())
                .modelId(random.nextLong())
                .msku(random.nextLong())
                .periodStat_1d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countAvgUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .build())
                .periodStat_7d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countAvgUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .build())
                .periodStat_30d(PeriodStatValues.builder()
                        .countAvgGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countAvgUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaGlue(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .countSigmaUser(BigDecimal.valueOf(random.nextDouble()).setScale(2, UP))
                        .build())
                .build();
    }
}
