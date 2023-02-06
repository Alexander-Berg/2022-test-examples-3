package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.retargeting.RetargetingStatesEnum;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StatesRetargetingTest {
    private static RetargetingStates retargetingStatesCalculator;

    @Parameterized.Parameter
    public Retargeting retargeting;

    @Parameterized.Parameter(1)
    public Collection<RetargetingStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "{index}: => States: {1}")
    public static Object[][] params() {
        return new Object[][]{
                {new Retargeting(),
                        List.of()},

                {new Retargeting()
                        .withIsSuspended(true),
                        List.of(RetargetingStatesEnum.SUSPENDED)},
        };
    }

    @BeforeClass
    public static void prepare() {
        retargetingStatesCalculator = new RetargetingStates();
    }

    @Test
    public void test() {
        Collection<RetargetingStatesEnum> states = retargetingStatesCalculator.calc(retargeting);

        assertEquals("got right states", states, expectedStates);
    }
}
