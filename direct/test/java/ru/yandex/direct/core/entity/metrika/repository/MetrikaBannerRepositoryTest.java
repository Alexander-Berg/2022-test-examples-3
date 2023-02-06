package ru.yandex.direct.core.entity.metrika.repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerImage;
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.metrika.container.BannerWithTitleAndBody;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaBannerRepositoryTest {
    @Autowired
    private MetrikaBannerRepository repoUnderTest;

    @Autowired
    public AdGroupSteps adGroupSteps;
    @Autowired
    private Steps steps;
    @Autowired
    private OldBannerRepository bannerRepository;

    private int shard;
    private long adGroupId;
    private long campaignId;
    private long sitelinkSetId;
    private Long calloutId;

    @Before
    public void before() {
        AdGroupInfo adGroupInfo = adGroupSteps.createDefaultAdGroup();

        ClientInfo clientInfo = adGroupInfo.getClientInfo();
        shard = adGroupInfo.getShard();
        adGroupId = adGroupInfo.getAdGroupId();
        campaignId = adGroupInfo.getCampaignId();
        calloutId = steps.calloutSteps().createDefaultCallout(clientInfo).getId();

        sitelinkSetId = steps.sitelinkSetSteps().createDefaultSitelinkSet(clientInfo).getSitelinkSetId();
    }

    @Test
    public void testGetBannerWithTitleAndBodyFromImageIds() {
        var textBanner = activeTextBanner(campaignId, adGroupId)
                .withCalloutIds(Collections.singletonList(calloutId))
                .withSitelinksSetId(sitelinkSetId)
                .withBannerImage(defaultBannerImage());
        bannerRepository.addBanners(shard, singletonList(textBanner));
        List<BannerWithTitleAndBody> got = repoUnderTest.getBannerWithTitleAndBodyFromImageIds(shard,
                singleton(textBanner.getBannerImage().getId()));
        List<BannerWithTitleAndBody> expected =
                singletonList(new BannerWithTitleAndBody(textBanner.getBannerImage().getBannerId(),
                        textBanner.getBannerImage().getBsBannerId(), textBanner.getTitle(), textBanner.getBody()));
        assertThat(got).hasSize(1);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    public void testGetBannerWithTitleAndBodyFromImageIds_ZeroBsBannerId() {
        var textBanner = activeTextBanner(campaignId, adGroupId)
                .withCalloutIds(Collections.singletonList(calloutId))
                .withSitelinksSetId(sitelinkSetId)
                .withBannerImage(defaultBannerImage());
        textBanner.getBannerImage().withBsBannerId(0L);
        bannerRepository.addBanners(shard, singletonList(textBanner));
        List<BannerWithTitleAndBody> got = repoUnderTest.getBannerWithTitleAndBodyFromImageIds(shard,
                singleton(textBanner.getBannerImage().getId()));

        assertThat(got).hasSize(0);
    }

    @Test
    public void testGetBannerWithTitleAndBodyFromBids() {
        var textBanner = activeTextBanner(campaignId, adGroupId)
                .withCalloutIds(Collections.singletonList(calloutId))
                .withSitelinksSetId(sitelinkSetId);
        bannerRepository.addBanners(shard, singletonList(textBanner));
        Map<Long, BannerWithTitleAndBody> got = repoUnderTest.getBannerWithTitleAndBodyFromBids(shard,
                singleton(textBanner.getId()));
        Map<Long, BannerWithTitleAndBody> expected =
                Map.of(textBanner.getId(), new BannerWithTitleAndBody(textBanner.getId(),
                        textBanner.getBsBannerId(), textBanner.getTitle(), textBanner.getBody()));
        assertThat(got).hasSize(1);
        assertThat(got).isEqualTo(expected);
    }

    @Test
    public void testGetBannerWithTitleAndBodyFromBids_ZeroBsBannerId() {
        var textBanner = activeTextBanner(campaignId, adGroupId)
                .withCalloutIds(Collections.singletonList(calloutId))
                .withSitelinksSetId(sitelinkSetId);
        textBanner.withBsBannerId(0L);
        bannerRepository.addBanners(shard, singletonList(textBanner));
        Map<Long, BannerWithTitleAndBody> got = repoUnderTest.getBannerWithTitleAndBodyFromBids(shard,
                singleton(textBanner.getId()));

        assertThat(got).hasSize(0);
    }

    private OldBannerImage defaultBannerImage() {
        return new OldBannerImage()
                .withImageHash(random(5, true, true))
                .withBsBannerId(Long.valueOf(randomNumeric(5)))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withStatusShow(true)
                .withDateAdded(LocalDateTime.now());
    }
}
