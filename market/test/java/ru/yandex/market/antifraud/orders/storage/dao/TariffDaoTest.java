package ru.yandex.market.antifraud.orders.storage.dao;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.ue.Cogs;
import ru.yandex.market.antifraud.orders.storage.entity.ue.Tariff;
import ru.yandex.market.antifraud.orders.storage.entity.ue.UeGlobalParams;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TariffDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private TariffDao tariffDao;

    @Before
    public void init() {
        tariffDao = new TariffDao(jdbcTemplate);
    }

    @Test
    public void findTariffs() {
        Tariff t1 = Tariff.builder()
                .k(123)
                .delServiceId(234L)
                .channel("PO_BOX")
                .fromLocation(1L)
                .toLocation(2L)
                .weightMin(BigDecimal.ZERO)
                .weightMax(BigDecimal.TEN)
                .cost(23)
                .deltaCost(1)
                .build();
        Tariff t2 = t1.toBuilder()
                .fromLocation(2L)
                .toLocation(3L)
                .build();
        Tariff t3 = t1.toBuilder()
                .weightMin(BigDecimal.TEN)
                .weightMax(new BigDecimal("20"))
                .build();
        Tariff t4 = t1.toBuilder()
                .delServiceId(236L)
                .build();
        Tariff t5 = t1.toBuilder()
                .channel("COURIER")
                .build();
        t1 = tariffDao.saveTariff(t1);
        t2 = tariffDao.saveTariff(t2);
        t3 = tariffDao.saveTariff(t3);
        t4 = tariffDao.saveTariff(t4);
        t5 = tariffDao.saveTariff(t5);

        List<Tariff> tariffs = tariffDao.findTariffs(234L, 1L, 2L, "PO_BOX");
        assertThat(tariffs).contains(t1, t3);
        assertThat(tariffs).doesNotContain(t2, t4, t5);
    }

    @Test
    public void findCogs() {
        Cogs c1 = new Cogs(1L, new BigDecimal("100"));
        Cogs c2 = new Cogs(2L, new BigDecimal("200"));
        Cogs c3 = new Cogs(3L, new BigDecimal("300"));

        tariffDao.saveCogs(c1);
        tariffDao.saveCogs(c2);
        tariffDao.saveCogs(c3);

        List<Cogs> cogs = tariffDao.getCogs(List.of(1L, 2L));

        assertThat(cogs).contains(c1, c2);
        assertThat(cogs).doesNotContain(c3);
    }

    @Test
    public void checkGlobalParamsMigration() {
        Optional<UeGlobalParams> params = tariffDao.getLatestGlobalParams();
        assertThat(params).isPresent();
    }

    @Test
    public void saveGlobalParams() {
        var params = tariffDao.saveGlobalParams(UeGlobalParams.builder()
                .fee3p(new BigDecimal("1"))
                .feeFf3p(new BigDecimal("2"))
                .storageRevenue(new BigDecimal("3"))
                .withdrawRevenue(new BigDecimal("4"))
                .ffCost(new BigDecimal("5"))
                .callCenterCost(new BigDecimal("6"))
                .spasiboCost(new BigDecimal("7"))
                .addedAt(Instant.now())
                .build());
        var params2 = tariffDao.getLatestGlobalParams().get();
        assertThat(params).isEqualTo(params2);

    }
}
