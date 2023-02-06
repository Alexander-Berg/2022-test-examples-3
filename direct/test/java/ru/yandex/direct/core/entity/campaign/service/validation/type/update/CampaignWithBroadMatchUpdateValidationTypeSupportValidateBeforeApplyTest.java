package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.BroadMatch;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBroadMatch;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestCampaigns.manualStrategy;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class CampaignWithBroadMatchUpdateValidationTypeSupportValidateBeforeApplyTest {

    @Autowired
    private CampaignWithBroadMatchUpdateValidationTypeSupport typeSupport;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private Steps steps;

    private CampaignInfo campaignInfo1;
    private CampaignValidationContainer container;

    @Before
    public void before() {
        campaignInfo1 = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(manualStrategy()));
        container = CampaignValidationContainer
                .create(campaignInfo1.getShard(), campaignInfo1.getUid(), campaignInfo1.getClientId());
    }

    @Test
    public void validate_Successfully() {
        CampaignWithBroadMatch campaign =
                (CampaignWithBroadMatch) campaignTypedRepository.getTypedCampaigns(campaignInfo1.getShard(),
                        singletonList(campaignInfo1.getCampaignId())).get(0);

        campaign.withBroadMatch(new BroadMatch()
                .withBroadMatchFlag(true)
                .withBroadMatchLimit(40));

        var modelChanges = List.of(ModelChanges.build(campaign, TextCampaign.BROAD_MATCH, campaign.getBroadMatch()));

        var unmodifiedModels = Map.of(campaign.getId(),
                campaign.withBroadMatch(null));

        var vr = typeSupport.validateBeforeApply(container, new ValidationResult<>(modelChanges), unmodifiedModels);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_ValidButMissingFromUnmodifiedModelsCampaign_Successfully() {
        CampaignWithBroadMatch campaign =
                (CampaignWithBroadMatch) campaignTypedRepository.getTypedCampaigns(campaignInfo1.getShard(),
                        singletonList(campaignInfo1.getCampaignId())).get(0);

        var modelChanges = List.of(ModelChanges.build(campaign, TextCampaign.BROAD_MATCH,
                new BroadMatch()
                        .withBroadMatchFlag(true)
                        .withBroadMatchLimit(40)));

        Map<Long, CampaignWithBroadMatch> unmodifiedModels = Map.of(campaign.getId(), campaign);

        var vr = typeSupport.validateBeforeApply(container, new ValidationResult<>(modelChanges), unmodifiedModels);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBeforeApply_MetrikaUnavailable_Error() {
        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(manualStrategy()), campaignInfo1.getClientInfo());
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(
                campaignInfo1.getShard(), asList(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));
        ((TextCampaign) typedCampaigns.get(0)).withMetrikaCounters(singletonList(456L));

        MetrikaGoalsService metrikaGoalsService = mock(MetrikaGoalsService.class);
        when(metrikaGoalsService.getAvailableBroadMatchesForCampaignId(anyLong(), any(ClientId.class), anyMap()))
                .thenThrow(MetrikaClientException.class);

        var modelChanges = mapList(typedCampaigns, campaign ->
                new ModelChanges<>(campaign.getId(), CampaignWithBroadMatch.class)
                        .process(((CampaignWithBroadMatch) campaign).getBroadMatch(),
                                CampaignWithBroadMatch.BROAD_MATCH));

        var unmodifiedModels = Map.of(typedCampaigns.get(0).getId(),
                ((CampaignWithBroadMatch) typedCampaigns.get(0)).withBroadMatch(null),
                typedCampaigns.get(1).getId(),
                ((CampaignWithBroadMatch) typedCampaigns.get(1)).withBroadMatch(null));
        var vr = new CampaignWithBroadMatchUpdateValidationTypeSupport(metrikaGoalsService)
                .validateBeforeApply(container,
                        new ValidationResult<>(modelChanges), unmodifiedModels);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
        assertThat(vr, hasDefectWithDefinition(validationError(path(index(1)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void validateBeforeApply_MetrikaUnavailableOneGoalNotChangedOneGoalChanged_Error() {
        CampaignInfo campaignInfo2 = steps.campaignSteps().createCampaign(TestCampaigns.activeTextCampaign(null, null)
                .withStrategy(manualStrategy()), campaignInfo1.getClientInfo());
        List<? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaigns(
                campaignInfo1.getShard(), asList(campaignInfo1.getCampaignId(), campaignInfo2.getCampaignId()));
        ((TextCampaign) typedCampaigns.get(0)).withMetrikaCounters(singletonList(456L));

        MetrikaGoalsService metrikaGoalsService = mock(MetrikaGoalsService.class);
        when(metrikaGoalsService.getAvailableBroadMatchesForCampaignId(anyLong(), any(ClientId.class), anyMap()))
                .thenThrow(MetrikaClientException.class);

        var modelChanges = mapList(typedCampaigns, campaign -> ModelChanges
                .build((CampaignWithBroadMatch) campaign, TextCampaign.BROAD_MATCH,
                        ((CampaignWithBroadMatch) campaign).getBroadMatch()));
        var unmodifiedModels = Map.of(typedCampaigns.get(0).getId(),
                ((CampaignWithBroadMatch) typedCampaigns.get(0)).withBroadMatch(null),
                typedCampaigns.get(1).getId(),
                ((CampaignWithBroadMatch) typedCampaigns.get(1)));
        var vr = new CampaignWithBroadMatchUpdateValidationTypeSupport(metrikaGoalsService)
                .validateBeforeApply(container, new ValidationResult<>(modelChanges), unmodifiedModels);

        assertThat(vr, hasDefectWithDefinition(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
        Assertions.assertThat(vr.flattenErrors().size() == 1).isTrue();
    }

}
