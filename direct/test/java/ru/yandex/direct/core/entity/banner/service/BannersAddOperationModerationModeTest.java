package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Objects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestNewPerformanceMainBanners.clientPerformanceMainBanner;
import static ru.yandex.direct.utils.FunctionalUtils.mapAndFilterList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersAddOperationModerationModeTest extends BannerClientInfoAddOperationTestBase {
    @Autowired
    private Steps steps;

    @Before
    public void setUp() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void addBannersWithDefaultModerationMode() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.SMART_NO_CREATIVES, true);

        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        AdGroupInfo activeAdGroupInfo = steps.adGroupSteps()
                .createActivePerformanceAdGroup(clientInfo, feedInfo.getFeedId());
        AdGroupInfo draftAdGroupInfo = steps.adGroupSteps()
                .createDraftPerformanceAdGroup(activeAdGroupInfo.getCampaignInfo());

        PerformanceBannerMain activeBanner = clientPerformanceMainBanner()
                .withAdGroupId(activeAdGroupInfo.getAdGroupId());
        PerformanceBannerMain draftBanner = clientPerformanceMainBanner()
                .withAdGroupId(draftAdGroupInfo.getAdGroupId());

        MassResult<Long> result = createOperation(List.of(activeBanner, draftBanner), ModerationMode.DEFAULT,
                Applicability.PARTIAL).prepareAndApply();

        List<Long> ids = mapAndFilterList(result.getResult(), Result::getResult, Objects::nonNull);
        assertThat(ids).hasSize(2);
        PerformanceBannerMain actualActiveBanner = getBanner(ids.get(0), PerformanceBannerMain.class);
        PerformanceBannerMain actualDraftBanner = getBanner(ids.get(1), PerformanceBannerMain.class);
        assertSoftly(softly -> {
            softly.assertThat(actualActiveBanner.getStatusModerate()).isEqualTo(BannerStatusModerate.READY);
            softly.assertThat(actualDraftBanner.getStatusModerate()).isEqualTo(BannerStatusModerate.NEW);
        });
    }
}
