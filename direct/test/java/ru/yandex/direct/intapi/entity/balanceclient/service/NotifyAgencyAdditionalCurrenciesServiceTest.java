package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.agency.model.AgencyAdditionalCurrency;
import ru.yandex.direct.core.entity.agency.service.AgencyService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyAgencyAdditionalCurrenciesItem;
import ru.yandex.direct.intapi.entity.balanceclient.model.NotifyAgencyAdditionalCurrenciesParameters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyAgencyAdditionalCurrenciesService.CURRENCY_DISAPPEARED_ERROR_CODE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.NotifyAgencyAdditionalCurrenciesService.INVALID_INPUT_ERROR_CODE;

public class NotifyAgencyAdditionalCurrenciesServiceTest {
    private static final long TEST_CLIENT_ID = 1;
    private static final CurrencyCode TEST_CODE = CurrencyCode.CHF;
    private static final CurrencyCode TEST_CODE_TWO = CurrencyCode.RUB;
    private static final LocalDate TEST_EXPIRE_DATE = LocalDate.now();
    private static final AgencyAdditionalCurrency TEST_ADD_CUR = new AgencyAdditionalCurrency()
            .withClientId(TEST_CLIENT_ID)
            .withCurrencyCode(TEST_CODE)
            .withExpirationDate(TEST_EXPIRE_DATE);

    @Mock
    private AgencyService agencyService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ClientService clientService;
    private NotifyAgencyAdditionalCurrenciesService service;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        service = new NotifyAgencyAdditionalCurrenciesService(agencyService, clientService);
    }

    @Test
    public void testCurrencyNotChangedTrue() {
        assertThat(service.currencyNotChanged(TEST_ADD_CUR,
                Collections.singletonMap(TEST_CODE, TEST_EXPIRE_DATE)))
                .isTrue();
    }

    @Test
    public void testCurrencyNotChangedFalseWhenNoOldValue() {
        assertThat(service.currencyNotChanged(TEST_ADD_CUR, Collections.emptyMap()))
                .isFalse();
    }

    @Test
    public void testCurrencyNotChangedFalseWhenChangedOldValue() {
        assertThat(service.currencyNotChanged(TEST_ADD_CUR,
                Collections.singletonMap(TEST_CODE, TEST_EXPIRE_DATE.minusDays(1))))
                .isFalse();
    }

    @Test
    public void testValidateInputParamsAllCorrect() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name())
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        assertThatCode(() -> service.inputParamsAreValid(params))
                .doesNotThrowAnyException();
    }

    @Test
    public void testValidateInputParamsCurrenciesEmptyListCorrect() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.emptyList());
        assertThat(service.inputParamsAreValid(params))
                .isTrue();
    }

    @Test
    public void testValidateInputParamsNull() {
        assertThat(service.inputParamsAreValid(null))
                .isFalse();
    }

    @Test
    public void testValidateInputParamsClientIdNull() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(null)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name())
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        assertThat(service.inputParamsAreValid(params))
                .isFalse();
    }

    @Test
    public void testValidateInputParamsCurrenciesNull() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(null);
        assertThat(service.inputParamsAreValid(params))
                .isFalse();
    }

    @Test
    public void testValidateInputParamsCurrenciesListOfNull() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(null));
        assertThat(service.inputParamsAreValid(params))
                .isFalse();
    }

    @Test
    public void testValidateInputParamsCurrenciesBadCurrency() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name() + "Wrong")
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        assertThat(service.inputParamsAreValid(params))
                .isFalse();
    }

    @Test
    public void testProcessAgencyCorrect() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name())
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        doReturn(Collections.singletonList(new AgencyAdditionalCurrency()
                .withClientId(TEST_CLIENT_ID)
                .withCurrencyCode(TEST_CODE)
                .withExpirationDate(TEST_EXPIRE_DATE.minusDays(1))
                .withLastChange(LocalDateTime.now())))
                .when(agencyService).getAllAdditionalCurrencies(eq(TEST_CLIENT_ID));
        service.processAgency(params);

        verify(agencyService).addAdditionalCurrencies(eq(Collections.singletonList(new AgencyAdditionalCurrency()
                .withClientId(TEST_CLIENT_ID)
                .withCurrencyCode(TEST_CODE)
                .withExpirationDate(TEST_EXPIRE_DATE))));
    }

    @Test
    public void testProcessAgencyNoAction() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name())
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        doReturn(Collections.singletonList(new AgencyAdditionalCurrency()
                .withClientId(TEST_CLIENT_ID)
                .withCurrencyCode(TEST_CODE)
                .withExpirationDate(TEST_EXPIRE_DATE)
                .withLastChange(LocalDateTime.now())))
                .when(agencyService).getAllAdditionalCurrencies(eq(TEST_CLIENT_ID));
        service.processAgency(params);

        verify(agencyService, never()).addAdditionalCurrencies(any());
    }

    @Test
    public void testProcessInvalidInput() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(null);
        BalanceClientResponse balanceClientResponse = service.processAgency(params);
        assertThat(balanceClientResponse.getBody().getResponseCode())
                .isEqualTo(INVALID_INPUT_ERROR_CODE);
    }

    @Test
    public void testProcessAgencyDeletedCurrency() {
        NotifyAgencyAdditionalCurrenciesParameters params = new NotifyAgencyAdditionalCurrenciesParameters()
                .withClientId(TEST_CLIENT_ID)
                .withAdditionalCurrencies(Collections.singletonList(
                        new NotifyAgencyAdditionalCurrenciesItem()
                                .withCurrencyCode(TEST_CODE.name())
                                .withExpireDate(TEST_EXPIRE_DATE)
                ));
        doReturn(
                Arrays.asList(
                        new AgencyAdditionalCurrency()
                                .withClientId(TEST_CLIENT_ID)
                                .withCurrencyCode(TEST_CODE)
                                .withExpirationDate(TEST_EXPIRE_DATE)
                                .withLastChange(LocalDateTime.now()),
                        new AgencyAdditionalCurrency()
                                .withClientId(TEST_CLIENT_ID)
                                .withCurrencyCode(TEST_CODE_TWO)
                                .withExpirationDate(TEST_EXPIRE_DATE)
                                .withLastChange(LocalDateTime.now())))
                .when(agencyService).getAllAdditionalCurrencies(eq(TEST_CLIENT_ID));

        BalanceClientResponse balanceClientResponse = service.processAgency(params);
        assertThat(balanceClientResponse.getBody().getResponseCode())
                .isEqualTo(CURRENCY_DISAPPEARED_ERROR_CODE);
    }
}
