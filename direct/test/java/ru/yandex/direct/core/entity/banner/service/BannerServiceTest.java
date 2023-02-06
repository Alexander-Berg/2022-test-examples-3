package ru.yandex.direct.core.entity.banner.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.container.AdsSelectionCriteria;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private TestBannerRepository bannerRepository;


    @Test
    public void getBannersBySelectionCriteria_BannerWithWrongDomainId_ReturnsBannerWithCorrectDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        int shard = adGroupInfo.getShard();

        Domain domain = steps.domainSteps().createDomain(shard).getDomain();
        Domain domain2 = steps.domainSteps().createDomain(shard).getDomain();

        Long adGroupId = adGroupInfo.getAdGroupId();
        Long campaignId = adGroupInfo.getCampaignId();
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(
                defaultTextBanner(campaignId, adGroupId)
                        .withDomain(domain.getDomain()));

        Long bannerId = bannerInfo.getBannerId();
        ClientId clientId = bannerInfo.getClientId();
        Long operatorUid = bannerInfo.getUid();

        bannerRepository.setDomainId(shard, bannerId, domain2.getId());

        var banners = bannerService.getBannersBySelectionCriteria(
                operatorUid, clientId,
                new AdsSelectionCriteria().withAdIds(bannerId),
                new LimitOffset(1, 0));

        assumeThat("Вернулся 1 баннер", banners, hasSize(1));

        TextBanner actualBanner = (TextBanner) banners.get(0);

        assertThat("У баннера установлен правильный домен", actualBanner.getDomain(), is(domain.getDomain()));
    }
}
