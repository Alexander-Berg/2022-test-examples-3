package ru.yandex.direct.core.entity.banner.service;

import java.util.List;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.misc.lang.StringUtils;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ChangeCampaignBannerDomainsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerService bannerService;
    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private CampaignInfo campaignInfo;
    private UserInfo superOperator;

    @Before
    public void before() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(activeTextBanner().withHref("http://ya.ru/123"));
        campaignInfo = bannerInfo.getCampaignInfo();
        steps.bannerSteps().createBanner(activeTextBanner().withHref("http://ya.ru/456"), campaignInfo);

        superOperator = steps.userSteps().createDefaultUserWithRole(RbacRole.SUPER);
    }

    @Test
    public void wrongRole_DomainWasNotChanged() {
        ClientInfo client = campaignInfo.getClientInfo();
        Long campaignId = campaignInfo.getCampaignId();
        assumeThat("у оператора должна быть недопустимая роль",
                client.getClient().getRole().anyOf(RbacRole.SUPER, RbacRole.SUPPORT, RbacRole.PLACER, RbacRole.MANAGER),
                is(false));

        String newDomain = "any_domain.com";
        bannerService.changeCampaignBannersDomains(client.getUid(), campaignId, newDomain);
        var banners =
                bannerTypedRepository.getBannersByCampaignIds(campaignInfo.getShard(), singletonList(campaignId), null);
        List<String> bannersDomains =
                StreamEx.of(banners)
                .map(b -> (BannerWithHref) b)
                .map(BannerWithHref::getDomain)
                .toList();
        assertThat("домены у баннеров не должны были измениться", bannersDomains, everyItem(not(is(newDomain))));
    }

    @Test
    public void domainSuccessfullySaved() {
        String newDomain = "new-valid.ru";
        bannerService.changeCampaignBannersDomains(superOperator.getUid(), campaignInfo.getCampaignId(),
                newDomain);
        var banners = bannerTypedRepository
                .getBannersByCampaignIds(campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId()), null);

        String reverseDomain = StringUtils.reverse(newDomain);
        assertThat(banners, everyItem(
                allOf(hasProperty("domain", is(newDomain)),
                        hasProperty("statusBsSynced", is(StatusBsSynced.NO)))));
    }
}
