package ru.yandex.direct.grid.processing.service.feed;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collections;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.FeedCategory;
import ru.yandex.direct.core.entity.feed.model.FeedHistoryItemParseResultsDefect;
import ru.yandex.direct.core.entity.feed.model.FeedType;
import ru.yandex.direct.core.entity.feed.model.MasterSystem;
import ru.yandex.direct.core.entity.feed.model.Source;
import ru.yandex.direct.core.entity.feed.model.UpdateStatus;
import ru.yandex.direct.core.testing.data.TestFeeds;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.model.feed.GdBusinessType;
import ru.yandex.direct.grid.model.feed.GdFeed;
import ru.yandex.direct.grid.model.feed.GdFeedCategory;
import ru.yandex.direct.grid.model.feed.GdFeedDefect;
import ru.yandex.direct.grid.model.feed.GdFeedType;
import ru.yandex.direct.grid.model.feed.GdSource;
import ru.yandex.direct.grid.model.feed.GdUpdateStatus;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.service.feed.FeedConverter.convertFeedToGd;
import static ru.yandex.direct.grid.processing.service.feed.FeedConverter.convertUpdateStatusFromGd;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class FeedConverterTest {

    @Test
    public void convertUpdateStatusFromGd_successForNull() {
        assertThat(convertUpdateStatusFromGd(null))
                .isNull();
    }

    @Test
    public void convertUpdateStatusFromGd_successForDone() {
        assertThat(convertUpdateStatusFromGd(GdUpdateStatus.DONE))
                .isEqualTo(UpdateStatus.DONE);
    }

    @Test
    public void convertUpdateStatusFromGd_successForOutdated() {
        assertThat(convertUpdateStatusFromGd(GdUpdateStatus.OUTDATED))
                .isEqualTo(UpdateStatus.OUTDATED);
    }

    @Test
    public void convertUpdateStatusFromGd_successForAllValue() {
        SoftAssertions.assertSoftly(soft -> {
            for (GdUpdateStatus gdUpdateStatus : GdUpdateStatus.values()) {
                soft.assertThatCode(() -> convertUpdateStatusFromGd(gdUpdateStatus)).doesNotThrowAnyException();
            }
        });
    }

    @Test
    public void convertFeedDefectsToGd_successNullFields() {
        assertThat(FeedConverter.convertFeedDefectListToGd(null)).isNull();
    }

    @Test
    public void convertFeedDefectsToGd_successEmpty() {
        assertThat(FeedConverter.convertFeedDefectListToGd(Collections.emptyList())).isNull();
    }

    @Test
    public void convertFeedDefectsToGd_success() {
        FeedHistoryItemParseResultsDefect error = new FeedHistoryItemParseResultsDefect()
                .withCode(1201L)
                .withMessageEn("Err1201: can't download url");
        GdFeedDefect expectedError = new GdFeedDefect()
                .withCode(1201L)
                .withMessageEn("Err1201: can't download url");
        assertThat(FeedConverter.convertFeedDefectListToGd(singletonList(error)))
                .is(matchedBy(beanDiffer(singletonList(expectedError))));
    }

    /**
     * Проверяем, что нулабельные в pcc.feeds поля не приводят к NPE в конвертере.
     */
    @Test()
    public void convertFeedToGd_successNullFields() {
        Feed feed = TestFeeds.defaultFeed(ClientId.fromLong(1L))
                .withId(1L)
                .withMarketBusinessId(null)
                .withMarketShopId(null)
                .withMarketFeedId(null)
                .withFeedType(null)
                .withUrl(null)
                .withFilename(null)
                .withLogin(null)
                .withPlainPassword(null)
                .withEmail(null)
                .withOffersCount(null)
                .withLastRefreshed(null)
                .withCachedFileHash(null)
                .withOfferExamples(null);
        assertThatCode(() -> convertFeedToGd(feed, null, emptyList(), true))
                .doesNotThrowAnyException();
    }

    @Test
    public void convertFeedCategoriesToGd_successNullFields() {
        assertThat(FeedConverter.convertFeedCategoryListToGd(null))
                .isEmpty();
    }

    @Test
    public void convertFeedToGd_successOutdatedUpdateStatus() {
        assertThat(convertFeedToGd(new Feed()
                .withId(777L)
                .withName("Test name 1")
                .withFeedType(FeedType.YANDEX_MARKET)
                .withBusinessType(BusinessType.NEWS)
                .withUpdateStatus(UpdateStatus.OUTDATED), null, emptyList(), true))
                .is(matchedBy(beanDiffer(
                        new GdFeed()
                                .withId(777L)
                                .withName("Test name 1")
                                .withFeedType(GdFeedType.YANDEX_MARKET)
                                .withBusinessType(GdBusinessType.NEWS)
                                .withUpdateStatus(GdUpdateStatus.OUTDATED))
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void convertFeedToGd_successRefreshingIsAllowed() {
        var lastChange = LocalDateTime.now().withNano(0).minusDays(1);
        assertThat(convertFeedToGd(new Feed()
                .withId(777L)
                .withName("Test name 1")
                .withBusinessType(BusinessType.NEWS)
                .withUpdateStatus(UpdateStatus.DONE)
                .withSource(Source.URL)
                .withLastChange(lastChange), null, emptyList(), true))
                .is(matchedBy(beanDiffer(
                        new GdFeed()
                                .withId(777L)
                                .withName("Test name 1")
                                .withBusinessType(GdBusinessType.NEWS)
                                .withUpdateStatus(GdUpdateStatus.DONE)
                                .withSource(GdSource.URL)
                                .withLastChange(lastChange)
                                .withIsRefreshingAllowed(true))
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void convertFeedToGd_successRefreshingIsNotAllowed() {
        var lastChange = LocalDateTime.now().withNano(0);
        assertThat(convertFeedToGd(new Feed()
                .withId(777L)
                .withName("Test name 1")
                .withBusinessType(BusinessType.NEWS)
                .withUpdateStatus(UpdateStatus.DONE)
                .withSource(Source.SITE)
                .withLastChange(lastChange), null, emptyList(), true))
                .is(matchedBy(beanDiffer(
                        new GdFeed()
                                .withId(777L)
                                .withName("Test name 1")
                                .withBusinessType(GdBusinessType.NEWS)
                                .withUpdateStatus(GdUpdateStatus.DONE)
                                .withSource(GdSource.SITE)
                                .withLastChange(lastChange)
                                .withIsRefreshingAllowed(false))
                        .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void convertFeedCategoriesToGd_success() {
        FeedCategory category = new FeedCategory()
                .withCategoryId(BigInteger.TEN)
                .withIsDeleted(false)
                .withName("category_name")
                .withOfferCount(1L)
                .withParentCategoryId(BigInteger.ONE)
                .withFeedId(777L);
        GdFeedCategory expectedCategory = new GdFeedCategory()
                .withCategoryId(BigInteger.TEN)
                .withIsDeleted(false)
                .withName("category_name")
                .withOfferCount(1L)
                .withParentCategoryId(BigInteger.ONE);
        assertThat(FeedConverter.convertFeedCategoryListToGd(singletonList(category)))
                .is(matchedBy(beanDiffer(singletonList(expectedCategory))));
    }

    @Test
    public void convertFeedToGd_successAnyUpdateStatus() {
        SoftAssertions.assertSoftly(soft -> {
            for (UpdateStatus updateStatus : UpdateStatus.values()) {
                soft.assertThatCode(() -> convertFeedToGd(new Feed()
                        .withUpdateStatus(updateStatus), null, emptyList(), true)
                ).doesNotThrowAnyException();
            }
        });
    }

    @Test
    public void convertFeedToGd_successAnyBusinessType() {
        SoftAssertions.assertSoftly(soft -> {
            for (BusinessType businessType : BusinessType.values()) {
                soft.assertThatCode(() -> convertFeedToGd(new Feed()
                        .withBusinessType(businessType), null, emptyList(), true)
                ).doesNotThrowAnyException();
            }
        });
    }

    @Test
    public void convertFeedToGd_successAnyFeedType() {
        SoftAssertions.assertSoftly(soft -> {
            for (FeedType feedType : FeedType.values()) {
                soft.assertThatCode(() -> convertFeedToGd(new Feed()
                        .withFeedType(feedType), null, emptyList(), true)
                ).doesNotThrowAnyException();
            }
        });
    }

    @Test
    public void convertFeedSourceToManual_success() {
        assertThat(convertFeedToGd(new Feed()
                .withSource(Source.FILE)
                .withMasterSystem(MasterSystem.MANUAL), null, emptyList(), true))
                .extracting("source")
                .isEqualTo(GdSource.MANUAL);
    }
}
