package ru.yandex.direct.core.units.api.decorator;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.units.api.UnitsBalance;
import ru.yandex.direct.core.units.decorator.AdjustedUnitsBalanceDecorator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AdjustedUnitsBalanceDecoratorTest {

    private UnitsBalance originUnitsBalanceMock;

    @Before
    public void setup() {
        originUnitsBalanceMock = mock(UnitsBalance.class);
    }

    @Test
    public void withdraw_NullWhenOutOfCharge() {
        UnitsBalance u = new AdjustedUnitsBalanceDecorator(originUnitsBalanceMock, 0.0);
        u.withdraw(10);
        verify(originUnitsBalanceMock).withdraw(0);
    }

    @Test
    public void withdraw_DoubleWithCoefficient() {
        UnitsBalance u = new AdjustedUnitsBalanceDecorator(originUnitsBalanceMock, 2.0);
        u.withdraw(10);
        verify(originUnitsBalanceMock).withdraw(20);
    }

    @Test
    public void isAvailable_TrueWhenOutOfCharge() {
        when(originUnitsBalanceMock.isAvailable(anyInt())).thenReturn(false);
        when(originUnitsBalanceMock.isAvailable(0)).thenReturn(true);
        UnitsBalance u = new AdjustedUnitsBalanceDecorator(originUnitsBalanceMock, 0.0);
        assertThat(u.isAvailable(10), is(true));
    }

    @Test
    public void isAvailable_FalseWithDoubleCoefficient() {
        when(originUnitsBalanceMock.balance()).thenReturn(10);
        UnitsBalance u = new AdjustedUnitsBalanceDecorator(originUnitsBalanceMock, 2.0);
        assertThat(u.isAvailable(10), is(false));
    }

}
