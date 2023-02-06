package ru.yandex.direct.api.v5.units;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.units.UnitsContext;
import ru.yandex.direct.api.v5.security.DirectApiCredentials;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.api.v5.units.logging.UnitsLogDataFactory;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.AGENCY;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.AGENCY_CHIEF;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.APPLICATION_ID;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.CLIENT;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.CLIENT_CHIEF;
import static ru.yandex.direct.api.v5.units.UseOperatorUnitsMode.FALSE;

@ParametersAreNonnullByDefault
public class UnitsContextFactoryTest {

    private static final int OPERATION_COST = 0;

    private static DirectApiPreAuthentication auth;

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private UnitsHolderDetector unitsHolderDetector;

    @Mock
    private UnitsLogDataFactory unitsLogDataFactory;

    @Mock
    private UnitsBalance unitsBalance;

    @Mock
    private UnitsBalance operatorUnitsBalance;

    @Mock
    private ApiUser unitsHolder;

    @Mock
    private ApiUser operatorUnitsHolder;

    @InjectMocks
    private UnitsContextFactory unitsContextFactory;

    private ApiUser unitsHolderToDisplay;

    private UnitsContext unitsContext;

    @BeforeClass
    public static void init() {
        DirectApiCredentials credentials = mock(DirectApiCredentials.class);
        when(credentials.getUseOperatorUnitsMode()).thenReturn(FALSE);

        auth = new DirectApiPreAuthentication(
                credentials, APPLICATION_ID, AGENCY, AGENCY_CHIEF, CLIENT, CLIENT_CHIEF);
    }

    @Before
    public void before() {
        initMocks(this);

        when(unitsHolderDetector.detectUnitsHolder(same(auth), anyInt())).thenReturn(unitsHolder);
        when(unitsHolderDetector.detectOperatorUnitsHolder(same(auth))).thenReturn(operatorUnitsHolder);

        unitsHolderToDisplay = unitsHolder;
        when(unitsHolderDetector.getUnitsHolderToDisplay(same(auth), anyInt())).thenReturn(unitsHolderToDisplay);

        when(apiUnitsService.getAdjustedUnitsBalance(same(auth), same(unitsHolder)))
                .thenReturn(unitsBalance);
        when(apiUnitsService.getAdjustedUnitsBalance(same(auth), same(operatorUnitsHolder)))
                .thenReturn(operatorUnitsBalance);

        when(unitsLogDataFactory.createUnitsLogData(same(auth))).thenReturn(null);
    }

    @Test
    public void shouldSetUnitsBalanceToContext() throws Exception {
        callFactory();
        assertThat(unitsContext.getUnitsBalance()).isSameAs(unitsBalance);
    }

    @Test
    public void shouldSetOperatorUnitsBalanceToContext() throws Exception {
        callFactory();
        assertThat(unitsContext.getOperatorUnitsBalance()).isSameAs(operatorUnitsBalance);
    }

    @Test
    public void shouldSetUnitsUsedUserToContext_isClient() throws Exception {
        unitsHolderToDisplay = CLIENT;
        doReturn(unitsHolderToDisplay).when(unitsHolderDetector).getUnitsHolderToDisplay(any(), anyInt());

        callFactory();

        assertThat(unitsContext.getUnitsUsedUser()).isSameAs(unitsHolderToDisplay);
    }

    @Test
    public void shouldSetUnitsUsedUserToContext_isOperator() throws Exception {
        unitsHolderToDisplay = AGENCY;
        doReturn(unitsHolderToDisplay).when(unitsHolderDetector).getUnitsHolderToDisplay(any(), anyInt());

        callFactory();

        assertThat(unitsContext.getUnitsUsedUser()).isSameAs(unitsHolderToDisplay);
    }

    @Test
    public void shouldSetUnitsBalanceExceptionToContextIfHolderDetectorFailed() throws Exception {
        doThrow(new RuntimeException()).when(unitsHolderDetector).detectUnitsHolder(any(), anyInt());
        callFactory();

        assertThat(unitsContext.getUnitsBalanceException().isPresent()).isTrue();
    }

    @Test
    public void shouldNotFailAndReturnUnitsContextWithExceptionIfAuthenticationIsNull() throws Exception {
        UnitsContext unitsContext = unitsContextFactory.createUnitsContext(null, OPERATION_COST);
        assertThat(unitsContext.getUnitsBalanceException()).isNotNull();
    }

    private void callFactory() throws Exception {
        unitsContext = unitsContextFactory.createUnitsContext(auth, OPERATION_COST);
    }

}
