package ru.yandex.direct.core.entity.banner.service.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerationHelper;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singleton;
import static java.util.function.Function.identity;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(MockitoJUnitRunner.class)
public class BannersArchiveUnarchiveModerationTest {
    private final long adGroupId = nextLong();
    private final long imageId = nextLong();
    private final long campaignId = nextLong();
    private List<AppliedChanges<BannerWithSystemFields>> appliedChanges;

    @Mock
    private BannerTypedRepository bannerRepository;

    private BannerModerationHelper moderationHelper;

    @Before
    public void setUp() {
        List<BannerWithSystemFields> banners = Arrays.asList(
                new TextBanner()
                        .withId(nextLong())
                        .withCampaignId(campaignId)
                        .withAdGroupId(adGroupId),
                new TextBanner()
                        .withId(nextLong())
                        .withCampaignId(campaignId)
                        .withAdGroupId(adGroupId)
                        .withSitelinksSetId(1L),
                new ImageBanner()
                        .withId(nextLong())
                        .withCampaignId(campaignId)
                        .withAdGroupId(adGroupId)
                        .withImageId(imageId)
        );

        Map<BannerWithSystemFields, ModelChanges<BannerWithSystemFields>> modelChanges =
                listToMap(banners, identity(),
                        banner -> new ModelChanges<>(banner.getId(), BannerWithSystemFields.class));
        appliedChanges = mapList(modelChanges.entrySet(),
                e -> e.getValue().process(Boolean.TRUE, BannerWithSystemFields.STATUS_ARCHIVED).applyTo(e.getKey()));

        moderationHelper = new BannerModerationHelper(bannerRepository, null);
    }

    @Test
    public void collectBannersWithImagesToModerate_hasOne() {
        Set<Long> result = moderationHelper.collectBannersWithImagesToModerate(appliedChanges);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next()).isEqualTo(imageId);
    }

    @Test
    public void collectAdGroupsToModerateTest_hasOne() {
        List<BannerWithSystemFields> banners = mapList(appliedChanges, AppliedChanges::getModel);
        when(bannerRepository.getBannersByGroupIds(anyInt(), anyCollection(), eq(BannerWithSystemFields.class)))
                .thenReturn(banners);

        Set<Long> result = moderationHelper.collectArchivedAdGroups(singleton(adGroupId), 1);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next()).isEqualTo(adGroupId);
    }

    @Test
    public void collectCampaignsToModerateTest_hasOne() {
        Map<Long, List<Long>> bannerIdsByCampaignIds = new HashMap<>();
        appliedChanges.forEach(ac -> bannerIdsByCampaignIds.computeIfAbsent(ac.getModel().getCampaignId(),
                id -> new ArrayList<>()).add(ac.getModel().getId()));


        List<BannerWithSystemFields> banners = mapList(appliedChanges, AppliedChanges::getModel);
        when(bannerRepository.getBannersByGroupIds(anyInt(), anyCollection(), eq(BannerWithSystemFields.class)))
                .thenReturn(banners);

        Set<Long> adGroups = singleton(adGroupId);
        Set<Long> result = moderationHelper.collectCampaignsToModerate(adGroups, 1);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.iterator().next()).isEqualTo(campaignId);
    }

    @Test
    public void setFlagsForBannersWithShowConditions() {
        moderationHelper.setFlagsForBannersWithShowConditions(appliedChanges);

        AtomicInteger numberOfTextBanners = new AtomicInteger();
        AtomicInteger numberOfBannersWithHref = new AtomicInteger();
        AtomicInteger numberOfBannersWithSitelinks = new AtomicInteger();
        appliedChanges.forEach(ac -> {
            assertThat(ac.getModel().getStatusModerate()).isEqualTo(BannerStatusModerate.READY);
            assertThat(ac.getModel().getStatusPostModerate()).isEqualTo(BannerStatusPostModerate.NO);
            BannerWithSystemFields banner = ac.getModel();
            if (TextBanner.class.isAssignableFrom(banner.getClass())) {
                numberOfTextBanners.incrementAndGet();
                if (((TextBanner) banner).getDisplayHrefStatusModerate() == BannerDisplayHrefStatusModerate.READY) {
                    numberOfBannersWithHref.incrementAndGet();
                }
                if (((TextBanner) banner).getStatusSitelinksModerate() == BannerStatusSitelinksModerate.READY) {
                    numberOfBannersWithSitelinks.incrementAndGet();
                }
            }
        });

        assertThat(numberOfTextBanners.get()).isEqualTo(2);
        assertThat(numberOfBannersWithSitelinks.get()).isEqualTo(1);
    }
}
