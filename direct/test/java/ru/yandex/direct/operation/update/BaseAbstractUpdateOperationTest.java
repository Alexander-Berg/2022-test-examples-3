package ru.yandex.direct.operation.update;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import one.util.streamex.StreamEx;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.Goal;
import ru.yandex.direct.operation.testing.entity.RetargetingCondition;
import ru.yandex.direct.operation.testing.entity.Rule;
import ru.yandex.direct.operation.testing.entity.RuleType;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.builder.ItemValidationBuilder;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.validation.result.PathHelper.index;

@SuppressWarnings("unchecked")
public class BaseAbstractUpdateOperationTest {

    protected static final boolean MUST_BE_CALLED = true;
    protected static final boolean MUST_NOT_BE_CALLED = false;

    protected static final String NEW_NAME = "ret cond 1 new name";
    protected static final String NEW_DESCRIPTION = "ret cond 2 new desc";

    private static final Defect<Void> INVALID_VALUE_DEFECT = new Defect<>(DefectIds.INVALID_VALUE);
    private static final Defect<Void> DUPLICATE_DEFECT = new Defect<>(new DefectId<>() {
        @Override
        public String getCode() {
            return "MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS";
        }
    });
    private static final Defect<Void> SIZE_MORE_THAN_MAX_DEFECT = new Defect<>(new DefectId<>() {
        @Override
        public String getCode() {
            return "SIZE_CANNOT_BE_MORE_THAN_MAX";
        }
    });

    protected RetargetingCondition retCond1 = createRetCond1();
    protected RetargetingCondition retCond2 = createRetCond2();
    protected List<RetargetingCondition> retCondsList = asList(retCond1, retCond2);

    protected ModelChanges<RetargetingCondition> retCond1Changes = createRetCond1Changes(retCond1.getId());
    protected ModelChanges<RetargetingCondition> retCond2Changes = createRetCond2Changes(retCond2.getId());
    protected List<ModelChanges<RetargetingCondition>> modelChangesList = asList(retCond1Changes, retCond2Changes);

    protected Answer<ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect>>
            modelChangesValidationResultAnswer;
    protected Answer<ValidationResult<List<RetargetingCondition>, Defect>> appliedChangesValidationAnswer;


    protected TestableUpdateOperation createUpdateOperationMock(Applicability applicability) {
        TestableUpdateOperation updateOperation =
                new TestableUpdateOperation(applicability, modelChangesList,
                        id -> {
                            RetargetingCondition retargetingCondition = new RetargetingCondition();
                            retargetingCondition.withId(id);
                            return retargetingCondition;
                        });

        TestableUpdateOperation mockedUpdateOperation = spy(updateOperation);

        Answer<Collection<RetargetingCondition>> getModelsAnswer = invocation -> {
            Collection<Long> ids = (Collection<Long>) invocation.getArguments()[0];
            return StreamEx.of(retCondsList)
                    .filter(rc -> ids.contains(rc.getId()))
                    .toList();
        };

        when(mockedUpdateOperation.validateModelChanges(any()))
                .then(modelChangesValidationResultAnswer);
        when(mockedUpdateOperation.getModels(any()))
                .then(getModelsAnswer);
        when(mockedUpdateOperation.validateAppliedChanges(any()))
                .then(appliedChangesValidationAnswer);

        return mockedUpdateOperation;
    }

    protected TestableSimpleUpdateOperation createSimpleUpdateOperationMock(Applicability applicability) {
        TestableSimpleUpdateOperation updateOperation =
                new TestableSimpleUpdateOperation(applicability, modelChangesList,
                        id -> {
                            RetargetingCondition retargetingCondition = new RetargetingCondition();
                            retargetingCondition.withId(id);
                            return retargetingCondition;
                        });

        TestableSimpleUpdateOperation mockedUpdateOperation = spy(updateOperation);

        Answer<Collection<RetargetingCondition>> getModelsAnswer = invocation -> {
            Collection<Long> ids = (Collection<Long>) invocation.getArguments()[0];
            return StreamEx.of(retCondsList)
                    .filter(rc -> ids.contains(rc.getId()))
                    .toList();
        };

        when(mockedUpdateOperation.validateModelChanges(any()))
                .then(modelChangesValidationResultAnswer);
        when(mockedUpdateOperation.getModels(any()))
                .then(getModelsAnswer);
        when(mockedUpdateOperation.validateAppliedChanges(any()))
                .then(appliedChangesValidationAnswer);

        return mockedUpdateOperation;
    }

