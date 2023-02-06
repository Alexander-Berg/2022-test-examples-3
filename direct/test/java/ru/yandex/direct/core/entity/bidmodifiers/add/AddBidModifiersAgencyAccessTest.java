package ru.yandex.direct.core.entity.bidmodifiers.add;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.adGroupNotFound;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientMobileAdjustment;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientMobileModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка доступа агентства к кампаниям и группам клиента при добавлении корректировок ставок")
public class AddBidModifiersAgencyAccessTest {
    @Autowired
    private BidModifierService bidModifierService;


    @Autowired
    private AdGroupRepository adGroupRepository;

    @Autowired
    private AdGroupSteps adGroupSteps;

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

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

        subclient1Campaign = steps.campaignSteps().createCampaign(
                activeTextCampaign(subclient1.getClientId(), subclient1.getUid())
                .withAgencyUid(agency.getUid()).withAgencyId(agency.getClientId().asLong()), subclient1);
        subclient2Campaign = steps.campaignSteps().createCampaign(
                activeTextCampaign(subclient2.getClientId(), subclient2.getUid())
                        .withAgencyUid(agency.getUid()).withAgencyId(agency.getClientId().asLong()), subclient2);
        standaloneAdGroup = adGroupSteps.createActiveDynamicTextAdGroup();
        standaloneCampaign = standaloneAdGroup.getCampaignInfo();

        // Группы объявлений
        subclient1AdGroup = createAdGroup(subclient1, defaultTextAdGroup(subclient1Campaign.getCampaignId()));
        subclient2AdGroup = createAdGroup(subclient2, defaultTextAdGroup(subclient2Campaign.getCampaignId()));
    }

    @Test
    @Description("Добавим агентством корректировку ставок в кампанию, созданную сублиентом")
    public void addBidModifierByAgencyTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав кампанию другого сублиента")
    public void anotherSubclientCampaignTest() {
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient2.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND))));
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав свою кампанию и кампанию другого сублиента")
    public void ownedCampaignAndAnotherSubclientCampaignTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment()),
                        createEmptyClientMobileModifier()
                                .withCampaignId(subclient2Campaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid()
        );
        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("campaignId")),
                            new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND)))));
        });
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав кампанию самостоятельного клиента")
    public void nonOwnedCampaignByAgencyTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withCampaignId(standaloneCampaign.getCampaignId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid()
        );
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("campaignId")),
                        new Defect<>(CampaignDefectIds.Gen.CAMPAIGN_NOT_FOUND))));
    }

    @Test
    @Description("Добавим агентством корректировку ставок в группу, созданную сублиентом")
    public void addBidModifierToAdGroupByAgencyTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(subclient1AdGroup.getId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid()
        );
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав группу другого сублиента")
    public void anotherSubclientGroupTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(subclient1AdGroup.getId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient2.getClientId(), agency.getUid()
        );
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")), adGroupNotFound(subclient1AdGroup.getId()))));
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав свою группу и группу другого сублиента")
    public void ownedGroupAndAnotherSubclientGroupTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(subclient1AdGroup.getId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment()),
                        createEmptyClientMobileModifier()
                                .withAdGroupId(subclient2AdGroup.getId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid()
        );
        assertSoftly(softly -> {
            softly.assertThat(result.getResult()).hasSize(2);
            softly.assertThat(result.getResult().get(0).getValidationResult()).is(matchedBy(hasNoDefectsDefinitions()));
            softly.assertThat(result.getResult().get(1).getValidationResult()).is(matchedBy(hasDefectWithDefinition(
                    validationError(path(field("adGroupId")), adGroupNotFound(subclient2AdGroup.getId())))));
        });
    }

    @Test
    @Description("Добавим агентством корректировку ставок субклиенту, указав группу самостоятельного клиента")
    public void nonOwnedGroupByAgencyTest() {
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientMobileModifier()
                                .withAdGroupId(standaloneAdGroup.getAdGroupId())
                                .withMobileAdjustment(createDefaultClientMobileAdjustment())),
                subclient1.getClientId(), agency.getUid()
        );
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("adGroupId")),
                        adGroupNotFound(standaloneAdGroup.getAdGroupId()))));
    }

    private AdGroup createAdGroup(ClientInfo client, AdGroup adGroup) {
        Long adGroupId = adGroupRepository.addAdGroups(dslContextProvider.ppc(client.getShard()).configuration(),
                client.getClientId(), singletonList(adGroup)).get(0);
        return adGroup.withId(adGroupId);
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
