package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithDisallowedPageIds;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
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
public class CampaignWithDisallowedPageIdsUpdateValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private CampaignWithDisallowedPageIdsUpdateValidationTypeSupport typeSupport;

    private static ClientId clientId;
    private static Long uid;
    private static CampaignValidationContainer container;

    @Before
    public void before() {
        doReturn(true)
                .when(featureService).isEnabledForUid((Long) any(),
                eq(List.of(FeatureName.SET_CAMPAIGN_DISALLOWED_PAGE_IDS)));
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, uid, clientId);
    }

    @Test
    public void preValidate_Successfully() {
        CampaignWithDisallowedPageIds campaign = createCampaign();
        ModelChanges<CampaignWithDisallowedPageIds> modelChanges =
                ModelChanges.build(campaign, CpmBannerCampaign.DISALLOWED_PAGE_IDS, null);
        var vr = typeSupport.preValidate(
                container,
                new ValidationResult<>(List.of(modelChanges)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_expectedMustBeValidIdError() {
        CampaignWithDisallowedPageIds campaign = createCampaign().withDisallowedPageIds(List.of(123L, -12345L));
        var vr = typeSupport.validate(container,
                new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithDisallowedPageIds.DISALLOWED_PAGE_IDS), index(1)),
                DefectIds.MUST_BE_VALID_ID)));
    }

    private static CampaignWithDisallowedPageIds createCampaign() {
        return new CpmBannerCampaign()
                .withClientId(clientId.asLong())
                .withDisallowedPageIds(List.of(12345L))
                .withName("campaign")
                .withUid(uid);
    }
}
