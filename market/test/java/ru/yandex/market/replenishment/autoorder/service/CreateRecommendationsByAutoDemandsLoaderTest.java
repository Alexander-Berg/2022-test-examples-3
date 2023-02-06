package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDateTime;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.CreateRecommendationsByAutoDemandsLoader;

public class CreateRecommendationsByAutoDemandsLoaderTest extends FunctionalTest {

    @Autowired
    private CreateRecommendationsByAutoDemandsLoader loader;

    @Before
    public void mockCurrentDateTime() {
        setTestTime(LocalDateTime.of(2020, 5, 15, 0, 0));
    }

    @Test
    @DbUnitDataSet(before = "CreateRecommendationsByAutoDemandsLoaderTest.simple.before.csv",
        after = "CreateRecommendationsByAutoDemandsLoaderTest.simple.after.csv")
    public void testLoader() {
        loader.load();
    }

    @Test
    @DbUnitDataSet(before = "CreateRecommendationsByAutoDemandsLoaderTest.testLoaderNoVendor.before.csv")
    public void testLoaderNoVendor() {
        Assertions.assertDoesNotThrow(() -> loader.load());
    }

//    @Test
//    @DbUnitDataSet(before = "CreateRecommendationsByAutoDemandsLoaderTest.simple.before.csv",
//        after = "CreateRecommendationsByAutoDemandsLoaderTest.simple.after.csv")
//    public void testLoader_consolidatedSupply() {
//        loader.load();
//    }
}
