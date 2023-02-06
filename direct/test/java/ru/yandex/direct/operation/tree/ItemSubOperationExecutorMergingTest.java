package ru.yandex.direct.operation.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.operation.tree.ItemSubOperationExecutor.mergeSubItemValidationResults;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@SuppressWarnings("All")
public class ItemSubOperationExecutorMergingTest {

    private static final String PARENT_TO_CHILD_FIELD = "child";
    private static final String PARENT_FIELD = "parentField";
    private static final String CHILD_FIELD = "childField";
    private static final Defect<Void> PARENT_ERROR = new Defect<>(DefectIds.INVALID_VALUE);
    private static final Defect<Void> PARENT_FIELD_ERROR = new Defect<>(DefectIds.ABSENT_REQUIRED_FIELD);
    // ошибка ребенка, изначально лежащая в родительском результате валидации
    private static final Defect<Void> PARENT_CHILD_ERROR = new Defect<>(DefectIds.INCONSISTENT_STATE);
    // ошибка на поле ребенка, изначально лежащая в родительском результате валидации
    private static final Defect<Void> PARENT_CHILD_FIELD_ERROR = new Defect<>(DefectIds.MUST_BE_NULL);
    private static final Defect<Void> CHILD_ERROR = new Defect<>(DefectIds.OBJECT_NOT_FOUND);
    private static final Defect<Void> CHILD_FIELD_ERROR = new Defect<>(DefectIds.REQUIRED_BUT_EMPTY);

    private ValidationResult<List<Parent>, Defect> parentValidationResult;
    private ValidationResult<List<Child>, Defect> childrenValidationResult;

    private Map<Integer, Integer> parentToChildIndexMap = new HashMap<>();
    private Map<Integer, Child> parentIndexToChildMap = new HashMap<>();

    // структура конечного результата валидации, когда нет ошибок валидации

    @Test
    public void doesntFailWhenEmptyParents() {
        parents(0);
        children(0);
        merge();
    }

    @Test
    public void doesntFailWhenOneParentWithoutChildren() {
        parents(1);
        children(0);
        merge();
    }