    protected RetargetingCondition createRetCond1() {
        Rule rule1 = new Rule();
        Goal goal = new Goal();
        goal.withId(123L);
        rule1.withType(RuleType.ALL)
                .withGoals(singletonList(goal));

        Goal goal1 = new Goal();
        goal1.withId(1234L);

        Goal goal2 = new Goal();
        goal2.withId(12345L);

        Rule rule2 = new Rule();
        rule2.withType(RuleType.OR)
                .withGoals(asList(goal1, goal2));

        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withId(1210284L)
                .withName("ret cond 1")
                .withDescription("ret cond 1 desc")
                .withRules(asList(rule1, rule2));

        return retargetingCondition;
    }

    protected RetargetingCondition createRetCond2() {
        Goal goal = new Goal();
        goal.withId(123L);
        Rule rule1 = new Rule();
        rule1.withType(RuleType.NOT)
                .withGoals(singletonList(goal));

        Rule rule2 = new Rule();

        Goal goal1 = new Goal();
        goal1.withId(1234L);

        Goal goal2 = new Goal();
        goal2.withId(12345L);

        rule2.withType(RuleType.OR)
                .withGoals(asList(goal1, goal2));

        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition.withId(2394809L)
                .withName("ret cond 2")
                .withDescription("ret cond 2 desc")
                .withRules(asList(rule1, rule2));
        return retargetingCondition;
    }

    protected ModelChanges<RetargetingCondition> createRetCond1Changes(Long id) {
        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(id);
        modelChanges.process(NEW_NAME, RetargetingCondition.NAME);
        return modelChanges;
    }

    protected ModelChanges<RetargetingCondition> createRetCond2Changes(Long id) {
        ModelChanges<RetargetingCondition> modelChanges = retargetingConditionModelChanges(id);
        modelChanges.process(NEW_DESCRIPTION, RetargetingCondition.DESCRIPTION);
        return modelChanges;
    }

    protected void modelChangesValidationIsFullyValid() {
        modelChangesValidationResultAnswer = invocation ->
                new ValidationResult<>((List<ModelChanges<RetargetingCondition>>) invocation.getArguments()[0]);
    }

    protected void modelChangesValidationWithTopLevelError() {
        modelChangesValidationResultAnswer = invocation -> {
            List<ModelChanges<RetargetingCondition>> modelChanges =
                    (List<ModelChanges<RetargetingCondition>>) invocation.getArguments()[0];
            return ItemValidationBuilder.of(modelChanges, Defect.class)
                    .check(t -> INVALID_VALUE_DEFECT)
                    .getResult();
        };
    }

    protected void modelChangesValidationWithInvalidFirstItem() {
        modelChangesValidationResultAnswer = invocation -> {
            List<ModelChanges<RetargetingCondition>> modelChanges =
                    (List<ModelChanges<RetargetingCondition>>) invocation.getArguments()[0];
            ValidationResult<List<ModelChanges<RetargetingCondition>>, Defect> validationResult =
                    new ValidationResult<>(modelChanges);
            ValidationResult<?, Defect> subResult =
                    validationResult.getOrCreateSubValidationResult(index(0), modelChangesList.get(0));
            subResult.addError(INVALID_VALUE_DEFECT);
            return validationResult;
        };
    }

    protected void modelChangesValidationWithAllInvalidItems() {
        modelChangesValidationResultAnswer = invocation -> {
            List<ModelChanges<RetargetingCondition>> modelChanges =
                    (List<ModelChanges<RetargetingCondition>>) invocation.getArguments()[0];
            return ListValidationBuilder.of(modelChanges, Defect.class)
                    .checkEach((Constraint<ModelChanges<RetargetingCondition>, Defect>)
                            (t -> INVALID_VALUE_DEFECT))
                    .getResult();
        };
    }

