package ru.yandex.direct.intapi.entity.statistic;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import ru.yandex.direct.intapi.entity.statistic.model.phrase.GetPhraseStatisticsRequest;
import ru.yandex.direct.intapi.entity.statistic.model.phrase.GetPhraseStatisticsRequestItem;
import ru.yandex.direct.intapi.entity.statistic.service.StatisticValidationService;
import ru.yandex.direct.intapi.validation.IntApiDefect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.intapi.validation.ValidationUtils.getErrorText;

public class StatisticValidationServiceTest {
    private StatisticValidationService statisticValidationService;

    public StatisticValidationServiceTest() {
        this.statisticValidationService = new StatisticValidationService(null);
    }

    @Test
    public void validateNullRequest() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest = null;
        @SuppressWarnings("ConstantConditions")
        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);

        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError, equalTo("Request body must be specified"));
    }

    @Test
    public void validateNullInterval() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest().withPhraseStatisticsRequestItems(
                        Collections.singletonList(new GetPhraseStatisticsRequestItem()));

        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);

        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError,
                equalTo(GetPhraseStatisticsRequest.INTERVAL_DAYS + " cannot be null"));
    }

    @Test
    public void validateZeroInterval() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest().withPhraseStatisticsRequestItems(
                        Collections.singletonList(new GetPhraseStatisticsRequestItem()))
                        .withIntervalDays(0);
        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);

        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError,
                equalTo(GetPhraseStatisticsRequest.INTERVAL_DAYS + " must be greater than 0"));
    }

    @Test
    public void validateNullSelectionCriteria() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest =
                new GetPhraseStatisticsRequest().withIntervalDays(1);
        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);

        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError,
                equalTo(GetPhraseStatisticsRequest.SELECTION_CRITERIA + " cannot be null"));
    }

    @Test
    public void validateEmptySelectionCriteria() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest = new GetPhraseStatisticsRequest().withIntervalDays(1)
                .withPhraseStatisticsRequestItems(new ArrayList<>());

        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);


        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError,
                equalTo(GetPhraseStatisticsRequest.SELECTION_CRITERIA + " cannot be empty"));
    }

    @Test
    public void validateNullGetPhraseStatisticsRequestItem() {
        GetPhraseStatisticsRequest getPhraseStatisticsRequest = new GetPhraseStatisticsRequest().withIntervalDays(1)
                .withPhraseStatisticsRequestItems(Collections.singletonList(null));

        ValidationResult<GetPhraseStatisticsRequest, IntApiDefect> vr =
                statisticValidationService.validate(getPhraseStatisticsRequest);

        String validationError = getErrorText(vr);
        assertThat("получили корректную ошибку валидации", validationError,
                equalTo(GetPhraseStatisticsRequest.SELECTION_CRITERIA + "[0] cannot be null"));
    }

    // todo maxlog: тесты на валидацию GetRelevanceMatchStatisticsRequest
}
