package ru.yandex.market.core.feature.cutoff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.feature.model.cutoff.UtilityCutoffs;
import ru.yandex.market.core.param.model.ParamCheckStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static ru.yandex.market.core.param.model.ParamCheckStatus.DONT_WANT;
import static ru.yandex.market.core.param.model.ParamCheckStatus.FAIL;
import static ru.yandex.market.core.param.model.ParamCheckStatus.FAIL_MANUAL;
import static ru.yandex.market.core.param.model.ParamCheckStatus.NEW;
import static ru.yandex.market.core.param.model.ParamCheckStatus.REVOKE;
import static ru.yandex.market.core.param.model.ParamCheckStatus.SUCCESS;

/**
 * Тесты для {@link FeatureCutoffStatusStateMachine}
 */
class FeatureCutoffStatusStateMachineTest {

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "]: {0} -> {1}: openCutoffs: {2}, expectedCutoffsToClose: {3}")
    @MethodSource("getAllBlockingTargetStatusCutoffsTestData")
    @DisplayName("Проверка, что у катофов, которые необходимо закрыть, порядок следования статусов совпадает")
    void getAllBlockingTargetStatusCutoffsOrderTest(ParamCheckStatus currentStatus,
                                                    ParamCheckStatus targetStatus,
                                                    List<FeatureCustomCutoffType> openCutoffs,
                                                    List<FeatureCustomCutoffType> expectedCutoffsToClose) {
        var actualBlockingCutoffs = getBlockingCutoffs(currentStatus, targetStatus, openCutoffs);

        var expectedStatusOrder = expectedCutoffsToClose.stream()
                .map(cutoff -> cutoff.getTargetStatus().orElseThrow())
                .distinct()
                .collect(Collectors.toList());
        var actualStatusOrder = actualBlockingCutoffs.stream()
                .map(cutoff -> cutoff.getTargetStatus().orElseThrow())
                .distinct()
                .collect(Collectors.toList());
        assertThat(actualStatusOrder, is(expectedStatusOrder));
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "]: {0} -> {1}: openCutoffs: {2}, expectedCutoffsToClose: {3}")
    @MethodSource("getAllBlockingTargetStatusCutoffsTestData")
    @DisplayName("Проверка получения катофов, которые необходимо закрыть, чтобы перейти в другой статус")
    void getAllBlockingTargetStatusCutoffsTest(ParamCheckStatus currentStatus,
                                               ParamCheckStatus targetStatus,
                                               List<FeatureCustomCutoffType> openCutoffs,
                                               List<FeatureCustomCutoffType> expectedCutoffsToClose) {
        var actualBlockingCutoffs = getBlockingCutoffs(currentStatus, targetStatus, openCutoffs);

        assertThat(actualBlockingCutoffs, containsInAnyOrder(expectedCutoffsToClose.toArray()));
    }

    private static Stream<Arguments> getAllBlockingTargetStatusCutoffsTestData() {
        return Stream.of(
                Arguments.of(FAIL, NEW,
                        List.of(FeatureCutoffType.QUALITY, FeatureCutoffType.PARTNER, FeatureCutoffType.PINGER),
                        List.of(FeatureCutoffType.QUALITY, FeatureCutoffType.PARTNER)),
                Arguments.of(REVOKE, NEW,
                        List.of(DSBSCutoffs.QUALITY_SERIOUS,
                                DSBSCutoffs.QUALITY_OTHER,
                                UtilityCutoffs.NEED_TESTING),
                        List.of(DSBSCutoffs.QUALITY_SERIOUS,
                                DSBSCutoffs.QUALITY_OTHER,
                                UtilityCutoffs.NEED_TESTING)),
                Arguments.of(DONT_WANT, NEW,
                        List.of(FeatureCutoffType.PARTNER),
                        List.of(FeatureCutoffType.PARTNER)),
                Arguments.of(FAIL, SUCCESS,
                        List.of(FeatureCutoffType.QUALITY),
                        List.of(FeatureCutoffType.QUALITY, FeatureCutoffType.TESTING)),
                Arguments.of(FAIL, SUCCESS,
                        List.of(FeatureCutoffType.QUALITY, DSBSCutoffs.ORDER_NOT_ACCEPTED),
                        List.of(FeatureCutoffType.QUALITY, FeatureCutoffType.TESTING)),
                Arguments.of(FAIL, NEW,
                        List.of(FeatureCutoffType.QUALITY, DSBSCutoffs.ORDER_NOT_ACCEPTED),
                        List.of(FeatureCutoffType.QUALITY))
        );
    }

    private List<FeatureCustomCutoffType> getBlockingCutoffs(ParamCheckStatus currentStatus,
                                                             ParamCheckStatus targetStatus,
                                                             List<FeatureCustomCutoffType> openCutoffs) {
        return FeatureCutoffStatusStateMachine.getAllBlockingTargetStatusCutoffs(currentStatus, targetStatus,
                Set.copyOf(openCutoffs));
    }

    @ParameterizedTest(name = "[" + INDEX_PLACEHOLDER + "]: {0} -> {1}: {2}")
    @MethodSource("statusCanBeReachFromTestData")
    @DisplayName("Проверка возможности перехода из различных статусов")
    void statusCanBeReachFromTest(ParamCheckStatus from, ParamCheckStatus to, boolean expected) {
        assertThat(FeatureCutoffStatusStateMachine.statusCanBeReachByClosingCutoffsFrom(from, to), is(expected));
    }

    private static Stream<Arguments> statusCanBeReachFromTestData() {
        List<Arguments> data = new ArrayList<>();
        data.addAll(addValidTransition(REVOKE, FAIL));
        data.addAll(addValidTransition(FAIL, DONT_WANT));
        data.addAll(addValidTransition(DONT_WANT, NEW));
        data.addAll(addValidTransition(NEW, SUCCESS));

        data.addAll(addValidTransition(REVOKE, DONT_WANT));
        data.addAll(addValidTransition(REVOKE, NEW));
        data.addAll(addValidTransition(REVOKE, SUCCESS));

        data.addAll(addValidTransition(FAIL, NEW));
        data.addAll(addValidTransition(FAIL, SUCCESS));

        data.add(addTransitionToHimself(REVOKE));
        data.add(addTransitionToHimself(FAIL));
        data.add(addTransitionToHimself(DONT_WANT));
        data.add(addTransitionToHimself(NEW));
        data.add(addTransitionToHimself(SUCCESS));

        data.add(addInvalidTransition(FAIL_MANUAL, REVOKE));
        data.add(addInvalidTransition(FAIL_MANUAL, FAIL));
        data.add(addInvalidTransition(FAIL_MANUAL, DONT_WANT));
        data.add(addInvalidTransition(FAIL_MANUAL, NEW));
        data.add(addInvalidTransition(FAIL_MANUAL, SUCCESS));

        return data.stream();
    }

    private static List<Arguments> addValidTransition(ParamCheckStatus from, ParamCheckStatus to) {
        return List.of(
                Arguments.of(from, to, true),
                Arguments.of(to, from, false)
        );
    }

    private static Arguments addTransitionToHimself(ParamCheckStatus status) {
        return Arguments.of(status, status, false);
    }

    private static Arguments addInvalidTransition(ParamCheckStatus from, ParamCheckStatus to) {
        return Arguments.of(from, to, false);
    }
}
