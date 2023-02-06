package ru.yandex.direct.core.entity.banner.service.deleteoperation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.core.entity.organization.model.Organization;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestBannerRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.MassResult;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannersDeleteOperationOrganizationsTest {

    @Autowired
    private Steps steps;

    @Autowired
    private TestBannerRepository bannerRepository;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Autowired
    private BannerService bannerService;

    @Autowired
    private OrganizationRepository organizationRepository;

    private int shard;

    private ClientId clientId;
    private Long clientUid;
    private Long bannerId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo);

        shard = clientInfo.getShard();
        clientId = clientInfo.getClientId();
        clientUid = clientInfo.getUid();
        Long campaignId = campaignInfo.getCampaignId();
        bannerId = bannerInfo.getBannerId();

        Organization organization = defaultActiveOrganization(clientId);
        organizationRepository.addOrUpdateAndLinkOrganizations(shard, Map.of(bannerId, organization));

        bannerRepository.updateBannerId(shard, bannerInfo, 0L);
        campaignRepository.setCampaignMoney(campaignId, shard, BigDecimal.ZERO, BigDecimal.ZERO);
        assumeThat(bannerService.getCanBeDeletedBannersByIds(shard, clientId, List.of(bannerId)).get(bannerId),
                is(true));
    }

    @Test
    public void bannerDeleted_organizationLinkDeleted() {
        MassResult<Long> result = bannerService.deleteBannersPartial(clientUid, clientId, List.of(bannerId));
        assertThat(result, isSuccessful());
        assertThat(result.get(0).getResult(), equalTo(bannerId));
        var organizationLinks = organizationRepository.getOrganizationsByBannerIds(shard, List.of(bannerId));
        assertThat(organizationLinks.values(), empty());
    }
}
