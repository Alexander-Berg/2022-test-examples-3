package ru.yandex.market.core.samovar;

import java.util.List;

import javax.annotation.Nonnull;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.samovar.model.SamovarFullFeedDownloadInfoError;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Date: 25.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "SamovarFeedDownloadErrorsDaoImplTest.before.csv")
class SamovarFeedDownloadErrorsDaoImplTest extends FunctionalTest {

    @Autowired
    private SamovarFeedDownloadErrorsDao samovarFeedDownloadErrorsDao;

    @DisplayName("Проверка возвращаемого списка магазинов для отключения")
    @Test
    void getShopsForCutoff_allCases_setWithTwoResult() {
        var shopsForCutoff = samovarFeedDownloadErrorsDao.getShopsForCutoff();
        assertEquals(3, shopsForCutoff.size());
        checkFullFeeds(shopsForCutoff.get(708L), 2, Matchers.hasItems(
                new SamovarFullFeedDownloadInfoError(8L, 403L, "http://test8.feed.url/"),
                new SamovarFullFeedDownloadInfoError(15L, 404L, "http://test15.feed.url/")
        ));
        checkFullFeeds(shopsForCutoff.get(712L), 1, Matchers.hasItems(
                new SamovarFullFeedDownloadInfoError(12L, 500L, "http://test12.feed.url/")
        ));
        checkFullFeeds(shopsForCutoff.get(713L), 1, Matchers.hasItems(
                new SamovarFullFeedDownloadInfoError(13L, 504L, "http://test13.feed.url/")
        ));
    }

    private void checkFullFeeds(@Nonnull List<SamovarFullFeedDownloadInfoError> fullFeeds,
                                int size,
                                Matcher<? super List<SamovarFullFeedDownloadInfoError>> matcher) {
        assertEquals(size, fullFeeds.size());
        MatcherAssert.assertThat(fullFeeds, matcher);
    }
}
