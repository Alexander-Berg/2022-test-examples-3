package ru.yandex.direct.core.entity.bidmodifiers.add.retargeting;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargetingAdjustment;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.core.testing.steps.RetConditionSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.retargeting.service.validation2.RetargetingDefects.retargetingConditionNotFoundDetailed;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultClientRetargetingAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyClientRetargetingModifier;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Проверка доступа агентства к кампаниям клиента при добавлении корректировок ставок")
public class AddBidModifiersRetargetingConditionAgencyAccessTest {

    @Autowired
    private BidModifierService bidModifierService;

    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private RetConditionSteps retConditionSteps;

    @Autowired
    private ClientSteps clientSteps;

    private ClientInfo agency;
    private ClientInfo subclient1;
    private ClientInfo subclient2;
    private CampaignInfo subclient1Campaign;  // Кампания субклиента 1
    private CampaignInfo standaloneCampaign;  // Кампания самостоятельного клиента

    @Autowired
    private UserSteps userSteps;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Before
    public void before() {
        agency = clientSteps.createDefaultClientWithRole(RbacRole.AGENCY);

        subclient1 = clientSteps.createDefaultClientUnderAgency(agency);
        subclient2 = clientSteps.createDefaultClientUnderAgency(agency);

        subclient1Campaign = campaignSteps.createCampaign(
                TestCampaigns.activeTextCampaign(subclient1.getClientId(), subclient1.getUid())
                        .withAgencyUid(agency.getUid()).withAgencyId(agency.getClientId().asLong()),
                subclient1
        );
        standaloneCampaign = campaignSteps.createActiveTextCampaign();
    }

    @Test
    @Description("Добавим агентством ретаргетинговую корректировку, используя условие ретаргетинга, созданное субклиентом")
    public void addBidModifierByAgencyTest() {
        long retCondId = retConditionSteps.createDefaultRetCondition(subclient1).getRetConditionId();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId))),
                subclient1.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasNoDefectsDefinitions());
    }

    @Test
    @Description("Добавим агентством ретаргетинговую корректировку субклиенту, указав условие ретаргетинга другого субклиента")
    public void anotherSubclientRetargetingConditionTest() {
        long retCondId = retConditionSteps.createDefaultRetCondition(subclient2).getRetConditionId();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withRetargetingAdjustments(createDefaultClientRetargetingAdjustments(retCondId))),
                subclient1.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments"), index(0)),
                        retargetingConditionNotFoundDetailed(retCondId))));
    }

    @Test
    @Description("Добавим агентством ретаргетинговую корректировку ставок субклиенту, " +
            "указав своё условие ретаргетинга и условие ретаргетинга другого субклиента")
    public void ownedConditionAndAnotherSubclientConditionTest() {
        long retCondId = retConditionSteps.createDefaultRetCondition(subclient1).getRetConditionId();
        long anotherClientRetCondId = retConditionSteps.createDefaultRetCondition(subclient2).getRetConditionId();
        MassResult<List<Long>> result = addBidModifiers(
                Lists.newArrayList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withRetargetingAdjustments(
                                        Lists.newArrayList(
                                                new BidModifierRetargetingAdjustment()
                                                        .withRetargetingConditionId(retCondId).withPercent(110),
                                                new BidModifierRetargetingAdjustment()
                                                        .withRetargetingConditionId(anotherClientRetCondId)
                                                        .withPercent(120)))),
                subclient1.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments"), index(1)),
                        retargetingConditionNotFoundDetailed(anotherClientRetCondId))));
    }

    @Test
    @Description("Добавим агентством ретаргетинговую корректировку ставок субклиенту, указав условие ретаргетинга самостоятельного клиента")
    public void nonOwnedConditionByAgencyTest() {
        long anotherClientRetCondId =
                retConditionSteps.createDefaultRetCondition(standaloneCampaign.getClientInfo()).getRetConditionId();
        MassResult<List<Long>> result = addBidModifiers(
                singletonList(
                        createEmptyClientRetargetingModifier()
                                .withCampaignId(subclient1Campaign.getCampaignId())
                                .withRetargetingAdjustments(
                                        createDefaultClientRetargetingAdjustments(anotherClientRetCondId))),
                subclient1.getClientId(), agency.getUid());
        assertThat(result.getValidationResult(), hasDefectWithDefinition(
                validationError(path(index(0), field("retargetingAdjustments"), index(0)),
                        retargetingConditionNotFoundDetailed(anotherClientRetCondId))));
    }

    private MassResult<List<Long>> addBidModifiers(List<BidModifier> bidModifiers, ClientId clientId, Long clientUid) {
        bidModifiers.forEach(bidModifier -> bidModifier.setEnabled(true));
        return bidModifierService.add(bidModifiers, clientId, clientUid);
    }
}
