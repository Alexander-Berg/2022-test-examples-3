package ru.yandex.direct.web.entity.inventori.service;

import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessChecker;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessValidator;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignAccessType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.web.core.entity.inventori.model.CampaignStrategy;
import ru.yandex.direct.web.core.entity.inventori.model.CpmForecastRequest;
import ru.yandex.direct.web.core.entity.inventori.model.ImpressionLimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.CommonDefects.validId;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThanOrEqualTo;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignForecastValidationServiceTest {
    private static final Long campaignId = 31629089L;
    private static final ClientId clientId = ClientId.fromLong(1L);
    private static final Long operatorId = 1L;

    private CampaignForecastValidationService service;
    private CampaignSubObjectAccessValidator mockValidator;

    @Before
    public void setup() {
        mockValidator = mock(CampaignSubObjectAccessValidator.class);
        CampaignSubObjectAccessChecker mockChecker = mock(CampaignSubObjectAccessChecker.class);
        CampaignSubObjectAccessCheckerFactory mockFactory = mock(CampaignSubObjectAccessCheckerFactory.class);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        service = new CampaignForecastValidationService(mockFactory, campaignRepository, shardHelper);

        when(mockValidator.apply(campaignId))
                .thenReturn(new ValidationResult<>(campaignId));
        when(mockChecker.createValidator(CampaignAccessType.READ))
                .thenReturn(mockValidator);
        when(mockFactory.newCampaignChecker(anyLong(), any(ClientId.class), anyCollection()))
                .thenReturn(mockChecker);
    }

    @Test
    public void validate_campaignAccessByClientIsChecked() {
        CpmForecastRequest request = new CpmForecastRequest(campaignId, null, new CampaignStrategy());

        ValidationResult<CpmForecastRequest, Defect> result = service.validateCampaignForecastRequest(request, operatorId, clientId);

        verify(mockValidator, atLeastOnce()).apply(anyLong());
    }

    @Test
    public void validate_fullRequest_noError() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(request(), operatorId, clientId);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_CampaignIdAndExampleTypeAreNull_errorsAreGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(null),
                operatorId,
                clientId);
        assertThat(vr.flattenErrors()).is(matchedBy(hasItems(
                validationError(path(field("campaign_id")), notNull()),
                validationError(path(field("new_campaign_example_type")), notNull()))));
    }

    @Test
    public void validate_CampaignIdAndExampleTypeAreValid_errorsAreGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(campaignId).withNewCampaignExampleType(1),
                operatorId,
                clientId);
        assertThat(vr.flattenErrors()).is(matchedBy(hasItems(
                validationError(path(field("campaign_id")), isNull()),
                validationError(path(field("new_campaign_example_type")), isNull()))));
    }

    @Test
    public void validate_campaignIdIsZero_errorIsGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(0L),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("campaign_id")), validId()))));
    }

    @Test
    public void validate_exampleTypeIsNegative_errorIsGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(null).withNewCampaignExampleType(-1),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("new_campaign_example_type")), inCollection()))));
    }

    @Test
    public void validate_exampleTypeIsGreaterThanOne_errorIsGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(null).withNewCampaignExampleType(2),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("new_campaign_example_type")), inCollection()))));
    }

    @Test
    public void validate_exampleTypeIsZero_validationSuccessful() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(null).withNewCampaignExampleType(0),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_exampleTypeIsOne_validationSuccessful() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withCampaignId(null).withNewCampaignExampleType(1),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_strategyIsNull_errorIsGenerated() {
        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request().withStrategy(null),
                operatorId,
                clientId);
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy")), notNull()))));
    }

    @Test
    public void validate_strategyTypeIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withType(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("type")), notNull()))));
    }

    @Test
    public void validate_strategyTypeIsNotInSet_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withType("test");

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("type")), inCollection()))));
    }

    @Test
    public void validate_strategyBudgetIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withBudget(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("budget")), notNull()))));
    }

    @Test
    public void validate_strategyBudgetIsZero_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withBudget(0.0);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("budget")), greaterThan(0.0)))));
    }

    @Test
    public void validate_strategyStartDateIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withStartDate(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("start_date")), notNull()))));
    }

    @Test
    public void validate_strategyEndDateIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withEndDate(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("end_date")), notNull()))));
    }

    @Test
    public void validate_impressionLimitIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().withImpressionLimit(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("impression_limit")), notNull()))));
    }

    @Test
    public void validate_impressionLimitDaysIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().getImpressionLimit().withDays(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("impression_limit"), field("days")),
                        notNull()))));
    }

    @Test
    public void validate_impressionLimitDaysIsLessThanZero_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().getImpressionLimit().withDays(-1L);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("impression_limit"), field("days")),
                        greaterThanOrEqualTo(0L)))));
    }

    @Test
    public void validate_impressionLimitImpressionsIsNull_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().getImpressionLimit().withImpressions(null);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("impression_limit"), field("impressions")),
                        notNull()))));
    }

    @Test
    public void validate_impressionLimitImpressionsIsLessThanZero_errorIsGenerated() {
        CpmForecastRequest request = request();
        request.getStrategy().getImpressionLimit().withImpressions(-1L);

        ValidationResult<CpmForecastRequest, Defect> vr = service.validateCampaignForecastRequest(
                request,
                operatorId,
                clientId);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(
                validationError(path(field("strategy"), field("impression_limit"), field("impressions")),
                        greaterThanOrEqualTo(0L)))));
    }

    private CpmForecastRequest request() {
        return new CpmForecastRequest(campaignId, null,
                new CampaignStrategy("MAX_REACH",
                        100.0,
                        LocalDate.of(2018, 8, 1),
                        LocalDate.of(2018, 8, 31),
                        new ImpressionLimit(0L, 0L),
                        null,
                        null)
        );
    }
}
