package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegmentAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.core.testing.stub.MetrikaHelperStub;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.abSegmentNotFound;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultMetrikaGoals;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.listToSet;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithBidModifiersUpdateValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaClientStub metrikaClientStub;
    @Autowired
    private CampaignWithBidModifiersUpdateValidationTypeSupport typeSupport;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MetrikaHelperStub metrikaHelperStub;
    private ClientInfo defaultClient;
    private long invalidAbSegment;
    private long sectionId;
    private Goal abSegment;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Before
    public void before() {
        defaultClient = steps.clientSteps().createDefaultClient();
        invalidAbSegment = 1L;
        sectionId = 1L;
        List<Goal> goals = defaultMetrikaGoals();
        abSegment = goals.get(4);
        abSegment.setSectionId(sectionId);
        metrikaClientStub.addGoals(defaultClient.getUid(), new HashSet<>(goals));
        metrikaHelperStub.addGoalIds(defaultClient.getUid(), listToSet(goals, GoalBase::getId));
    }

    @Test
    public void validate_ThreeCampaignsWithBidModifiers() {
        CampaignWithBidModifiers campaignWithoutBidModifiers = getTypedCampaign();

        BidModifierABSegment invalidBidModifierABSegment = getBidModifierABSegment(invalidAbSegment, sectionId);
        CampaignWithBidModifiers campaignWithInvalidBidModifiers =
                getCampaignWithBidModifiers(invalidBidModifierABSegment);

        BidModifierABSegment validBidModifierABSegment = getBidModifierABSegment(abSegment.getId(),
                abSegment.getSectionId());
        CampaignWithBidModifiers campaignWithValidBidModifiers =
                getCampaignWithBidModifiers(validBidModifierABSegment);
        var container = CampaignValidationContainer
                .create(defaultClient.getShard(), defaultClient.getUid(), defaultClient.getClientId());
        var vr =
                typeSupport.validate(container,
                        new ValidationResult<>(List.of(campaignWithoutBidModifiers, campaignWithValidBidModifiers,
                                campaignWithInvalidBidModifiers)));

        assertThat(vr.flattenErrors()).hasSize(1);
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(
                path(
                        index(2), field(CampaignWithBidModifiers.BID_MODIFIERS),
                        index(0), field(BidModifierABSegment.AB_SEGMENT_ADJUSTMENTS),
                        index(0)),
                abSegmentNotFound(invalidAbSegment)))));
    }

    private BidModifierABSegment getBidModifierABSegment(long abSegmentId, long sectionId) {
        return new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                .withAbSegmentAdjustments(singletonList(new BidModifierABSegmentAdjustment()
                                .withPercent(11)
                                .withSectionId(sectionId)
                                .withSegmentId(abSegmentId)
                        )
                );
    }

    private CampaignWithBidModifiers getCampaignWithBidModifiers(BidModifierABSegment invalidBidModifierABSegment) {
        CampaignWithBidModifiers campaignWithInvalidBidModifiers = getTypedCampaign();
        campaignWithInvalidBidModifiers.setBidModifiers(singletonList(invalidBidModifierABSegment));
        return campaignWithInvalidBidModifiers;
    }

    private CampaignWithBidModifiers getTypedCampaign() {
        CampaignInfo campaignInfoFirst =
                steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, defaultClient);
        return (CampaignWithBidModifiers) campaignTypedRepository.getTypedCampaigns(campaignInfoFirst.getShard(),
                singletonList(campaignInfoFirst.getCampaignId())).get(0);
    }

}
