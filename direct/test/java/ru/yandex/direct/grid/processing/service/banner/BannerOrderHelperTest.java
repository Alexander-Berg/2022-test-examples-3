package ru.yandex.direct.grid.processing.service.banner;

import java.util.Comparator;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.grid.model.Order;
import ru.yandex.direct.grid.processing.model.banner.GdAd;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderBy;
import ru.yandex.direct.grid.processing.model.banner.GdAdOrderByField;
import ru.yandex.direct.grid.processing.model.banner.GdAdPrimaryStatus;
import ru.yandex.direct.grid.processing.model.banner.GdAdStatus;
import ru.yandex.direct.grid.processing.model.banner.GdTextAd;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class BannerOrderHelperTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private List<GdAd> expectedBanner;
    private List<GdAd> actualBanners;
    private static GdAd activeBanner;
    private static GdAd archivedBanner;
    private static GdAd draftBanner;
    private static GdAd manuallySuspendedBanner;
    private static GdAd moderationBanner;
    private static GdAd temporallySuspendedBanner;
    private static GdAd moderationRejectedBanner;

    @BeforeClass
    public static void beforeClass() {
        activeBanner = new GdTextAd()
                .withStatus(new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.ACTIVE))
                .withId(1L);
        archivedBanner = new GdTextAd()
                .withStatus(new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.ARCHIVED))
                .withId(2L);
        draftBanner = new GdTextAd()
                .withStatus(new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.DRAFT))
                .withId(3L);
        manuallySuspendedBanner = new GdTextAd().withStatus(
                new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.MANUALLY_SUSPENDED))
                .withId(4L);
        moderationBanner = new GdTextAd().withStatus(
                new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.MODERATION))
                .withId(5L);
        temporallySuspendedBanner = new GdTextAd().withStatus(
                new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.TEMPORARILY_SUSPENDED))
                .withId(6L);
        moderationRejectedBanner = new GdTextAd().withStatus(
                new GdAdStatus().withPrimaryStatus(GdAdPrimaryStatus.MODERATION_REJECTED))
                .withId(7L);
    }

    @Before
    public void before() {
        actualBanners = asList(manuallySuspendedBanner,
                draftBanner,
                archivedBanner,
                moderationRejectedBanner,
                temporallySuspendedBanner,
                activeBanner,
                moderationBanner);

    }


    @Test
    public void testGetBanners_OrderByPrimaryStatus() {
        GdAdOrderBy gdAdOrderBy = new GdAdOrderBy()
                .withField(GdAdOrderByField.PRIMARY_STATUS)
                .withOrder(Order.ASC);

        Comparator<GdAd> comparator = BannerOrderHelper.createOrderComparator(singletonList(gdAdOrderBy));

        actualBanners.sort(comparator);

        expectedBanner = asList(
                moderationRejectedBanner,
                temporallySuspendedBanner,
                activeBanner,
                moderationBanner,
                manuallySuspendedBanner,
                draftBanner,
                archivedBanner);

        assertThat(actualBanners)
                .is(matchedBy(beanDiffer(expectedBanner)));

    }

    @Test
    public void testGetBanners_OrderByPrimaryStatus_ReverseOrder() {
        GdAdOrderBy gdAdOrderBy = new GdAdOrderBy()
                .withField(GdAdOrderByField.PRIMARY_STATUS)
                .withOrder(Order.DESC);

        Comparator<GdAd> comparator = BannerOrderHelper.createOrderComparator(singletonList(gdAdOrderBy));

        actualBanners.sort(comparator);

        expectedBanner = asList(
                archivedBanner,
                draftBanner,
                manuallySuspendedBanner,
                moderationBanner,
                activeBanner,
                temporallySuspendedBanner,
                moderationRejectedBanner);

        assertThat(actualBanners)
                .is(matchedBy(beanDiffer(expectedBanner)));
    }


    @Test
    public void testGetBanners_OrderByNotSupportedField_ThrowIllegalArgumentException() {
        GdAdOrderBy gdAdOrderBy = new GdAdOrderBy()
                .withField(GdAdOrderByField.BANNER_TYPE)
                .withOrder(Order.DESC);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("field is not supported");

        Comparator<GdAd> comparator = BannerOrderHelper.createOrderComparator(singletonList(gdAdOrderBy));
        actualBanners.sort(comparator);
    }

}
