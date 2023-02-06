package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.notAllowedAttributionType;
import static ru.yandex.direct.feature.FeatureName.CROSS_DEVICE_ATTRIBUTION_TYPES;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithAttributionModelAddValidationTypeSupportTest {
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private CampaignWithAttributionModelAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignValidationContainer container;

    @Mock
    public FeatureService featureService;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MCBANNER},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        validationTypeSupport = new CampaignWithAttributionModelAddValidationTypeSupport(featureService);
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        operatorUid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, operatorUid, clientId);

        doReturn(emptySet())
                .when(featureService).getEnabledForClientId(clientId);
    }

    @Test
    public void validate_Successfully() {
        List<CampaignWithAttributionModel> validCampaigns = List.of(
                createCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK),
                createCampaign(CampaignAttributionModel.FIRST_CLICK),
                createCampaign(CampaignAttributionModel.LAST_CLICK),
                createCampaign(CampaignAttributionModel.LAST_SIGNIFICANT_CLICK),
                createCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE),
                createCampaign(CampaignAttributionModel.FIRST_CLICK_CROSS_DEVICE),
                createCampaign(CampaignAttributionModel.LAST_SIGNIFICANT_CLICK_CROSS_DEVICE));

        doReturn(Set.of(CROSS_DEVICE_ATTRIBUTION_TYPES.getName()))
                .when(featureService).getEnabledForClientId(clientId);

        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(validCampaigns));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_CrossDeviceAttributionTypesNotAllowed_WhenFeatureNotEnabled() {
        List<CampaignWithAttributionModel> validCampaigns = List.of(
                createCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK),
                createCampaign(CampaignAttributionModel.FIRST_CLICK),
                createCampaign(CampaignAttributionModel.LAST_CLICK),
                createCampaign(CampaignAttributionModel.LAST_SIGNIFICANT_CLICK));

        var vr = validationTypeSupport.validate(container, new ValidationResult<>(validCampaigns));
        assertThat(vr, hasNoDefectsDefinitions());

        List<CampaignWithAttributionModel> invalidCampaigns = List.of(
                createCampaign(CampaignAttributionModel.LAST_YANDEX_DIRECT_CLICK_CROSS_DEVICE),
                createCampaign(CampaignAttributionModel.FIRST_CLICK_CROSS_DEVICE),
                createCampaign(CampaignAttributionModel.LAST_SIGNIFICANT_CLICK_CROSS_DEVICE));

        vr = validationTypeSupport.validate(container, new ValidationResult<>(invalidCampaigns));
        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAttributionModel.ATTRIBUTION_MODEL)),
                notAllowedAttributionType())));
    }

    @Test
    public void validate_expectCannotBeNull() {
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(createCampaign(null))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAttributionModel.ATTRIBUTION_MODEL)),
                DefectIds.CANNOT_BE_NULL)));
    }

    private CampaignWithAttributionModel createCampaign(CampaignAttributionModel attributionModel) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType);
        campaign.withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithAttributionModel) campaign)
                .withAttributionModel(attributionModel);
    }
}
