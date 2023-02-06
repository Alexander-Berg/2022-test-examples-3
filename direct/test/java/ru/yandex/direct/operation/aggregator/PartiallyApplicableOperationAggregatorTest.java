package ru.yandex.direct.operation.aggregator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.operation.PartiallyApplicableOperation;
import ru.yandex.direct.operation.creator.OperationCreator;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.operation.aggregator.PartiallyApplicableOperationAggregator.checkAggregatedResultState;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;


@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class PartiallyApplicableOperationAggregatorTest {
    private static final List<Long> SRC_1_1 = Collections.singletonList(11L);
    private static final List<Boolean> SRC_1_2 = Collections.singletonList(Boolean.FALSE);

    private static final List<Long> SRC_2_1 = Arrays.asList(11L, 22L);
    private static final List<Integer> SRC_2_2 = Arrays.asList(10011, 10022);

    private static final List<Long> SRC_3_1 = Arrays.asList(11L, 22L, 33L);
    private static final List<Object> SRC_3_2 = Arrays.asList(new Object(), new Object(), new Object());

    @Captor
    private ArgumentCaptor<Set<Integer>> indexesArgumentCaptor;

    @Test
    public void noAnyApplyMethodCallWhenMasterOpHasOperationalError() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_1_1)
                .withOperationalError(new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_1_2)
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        aggOperation.prepareAndApplyPartialTogether();

        verify(op1, never()).cancel();
        verify(op2).cancel();
        verify(op1, never()).apply(anySet());
        verify(op1, never()).apply();
        verify(op2, never()).apply(anySet());
        verify(op2, never()).apply();
    }

    @Test
    public void noAnyApplyMethodCallWhenSecondOpHasOperationalError() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_1_1)
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_1_2)
                .withOperationalError(new Defect<>(DefectIds.INVALID_VALUE))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        aggOperation.prepareAndApplyPartialTogether();

        verify(op1).cancel();
        verify(op1).cancel();
        verify(op1, never()).apply(anySet());
        verify(op1, never()).apply();
        verify(op2, never()).apply(anySet());
        verify(op2, never()).apply();
    }

    @Test
    public void noAnyApplyMethodCallWhenFirstAndSecondOperationHasAllElementErrorTogether() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_2_1)
                .withError(0, new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_2_2)
                .withError(1, new Defect<>(DefectIds.INVALID_VALUE))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        aggOperation.prepareAndApplyPartialTogether();

        verify(op1).cancel();
        verify(op1).cancel();
        verify(op1, never()).apply(anySet());
        verify(op1, never()).apply();
        verify(op2, never()).apply(anySet());
        verify(op2, never()).apply();
    }

    @Test
    public void applyIntersectionOfValidElementsOnly() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_3_1)
                .withError(2, new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_3_2)
                .withError(0, new Defect<>(DefectIds.INVALID_VALUE))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        aggOperation.prepareAndApplyPartialTogether();

        verify(op1, never()).cancel();
        verify(op1, never()).cancel();
        verify(op1).apply(indexesArgumentCaptor.capture());
        assertThat("Применение операции должно вызываться только для тех элементов, "
                        + "которые валидны и в первой и второй операции",
                indexesArgumentCaptor.getValue(), equalTo(singleton(1)));
    }

    @Test
    public void aggregatedResultHasMergedErrors() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_1_1)
                .withError(0, new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_1_2)
                .withError(0, new Defect<>(DefectIds.NO_RIGHTS))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);

        MassResult<Long> result = aggOperation.prepareAndApplyPartialTogether();

        List<DefectInfo<Defect>> errorsForFirstElement = result.get(0).getErrors();
        assertThat("Для первого элемента должны быть возвращены ошибки и из первой и из второй операции",
                errorsForFirstElement, containsInAnyOrder(
                        validationError(path(field("op1")), new Defect<>(DefectIds.INVALID_VALUE)),
                        validationError(path(field("op2")), new Defect<>(DefectIds.NO_RIGHTS))
                ));
    }

    @Test
    public void aggregatedResultHasBrokenItemsWhenResultsOfInnerOperationsHasErrors() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_2_1)
                .withError(0, new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_2_2)
                .withError(1, new Defect<>(DefectIds.NO_RIGHTS))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        MassResult<Long> result = aggOperation.prepareAndApplyPartialTogether();
        verify(op1).cancel();
        verify(op2).cancel();
        assertThat("Не должно быть ошибки уровня операции",
                result.getState(), equalTo(ResultState.SUCCESSFUL));
        assertThat("Для первого элемента должна быть возвращена ожидаемая ошибка",
                result.get(0).getErrors(),
                contains(validationError(path(field("op1")), new Defect<>(DefectIds.INVALID_VALUE))));
        assertThat("Для второго элемента должна быть возвращена ожидаемая ошибка",
                result.get(1).getErrors(), contains(validationError(path(field("op2")), new Defect<>(DefectIds.NO_RIGHTS))));
    }

    @Test
    public void aggregatedResultIsBrokenWhenFirstOperationResultIsBroken() {
        PartiallyApplicableOperation op1 = new OperationMockBuilder<>(SRC_2_1)
                .withOperationalError(new Defect<>(DefectIds.INVALID_VALUE))
                .build();
        PartiallyApplicableOperation op2 = new OperationMockBuilder<>(SRC_2_2)
                .withError(1, new Defect<>(DefectIds.NO_RIGHTS))
                .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", op1,
                "op2", op2);
        MassResult<Long> result = aggOperation.prepareAndApplyPartialTogether();
        verify(op1, never()).cancel();
        verify(op2).cancel();
        assertThat("Должна вернуться ошибка уровня операции", result.getState(), equalTo(ResultState.BROKEN));
        assertThat("Ошибка операции общего результата должна быть правильной", result.getErrors(),
                contains(validationError(path(), new Defect<>(DefectIds.INVALID_VALUE))));
    }

    @Test
    public void aggregatedResultIsBrokenWhenFirstOperationResultIsBrokenWithOperationCreators() {
        OperationCreator<Long, PartiallyApplicableOperation<Long>> opCreator1 =
                lst -> new OperationMockBuilder<>(lst)
                        .withOperationalError(new Defect<>(DefectIds.INVALID_VALUE))
                        .build();
        OperationCreator<Integer, PartiallyApplicableOperation<Long>> opCreator2 =
                lst -> new OperationMockBuilder<>(lst)
                        .withError(1, new Defect<>(DefectIds.NO_RIGHTS))
                        .build();

        PartiallyApplicableOperationAggregator aggOperation = PartiallyApplicableOperationAggregator.of(
                "op1", SRC_2_1, opCreator1,
                "op2", SRC_2_2, opCreator2);
        MassResult<Long> result = aggOperation.prepareAndApplyPartialTogether();
        assertThat("Должна вернуться ошибка уровня операции", result.getState(), equalTo(ResultState.BROKEN));
        assertThat("Ошибка операции общего результата должна быть правильной", result.getErrors(),
                contains(validationError(path(), new Defect<>(DefectIds.INVALID_VALUE))));
    }

    @Test
    public void checkAggregatedResultStateTest() {
        Collection<Result<?>> allResults = List.of(
                new Result<>(null, new ValidationResult<>(emptyList()), ResultState.SUCCESSFUL),
                new Result<>(null, new ValidationResult<>(emptyList()), ResultState.CANCELED));
        checkAggregatedResultState(allResults);
    }

    static class OperationMockBuilder<T> {
        private List<T> elements;
        private LinkedHashMap<Integer, List<Defect>> errors = new LinkedHashMap<>();
        private List<Defect> operationalErrors = new ArrayList<>();
        private Set<Integer> applicableIndexes;
        private boolean returnCanceled = false;
        private boolean executed = false;

        OperationMockBuilder(List<T> elements) {
            this.elements = elements;
        }

        OperationMockBuilder withError(int elementIndex, Defect defectType) {
            List<Defect> defectTypes = errors.computeIfAbsent(elementIndex, idx -> new ArrayList<>());
            defectTypes.add(defectType);
            return this;
        }

        public OperationMockBuilder withOperationalError(Defect defectType) {
            operationalErrors.add(defectType);
            return this;
        }

        OperationMockBuilder withReturnCanceled() {
            returnCanceled = true;
            return this;
        }

        PartiallyApplicableOperation build() {
            PartiallyApplicableOperation mock = mock(PartiallyApplicableOperation.class);
            when(mock.prepare()).thenReturn(Optional.empty());

            ValidationResult<List<T>, Defect> vr = new ValidationResult<>(elements);
            operationalErrors.forEach(vr::addError);
            errors.forEach((idx, errors) ->
                    errors.forEach(e -> vr.getOrCreateSubValidationResult(index(idx), elements.get(idx)).addError(e)));

            Set<Integer> validIndexes = IntStream.range(0, elements.size()).boxed()
                    .filter(idx -> !errors.containsKey(idx))
                    .filter(idx -> operationalErrors.isEmpty())
                    .collect(Collectors.toSet());
            when(mock.getValidElementIndexes()).thenReturn(validIndexes);

            when(mock.apply(anySet())).thenAnswer(invocation -> {
                applicableIndexes = invocation.getArgument(0);
                executed = true;
                return MassResult.successfulMassAction(elements, vr, Sets.difference(validIndexes, applicableIndexes));
            });

            when(mock.cancel()).thenAnswer(invocation -> {
                returnCanceled = true;
                executed = true;
                if (!operationalErrors.isEmpty()) {
                    return MassResult.brokenMassAction(elements, vr);
                } else {
                    // todo
                    return MassResult.successfulMassAction(elements, vr, validIndexes);
                }
            });

            when(mock.getResult()).thenAnswer(invocation -> {
                if (!operationalErrors.isEmpty()) {
                    return Optional.of(MassResult.brokenMassAction(elements, vr));
                }
                if (returnCanceled) {
                    // todo
                    return Optional.of(MassResult.successfulMassAction(elements, vr, validIndexes));
                }
                if (applicableIndexes != null) {
                    return Optional.of(MassResult.successfulMassAction(
                            elements, vr, Sets.difference(validIndexes, applicableIndexes)));
                }
                if (!executed) {
                    return Optional.empty();
                }

                return Optional.of(MassResult.successfulMassAction(elements, vr));
            });
            return mock;
        }
    }
}
