package ru.yandex.direct.api.v5.units;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.entity.user.service.ApiUserService;
import ru.yandex.direct.core.units.api.UnitsBalance;
import ru.yandex.direct.core.units.service.UnitsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.api.v5.units.UnitsHolderDetectorTestData.BASIC_UNITS_LIMIT;

@RunWith(Parameterized.class)
public class UnitsHolderDetectorTest {

    private static final int OPERATION_COST = 0;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private ApiUserService apiUserService;

    @Mock
    private UnitsService unitsService;

    @Mock
    private UnitsBalance balanceWithUnits;

    @Mock
    private UnitsBalance balanceWithoutUnits;

    @InjectMocks
    private ApiContext apiContext;

    @InjectMocks
    private UnitsHolderDetector unitsHolderDetector;

    @Parameter()
    public String description;

    @Parameter(1)
    public DirectApiPreAuthentication auth;

    @Parameter(2)
    public ApiUser brandChief;

    @Parameter(3)
    public Integer balanceDiff;

    @Parameter(4)
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public ApiUser expectedUnitsHolder;

    @Parameter(5)
    public ApiUser expectedOperatorUnitsHolder;

    @Parameter(6)
    public ApiUser expectedUnitsUsedLogin;

    @Parameter(7)
    public boolean insufficientUnits;

    @Parameters(name = "{0}")
    public static Iterable<Object[]> parameters() {
        return UnitsHolderDetectorTestData.provideData();
    }

    @Before
    public void before() {
        initMocks(this);

        if (auth.getChiefSubjectUser().isPresent()) {
            when(apiUserService.getBrandChiefRepFor(auth.getChiefSubjectUser().get())).thenReturn(brandChief);
            when(unitsService.getLimit(auth.getChiefSubjectUser().get())).thenReturn(BASIC_UNITS_LIMIT);
        }

        when(unitsService.getLimit(brandChief)).thenReturn(BASIC_UNITS_LIMIT + balanceDiff);

        when(balanceWithUnits.isAvailable(anyInt())).thenReturn(true);
        when(balanceWithoutUnits.isAvailable(anyInt())).thenReturn(false);

        when(unitsService.getUnitsBalance(any(ApiUser.class))).thenReturn(insufficientUnits ?
                balanceWithoutUnits : balanceWithUnits);

        when(apiContextHolder.get()).thenReturn(apiContext);
    }

    @Test
    public void shouldDetectProperUnitsHolder() {
        assertThat(unitsHolderDetector.detectUnitsHolder(auth, OPERATION_COST)).isEqualTo(expectedUnitsHolder);
    }

    @Test
    public void shouldDetectProperOperatorUnitsHolder() {
        assertThat(unitsHolderDetector.detectOperatorUnitsHolder(auth)).isEqualTo(expectedOperatorUnitsHolder);
    }

    @Test
    public void shouldDetectProperUnitsUsedLogin() {
        assertThat(unitsHolderDetector.getUnitsHolderToDisplay(auth, OPERATION_COST))
                .usingRecursiveComparison()
                .isEqualTo(expectedUnitsUsedLogin);
    }
}
