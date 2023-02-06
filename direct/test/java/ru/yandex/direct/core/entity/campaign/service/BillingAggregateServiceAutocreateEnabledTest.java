package ru.yandex.direct.core.entity.campaign.service;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.feature.FeatureName;

import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.service.BillingAggregateService.UIDS_TO_DISABLE_BILLING_AGGREGATES;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class BillingAggregateServiceAutocreateEnabledTest {
    public static final Long CLIENT_ID = nextLong();
    public static final Long AGENCY_CLIENT_ID = nextLong();
    public static final Long CHIEF_UID = nextLong();

    @Mock
    private FeatureService featureService;

    @InjectMocks
    private BillingAggregateService billingAggregateService;

    private Client client = new Client()
            .withClientId(CLIENT_ID)
            .withChiefUid(CHIEF_UID);

    private static Object[] parametersData() {
        return new Object[][]{
                {
                        "Валюта — УЕ",
                        CurrencyCode.YND_FIXED,
                        CHIEF_UID,
                        AGENCY_CLIENT_ID,
                        false,
                        false,
                        false
                },
                {
                        "Агентский, агентству запрещено, клиенту не запрещено",
                        CurrencyCode.RUB,
                        CHIEF_UID,
                        AGENCY_CLIENT_ID,
                        true,
                        false,
                        false
                },
                {
                        "Агентский, агентству не запрещено, клиенту запрещено",
                        CurrencyCode.RUB,
                        CHIEF_UID,
                        AGENCY_CLIENT_ID,
                        false,
                        true,
                        false
                },
                {
                        "UID из запрещенных",
                        CurrencyCode.RUB,
                        UIDS_TO_DISABLE_BILLING_AGGREGATES.iterator().next(),
                        AGENCY_CLIENT_ID,
                        false,
                        false,
                        false
                },
                {
                        "Не агентский",
                        CurrencyCode.RUB,
                        CHIEF_UID,
                        null,
                        true,
                        false,
                        true
                },
                {
                        "Все выполняется",
                        CurrencyCode.RUB,
                        CHIEF_UID,
                        AGENCY_CLIENT_ID,
                        false,
                        false,
                        true
                },
        };
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Parameters(method = "parametersData")
    @TestCaseName("{0}")
    public void checkAutocreateEnabled(String caseName, CurrencyCode currencyCode, Long chiefUid,
                                               @Nullable Long agencyClientId,
                                               @Nullable Boolean agencyHasDisableFeature,
                                               Boolean clientHasDisableFeature,
                                               Boolean expectedResult) {
        client.withWorkCurrency(currencyCode)
                .withAgencyClientId(agencyClientId)
                .withChiefUid(chiefUid);
        if (agencyClientId != null) {
            when(featureService.isEnabledForClientId(eq(ClientId.fromLong(agencyClientId)),
                    eq(FeatureName.DISABLE_BILLING_AGGREGATES))).thenReturn(agencyHasDisableFeature);
        }
        when(featureService.isEnabledForClientId(eq(ClientId.fromLong(CLIENT_ID)),
                eq(FeatureName.DISABLE_BILLING_AGGREGATES))).thenReturn(clientHasDisableFeature);

        assertThat(billingAggregateService.autoCreateEnabled(client)).isEqualTo(expectedResult);
    }
}
