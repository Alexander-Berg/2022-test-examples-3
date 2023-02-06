package ru.yandex.market.core.indexer.feedlog;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feed.FeedService;
import ru.yandex.market.core.indexer.model.DatasourceIndexedState;
import ru.yandex.market.core.indexer.model.IndexDaySummary;
import ru.yandex.market.core.indexer.model.ReturnCode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для {@link FeedLogService}.
 */
@DbUnitDataSet(before = "FeedLogServiceTest.getFeedLog.before.csv")
class FeedLogServiceTest extends FunctionalTest {

    @Autowired
    private FeedLogService feedLogService;
    @Autowired
    private FeedService feedService;

    @Test
    @DisplayName("Получение IndexedState. У магазина 2 фида. Только для одного есть feed_status")
    void testGetDatasourceIndexState() {
        DatasourceIndexedState expected = new DatasourceIndexedState();
        expected.setShopId(774);
        expected.setFeedsCount(1);
        expected.setStatus(ReturnCode.OK);
        expected.setOffersCount(1000);
        expected.setPublishDate((Date.valueOf(LocalDate.of(2017, Month.MARCH, 1))));
        expected.setDownloadDate((Date.valueOf(LocalDate.of(2017, Month.JANUARY, 1))));
        expected.setEnabledFeedsCount(2);
        DatasourceIndexedState generation =
                feedLogService.getDatasourceIndexState(774, feedService.getFullFeeds(774, true));
        ReflectionAssert.assertReflectionEquals(expected, generation);
    }

    @Test
    @DisplayName("Получение IndexedState. У магазина 2 фида. Происходит импорт поколения. " +
            "Один фид уже обновился и попал в новое поколение, второй еще не обновился и пока что лежит в старом. " +
            "В результате должны учитываться оба")
    void testGetDatasourceIndexStateImport() {
        DatasourceIndexedState expected = new DatasourceIndexedState();
        expected.setShopId(5000);
        expected.setFeedsCount(2);
        expected.setStatus(ReturnCode.OK);
        expected.setOffersCount(3000);
        expected.setPublishDate((Date.valueOf(LocalDate.of(2017, Month.MARCH, 8))));
        expected.setDownloadDate((Date.valueOf(LocalDate.of(2017, Month.JANUARY, 1))));
        expected.setEnabledFeedsCount(2);
        expected.setCpcRealOffersCount(2200);
        expected.setCpaRealOffersCount(800);
        DatasourceIndexedState generation =
                feedLogService.getDatasourceIndexState(5000, feedService.getFullFeeds(5000, true));
        ReflectionAssert.assertReflectionEquals(expected, generation);
    }

    @Test
    @DisplayName("Получение IndexedState. У магазина 2 фида. Оба в индексе")
    void testGetDatasourceIndexStateNewSchema() {
        DatasourceIndexedState expected = new DatasourceIndexedState();
        expected.setShopId(2774);
        expected.setFeedsCount(2);
        expected.setStatus(ReturnCode.ERROR);
        expected.setOffersCount(195);
        expected.setPublishDate((Date.valueOf(LocalDate.of(2017, Month.MARCH, 4))));
        expected.setDownloadDate((Date.valueOf(LocalDate.of(2018, Month.DECEMBER, 13))));
        expected.setEnabledFeedsCount(2);
        DatasourceIndexedState generation =
                feedLogService.getDatasourceIndexState(2774, feedService.getFullFeeds(2774, true));
        ReflectionAssert.assertReflectionEquals(expected, generation);
    }

    @Test
    @DisplayName("Получение отчета по индексации")
    void testGetDatasourceIndexDaySummary() {
        List<IndexDaySummary> feedLogSummary = feedLogService.getDatasourceIndexDaySummary(5678, 7);
        assertThat(feedLogSummary).hasSize(1);
        ReflectionAssert.assertReflectionEquals(
                new IndexDaySummary(feedLogSummary.iterator().next().getDate(), ReturnCode.OK, 2000L),
                feedLogSummary.iterator().next()
        );
    }

    @Test
    @DbUnitDataSet(before = "FeedLogServiceTest.getDatasourceIndexDayFromReport.before.csv")
    @DisplayName("Получение отчета по индексации в случае если в репорте больше предложений")
    void testGetDatasourceIndexDayFromReportSummary() {
        List<IndexDaySummary> feedLogSummary = feedLogService.getDatasourceIndexDaySummary(5678, 7);
        assertThat(feedLogSummary).hasSize(1);
        ReflectionAssert.assertReflectionEquals(
                new IndexDaySummary(feedLogSummary.iterator().next().getDate(), ReturnCode.OK, 2010L),
                feedLogSummary.iterator().next()
        );
    }
}
