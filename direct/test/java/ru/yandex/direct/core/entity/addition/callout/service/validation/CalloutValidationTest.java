package ru.yandex.direct.core.entity.addition.callout.service.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutConstants.MAX_CALLOUT_TEXT_LENGTH;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefinitions.allowedSymbolsCalloutText;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefinitions.duplicateCalloutTexts;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefinitions.maxClientCallouts;
import static ru.yandex.direct.core.entity.addition.callout.service.validation.CalloutDefinitions.maxClientCalloutsWithDeleted;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.greaterThan;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CalloutValidationTest {

    private static final Long CLIENT_ID = 1L;

    private CalloutValidationService calloutValidationService;

    private Callout defaultCallout;

    @Before
    public void before() {
        calloutValidationService = new CalloutValidationService(null, null);
        defaultCallout = new Callout()
                .withClientId(CLIENT_ID)
                .withText(RandomStringUtils.randomAlphanumeric(5));
    }

    @Test
    public void positiveValidationResultWhenNoErrors() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(singletonList(defaultCallout), emptyList());
        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void operationalErrorInvalidValueWhenNull() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(null, emptyList());
        assertThat(actual.flattenErrors(),
                contains(validationError(path(), notNull())));
    }

    @Test
    public void validate_CalloutNull() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(singletonList(null), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(),
                        notNull())));
    }

    @Test
    public void validate_TextNull() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(singletonList(defaultCallout.withText(null)), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("text")),
                        notNull())));
    }

    @Test
    public void validate_TextLengthGreaterMax() {
        ValidationResult<List<Callout>, Defect> actual = calloutValidationService.generateValidation(
                singletonList(defaultCallout
                        .withText(RandomStringUtils.randomAlphabetic(MAX_CALLOUT_TEXT_LENGTH + 1))),
                emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("text")),
                        CalloutDefinitions.calloutTextLengthExceeded())));
    }

    @Test
    public void validate_TextAllowExclamationLetter() {
        ValidationResult<List<Callout>, Defect> actual = calloutValidationService
                .generateValidation(singletonList(defaultCallout.withText("test!")), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("text")),
                        allowedSymbolsCalloutText())));
    }

    @Test
    public void validate_TextAllowQuestionMarkLetter() {
        ValidationResult<List<Callout>, Defect> actual = calloutValidationService
                .generateValidation(singletonList(defaultCallout.withText("test?")), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("text")),
                        allowedSymbolsCalloutText())));
    }

    @Test
    public void validate_TextHasValidNationalLetter() {
        ValidationResult<List<Callout>, Defect> actual = calloutValidationService
                .generateValidation(singletonList(defaultCallout.withText("наяўнымі")), emptyList());

        assertThat(actual.flattenErrors(), hasSize(0));
    }

    @Test
    public void validate_ClientIdNull() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService
                        .generateValidation(singletonList(defaultCallout.withClientId(null)), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("clientId")),
                        notNull())));
    }

    @Test
    public void validate_ClientIdNegative() {
        ValidationResult<List<Callout>, Defect> actual = calloutValidationService.generateValidation(
                singletonList(defaultCallout.withClientId(-1L)), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(field("clientId")), greaterThan(0L))));
    }

    @Test
    public void validate_duplicateCalloutsTexts() {
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(asList(defaultCallout, defaultCallout), emptyList());

        assertThat(actual.getSubResults().get(index(0)).flattenErrors(),
                contains(validationError(path(), duplicateCalloutTexts())));
    }

    @Test
    public void validate_maxCalloutOnClient() {
        List<Callout> existCallouts = new ArrayList<>();
        for (int i = 0; i < CalloutConstants.MAX_CALLOUTS_COUNT_ON_CLIENT + 1; i++) {
            existCallouts
                    .add(new Callout().withText(RandomStringUtils.randomAlphanumeric(20)).withDeleted(Boolean.FALSE));
        }
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(singletonList(defaultCallout), existCallouts);

        assertThat(actual.flattenErrors(),
                contains(validationError(path(), maxClientCallouts())));
    }

    @Test
    public void validate_maxCalloutOnClientWithDeleted() {
        List<Callout> existCallouts = new ArrayList<>();
        for (int i = 0; i < CalloutConstants.MAX_CALLOUTS_COUNT_ON_CLIENT_WITH_DELETED + 1; i++) {
            existCallouts
                    .add(new Callout().withText(RandomStringUtils.randomAlphanumeric(20)).withDeleted(Boolean.TRUE));
        }
        ValidationResult<List<Callout>, Defect> actual =
                calloutValidationService.generateValidation(singletonList(defaultCallout), existCallouts);

        assertThat(actual.flattenErrors(),
                contains(validationError(path(), maxClientCalloutsWithDeleted())));
    }
}

