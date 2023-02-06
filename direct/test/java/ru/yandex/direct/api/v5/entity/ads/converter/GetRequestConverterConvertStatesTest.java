package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdStateSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.State;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.ads.converter.GetRequestConverter.convertStates;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetRequestConverterConvertStatesTest {

    @Parameterized.Parameter
    public List<AdStateSelectionEnum> states;

    @Parameterized.Parameter(1)
    public List<State> expectedStates;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList()},
                {singletonList(AdStateSelectionEnum.ARCHIVED), singletonList(State.ARCHIVED)},
                {singletonList(AdStateSelectionEnum.OFF), singletonList(State.OFF)},
                {singletonList(AdStateSelectionEnum.OFF_BY_MONITORING), singletonList(State.OFF_BY_MONITORING)},
                {singletonList(AdStateSelectionEnum.ON), singletonList(State.ON)},
                {singletonList(AdStateSelectionEnum.SUSPENDED), singletonList(State.SUSPENDED)},
                {asList(AdStateSelectionEnum.values()), asList(State.values())},
        };
    }

    @Test
    public void test() {
        assertThat(convertStates(states)).containsExactlyInAnyOrder(expectedStates.toArray(new State[0]));
    }

}
