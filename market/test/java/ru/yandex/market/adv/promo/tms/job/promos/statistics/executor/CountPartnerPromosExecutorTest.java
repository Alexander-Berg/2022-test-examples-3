package ru.yandex.market.adv.promo.tms.job.promos.statistics.executor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.tms.job.promos.statistics.dao.PartnerPromosYTDao;
import ru.yandex.market.adv.promo.tms.yt.YtCluster;
import ru.yandex.market.adv.promo.tms.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.*;

public class CountPartnerPromosExecutorTest extends FunctionalTest {

    @Autowired
    private CountPartnerPromosExecutor countPartnerPromosExecutor;

    @Autowired
    private YtTemplate promoYtTemplate;

    @Autowired
    private PartnerPromosYTDao partnerPromosYTDao;

    @Value("${yt.idx.datacamp.promo.desc}")
    private String promoDescTablePath;

    /**
     * Тест проверяет корректность работы джобы, если статистика ещё не была заведена
     */
    @Test
    @DbUnitDataSet(
            after = "CountPartnerPromosExecutorTest/countPromos_createStatistic/after.csv"
    )
    void countPromos_createStatistic() {
        createOrUpdateStatistic();
    }


    /**
     * Тест проверяет корректность работы джобы, если статистика уже была заведена
     */
    @Test
    @DbUnitDataSet(
            before = "CountPartnerPromosExecutorTest/countPromos_updateStatistic/before.csv",
            after = "CountPartnerPromosExecutorTest/countPromos_updateStatistic/after.csv"
    )
    void countPromos_updateStatistic() {
        createOrUpdateStatistic();
    }

    private void createOrUpdateStatistic() {
        YtCluster cluster = promoYtTemplate.getClusters()[0];
        when(partnerPromosYTDao.getPromoCount(cluster.getSimpleName(), promoDescTablePath)).
                thenReturn(321);

        countPartnerPromosExecutor.doJob(null);
    }

}
