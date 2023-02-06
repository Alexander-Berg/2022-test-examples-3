package ru.yandex.direct.grid.processing.service.client;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.client.GdRequestChoiceFromConversionModifiersPopup;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.ALL_BID_MODIFIER_TYPES;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultDemographicsAdjustments;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createEmptyDemographicsModifier;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAverageCpaStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.feature.FeatureName.CONVERSION_MODIFIERS_POPUP_ENABLED;

@GridProcessingTest
@RunWith(SpringRunner.class)
public class SaveChoiceFromConversionModifiersPopupTest {
    @Autowired
    ClientMutationService clientMutationService;

    @Autowired
    ClientService clientService;

    @Autowired
    BidModifierService bidModifierService;

    @Autowired
    Steps steps;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private GridGraphQLContext operatorContext;
    private UserInfo user;

    @Before
    public void before() {
        user = steps.userSteps().createDefaultUser();
        operatorContext = new GridGraphQLContext(user.getUser());
    }

    @Test
    public void clientWithEnabledPopup_TurnOffPopup() {
        steps.featureSteps().addClientFeature(user.getClientId(), CONVERSION_MODIFIERS_POPUP_ENABLED, true);
        GdRequestChoiceFromConversionModifiersPopup requestChoiceFromConversionModifiersPopup =
                new GdRequestChoiceFromConversionModifiersPopup()
                        .withShouldCleanMultipliers(true);
        clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup);
        Client client = clientService.getClient(user.getClientId());

        assertThat(client.getIsConversionMultipliersPopupDisabled()).isTrue();
    }

    @Test
    public void clientWithEnabledPopup_SentTrueChoice_DeleteBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);

        steps.featureSteps().addClientFeature(user.getClientId(), CONVERSION_MODIFIERS_POPUP_ENABLED, true);
        GdRequestChoiceFromConversionModifiersPopup requestChoiceFromConversionModifiersPopup =
                new GdRequestChoiceFromConversionModifiersPopup()
                        .withShouldCleanMultipliers(true);
        clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup);

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        Assertions.assertThat(actualBidModifiers).isEmpty();

    }

    @Test
    public void clientWithEnabledPopup_SentFalseChoice_NotDeleteBidModifiers() {
        TextCampaign textCampaign = getConversionCampaignWithBidModifier();
        TextCampaignInfo campaign = steps.textCampaignSteps().createCampaign(user.getClientInfo(), textCampaign);
        addCampaignBidModifier(campaign);

        steps.featureSteps().addClientFeature(user.getClientId(), CONVERSION_MODIFIERS_POPUP_ENABLED, true);
        GdRequestChoiceFromConversionModifiersPopup requestChoiceFromConversionModifiersPopup =
                new GdRequestChoiceFromConversionModifiersPopup()
                        .withShouldCleanMultipliers(false);
        clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup);

        List<BidModifier> actualBidModifiers = bidModifierService.getByCampaignIds(campaign.getShard(),
                Set.of(campaign.getId()), ALL_BID_MODIFIER_TYPES,
                Set.of(BidModifierLevel.CAMPAIGN, BidModifierLevel.ADGROUP));

        Assertions.assertThat(actualBidModifiers).isNotEmpty();

    }

    @Test
    public void clientWithDisablePopupFeature_ThrowValidationException() {
        GdRequestChoiceFromConversionModifiersPopup requestChoiceFromConversionModifiersPopup =
                new GdRequestChoiceFromConversionModifiersPopup()
                        .withShouldCleanMultipliers(true);
        assertThatThrownBy(() -> clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup)).isInstanceOf(GridValidationException.class);
    }

    @Test
    public void clientWithDisablePopupFlag_ThrowValidationException() {
        steps.featureSteps().addClientFeature(user.getClientId(), CONVERSION_MODIFIERS_POPUP_ENABLED, true);
        Client client = user.getClientInfo().getClient();
        AppliedChanges<Client> appliedChanges =
                ModelChanges.build(client, Client.IS_CONVERSION_MULTIPLIERS_POPUP_DISABLED, true)
                        .applyTo(client);
        clientService.update(appliedChanges);

        GdRequestChoiceFromConversionModifiersPopup requestChoiceFromConversionModifiersPopup =
                new GdRequestChoiceFromConversionModifiersPopup()
                        .withShouldCleanMultipliers(true);

        clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup);

        assertThatThrownBy(() -> clientMutationService.saveChoiceFromConversionModifiersPopup(
                operatorContext, requestChoiceFromConversionModifiersPopup)).isInstanceOf(GridValidationException.class);
    }

    private void addCampaignBidModifier(TextCampaignInfo campaign) {
        var demographicForCampaign = createEmptyDemographicsModifier()
                .withDemographicsAdjustments(createDefaultDemographicsAdjustments())
                .withCampaignId(campaign.getId());
        bidModifierService.add(List.of(demographicForCampaign), user.getClientId(), user.getUid());
    }

    private TextCampaign getConversionCampaignWithBidModifier() {
        TextCampaign textCampaign =
                defaultTextCampaignWithSystemFields(user.getClientInfo());

        DbStrategy strategy =
                (DbStrategy) defaultAverageCpaStrategy((long) RandomNumberUtils.nextPositiveInteger()).withAutobudget(CampaignsAutobudget.YES);
        textCampaign
                .withAttributionModel(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK)
                .withStrategy(strategy);
        return textCampaign;
    }

}
