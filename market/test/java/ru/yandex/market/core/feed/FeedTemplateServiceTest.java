package ru.yandex.market.core.feed;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.model.FeedTemplateInfo;

/**
 * Тесты на логику работы {@link FeedTemplateService}
 *
 * @author fbokovikov
 */
class FeedTemplateServiceTest extends FunctionalTest {

    @Autowired
    private FeedTemplateService feedTemplateService;

    /**
     * Тест на {@link FeedTemplateService#saveTemplateInfo(FeedTemplateInfo) сохранение} информации об excel-фиде.
     */
    @DbUnitDataSet(after = "testSaveFeedInfo.csv")
    @Test
    void testSaveFeedInfo() {
        feedTemplateService.saveTemplateInfo(new FeedTemplateInfo(15, 15, "winda", "95"));
    }

}
