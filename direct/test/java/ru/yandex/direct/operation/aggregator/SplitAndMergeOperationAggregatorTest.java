package ru.yandex.direct.operation.aggregator;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import one.util.streamex.EntryStream;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.operation.Operation;
import ru.yandex.direct.operation.creator.OperationCreator;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFailed;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithItems;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * В тестах обычно участвует две операции суммирования с разными слагаемыми. Это сделано, чтобы не проверять
 * непосредственно вызываемость операций с нужными входными данными, но в то же время иметь возможность
 * по элементам результата видеть из каких операций они получены
 */
public class SplitAndMergeOperationAggregatorTest {
    private static final List<Integer> INPUT = asList(1, 2, 3, 4);
    private static final Predicate<Integer> PREDICATE = x -> x % 2 == 1;

    private SplitAndMergeOperationAggregator<Integer, Integer> aggregator;
    private OperationCreator<Integer, Operation<Integer>> operation1Creator;
    private OperationCreator<Integer, Operation<Integer>> operation2Creator;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        operation1Creator = mock(OperationCreator.class);
        operation2Creator = mock(OperationCreator.class);
        aggregator = SplitAndMergeOperationAggregator.builderForPartialOperations()
                .addSubOperation(PREDICATE, operation1Creator)
                .addSubOperation(PREDICATE.negate(), operation2Creator)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwErrorWhenOperationDoesNotCoverAllInput() {
        SplitAndMergeOperationAggregator.builder()
                .addSubOperation(PREDICATE, operation1Creator)
                .build()
                .execute(INPUT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwErrorWhenOperationsCoverSameElements() {
        SplitAndMergeOperationAggregator.builder()
                .addSubOperation(x -> true, operation1Creator)
                .addSubOperation(x -> true, operation2Creator)
                .build()
                .execute(INPUT);
    }

    @Test
    public void checkResultWhenPredicatesRotesAllItemsToSecondOperation() {
        fullySuccessOperation(operation1Creator, 100);
        fullySuccessOperation(operation2Creator, 200);
        MassResult<Integer> result = SplitAndMergeOperationAggregator.builder()
                .addSubOperation(x -> false, operation1Creator)
                .addSubOperation(x -> true, operation2Creator)
                .build()
                .execute(INPUT);
        assertThat(result, isSuccessfulWithItems(201, 202, 203, 204));
    }

    @Test
    public void checkMergedResultOnSuccessfulOperations() {
        fullySuccessOperation(operation1Creator, 100);
        fullySuccessOperation(operation2Creator, 200);
        MassResult<Integer> result = aggregator.execute(INPUT);
        assertThat(result, isSuccessfulWithItems(101, 202, 103, 204));
    }

    @Test
    public void checkCallsOnSuccessfulOperations() {
        fullyBrokenOperation(operation1Creator);
        fullySuccessOperation(operation2Creator, 200);
        aggregator.execute(INPUT);
        verify(operation1Creator).create(eq(asList(1, 3)));
        verify(operation2Creator).create(eq(asList(2, 4)));
    }

    @Test
    public void checkResultWhenOneOfOperationIsBroken() {
        fullySuccessOperation(operation1Creator, 100);
        fullyBrokenOperation(operation2Creator);
        MassResult<Integer> result = aggregator.execute(INPUT);
        assertThat(result, isFailed());
    }

    @Test
    public void checkResultWhenAllOperationsAreBroken() {
        fullyBrokenOperation(operation1Creator);
        fullyBrokenOperation(operation2Creator);
        MassResult<Integer> result = aggregator.execute(INPUT);
        assertThat(result, isFailed());
    }

    @Test
    public void checkResultWhenOperationsIsSemiSuccessful() {
        semiSuccessOperation(operation1Creator, asList(true, false), 100);
        semiSuccessOperation(operation2Creator, asList(false, true), 200);
        MassResult<Integer> result = aggregator.execute(INPUT);
        assertThat(result, isSuccessfulWithMatchers(equalTo(101), null, null, equalTo(204)));
    }


    @SuppressWarnings("unchecked")
    private void fullySuccessOperation(OperationCreator<Integer, Operation<Integer>> operationCreator,
                                       Integer summator) {
        when(operationCreator.create(anyList())).thenAnswer(
                invocation -> new TestOperation(
                        (List<Integer>) invocation.getArguments()[0], v -> v + summator));
    }

    @SuppressWarnings("unchecked")
    private void semiSuccessOperation(OperationCreator<Integer, Operation<Integer>> operationCreator,
                                      List<Boolean> successfulElements, Integer summator) {
        when(operationCreator.create(anyList())).thenAnswer(
                invocation -> {
                    List<Integer> input = (List<Integer>) invocation.getArguments()[0];
                    return new TestOperation(input, successfulElements, v -> v + summator);
                });
    }

    @SuppressWarnings("unchecked")
    private void fullyBrokenOperation(OperationCreator<Integer, Operation<Integer>> operationCreator) {
        when(operationCreator.create(anyList())).thenAnswer(
                invocation -> new BrokenTestOperation((List<Integer>) invocation.getArguments()[0]));
    }


    static class TestOperation implements Operation<Integer> {
        protected final List<Integer> input;
        private final List<Boolean> successfulElements;
        protected MassResult<Integer> result;
        private Function<Integer, Integer> operation;

        TestOperation(List<Integer> input) {
            this(input, null, v -> v);
        }

        TestOperation(List<Integer> input, Function<Integer, Integer> operation) {
            this(input, null, operation);
        }

        TestOperation(List<Integer> input, List<Boolean> successfulElements, Function<Integer, Integer> operation) {
            this.input = input;
            this.successfulElements = successfulElements;
            this.operation = operation;
            Preconditions.checkArgument(successfulElements == null || input.size() == successfulElements.size());
        }

        public List<Integer> getInput() {
            return input;
        }

        @Override
        public Optional<MassResult<Integer>> prepare() {
            return Optional.ofNullable(result);
        }

        @Override
        public MassResult<Integer> apply() {
            if (result == null) {
                if (successfulElements == null) {
                    // не задано, значит все успешные
                    result = MassResult.successfulMassAction(
                            mapList(input, operation), ValidationResult.success(input));
                } else {
                    List<Integer> resultValues = mapList(input, operation);
                    List<Result<Integer>> results = EntryStream.zip(successfulElements, resultValues)
                            .mapKeyValue((success, val) -> success ?
                                    Result.successful(val)
                                    : Result.<Integer>broken(
                                    ValidationResult.failed(null, new Defect<>(DefectIds.INVALID_VALUE))))
                            .collect(Collectors.toList());
                    result = new MassResult<>(results, ValidationResult.success(input), ResultState.SUCCESSFUL);
                }
            }
            return result;
        }

        @Override
        public MassResult<Integer> cancel() {
            result = MassResult.successfulMassAction(
                    mapList(input, operation), ValidationResult.success(input));
            return result;
        }

        @Override
        public Optional<MassResult<Integer>> getResult() {
            return Optional.ofNullable(result);
        }
    }

    static class BrokenTestOperation extends TestOperation {
        BrokenTestOperation(List<Integer> input) {
            super(input);
            result = MassResult.brokenMassAction(
                    null, ValidationResult.failed(this.input, new Defect<>(DefectIds.INVALID_VALUE)));
        }
    }
}
