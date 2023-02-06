package ru.yandex.direct.core.entity.banner.type.bsresync;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields.STATUS_BS_SYNCED;

@CoreTest
@RunWith(Parameterized.class)
public class BannersUpdateBsResyncServiceTest {
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    private static final long AD_GROUP_ID = 2L;
    private static final long CAMPAIGN_ID = 3L;
    private static final long BANNER_ID = 4L;

    @Autowired
    private BannersUpdateBsResyncService serviceUnderTest;

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BannerWithSystemFields banner;

    @Parameterized.Parameter(2)
    public Set<Long> bannerIdsToBsResync;

    @Parameterized.Parameter(3)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(4)
    public StatusBsSynced statusBsSynced;


    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "Текстовый баннер с пустым списком bannerIdsToBsResync. Статус не изменен",
                        new TextBanner(),
                        emptySet(),
                        ModerationMode.FORCE_MODERATE,
                        StatusBsSynced.YES
                },

                {
                        "Текстовый баннер содержищийся в bannerIdsToBsResync. Статус изменен",
                        new TextBanner(),
                        Set.of(BANNER_ID),
                        ModerationMode.FORCE_MODERATE,
                        StatusBsSynced.NO
                },

                {
                        "Смарт-баннер с пустым bannerIdsToBsResync и default режимом модерации. Статус не изменен",
                        new PerformanceBanner(),
                        emptySet(),
                        ModerationMode.DEFAULT,
                        StatusBsSynced.YES
                },

                {
                        "Смарт-баннер  содержищийся в bannerIdsToBsResync. Статус изменен",
                        new PerformanceBanner(),
                        Set.of(BANNER_ID),
                        ModerationMode.DEFAULT,
                        StatusBsSynced.NO
                },

                {
                        "Смарт-баннер с пустым bannerIdsToBsResync и FORCE_MODERATE режимом модерации. Статус изменен",
                        new PerformanceBanner(),
                        emptySet(),
                        ModerationMode.FORCE_MODERATE,
                        StatusBsSynced.NO
                },

        });
    }


    @Test
    public void testResetBsSyncStatus() {
        var ac = createAppliedChanges(banner);
        BannersUpdateOperationContainer parametersContainer = createContainer(moderationMode, banner);
        resetBsSyncStatus(bannerIdsToBsResync, parametersContainer, ac);
        assertThatStatusIs(ac, statusBsSynced);
    }

    private void assertThatStatusIs(AppliedChanges<? extends BannerWithSystemFields> ac, StatusBsSynced no) {
        assertThat(ac.getNewValue(STATUS_BS_SYNCED), is(no));
    }

    private AppliedChanges<BannerWithSystemFields> createAppliedChanges(BannerWithSystemFields banner) {
        fillBanner(banner);
        return new ModelChanges<>(banner.getId(), banner.getClass())
                .castModelUp(BannerWithSystemFields.class)
                .applyTo(banner);
    }

    private void fillBanner(BannerWithSystemFields banner) {
        banner
                .withId(BANNER_ID)
                .withAdGroupId(AD_GROUP_ID)
                .withCampaignId(CAMPAIGN_ID)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusBsSynced(StatusBsSynced.YES);
    }

    private BannersUpdateOperationContainer createContainer(ModerationMode moderationMode, BannerWithSystemFields banner) {
        var parametersContainer = new BannersUpdateOperationContainerImpl(1, null, null,
                null, null, null,
                null, emptySet(), moderationMode, false,
                false, true);

        parametersContainer.setIndexToCampaignMap(Map.of(0, new TextCampaign().withId(CAMPAIGN_ID)));
        parametersContainer.setIndexToAdGroupMap(Map.of(0, new AdGroup().withId(AD_GROUP_ID)));
        parametersContainer.setBannerToIndexMap(new IdentityHashMap<>(Map.of(banner, 0)));
        return parametersContainer;
    }

    private void resetBsSyncStatus(Set<Long> bannerIdsToBsResync, BannersUpdateOperationContainer parametersContainer,
                                   AppliedChanges<? extends BannerWithSystemFields> ac) {
        serviceUnderTest.resetBsSyncStatus(parametersContainer, bannerIdsToBsResync,
                List.of(ac.castModelUp(BannerWithSystemFields.class)));
    }

}
