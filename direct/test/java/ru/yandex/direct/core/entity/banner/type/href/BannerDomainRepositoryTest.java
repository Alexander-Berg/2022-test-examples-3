package ru.yandex.direct.core.entity.banner.type.href;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.domain.model.Domain;
import ru.yandex.direct.core.entity.domain.repository.DomainRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.misc.lang.StringUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public class BannerDomainRepositoryTest {

    private static final String DOMAIN1 = "abc.ru";
    private static final String DOMAIN2 = "another-href.ru";

    @Autowired
    private Steps steps;
    @Autowired
    private BannerDomainRepository bannerDomainRepository;
    @Autowired
    private BannerTypedRepository typedRepository;
    @Autowired
    private DomainRepository domainRepository;

    private CampaignInfo campaignInfo;
    private List<Long> bannerIdsDomainNotChanged;
    private List<Long> bannerIdsDomainChanged;

    @Before
    public void before() {
        campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        TextBannerInfo banner1 = steps.bannerSteps().createBanner(activeTextBanner().withHref("http://" + DOMAIN1)
                        .withDomain(DOMAIN1)
                        .withReverseDomain(StringUtils.reverse(DOMAIN1))
                        .withStatusBsSynced(StatusBsSynced.YES),
                campaignInfo);

        TextBannerInfo banner2 =
                steps.bannerSteps().createBanner(activeTextBanner().withHref("http://" + DOMAIN2)
                                .withDomain(DOMAIN2)
                                .withReverseDomain(StringUtils.reverse(DOMAIN2))
                                .withStatusBsSynced(StatusBsSynced.YES),
                        campaignInfo);

        TextBannerInfo banner3 = steps.bannerSteps().createBanner(activeTextBanner().withHref(null), campaignInfo);
        TextBannerInfo banner4 =
                steps.bannerSteps().createBanner(activeTextBanner().withStatusArchived(true), campaignInfo);

        bannerIdsDomainNotChanged = asList(banner3.getBannerId(), banner4.getBannerId());
        bannerIdsDomainChanged = asList(banner1.getBannerId(), banner2.getBannerId());
    }

    @Test
    public void changeCampaignBannersDomains_ReturnsCorrectIds() {
        String newDomain = "new-domain.com";
        Domain newDomainModel = getDomainModel(campaignInfo.getShard(), newDomain);
        List<Long> actualBannerIds = bannerDomainRepository
                .changeCampaignBannersDomains(campaignInfo.getShard(), campaignInfo.getCampaignId(), newDomainModel);
        assertThat("должны вернуться id баннеров, подлежащих изменению домена", actualBannerIds,
                containsInAnyOrder(bannerIdsDomainChanged.toArray()));
    }

    @Test
    public void changeCampaignBannersDomains_BannersChanged() {
        String newDomain = "new-domain.com";
        Domain newDomainModel = getDomainModel(campaignInfo.getShard(), newDomain);
        bannerDomainRepository
                .changeCampaignBannersDomains(campaignInfo.getShard(), campaignInfo.getCampaignId(), newDomainModel);
        List<TextBanner> banners = typedRepository.getSafely(campaignInfo.getShard(), bannerIdsDomainChanged,
                TextBanner.class);
        assertThat("у баннеров должны были измениться поля", banners, everyItem(
                allOf(
                        hasProperty("domain", is(newDomain)),
                        hasProperty("statusBsSynced", is(StatusBsSynced.NO)),
                        hasProperty("statusModerate", is(BannerStatusModerate.READY))
                )
        ));
    }

    @Test
    public void changeCampaignBannersDomains_OtherBannersNotChanged() {
        String newDomain = "new-domain.com";
        Domain newDomainModel = getDomainModel(campaignInfo.getShard(), newDomain);
        bannerDomainRepository
                .changeCampaignBannersDomains(campaignInfo.getShard(), campaignInfo.getCampaignId(), newDomainModel);
        List<TextBanner> banners = typedRepository.getSafely(campaignInfo.getShard(), bannerIdsDomainNotChanged,
                TextBanner.class);
        assertThat("у баннеров должны были измениться поля", banners, everyItem(
                allOf(
                        hasProperty("domain", nullValue()),
                        hasProperty("statusBsSynced", is(StatusBsSynced.YES)),
                        hasProperty("statusModerate", is(BannerStatusModerate.YES))
                )
        ));
    }

    private Domain getDomainModel(int shard, String newDomain) {
        String reverseDomain = StringUtils.reverse(newDomain);

        Domain newDomainModel = new Domain()
                .withDomain(newDomain)
                .withReverseDomain(reverseDomain);
        domainRepository.addDomains(shard, singletonList(newDomainModel));
        return domainRepository.getDomains(shard, singletonList(newDomain)).get(0);
    }

}
