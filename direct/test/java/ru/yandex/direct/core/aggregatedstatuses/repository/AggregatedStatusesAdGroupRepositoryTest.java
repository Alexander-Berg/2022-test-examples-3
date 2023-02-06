package ru.yandex.direct.core.aggregatedstatuses.repository;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AggregatedStatusesAdGroupRepositoryTest {
    @Autowired
    private Steps steps;

    @Autowired
    private AggregatedStatusesAdGroupRepository aggregatedStatusesAdGroupRepository;

    @Test
    public void getAdGroupIdsWithGeoLegalFlagsTest() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bannerSteps().createActiveTextBannerWithFlags(adGroupInfo, new BannerFlags().with(BannerFlags.ALCOHOL,
                true));
        var adGroupIdsWithGeoLegalFlags =
                aggregatedStatusesAdGroupRepository.getAdGroupIdsWithGeoLegalFlags(adGroupInfo.getShard(),
                        Collections.singletonList(adGroupInfo.getAdGroupId()));
        assertThat(adGroupIdsWithGeoLegalFlags).hasSize(1);
    }

    @Test
    public void getAdGroupIdsWithNotGeoLegalFlagsTest() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bannerSteps().createActiveTextBannerWithFlags(adGroupInfo, new BannerFlags().with(BannerFlags.GOODFACE,
                true));
        var adGroupIdsWithGeoLegalFlags =
                aggregatedStatusesAdGroupRepository.getAdGroupIdsWithGeoLegalFlags(adGroupInfo.getShard(),
                        Collections.singletonList(adGroupInfo.getAdGroupId()));
        assertThat(adGroupIdsWithGeoLegalFlags).hasSize(0);
    }

    @Test
    public void getAdGroupIdsWithNullGeoLegalFlagsTest() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bannerSteps().createActiveTextBannerWithFlags(adGroupInfo, null);
        var adGroupIdsWithGeoLegalFlags =
                aggregatedStatusesAdGroupRepository.getAdGroupIdsWithGeoLegalFlags(adGroupInfo.getShard(),
                        Collections.singletonList(adGroupInfo.getAdGroupId()));
        assertThat(adGroupIdsWithGeoLegalFlags).hasSize(0);
    }

    @Test
    public void getAdGroupIdsWithEmptyGeoLegalFlagsTest() {
        var adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.bannerSteps().createActiveTextBannerWithEmptyFlags(adGroupInfo);
        var adGroupIdsWithGeoLegalFlags =
                aggregatedStatusesAdGroupRepository.getAdGroupIdsWithGeoLegalFlags(adGroupInfo.getShard(),
                        Collections.singletonList(adGroupInfo.getAdGroupId()));
        assertThat(adGroupIdsWithGeoLegalFlags).hasSize(0);
    }
}
