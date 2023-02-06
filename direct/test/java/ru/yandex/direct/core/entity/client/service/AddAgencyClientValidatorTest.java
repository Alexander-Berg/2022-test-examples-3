package ru.yandex.direct.core.entity.client.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.application.model.AgencyOptions;
import ru.yandex.direct.core.entity.client.model.AddAgencyClientRequest;
import ru.yandex.direct.core.entity.client.service.validation.AgencyClientCurrencyValidatorFactory;
import ru.yandex.direct.core.entity.client.service.validation.NotificationEmailValidator;
import ru.yandex.direct.core.entity.user.validator.FirstLastNameValidator;
import ru.yandex.direct.core.entity.user.validator.LoginValidator;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.validation.wrapper.DefaultValidator;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.AGENCY;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.CURRENCY;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.FIRST_NAME;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.LAST_NAME;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.LOGIN;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.NOTIFICATION_EMAIL;
import static ru.yandex.direct.core.entity.client.service.AddAgencyValidatorTests.defaultRequest;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AddAgencyClientValidatorTest {
    private static final Boolean IS_OPERATOR_GEO_MANAGER = false;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Mock
    private LoginValidator loginValidator;
    @Mock
    private FirstLastNameValidator firstNameValidator;
    @Mock
    private FirstLastNameValidator lastNameValidator;
    @Mock
    private NotificationEmailValidator notificationEmailValidator;
    @Mock
    private DefaultValidator<CurrencyCode> currencyValidator;
    @Mock
    private AgencyClientCurrencyValidatorFactory currencyValidatorFactory;

    private AddAgencyClientValidator testedValidator;
    private AddAgencyClientRequest request;
    @Autowired
    private AgencyService agencyService;
    private AgencyOptions agencyOpt;

    @Before
    public void setUp() {
        when(loginValidator.apply(LOGIN))
                .thenReturn(new ValidationResult<>(LOGIN));

        when(firstNameValidator.apply(FIRST_NAME))
                .thenReturn(new ValidationResult<>(FIRST_NAME));

        when(lastNameValidator.apply(LAST_NAME))
                .thenReturn(new ValidationResult<>(LAST_NAME));

        when(notificationEmailValidator.apply(NOTIFICATION_EMAIL))
                .thenReturn(new ValidationResult<>(NOTIFICATION_EMAIL));

        when(currencyValidator.apply(CURRENCY))
                .thenReturn(new ValidationResult<>(CURRENCY));

        when(currencyValidatorFactory.newInstance(eq(AGENCY.getClientId())))
                .thenReturn(currencyValidator);

        testedValidator = new AddAgencyClientValidator(
                loginValidator,
                firstNameValidator,
                lastNameValidator,
                notificationEmailValidator,
                currencyValidatorFactory);

        request = defaultRequest();
        agencyService = mock(AgencyService.class);
        when(agencyService.getAgencyOptions(any(ClientId.class)))
                .thenReturn(new AgencyOptions(false, false));
        agencyOpt = agencyService.getAgencyOptions(AGENCY.getClientId());
    }

    @Test
    public void testSuccess() {
        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(actualResult, hasNoDefectsDefinitions());
    }

    @Test
    public void testIfLoginInvalid() {
        when(loginValidator.apply(LOGIN))
                .thenReturn(ValidationResult.failed(LOGIN, invalidValue()));

        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.LOGIN.name())),
                                invalidValue())));
    }

    @Test
    public void testIfFirstNameInvalid() {
        when(firstNameValidator.apply(FIRST_NAME))
                .thenReturn(ValidationResult.failed(FIRST_NAME, invalidValue()));

        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.FIRST_NAME.name())),
                                invalidValue())));
    }

    @Test
    public void testIfLastNameInvalid() {
        when(lastNameValidator.apply(LAST_NAME))
                .thenReturn(ValidationResult.failed(LAST_NAME, invalidValue()));

        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.LAST_NAME.name())),
                                invalidValue())));
    }

    @Test
    public void testIfNotificationEmailNull() {
        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request.withNotificationEmail(null));

        assertTrue(actualResult.hasAnyErrors());
    }

    @Test
    public void testIfNotificationEmailInvalid() {
        when(notificationEmailValidator.apply(NOTIFICATION_EMAIL))
                .thenReturn(ValidationResult.failed(NOTIFICATION_EMAIL, invalidValue()));

        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.NOTIFICATION_EMAIL.name())),
                                invalidValue())));
    }

    @Test
    public void testIfCurrencyInvalid() {
        when(currencyValidator.apply(CURRENCY))
                .thenReturn(ValidationResult.failed(CURRENCY, invalidValue()));

        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY, agencyOpt, request);

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.CURRENCY.name())),
                                invalidValue())));
    }

    @Test
    public void testIfAllowImportXlsAndAllowEditCampaignInInconsistentState() {
        ValidationResult<AddAgencyClientRequest, Defect> actualResult = testedValidator.validate(
                AGENCY,
                agencyOpt, request.withAllowEditCampaigns(false).withAllowImportXls(true));

        assertThat(
                actualResult,
                hasDefectWithDefinition(
                        validationError(
                                path(field(AddAgencyClientRequest.ALLOW_IMPORT_XLS.name())),
                                AddAgencyClientValidator.DefectDefinitions
                                        .inconsistentStateAllowEditCampaignAndAllowImportXls())));
    }
}
