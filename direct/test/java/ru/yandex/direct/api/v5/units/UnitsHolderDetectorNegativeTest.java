package ru.yandex.direct.api.v5.units;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.units.exception.NotAnAgencyException;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.APPLICATION_ID;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.BRAND_CHIEF;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.CLIENT;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.CLIENT_CHIEF;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.TRUE;

public class UnitsHolderDetectorNegativeTest {

    private static final int OPERATION_COST = 0;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private ApiUserService apiUserService;

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private UnitsBalance unitsBalance;

    @InjectMocks
    private ApiContext apiContext;

    @InjectMocks
    private UnitsHolderDetector unitsHolderDetector;

    @Before
    public void before() {
        initMocks(this);

        when(apiUserService.getBrandChiefRepFor(CLIENT_CHIEF)).thenReturn(BRAND_CHIEF);
        when(apiUnitsService.getAdjustedUnitsBalance(any(), any())).thenReturn(unitsBalance);
        when(apiContextHolder.get()).thenReturn(apiContext);
    }

    @Test(expected = NotAnAgencyException.class)
    public void shouldThrowExceptionIfOperatorIsNotAnAgencyAndUseOperatorUnitsIsTrue() {
        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getUseOperatorUnitsMode()).thenReturn(TRUE);

        unitsHolderDetector.detectUnitsHolder(
                new DirectApiPreAuthentication(
                        credentials, APPLICATION_ID, CLIENT, CLIENT_CHIEF, CLIENT, CLIENT_CHIEF), OPERATION_COST);
    }

    @Test(expected = NullPointerException.class)
    public void failsIfAuthenticationIsNull() {
        unitsHolderDetector.detectUnitsHolder(null, OPERATION_COST);
    }

    @Test(expected = NullPointerException.class)
    public void failsIfAuthenticationIsNullOnOperatorUnitsHolder() {
        unitsHolderDetector.detectOperatorUnitsHolder(null);
    }

}
