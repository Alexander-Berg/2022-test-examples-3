package ru.yandex.direct.api.v5.units;

import javax.servlet.FilterChain;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.context.units.UnitsContext;
import ru.yandex.direct.api.v5.security.DirectApiPreAuthentication;
import ru.yandex.direct.core.units.api.UnitsBalance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class UnitsFilterTest {

    @Mock
    private ApiUnitsService apiUnitsService;

    @Mock
    private ApiContextHolder apiContextHolder;

    @Mock
    private UnitsContextFactory unitsContextFactory;

    @Mock
    private FilterChain filterChain;

    @Mock
    private DirectApiPreAuthentication preAuth;

    @Mock
    private UnitsBalance unitsBalance;

    @Mock
    private UnitsBalance operatorUnitsBalance;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ApiContext apiContext;

    @Mock
    private UnitsContext unitsContext;

    @Mock
    private UnitsContext failedUnitsContext;

    @InjectMocks
    private UnitsFilter unitsFilter;

    @Before
    public void before() {
        initMocks(this);

        apiContext.setPreAuthentication(preAuth);

        when(apiContextHolder.get()).thenReturn(apiContext);
        when(unitsContextFactory.createUnitsContext(same(preAuth))).thenReturn(unitsContext);
        when(unitsContextFactory.createUnitsContext(eq(null))).thenReturn(failedUnitsContext);
        when(unitsContext.getUnitsBalance()).thenReturn(unitsBalance);
        when(unitsContext.getOperatorUnitsBalance()).thenReturn(operatorUnitsBalance);
    }

    @Test
    public void doFilter_ShouldUpdateSpentUnitsForServiceCall() throws Exception {
        callFilter();
        verify(apiUnitsService).updateSpent(same(unitsBalance));
    }

    @Test
    public void doFilter_ShouldUpdateSpentUnitsForError() throws Exception {
        callFilter();
        verify(apiUnitsService).updateSpent(same(operatorUnitsBalance));
    }

    @Test
    public void doFilter_AuthenticationIsNull_NoFail() throws Exception {
        apiContext.setPreAuthentication(null);

        callFilter();
    }

    @Test
    public void doFilter_UnitsContextIsNull_NoFail() throws Exception {
        when(apiContext.getUnitsContext()).thenReturn(null);

        callFilter();
    }

    @Test
    public void doFilter_AuthenticationIsNull_UnitsContextIsSet() throws Exception {
        apiContext.setPreAuthentication(null);
        callFilter();

        assertThat(apiContext.getUnitsContext()).isEqualTo(failedUnitsContext);
    }

    @Test
    public void doFilter_UnitsContextFactoryReturnsNull_NoFail() throws Exception {
        when(unitsContextFactory.createUnitsContext(same(preAuth))).thenReturn(null);
        callFilter();
    }

    private void callFilter() throws Exception {
        unitsFilter.doFilter(null, null, filterChain);
    }

}
