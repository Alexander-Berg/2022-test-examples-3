package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierABSegment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierGeo;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithBidModifiers;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(Parameterized.class)
public class CampaignWithBidModifiersUpdateOperationSupportEnrichTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private FeatureService featureService;

    @Mock
    private BidModifierService bidModifierService;

    @InjectMocks
    private CampaignWithBidModifiersUpdateOperationSupport updateOperationSupport;

    private ModelChanges<CampaignWithBidModifiers> modelChanges;
    private BidModifierGeo oldGeoBidModifier;
    private BidModifierGeo newGeoBidModifier;
    private long campaignId;
    private ClientId clientId;
    private RestrictedCampaignsUpdateOperationContainer updateParameters;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.DYNAMIC},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.MCBANNER},
                {CampaignType.PERFORMANCE}
        });
    }

    @Before
    public void before() {
        campaignId = RandomNumberUtils.nextPositiveLong();
        modelChanges = new ModelChanges<>(campaignId, CampaignWithBidModifiers.class);
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        oldGeoBidModifier = new BidModifierGeo()
                .withType(BidModifierType.GEO_MULTIPLIER)
                .withCampaignId(campaignId)
                .withEnabled(false);
        newGeoBidModifier = new BidModifierGeo()
                .withType(BidModifierType.GEO_MULTIPLIER)
                .withCampaignId(campaignId)
                .withEnabled(true);
        updateParameters = RestrictedCampaignsUpdateOperationContainer.create(
                0,
                null,
                clientId,
                null,
                null
        );
    }

    @Test
    public void enrich_CopyOldGeoBidModifiers_NewIsNull() {
        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        modelChanges.process(null, CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).isNotEmpty();
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(0))
                .isEqualTo(oldGeoBidModifier);
    }

    @Test
    public void enrich_CopyOldGeoBidModifiers_NewIsNullAndFeatureOn() {
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA);

        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        modelChanges.process(null, CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).isNull();
    }

    @Test
    public void enrich_CopyOldGeoBidModifiers_NewIsEmpty() {
        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        modelChanges.process(emptyList(), CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).isNotEmpty();
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(0))
                .isEqualTo(oldGeoBidModifier);
    }

    @Test
    public void enrich_CopyOldGeoBidModifiers_NewIsEmptyAndFeatureOn() {
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA);
        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        modelChanges.process(emptyList(), CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).isEmpty();
    }

    @Test
    public void enrich_CopyOldGeoBidModifiers_NewIsNotEmpty() {
        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        BidModifierABSegment abSegmentBidModifier = new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                .withCampaignId(campaignId);
        modelChanges.process(List.of(abSegmentBidModifier), CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).hasSize(2);
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(0))
                .isEqualTo(oldGeoBidModifier);
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(1))
                .isEqualTo(abSegmentBidModifier);
    }

    @Test
    public void enrich_DoNotCopyOldGeoBidModifiers_NewIsNotEmptyAndFeatureOn() {
        doReturn(true).when(featureService).isEnabledForClientId(clientId,
                FeatureName.GEO_RATE_CORRECTIONS_ALLOWED_FOR_DNA);
        CampaignWithBidModifiers campaignFromDb =
                ((CampaignWithBidModifiers) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(campaignId)
                        .withBidModifiers(List.of(oldGeoBidModifier));

        BidModifierABSegment abSegmentBidModifier = new BidModifierABSegment()
                .withType(BidModifierType.AB_SEGMENT_MULTIPLIER)
                .withCampaignId(campaignId);
        modelChanges.process(List.of(newGeoBidModifier, abSegmentBidModifier), CampaignWithBidModifiers.BID_MODIFIERS);

        AppliedChanges<CampaignWithBidModifiers> appliedChanges = modelChanges.applyTo(campaignFromDb);

        updateOperationSupport.onChangesApplied(updateParameters, List.of(appliedChanges));

        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS)).hasSize(2);
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(0))
                .isEqualTo(newGeoBidModifier);
        assertThat(appliedChanges.getNewValue(CampaignWithBidModifiers.BID_MODIFIERS).get(1))
                .isEqualTo(abSegmentBidModifier);
    }

}
