package ru.yandex.autotests.market.shop.backend.tms;

import org.junit.Test;

import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.billing.backend.core.dao.entities.jobstate.JobState;
import ru.yandex.autotests.market.billing.backend.data.marketdynamic.MarketDynamicData;
import ru.yandex.autotests.market.shop.backend.core.dao.shop.ShopDao;
import ru.yandex.autotests.market.shop.backend.core.dao.shop.ShopDaoFactory;
import ru.yandex.autotests.market.shop.backend.steps.MarketDynamicReadSteps;
import ru.yandex.autotests.market.shop.backend.steps.TmsSteps;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Title;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 5/4/15
 */
@Aqua.Test(title = "Тест содержимого архива market-dynamic.tar.gz")
@Feature("shopStateReportExecutor")
@Description("Тест проверяет на полное соответствие содержимого файлов архива market-dynamic.tar.gz соответсвующим " +
        "значениям из базы")
public class MarketDynamicReadTest {
    private static final String JOB_NAME = "shopStateReportExecutor";

    static TmsSteps tester = new TmsSteps();
    private final ShopDao shopDao = ShopDaoFactory.getShopDao();
    private static MarketDynamicReadSteps marketDynamicReadSteps = new MarketDynamicReadSteps();

    @Test
    @Title("Проверка выгрузки всех данных из базы в динамик")
    public void testAllData() throws Exception {
        Long gid = shopDao.getLastDynamicGenerationId();
        JobState jobState = tester.getLastFinishedJobState(JOB_NAME);
        MarketDynamicData data = marketDynamicReadSteps.getMarketDynamicData();

        marketDynamicReadSteps.checkOfferFilter(data, gid);
        marketDynamicReadSteps.checkTimeStamp(data, jobState);
    }
}
