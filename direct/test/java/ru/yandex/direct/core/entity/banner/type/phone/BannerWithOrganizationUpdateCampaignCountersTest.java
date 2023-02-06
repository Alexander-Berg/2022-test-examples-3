package ru.yandex.direct.core.entity.banner.type.phone;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.clientphone.repository.ClientPhoneRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone.PERMALINK_ID;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganizationAndPhone.PHONE_ID;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationUpdateCampaignCountersTest extends BannerNewBannerInfoUpdateOperationTestBase {

    private long permalinkIdFirst = -1L;
    private long permalinkIdSecond = -1L;
    private long permalinkIdThird = -1L;
    private long otherPermalinkId = -1L;
    private static final long FIRST_METRIKA_COUNTER_ID = 2L;
    private static final long SECOND_METRIKA_COUNTER_ID = 5L;
    private static final long THIRD_METRIKA_COUNTER_ID = 6L;
    private static final long OTHER_METRIKA_COUNTER_ID = 113L;
    private static final long CAMPAIGN_COUNTER_ID = 99999L;

    @Autowired
    public TestOrganizationRepository testOrganizationRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    DslContextProvider contextProvider;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    ClientPhoneRepository clientPhoneRepository;

    @Autowired
    CampMetrikaCountersRepository campMetrikaCountersRepository;

    private ClientInfo clientInfo;

    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var clientId = clientInfo.getClientId();
        var organization1 = defaultActiveOrganization(clientId);
        var organization2 = defaultActiveOrganization(clientId);
        var organization3 = defaultActiveOrganization(clientId);
        var organizationOther = defaultActiveOrganization(clientId);
        var chiefUid = rbacService.getChiefByClientId(clientId);
        permalinkIdFirst = organization1.getPermalinkId();
        permalinkIdSecond = organization2.getPermalinkId();
        permalinkIdThird = organization3.getPermalinkId();
        otherPermalinkId = organizationOther.getPermalinkId();
        List<Long> chiefUids = List.of(chiefUid);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdFirst, chiefUids, FIRST_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdSecond, chiefUids, SECOND_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdThird, chiefUids, THIRD_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(otherPermalinkId, chiefUids, OTHER_METRIKA_COUNTER_ID);

        var campaignMetrikaCounter = new MetrikaCounter()
                .withId(CAMPAIGN_COUNTER_ID)
                .withHasEcommerce(true);

        campMetrikaCountersRepository.updateMetrikaCounters(adGroupInfo.getShard(),
                Map.of(adGroupInfo.getCampaignId(), List.of(campaignMetrikaCounter)));

        reset(organizationsClient);
    }

    @Test
    public void addPermalinkAndPhoneToExistingBanner() {
        bannerInfo = steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withAdGroupInfo(adGroupInfo));
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);

        Long phoneId = steps.clientPhoneSteps().addDefaultClientOrganizationPhone(clientInfo.getClientId(),
                permalinkIdFirst).getId();
        int shard = bannerInfo.getShard();
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID)
                .process(phoneId, PHONE_ID);
        prepareAndApplyValid(modelChanges);

        Long bannerId = bannerInfo.getBanner().getId();
        Map<Long, Long> bannerPhoneIds = clientPhoneRepository.getPhoneIdsByBannerIds(shard, List.of(bannerId));
        assertThat(bannerPhoneIds.get(bannerId)).isEqualTo(phoneId);
    }

}
