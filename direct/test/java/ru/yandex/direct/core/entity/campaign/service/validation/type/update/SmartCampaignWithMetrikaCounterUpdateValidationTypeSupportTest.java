package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;
import java.util.Set;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import one.util.streamex.LongStreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.SmartCampaign;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignWithMetrikaCountersValidatorProvider;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.METRIKA_COUNTER_IS_UNAVAILABLE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(JUnitParamsRunner.class)
public class SmartCampaignWithMetrikaCounterUpdateValidationTypeSupportTest {

    @Mock
    private FeatureService featureService;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    @InjectMocks
    private CampaignWithMetrikaCountersValidatorProvider validatorProvider;

    private final long validCounterId = 5L;
    private final long invalidCounterId = 1L;

    public Object[] countersAccessValidationEnabledParams() {
        return new Object[][]{
                {true},
                {false}
        };
    }

    private CampaignValidationContainer container;
    private CampaignWithMetrikaCountersUpdateValidationTypeSupport typeSupport;
    private ClientId clientId;
    private Long uid;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();

        MockitoAnnotations.initMocks(this);
        var userCounters = new UserCounters().withCounterIds(List.of((int) validCounterId));
        when(metrikaClientAdapter.getUsersCountersNumByCampaignCounterIds()).thenReturn(List.of(userCounters));
        when(featureService.getEnabledForClientId(clientId)).thenReturn(Set.of());

        typeSupport = new CampaignWithMetrikaCountersUpdateValidationTypeSupport(validatorProvider);
        container = new RestrictedCampaignsUpdateOperationContainerImpl(0, uid, clientId, null, null,
                metrikaClientAdapter, new CampaignOptions(), null, emptyMap());
    }

    @Test
    public void validate() {
        var campaign = getSmartCampaign(validCounterId);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaign)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_withoutCounters() {
        var smartCampaignModelChanges = getSmartCampaign();

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(smartCampaignModelChanges)));

        Path errPath = path(index(0), field(CampaignWithMetrikaCounters.METRIKA_COUNTERS));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, SIZE_CANNOT_BE_LESS_THAN_MIN)));
    }

    @Test
    public void validate_withTwoCounters() {
        var smartCampaignModelChanges = getSmartCampaign(validCounterId, invalidCounterId);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(smartCampaignModelChanges)));

        Path errPath = path(index(0), field(CampaignWithMetrikaCounters.METRIKA_COUNTERS));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, SIZE_CANNOT_BE_MORE_THAN_MAX)));
    }

    @Test
    @Parameters(method = "countersAccessValidationEnabledParams")
    public void validate_withInvalidCounterId(boolean countersAccessValidationEnabled) {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(countersAccessValidationEnabled ?
                        Set.of(FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED.getName()) :
                        Set.of());
        var campaign = getSmartCampaign(invalidCounterId);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaign)));

        Path errPath = path(index(0), field(CampaignWithMetrikaCounters.METRIKA_COUNTERS), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(errPath, METRIKA_COUNTER_IS_UNAVAILABLE)));
    }

    @Test
    public void validate_withInvalidCounterId_unavailableGoalsAllowed() {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(Set.of(FeatureName.DIRECT_UNAVAILABLE_GOALS_ALLOWED.getName()));
        var campaign = getSmartCampaign(invalidCounterId);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    private CampaignWithMetrikaCounters getSmartCampaign(long... counters) {
        return new SmartCampaign()
                .withMetrikaCounters(LongStreamEx.of(counters).boxed().toList());
    }
}
