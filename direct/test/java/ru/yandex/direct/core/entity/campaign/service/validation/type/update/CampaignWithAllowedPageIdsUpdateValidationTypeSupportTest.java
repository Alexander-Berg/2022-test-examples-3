package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithAllowedPageIds;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(MockitoJUnitRunner.class)
public class CampaignWithAllowedPageIdsUpdateValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    @Mock
    private SspPlatformsRepository sspPlatformsRepository;

    @InjectMocks
    private CampaignWithAllowedPageIdsUpdateValidationTypeSupport typeSupport;

    private static ClientId clientId;
    private static Long uid;
    private static CampaignValidationContainer container;

    @Before
    public void before() {
        doReturn(true)
                .when(featureService).isEnabledForUid((Long) any(),
                eq(List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS)));
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, uid, clientId);
    }

    @Test
    public void preValidate_Successfully() {
        CampaignWithAllowedPageIds campaign = createCampaign();
        ModelChanges<CampaignWithAllowedPageIds> textCampaignModelChanges =
                ModelChanges.build(campaign, TextCampaign.ALLOWED_PAGE_IDS, null);
        var vr = typeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(textCampaignModelChanges)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_expectedNoRightsError() {
        doReturn(false)
                .when(featureService).isEnabledForUid((Long) any(),
                eq(List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS)));
        CampaignWithAllowedPageIds campaign = createCampaign();
        ModelChanges<CampaignWithAllowedPageIds> textCampaignModelChanges =
                ModelChanges.build(campaign, TextCampaign.ALLOWED_PAGE_IDS, null);
        var vr = typeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(textCampaignModelChanges)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS)),
                CampaignDefects.operatorCannotSetAllowedPageIds())));
    }

    @Test
    public void validate_expectedMustBeValidIdError() {
        CampaignWithAllowedPageIds campaign = createCampaign().withAllowedPageIds(List.of(123L, -12345L));
        var vr = typeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_PAGE_IDS), index(1)),
                DefectIds.MUST_BE_VALID_ID)));
    }

    private static CampaignWithAllowedPageIds createCampaign() {
        return new TextCampaign()
                .withClientId(clientId.asLong())
                .withAllowedPageIds(List.of(12345L))
                .withName("campaign")
                .withUid(uid);
    }

    @Test
    public void preValidateAllowedDomain_Successfully() {
        CampaignWithAllowedPageIds campaign = createCampaign();
        ModelChanges<CampaignWithAllowedPageIds> textCampaignModelChanges =
                ModelChanges.build(campaign, TextCampaign.ALLOWED_DOMAINS, null);
        var vr = typeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(textCampaignModelChanges)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidateAllowedDomain_expectedNoRightsError() {
        doReturn(false)
                .when(featureService).isEnabledForUid((Long) any(),
                eq(List.of(FeatureName.SET_CAMPAIGN_ALLOWED_PAGE_IDS)));
        CampaignWithAllowedPageIds campaign = createCampaign();
        ModelChanges<CampaignWithAllowedPageIds> textCampaignModelChanges =
                ModelChanges.build(campaign, TextCampaign.ALLOWED_DOMAINS, null);
        var vr = typeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(textCampaignModelChanges)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithAllowedPageIds.ALLOWED_DOMAINS)),
                CampaignDefects.operatorCannotSetAllowedPageIds())));
    }
}
