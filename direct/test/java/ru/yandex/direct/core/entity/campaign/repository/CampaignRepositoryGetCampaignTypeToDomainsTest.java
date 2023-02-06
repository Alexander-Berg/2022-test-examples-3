package ru.yandex.direct.core.entity.campaign.repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.domain.service.DomainService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.DYNAMIC;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;
import static ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds.ALL;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeDynamicTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignRepositoryGetCampaignTypeToDomainsTest {

    private static final String DOMAIN_1 = "www.domain1.ru";
    private static final String DOMAIN_2 = "www.domain2.ru";

    @Autowired
    private Steps steps;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    public DomainService domainService;
    @Autowired
    public DslContextProvider dslContextProvider;

    private int shard;
    private ClientId clientId;
    private CampaignInfo textCampaignInfo;
    private CampaignInfo dynamicCampaignInfo;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createUser(generateNewUser());
        shard = userInfo.getShard();
        clientId = userInfo.getClientId();

        Campaign textCampaign = activeTextCampaign(userInfo.getClientId(), userInfo.getUid());
        textCampaignInfo = steps.campaignSteps().createCampaign(textCampaign, userInfo.getClientInfo());

        dynamicCampaignInfo = steps.campaignSteps().createActiveDynamicCampaign(userInfo.getClientInfo());
    }

    @Test
    public void getCampaignTypeToDomains_TextBannerWithoutDomain() {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(textCampaignInfo.getCampaignId()), textCampaignInfo);
        steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(TEXT));
        assertThat(campaignTypeToDomains)
                .isEmpty();
    }

    @Test
    public void getCampaignTypeToDomains_WithTextBanner() {
        addTextBanner(DOMAIN_1);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(TEXT));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(TEXT, Set.of(DOMAIN_1));
    }

    @Test
    public void getCampaignTypeToDomains_WithDomainBanner() {
        addDynamicBanner(DOMAIN_1);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(DYNAMIC));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(DYNAMIC, Set.of(DOMAIN_1));
    }

    @Test
    public void getCampaignTypeToDomains_WithTextAndDomainBanners() {
        addTextBanner(DOMAIN_1);
        addDynamicBanner(DOMAIN_2);

        Map<CampaignType, Set<String>> campaignTypeToDomains = campaignRepository
                .getCampaignTypeToDomains(shard, clientId, Set.of(TEXT, DYNAMIC));
        assertThat(campaignTypeToDomains)
                .hasSize(2)
                .containsEntry(TEXT, Set.of(DOMAIN_1))
                .containsEntry(DYNAMIC, Set.of(DOMAIN_2));
    }

    @Test
    public void getCampaignTypeToDomains_WithoutBanners() {
        Map<CampaignType, Set<String>> campaignTypeToDomains = campaignRepository
                .getCampaignTypeToDomains(shard, clientId, ALL);
        assertThat(campaignTypeToDomains)
                .isEmpty();
    }

    @Test
    public void getCampaignTypeToDomains_WithBannersButCampaignTypeNotInclude() {
        addTextBanner(DOMAIN_1);
        addDynamicBanner(DOMAIN_2);

        Set<CampaignType> campaignTypes = new HashSet<>(ALL);
        campaignTypes.removeAll(Set.of(TEXT, DYNAMIC));

        Map<CampaignType, Set<String>> campaignTypeToDomains = campaignRepository
                .getCampaignTypeToDomains(shard, clientId, campaignTypes);
        assertThat(campaignTypeToDomains)
                .isEmpty();
    }

    @Test
    public void getCampaignTypeToDomains_With2TextBanners() {
        addTextBanner(DOMAIN_1);
        addTextBanner(DOMAIN_2);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(TEXT));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(TEXT, Set.of(DOMAIN_1, DOMAIN_2));
    }

    @Test
    public void getCampaignTypeToDomains_With2DynamicBanners() {
        addDynamicBanner(DOMAIN_1);
        addDynamicBanner(DOMAIN_2);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(DYNAMIC));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(DYNAMIC, Set.of(DOMAIN_1, DOMAIN_2));
    }

    @Test
    public void getCampaignTypeToDomains_2TextBannersWithSameDomain() {
        addTextBanner(DOMAIN_1);
        addTextBanner(DOMAIN_1);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(TEXT));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(TEXT, Set.of(DOMAIN_1));
    }

    @Test
    public void getCampaignTypeToDomains_2DynamicBannersWithSameDomain() {
        addDynamicBanner(DOMAIN_1);
        addDynamicBanner(DOMAIN_1);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(DYNAMIC));
        assertThat(campaignTypeToDomains)
                .hasSize(1)
                .containsEntry(DYNAMIC, Set.of(DOMAIN_1));
    }

    @Test
    public void getCampaignTypeToDomains_TextAndDynamicBannersWithSameDomain() {
        addTextBanner(DOMAIN_1);
        addDynamicBanner(DOMAIN_1);

        Map<CampaignType, Set<String>> campaignTypeToDomains =
                campaignRepository.getCampaignTypeToDomains(shard, clientId, Set.of(TEXT, DYNAMIC));
        assertThat(campaignTypeToDomains)
                .hasSize(2)
                .containsEntry(TEXT, Set.of(DOMAIN_1))
                .containsEntry(DYNAMIC, Set.of(DOMAIN_1));
    }

    private void addTextBanner(String domain) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                defaultTextAdGroup(textCampaignInfo.getCampaignId()), textCampaignInfo);
        steps.bannerSteps().createBanner(activeTextBanner()
                .withDomain(domain), adGroupInfo);
    }

    private void addDynamicBanner(String domain) {
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(
                activeDynamicTextAdGroup(dynamicCampaignInfo.getCampaignId())
                        .withDomainUrl(domain), dynamicCampaignInfo);
        steps.bannerSteps().createActiveDynamicBanner(adGroupInfo);
    }
}
