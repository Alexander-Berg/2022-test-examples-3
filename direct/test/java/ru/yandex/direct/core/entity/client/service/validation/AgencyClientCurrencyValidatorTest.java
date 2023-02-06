package ru.yandex.direct.core.entity.client.service.validation;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.CommonTranslations;
import ru.yandex.direct.core.ErrorCodes;
import ru.yandex.direct.core.entity.client.exception.NoAvailableCurrenciesException;
import ru.yandex.direct.core.entity.currency.service.CurrencyService;
import ru.yandex.direct.core.validation.defects.MoneyDefects;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class AgencyClientCurrencyValidatorTest {
    private static final ClientId AGENCY_CLIENT_ID = ClientId.fromLong(1L);
    private static final CurrencyCode SOME_CURRENCY = CurrencyCode.RUB;
    private static final CurrencyCode INVALID_CURRENCY = CurrencyCode.EUR;
    private static final Set<CurrencyCode> VALID_CURRENCIES = EnumSet.of(CurrencyCode.RUB, CurrencyCode.USD);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameter
    public boolean isYndFixedAllowed;

    @Mock
    private CurrencyService currencyService;

    private AgencyClientCurrencyValidator currencyValidator;

    @Parameterized.Parameters(name = "isYndFixedAllowed = {0}")
    public static Collection<Boolean> getParameters() {
        return asList(Boolean.FALSE, Boolean.TRUE);
    }

    @Before
    public void setUp() {
        doReturn(VALID_CURRENCIES)
                .when(currencyService)
                .getAllowedCurrenciesForAgencyClient(
                        nullable(ClientId.class),
                        eq(AGENCY_CLIENT_ID));

        currencyValidator = new AgencyClientCurrencyValidator(currencyService, AGENCY_CLIENT_ID);
    }

    @Test
    public void testSuccess() {
        ValidationResult<CurrencyCode, Defect> validationResult = currencyValidator.apply(SOME_CURRENCY);

        assertThat(validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void testIfNoAvailableCurrencies() {
        doReturn(emptySet())
                .when(currencyService)
                .getAllowedCurrenciesForAgencyClient(
                        nullable(ClientId.class),
                        eq(AGENCY_CLIENT_ID));

        thrown.expect(NoAvailableCurrenciesException.class);
        thrown.expect(allOf(
                hasProperty("code", equalTo(ErrorCodes.NO_AVAILABLE_CURRENCIES)),
                hasProperty("shortMessage",
                        equalTo(CommonTranslations.INSTANCE.serviceInternalError())),
                hasProperty("detailedMessage",
                        equalTo(ClientDefectTranslations.INSTANCE.noAvailableCurrenciesDetails()))));

        currencyValidator.apply(SOME_CURRENCY);
    }

    @Test
    public void testIfInvalidCurrency() {
        ValidationResult<CurrencyCode, Defect> validationResult = currencyValidator.apply(INVALID_CURRENCY);

        assertThat(
                validationResult,
                hasDefectDefinitionWith(
                        validationError(
                                path(),
                                MoneyDefects.unavailableCurrency(INVALID_CURRENCY, VALID_CURRENCIES))));
    }
}
