package ru.yandex.direct.core.units.api.decorator;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.units.api.UnitsBalance;
import ru.yandex.direct.core.units.decorator.FreeOfChargeUnitsBalanceDecorator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

public class FreeOfChargeUnitsBalanceDecoratorTest {

    private UnitsBalance decoratedUnitsBalance;
    private UnitsBalance originUnitsBalanceMock;

    @Before
    public void setup() {
        originUnitsBalanceMock = mock(UnitsBalance.class);

        decoratedUnitsBalance = new FreeOfChargeUnitsBalanceDecorator(originUnitsBalanceMock);
    }

    @Test
    public void isAvailable_trueOnMaxInteger() {
        assertThat(decoratedUnitsBalance.isAvailable(Integer.MAX_VALUE), is(true));
    }

    @Test
    public void isAvailable_notInteractWithOrigin() {
        decoratedUnitsBalance.isAvailable(Integer.MAX_VALUE);
        verifyZeroInteractions(originUnitsBalanceMock);
    }

    @Test
    public void withdraw_notInteractWithOrigin() {
        decoratedUnitsBalance.withdraw(Integer.MAX_VALUE);
        verifyZeroInteractions(originUnitsBalanceMock);
    }
}
