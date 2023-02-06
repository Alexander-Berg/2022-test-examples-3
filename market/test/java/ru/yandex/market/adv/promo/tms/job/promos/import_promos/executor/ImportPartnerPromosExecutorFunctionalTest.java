package ru.yandex.market.adv.promo.tms.job.promos.import_promos.executor;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.adv.promo.FunctionalTest;
import ru.yandex.market.adv.promo.tms.job.promos.import_promos.dao.PartnerParticipatedPromosYTDao;
import ru.yandex.market.adv.promo.service.partner_promo.model.PartnerParticipatedPromo;
import ru.yandex.market.adv.promo.tms.job.promos.import_promos.service.PartnerParticipatedPromosYTHelperService;
import ru.yandex.market.adv.promo.tms.yt.YtCluster;
import ru.yandex.market.adv.promo.tms.yt.YtTemplate;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.when;

public class ImportPartnerPromosExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private ImportPartnerPromosExecutor importPartnerPromosExecutor;
    @Autowired
    private PartnerParticipatedPromosYTDao partnerParticipatedPromosYTDao;
    @Autowired
    private PartnerParticipatedPromosYTHelperService ytHelperService;
    @Autowired
    private YtTemplate idxOffersYtTemplate;
    @Value("#{${idx.offers.table.by.yt.host}}")
    private Map<String, String> idxOffersTablePathByYtHost;

    /**
     * Тест проверяет корректность работы джобы, если не появилось новой таблицы генлогов с момента
     * последнего импорта в mbi.
     */
    @Test
    @DbUnitDataSet(
            before = "ImportPartnerPromosExecutorFunctionalTest/importPartnerPromosNoExportTest/before.csv",
            after = "ImportPartnerPromosExecutorFunctionalTest/importPartnerPromosNoExportTest/after.csv"
    )
    void importPartnerPromosNoExportTest() {
        YtCluster cluster = idxOffersYtTemplate.getClusters()[0];
        YPath path = YPath.simple(idxOffersTablePathByYtHost.get(cluster.getName()) + "recent");
        when(ytHelperService.getNameOfTableForLink(cluster, path)).
                thenReturn("20200701_2211");

        importPartnerPromosExecutor.doJob(null);
    }

    /**
     * Тест проверяет корректность работы джобы, если появилась новая таблица генлогов
     * последнего импорта в mbi.
     */
    @Test
    @DbUnitDataSet(
            before = "ImportPartnerPromosExecutorFunctionalTest/importPartnerPromosExportTest/before.csv",
            after = "ImportPartnerPromosExecutorFunctionalTest/importPartnerPromosExportTest/after.csv"
    )
    void importPartnerPromosExportTest() {
        YtCluster cluster = idxOffersYtTemplate.getClusters()[0];
        String idxOffersFolderPath = idxOffersTablePathByYtHost.get(cluster.getName());

        YPath path = YPath.simple(idxOffersFolderPath + "recent");
        when(ytHelperService.getNameOfTableForLink(cluster, path)).thenReturn("20200701_2337");

        when(partnerParticipatedPromosYTDao.getCurrentPartnerPromos(
                cluster.getSimpleName(),
                idxOffersFolderPath + "20200701_2337"
        )).thenReturn(
                ImmutableSet.of(
                        new PartnerParticipatedPromo(222L, "#677", 1),
                        new PartnerParticipatedPromo(444L, "#677", 0),
                        new PartnerParticipatedPromo(555L, "#800", 2),
                        new PartnerParticipatedPromo(222L, "#123", 5)
                )
        );

        importPartnerPromosExecutor.doJob(null);
    }
}
