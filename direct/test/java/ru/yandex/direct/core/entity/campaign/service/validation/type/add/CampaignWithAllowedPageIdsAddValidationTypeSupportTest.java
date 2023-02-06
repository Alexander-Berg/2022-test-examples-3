package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

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

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedPageIds;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.operatorCannotSetAllowedPageIds;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithAllowedPageIdsAddValidationTypeSupportTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private FeatureService featureService;

    @Mock
    private SspPlatformsRepository sspPlatformsRepository;

    @InjectMocks
    private CampaignWithAllowedPageIdsAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignValidationContainer container;
    private List<Long> validPageIds;
    private List<Long> invalidPageIds;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        operatorUid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, operatorUid, clientId);
        validPageIds = List.of(RandomNumberUtils.nextPositiveLong(), RandomNumberUtils.nextPositiveLong());
        invalidPageIds = List.of(RandomNumberUtils.nextPositiveLong(), -1L);
        doReturn(true)
                .when(featureService).isEnabledForUid(operatorUid, List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS));
    }

    @Test
    public void preValidate_Successfully() {
        var vr = validationTypeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(createCampaign(validPageIds))));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_Successfully() {
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(createCampaign(validPageIds))));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_expectNoRightsError() {
        doReturn(false)
                .when(featureService).isEnabledForUid(operatorUid, List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS));
        var vr = validationTypeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(createCampaign(validPageIds))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS)),
                operatorCannotSetAllowedPageIds())));
    }

    @Test
    public void validate_expectMustBeValidId() {
        var vr = validationTypeSupport.validate(container,
                new ValidationResult<>(List.of(createCampaign(invalidPageIds))));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS), index(1)),
                DefectIds.MUST_BE_VALID_ID)));
    }

    private CampaignWithAllowedPageIds createCampaign(List<Long> pageIds) {
        CommonCampaign campaign =  TestCampaigns.newCampaignByCampaignType(campaignType);
        campaign.withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithAllowedPageIds) campaign)
                .withAllowedPageIds(pageIds);
    }

    @Test
    public void preValidateAllowedDomain_expectedNoRightsError() {
        doReturn(false)
                .when(featureService).isEnabledForUid(operatorUid, List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS));
        var campaign = createCampaign(null);
        campaign.setAllowedDomains(List.of("mail.ru"));
        var vr = validationTypeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_DOMAINS)),
                operatorCannotSetAllowedPageIds())));
    }
}