    @Test
    public void parentWithChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        merge();
        checkValidationResultStructure(0, 0);
    }

    @Test
    public void parentWithChildAndEmptyParent() {
        parents(2);
        children(1);
        addIndex(0, 0);
        merge();
        checkValidationResultStructure(0, 0);
    }

    @Test
    public void emptyParentAndParentWithChild() {
        parents(2);
        children(1);
        addIndex(1, 0);
        merge();
        checkValidationResultStructure(1, 0);
    }

    @Test
    public void doesntFailWhenTwoEmptyParents() {
        parents(2);
        children(0);
    }

    @Test
    public void twoParentsWithChildren() {
        parents(2);
        children(2);
        addIndex(0, 0);
        addIndex(1, 1);
        merge();
        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(1, 1);
    }

    @Test
    public void twoParentsWithChildrenWithEmptyParentBetween() {
        parents(3);
        children(2);
        addIndex(0, 0);
        addIndex(2, 1);
        merge();
        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(2, 1);
    }

    // структура конечного результата валидации, когда есть ошибки в родителях

    @Test
    public void invalidParentWithChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentError(0);
    }

    @Test
    public void validAndOneInvalidParentsWithChildren() {
        parents(2);
        children(2);
        addIndex(0, 0);
        addIndex(1, 1);
        addParentError(1);
        merge();
        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(1, 1);
        checkParentError(1);
    }

    @Test
    public void invalidFieldParentWithChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentFieldError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentFieldError(0);
    }

    @Test
    public void parentWithErrorOnChildIsMergedWithChildWithoutErrors() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentChildError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentChildError(0);
    }

    @Test
    public void parentWithErrorOnChildFieldIsMergedWithChildWithoutErrors() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentChildFieldError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentChildFieldError(0);
    }

    @Test
    public void mixedParentsWithErrorsAndChildrenWithoutErrors() {
        parents(3);
        children(2);
        addIndex(0, 0);
        addIndex(2, 1);

        addParentError(0);
        addParentFieldError(0);
        addParentFieldError(1);
        addParentError(2);
        addParentChildFieldError(2);

        merge();

        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(2, 1);
        checkParentError(0);
        checkParentFieldError(0);
        checkParentFieldError(1);
        checkParentError(2);
        checkParentChildFieldError(2);
    }

    // структура конечного результата валидации, когда есть ошибки в детях

    @Test
    public void parentWithInvalidChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addChildError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkChildError(0);
    }

    @Test
    public void parentWithValidChildAndParentWithInvalidChild() {
        parents(2);
        children(2);
        addIndex(0, 0);
        addIndex(1, 1);
        addChildError(1);
        merge();
        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(1, 1);
        checkChildError(1);
    }

    @Test
    public void emptyParentAndParentWithInvalidChild() {
        parents(2);
        children(1);
        addIndex(1, 0);
        addChildError(0);
        merge();
        checkValidationResultStructure(1, 0);
        checkChildError(1);
    }

    @Test
    public void parentWithInvalidFieldChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addChildFieldError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkChildFieldError(0);
    }

    // ошибки в родителях и детях

    @Test
    public void invalidParentWithInvalidChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentError(0);
        addChildError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentError(0);
        checkChildError(0);
    }

    @Test
    public void invalidFieldParentWithInvalidFieldChild() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentFieldError(0);
        addChildFieldError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentFieldError(0);
        checkChildFieldError(0);
    }

    @Test
    public void parentWithErrorOnChildIsMergedWithChildWithError() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentChildError(0);
        addChildError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentChildError(0);
        checkChildError(0);
    }

    @Test
    public void parentWithErrorOnChildFieldIsMergedWithChildWithErrorOnField() {
        parents(1);
        children(1);
        addIndex(0, 0);
        addParentChildFieldError(0);
        addChildFieldError(0);
        merge();
        checkValidationResultStructure(0, 0);
        checkParentChildFieldError(0);
        checkChildFieldError(0);
    }

    @Test
    public void parentWithMultipleErrorsIsMergedWithChildWithMultipleErrors() {
        parents(1);
        children(1);
        addIndex(0, 0);

        addParentError(0);
        addParentChildError(0);
        addParentChildFieldError(0);
        addChildError(0);
        addChildFieldError(0);

        merge();

        checkValidationResultStructure(0, 0);
        checkParentError(0);
        checkParentChildError(0);
        checkParentChildFieldError(0);
        checkChildError(0);
        checkChildFieldError(0);
    }

    @Test
    public void mixOfInvalidAndValidAndEmptyParentsAndChildren() {
        parents(4);
        children(3);
        addIndex(0, 0);
        addIndex(2, 1);
        addIndex(3, 2);

        addParentChildError(0);
        addParentChildFieldError(3);
        addChildError(0);
        addChildError(2);

        merge();

        checkValidationResultStructure(0, 0);
        checkValidationResultStructure(2, 1);
        checkValidationResultStructure(3, 2);

        checkParentChildError(0);
        checkChildError(0);

        checkParentChildFieldError(3);
        checkChildError(3);
    }

    private void parents(int num) {
        List<Parent> parents = list(num, Parent::new);
        parentValidationResult = new ValidationResult<>(parents);
    }

    private void children(int num) {
        checkArgument(num <= parentValidationResult.getValue().size());
        List<Child> children = list(num, Child::new);
        childrenValidationResult = new ValidationResult<>(children);
    }

    private void addIndex(int parentIndex, int childIndex) {
        parentToChildIndexMap.put(parentIndex, childIndex);
        parentIndexToChildMap.put(parentIndex, childrenValidationResult.getValue().get(childIndex));
    }

    private void addParentError(int parentIndex) {
        Parent parent = parentValidationResult.getValue().get(parentIndex);
        parentValidationResult.getOrCreateSubValidationResult(index(parentIndex), parent).addError(PARENT_ERROR);
    }

    private void addParentFieldError(int parentIndex) {
        Parent parent = parentValidationResult.getValue().get(parentIndex);
        parentValidationResult.getOrCreateSubValidationResult(index(parentIndex), parent)
                .getOrCreateSubValidationResult(field(PARENT_FIELD), null).addError(PARENT_FIELD_ERROR);
    }

    /**
     * Добавить ошибку на ребенка в исходный родительский результат валидации
     */
    private void addParentChildError(int parentIndex) {
        Parent parent = parentValidationResult.getValue().get(parentIndex);
        Child child = childrenValidationResult.getValue().get(parentToChildIndexMap.get(parentIndex));
        parentValidationResult.getOrCreateSubValidationResult(index(parentIndex), parent)
                .getOrCreateSubValidationResult(field(PARENT_TO_CHILD_FIELD), child)
                .addError(PARENT_CHILD_ERROR);
    }

    /**
     * Добавить ошибку на поле ребенка в исходный родительский результат валидации
     */
    private void addParentChildFieldError(int parentIndex) {
        Parent parent = parentValidationResult.getValue().get(parentIndex);
        Child child = childrenValidationResult.getValue().get(parentToChildIndexMap.get(parentIndex));
        parentValidationResult.getOrCreateSubValidationResult(index(parentIndex), parent)
                .getOrCreateSubValidationResult(field(PARENT_TO_CHILD_FIELD), child)
                .getOrCreateSubValidationResult(field(CHILD_FIELD), null)
                .addError(PARENT_CHILD_FIELD_ERROR);
    }

    private void addChildError(int childIndex) {
        Child child = childrenValidationResult.getValue().get(childIndex);
        childrenValidationResult.getOrCreateSubValidationResult(index(childIndex), child)
                .addError(CHILD_ERROR);
    }

    private void addChildFieldError(int childIndex) {
        Child child = childrenValidationResult.getValue().get(childIndex);
        childrenValidationResult.getOrCreateSubValidationResult(index(childIndex), child)
                .getOrCreateSubValidationResult(field(CHILD_FIELD), null)
                .addError(CHILD_FIELD_ERROR);
    }

    private void merge() {
        mergeSubItemValidationResults(parentValidationResult, childrenValidationResult,
                parentToChildIndexMap, parentIndexToChildMap, PARENT_TO_CHILD_FIELD);
    }

    private void checkValidationResultStructure(int parentIndex, int childIndex) {
        ValidationResult<?, Defect> expectedParentResult =
                parentValidationResult.getSubResults().get(index(parentIndex));
        assertThat("не найден результат валидации родительского объекта по индексу " + parentIndex,
                expectedParentResult, notNullValue());

        Parent expectedParent = parentValidationResult.getValue().get(parentIndex);
        assertThat("не найден соответствующий родитель в результате валидации "
                        + "родительского объекта по индексу " + parentIndex,
                expectedParentResult.getValue(), sameInstance(expectedParent));

        if (childrenValidationResult.getSubResults().get(index(childIndex)) != null) {
            ValidationResult<?, Defect> expectedChildResult =
                    expectedParentResult.getSubResults().get(field(PARENT_TO_CHILD_FIELD));
            assertThat("не найден результат валидации дочернего объекта по индексу родителя " + parentIndex,
                    expectedChildResult, notNullValue());

            Child expectedChild = childrenValidationResult.getValue().get(childIndex);
            assertThat("не найден соответствующий ребенок в результате валидации "
                            + "родительского объекта по индексу " + parentIndex,
                    expectedChildResult.getValue(), sameInstance(expectedChild));
        }
    }

    private void checkParentError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(validationError(path(index(parentIndex)), PARENT_ERROR)));
    }

    private void checkParentFieldError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(
                        validationError(path(index(parentIndex), field(PARENT_FIELD)), PARENT_FIELD_ERROR)));
    }

    private void checkParentChildError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(
                        validationError(path(index(parentIndex), field(PARENT_TO_CHILD_FIELD)), PARENT_CHILD_ERROR)));
    }

    private void checkParentChildFieldError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(
                        validationError(path(index(parentIndex), field(PARENT_TO_CHILD_FIELD), field(CHILD_FIELD)),
                                PARENT_CHILD_FIELD_ERROR)));
    }

    private void checkChildError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(
                        validationError(path(index(parentIndex), field(PARENT_TO_CHILD_FIELD)), CHILD_ERROR)));
    }

    private void checkChildFieldError(int parentIndex) {
        assertThat(parentValidationResult,
                hasDefectDefinitionWith(
                        validationError(path(index(parentIndex), field(PARENT_TO_CHILD_FIELD), field(CHILD_FIELD)),
                                CHILD_FIELD_ERROR)));
    }

    private <T> List<T> list(int num, Supplier<T> supplier) {
        return IntStream.range(0, num)
                .mapToObj(i -> supplier.get())
                .collect(toList());
    }

    private static class Parent {
        private Child child;
    }

    private static class Child {
        private String value;
    }
}
