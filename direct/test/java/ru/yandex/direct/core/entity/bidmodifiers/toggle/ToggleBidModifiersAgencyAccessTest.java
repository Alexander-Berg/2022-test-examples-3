package ru.yandex.direct.core.entity.bidmodifiers.toggle;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifiers.container.UntypedBidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.ClientPerm;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка доступа агентства к отключению наборов корректировок ставок у субклиентов")
public class ToggleBidModifiersAgencyAccessTest {
    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ClientRepository clientRepository;

    private ClientInfo agency;
    private ClientInfo subclient1;
    private ClientInfo subclient2;
    private CampaignInfo subclient1Campaign;  // Кампания субклиента 1
    private CampaignInfo subclient2Campaign;  // Кампания субклиента 2
    private CampaignInfo standaloneCampaign;  // Кампания самостоятельного клиента
    private AdGroup subclient1AdGroup;  // Группа объявлений в кампании subclient1Campaign
    private AdGroup subclient2AdGroup;  // Группа объявлений в кампании subclient2Campaign
    private AdGroupInfo standaloneAdGroup;  // Группа объявлений самостоятельного клиента

    @Before
    public void before() {
        agency = steps.clientSteps().createDefaultClientWithRole(RbacRole.AGENCY);
        subclient1 = steps.clientSteps().createDefaultClientUnderAgency(agency);
        subclient2 = steps.clientSteps().createDefaultClientUnderAgency(agency);
        clientRepository.setPerms(subclient1.getShard(), subclient1.getClientId(),
                singleton(ClientPerm.SUPER_SUBCLIENT));
        clientRepository.setPerms(subclient2.getShard(), subclient2.getClientId(),
                singleton(ClientPerm.SUPER_SUBCLIENT));

        subclient1Campaign = steps.campaignSteps().createCampaign(
                activeTextCampaign(subclient1.getClientId(), subclient1.getUid())
                        .withAgencyUid(agency.getUid()).withAgencyId(agency.getClientId().asLong()), subclient1);
        subclient2Campaign = steps.campaignSteps().createCampaign(
                activeTextCampaign(subclient2.getClientId(), subclient2.getUid())
                        .withAgencyUid(agency.getUid()).withAgencyId(agency.getClientId().asLong()), subclient2);
        standaloneAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        standaloneCampaign = standaloneAdGroup.getCampaignInfo();

        // Группы объявлений
        subclient1AdGroup = createAdGroup(subclient1, defaultTextAdGroup(subclient1Campaign.getCampaignId()));
        subclient2AdGroup = createAdGroup(subclient2, defaultTextAdGroup(subclient2Campaign.getCampaignId()));
    }

    @Test
    @Description("Отключаем агентством набор корректировок созданный сублиентом")
    public void toggleBidModifierByAgencyTest() {
        BidModifierMobile bidModifierMobile = createDefaultBidModifierMobile(subclient1Campaign.getCampaignId());
        bidModifierService.add(
                singletonList(bidModifierMobile),
                subclient1Campaign.getClientId(), subclient1Campaign.getUid());

        List<UntypedBidModifier> items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(subclient1Campaign.getCampaignId())
                .withType(bidModifierMobile.getType())
                .withEnabled(false));
        MassResult<UntypedBidModifier> updateResult =
                bidModifierService.toggle(items, subclient1.getClientId(), agency.getUid());


        assertThat(updateResult.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Отключаем агентством набор корректировок субклиенту, указав кампанию другого субклиента")
    public void anotherSubclientModifierTest() {
        BidModifierMobile bidModifierMobile = createDefaultBidModifierMobile(subclient1Campaign.getCampaignId());
        bidModifierService.add(
                singletonList(bidModifierMobile),
                subclient1Campaign.getClientId(), subclient1Campaign.getUid());

        List<UntypedBidModifier> items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(subclient1Campaign.getCampaignId())
                .withType(bidModifierMobile.getType())
                .withEnabled(false));
        MassResult<UntypedBidModifier> updateResult =
                bidModifierService.toggle(items, subclient2.getClientId(), agency.getUid());

        assertThat(updateResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("campaignId")), CAMPAIGN_NOT_FOUND)));
    }

    @Test
    @Description("Отключаем агентством набор корректировок субклиенту, указав свою кампанию и кампанию другого сублиента")
    public void ownedModifierAndAnotherSubсlientModifierTest() {
        BidModifierMobile bidModifierMobile1 = createDefaultBidModifierMobile(subclient1Campaign.getCampaignId());
        bidModifierService.add(
                singletonList(bidModifierMobile1),
                subclient1Campaign.getClientId(), subclient1Campaign.getUid());

        BidModifierMobile bidModifierMobile2 = createDefaultBidModifierMobile(subclient2Campaign.getCampaignId());
        bidModifierService.add(
                singletonList(bidModifierMobile2),
                subclient2Campaign.getClientId(), subclient2Campaign.getUid());


        List<UntypedBidModifier> items = Arrays.asList((UntypedBidModifier) new UntypedBidModifier()
                        .withCampaignId(subclient1Campaign.getCampaignId())
                        .withType(bidModifierMobile1.getType())
                        .withEnabled(false),
                (UntypedBidModifier) new UntypedBidModifier()
                        .withCampaignId(subclient2Campaign.getCampaignId())
                        .withType(bidModifierMobile2.getType())
                        .withEnabled(false));
        MassResult<UntypedBidModifier> updateResult =
                bidModifierService.toggle(items, subclient1.getClientId(), agency.getUid());

        assertTrue(updateResult.getResult().get(0).isSuccessful());
        assertThat(updateResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(1), field("campaignId")), CAMPAIGN_NOT_FOUND)));
    }

    @Test
    @Description("Отключаем агентством набор корректировок субклиенту, указав кампанию самостоятельного клиента")
    public void nonOwnedModifierByAgencyTest() {
        BidModifierMobile bidModifierMobile = createDefaultBidModifierMobile(standaloneCampaign.getCampaignId());
        bidModifierService.add(
                singletonList(bidModifierMobile),
                standaloneCampaign.getClientId(), standaloneCampaign.getUid());

        List<UntypedBidModifier> items = singletonList((UntypedBidModifier) new UntypedBidModifier()
                .withCampaignId(standaloneCampaign.getCampaignId())
                .withType(bidModifierMobile.getType())
                .withEnabled(false));
        MassResult<UntypedBidModifier> updateResult =
                bidModifierService.toggle(items, subclient1.getClientId(), agency.getUid());

        assertThat(updateResult.getValidationResult(),
                hasDefectDefinitionWith(validationError(path(index(0), field("campaignId")), CAMPAIGN_NOT_FOUND)));
    }

    private AdGroup createAdGroup(ClientInfo client, AdGroup adGroup) {
        Long adGroupId = adGroupRepository.addAdGroups(dslContextProvider.ppc(client.getShard()).configuration(),
                client.getClientId(), singletonList(adGroup)).get(0);
        return adGroup.withId(adGroupId);
    }
}
