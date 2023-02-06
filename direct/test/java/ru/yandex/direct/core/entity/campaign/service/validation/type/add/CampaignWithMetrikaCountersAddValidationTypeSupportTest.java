package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Assert;
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
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.add.container.RestrictedCampaignsAddOperationContainerImpl;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignWithMetrikaCountersValidatorProvider;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.metrika.client.MetrikaClientException;
import ru.yandex.direct.metrika.client.model.response.UserCounters;
import ru.yandex.direct.utils.InterruptedRuntimeException;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.metrikaCounterIsUnavailable;
import static ru.yandex.direct.core.validation.defects.Defects.metrikaReturnsResultWithErrors;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveInteger;
import static ru.yandex.direct.test.utils.RandomNumberUtils.nextPositiveLong;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class CampaignWithMetrikaCountersAddValidationTypeSupportTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private FeatureService featureService;

    @Mock
    private RequestBasedMetrikaClientAdapter metrikaClientAdapter;

    @InjectMocks
    private CampaignWithMetrikaCountersValidatorProvider validatorProvider;


    private ClientId clientId;
    private Long uid;
    private int defaultCounterId;
    private CampaignWithMetrikaCounters defaultCampaign;
    private CampaignWithMetrikaCountersAddValidationTypeSupport typeSupport;
    private CampaignValidationContainer container;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(nextPositiveLong());
        uid = nextPositiveLong();
        defaultCounterId = nextPositiveInteger();

        defaultCampaign = (CampaignWithMetrikaCounters) TestCampaigns.newCampaignByCampaignType(campaignType);
        defaultCampaign.withMetrikaCounters(List.of((long) defaultCounterId));

        typeSupport = new CampaignWithMetrikaCountersAddValidationTypeSupport(validatorProvider);
        container = new RestrictedCampaignsAddOperationContainerImpl(0, uid, clientId, null, null, null,
                new CampaignOptions(), metrikaClientAdapter, emptyMap());
    }

    @Test
    public void testValidateSuccessfully_WithCountersAccessValidation() {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(Set.of(FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED.getName()));
        var userCounters = new UserCounters().withCounterIds(List.of(defaultCounterId));
        doReturn(List.of(userCounters)).when(metrikaClientAdapter).getUsersCountersNumByCampaignCounterIds();

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(defaultCampaign)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidateWithMetrikaClientException_WithCountersAccessValidation() {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(Set.of(FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED.getName()));
        doThrow(new MetrikaClientException())
                .when(metrikaClientAdapter).getUsersCountersNumByCampaignCounterIds();
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(defaultCampaign)));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void testValidateWithInterruptedRuntimeException_WithCountersAccessValidation() {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(Set.of(FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED.getName()));
        doThrow(new InterruptedRuntimeException())
                .when(metrikaClientAdapter).getUsersCountersNumByCampaignCounterIds();
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(defaultCampaign)));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), metrikaReturnsResultWithErrors())));
    }

    @Test
    public void testValidateErrors_WithCountersAccessValidation() {
        when(featureService.getEnabledForClientId(clientId))
                .thenReturn(Set.of(FeatureName.METRIKA_COUNTERS_ACCESS_VALIDATION_ON_SAVE_CAMPAIGN_ENABLED.getName()));
        var userCounters = new UserCounters().withCounterIds(List.of(2));
        doReturn(List.of(userCounters)).when(metrikaClientAdapter).getUsersCountersNumByCampaignCounterIds();

        var vr = typeSupport.validate(container,
                new ValidationResult<>(List.of(defaultCampaign)));

        var path = path(index(0), field(CampaignWithMetrikaCounters.METRIKA_COUNTERS), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(path, metrikaCounterIsUnavailable())));
    }

    @Test
    public void testSuccessfully_WithoutCountersAccessValidation() {
        when(featureService.getEnabledForClientId(clientId)).thenReturn(Set.of());
        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(defaultCampaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_WithMaxCountOfMetrikaCounters() {
        when(featureService.getEnabledForClientId(clientId)).thenReturn(Set.of());

        List<Long> counters = LongStream.range(1, CampaignConstants.MAX_NUMBER_OF_OPTIONAL_METRIKA_COUNTERS + 1)
                .boxed()
                .collect(Collectors.toList());
        CampaignWithMetrikaCounters campaign =
                (CampaignWithMetrikaCounters) TestCampaigns.newCampaignByCampaignType(campaignType);
        campaign.withMetrikaCounters(counters);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaign)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_WithTooManyMetrikaCounters() {
        when(featureService.getEnabledForClientId(clientId)).thenReturn(Set.of());

        List<Long> counters = LongStream.range(0, 101)
                .boxed()
                .collect(Collectors.toList());
        CampaignWithMetrikaCounters campaign =
                (CampaignWithMetrikaCounters) TestCampaigns.newCampaignByCampaignType(campaignType);
        campaign.withMetrikaCounters(counters);

        var vr = typeSupport.validate(container, new ValidationResult<>(List.of(campaign)));

        Path errPath = path(index(0), field(CampaignWithMetrikaCounters.METRIKA_COUNTERS));
        Assert.assertThat(vr, hasDefectDefinitionWith(validationError(errPath, SIZE_CANNOT_BE_MORE_THAN_MAX)));
    }
}
