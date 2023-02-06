package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.YtTableWatchLogRepository;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SpecialOrderRecommendationsCreationLoader;
public class SpecialOrderRecommendationsCreationLoaderTest extends FunctionalTest {

    @Autowired
    YtTableWatchLogRepository ytTableWatchLogRepository;

    @Autowired
    SpecialOrderRecommendationsCreationLoader specialOrderRecommendationsCreationLoader;

    @Autowired
    SqlSessionFactory sqlSessionFactory;

    @Before
    public void mockCurrentDateTime() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 0, 0));
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderRecommendationsCreationLoaderTest.simple.before.csv",
            after = "SpecialOrderRecommendationsCreationLoaderTest.simple.after.csv")
    public void testSimpleDemandCreation() {
        specialOrderRecommendationsCreationLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderRecommendationsCreationLoaderTest.simpleXDOC.before.csv",
            after = "SpecialOrderRecommendationsCreationLoaderTest.simpleXDOC.after.csv")
    public void testSimpleDemandCreationWithXDOCLogParam() {
        specialOrderRecommendationsCreationLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "SpecialOrderRecommendationsCreationLoaderTest.rostov_xdock.before.csv",
            after = "SpecialOrderRecommendationsCreationLoaderTest.rostov_xdock.after.csv")
    public void testRostovCrossDockDemandCreation() {
        specialOrderRecommendationsCreationLoader.load();
    }
}