    @SuppressWarnings("unchecked")
    protected void appliedChangesValidationIsFullyValid() {
        appliedChangesValidationAnswer = invocation ->
                (ValidationResult<List<RetargetingCondition>, Defect>) invocation.getArguments()[0];
    }

    @SuppressWarnings("unchecked")
    protected void appliedChangesValidationWithTopLevelError() {
        appliedChangesValidationAnswer = invocation -> {
            ValidationResult<List<RetargetingCondition>, Defect> result =
                    (ValidationResult<List<RetargetingCondition>, Defect>) invocation.getArguments()[0];
            return new ListValidationBuilder<>(result)
                    .check(t -> SIZE_MORE_THAN_MAX_DEFECT)
                    .getResult();
        };
    }

    @SuppressWarnings("unchecked")
    protected void appliedChangesValidationWithInvalidSecondItem() {
        appliedChangesValidationAnswer = invocation -> {
            ValidationResult<List<RetargetingCondition>, Defect> result =
                    (ValidationResult<List<RetargetingCondition>, Defect>) invocation.getArguments()[0];
            ValidationResult<RetargetingCondition, Defect> subResult =
                    result.getOrCreateSubValidationResult(index(1), result.getValue().get(1));
            subResult.addError(DUPLICATE_DEFECT);
            return result;
        };
    }

    @SuppressWarnings("unchecked")
    protected void appliedChangesValidationWithAllInvalidItems() {
        appliedChangesValidationAnswer = invocation -> {
            ValidationResult<List<RetargetingCondition>, Defect> result =
                    (ValidationResult<List<RetargetingCondition>, Defect>) invocation.getArguments()[0];
            return new ListValidationBuilder<>(result)
                    .checkEach((Constraint<RetargetingCondition, Defect>) t -> DUPLICATE_DEFECT)
                    .getResult();
        };
    }

    protected void checkPrepareReturnsResultWithItemsErrors(Applicability applicability, boolean firstItemError,
                                                            boolean secondItemError) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        Optional<MassResult<Long>> resultOptional = updateOperation.prepare();
        assertThat("prepare() должен вернуть результат", resultOptional.isPresent(), is(true));

        MassResult<Long> result = resultOptional.get();

        if (firstItemError) {
            assertThat("результат должен содержать ошибку в первом элементе",
                    result.getResult().get(0).isSuccessful(), is(false));
        } else {
            assertThat("результат не должен содержать ошибок в первом элементе",
                    result.getResult().get(0).isSuccessful(), is(true));
            assertThat("результат - null, так как результата после prepare еще быть не может",
                    result.getResult().get(0).getResult(), is(nullValue()));
        }

        if (secondItemError) {
            assertThat("результат должен содержать ошибку во втором элементе",
                    result.getResult().get(1).isSuccessful(), is(false));
        } else {
            assertThat("результат не должен содержать ошибок во втором элементе",
                    result.getResult().get(1).isSuccessful(), is(true));
            assertThat("результат - null, так как результата после prepare еще быть не может",
                    result.getResult().get(1).getResult(), is(nullValue()));
        }
    }

    protected void checkPrepareReturnsResultWithGlobalError(Applicability applicability) {
        TestableUpdateOperation updateOperation = createUpdateOperationMock(applicability);
        Optional<MassResult<Long>> resultOptional = updateOperation.prepare();
        assertThat("prepare() должен вернуть результат", resultOptional.isPresent(), is(true));

        MassResult<Long> result = resultOptional.get();

        assertThat("результат должен содержать ошибку уровня операции", result.getErrors(),
                hasSize(greaterThan(0)));
    }

    private static ModelChanges<RetargetingCondition> retargetingConditionModelChanges(Long id) {
        return new ModelChanges<>(id, RetargetingCondition.class);
    }
}
