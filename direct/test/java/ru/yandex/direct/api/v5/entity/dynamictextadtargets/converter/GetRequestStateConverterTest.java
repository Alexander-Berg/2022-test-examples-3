package ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter;

import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.general.AdTargetStateSelectionEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTargetState;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class GetRequestStateConverterTest {

    private static final Set<DynamicTextAdTargetState> ALL_STATES = ImmutableSet.of(
            DynamicTextAdTargetState.ON,
            DynamicTextAdTargetState.OFF,
            DynamicTextAdTargetState.SUSPENDED,
            DynamicTextAdTargetState.DELETED);

    private static final Set<DynamicTextAdTargetState> ALL_STATES_WITHOUT_DELETED = ImmutableSet.of(
            DynamicTextAdTargetState.ON,
            DynamicTextAdTargetState.OFF,
            DynamicTextAdTargetState.SUSPENDED);


    @Parameterized.Parameter
    public List<AdTargetStateSelectionEnum> states;

    @Parameterized.Parameter(1)
    public List<Long> ids;

    @Parameterized.Parameter(2)
    public Set<DynamicTextAdTargetState> expectedStates;

    @Parameterized.Parameters(name = "states: {0}, id: {1}, result states: {2}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {emptyList(), emptyList(), ALL_STATES_WITHOUT_DELETED},
                {emptyList(), singletonList(1), ALL_STATES},
                {singletonList(AdTargetStateSelectionEnum.ON), emptyList(),
                        ImmutableSet.of(DynamicTextAdTargetState.ON)},
                {singletonList(AdTargetStateSelectionEnum.OFF), emptyList(),
                        ImmutableSet.of(DynamicTextAdTargetState.OFF)},
                {singletonList(AdTargetStateSelectionEnum.SUSPENDED), emptyList(),
                        ImmutableSet.of(DynamicTextAdTargetState.SUSPENDED)},
                {singletonList(AdTargetStateSelectionEnum.DELETED), emptyList(),
                        ImmutableSet.of(DynamicTextAdTargetState.DELETED)},
                {singletonList(AdTargetStateSelectionEnum.ON), singletonList(1),
                        ImmutableSet.of(DynamicTextAdTargetState.ON)},
                {ImmutableList.of(AdTargetStateSelectionEnum.ON, AdTargetStateSelectionEnum.OFF,
                        AdTargetStateSelectionEnum.SUSPENDED, AdTargetStateSelectionEnum.DELETED), emptyList(),
                        ALL_STATES}, {ImmutableList.of(AdTargetStateSelectionEnum.ON, AdTargetStateSelectionEnum.OFF,
                AdTargetStateSelectionEnum.SUSPENDED), emptyList(), ALL_STATES_WITHOUT_DELETED}
        };
    }

    @Test
    public void test() {
        assertThat(GetRequestConverter.convertStates(states, ids.isEmpty()))
                .containsExactlyInAnyOrder(expectedStates.toArray(new DynamicTextAdTargetState[0]));
    }
}
