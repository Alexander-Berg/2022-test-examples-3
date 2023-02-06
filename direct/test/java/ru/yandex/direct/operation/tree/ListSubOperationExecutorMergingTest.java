package ru.yandex.direct.operation.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import org.junit.Test;

import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.operation.testing.entity.Keyword;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.operation.tree.ListSubOperationExecutor.mergeChildrenSubListValidationResults;
import static ru.yandex.direct.utils.FunctionalUtils.intRange;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;

@SuppressWarnings("ALL")
public class ListSubOperationExecutorMergingTest {

    private static final String PROPERTY = "keywords";

    private static final String PARENT_FIELD_1 = AdGroup.ID.name();
    private static final String PARENT_FIELD_2 = AdGroup.MINUS_KEYWORDS.name();
    private static final String CHILD_FIELD_1 = Keyword.PHRASE.name();
    private static final String CHILD_FIELD_2 = Keyword.PRICE.name();
    private static final Defect ERR_1 = new Defect<>(DefectIds.INVALID_VALUE);
    private static final Defect ERR_2 = new Defect<>(DefectIds.NO_RIGHTS);
    private static final Defect ERR_3 = new Defect<>(DefectIds.ABSENT_REQUIRED_FIELD);
    private static final Defect ERR_4 = new Defect<>(new DefectId<Void>() {
        @Override
        public String getCode() {
            return "MUST_CONTAIN_ONLY_SYMBOLS";
        }
    });
    private static final Defect ERR_5 = new Defect<>(new DefectId<Void>() {
        @Override
        public String getCode() {
            return "MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS";
        }
    });

    private List<AdGroup> parents;
    private ValidationResult<List<AdGroup>, Defect> parentsResult;

    private List<Keyword> flatChildren;
    private ValidationResult<List<Keyword>, Defect> flatChildrenResult;

    private ListMultimap<Integer, Integer> indexMap;
    private ListMultimap<Integer, Keyword> parentIndexToChildrenMap;

    // 0 родителей

    @Test
    public void emptyParents() {
        parentResults(0);
        flatChildrenResults(0);
        emptyIndexMaps();
        mergeAndCheckValidationResultStructure();
    }

    // 1 родитель + 0 детей

    @Test
    public void validParentAndNullChildren() {
        parentResults(1);
        flatChildrenResults(0);
        emptyIndexMaps();
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentAndEmptyChildren() {
        parentResults(1);
        flatChildrenResults(0);
        indexMaps(0, emptyList());
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentWithEmptyResultAndEmptyChildren() {
        parentResults(1);
        flatChildrenResults(0);
        indexMaps(0, emptyList());

        parentsResult.getOrCreateSubValidationResult(index(0), parents.get(0));

        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void parentWithErrorAndNullChildren() {
        parentResults(1);
        flatChildrenResults(0);
        emptyIndexMaps();

        putParentError(0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentError(0, ERR_1);
    }

    @Test
    public void parentWithErrorAndEmptyChildren() {
        parentResults(1);
        flatChildrenResults(0);
        indexMaps(0, emptyList());

        putParentError(0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentError(0, ERR_1);
    }

    @Test
    public void parentWithFieldErrorAndEmptyChildren() {
        parentResults(1);
        flatChildrenResults(0);
        indexMaps(0, emptyList());

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
    }

    // 1 родитель + 1 ребенок

    @Test
    public void validParentAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentWithEmptyResultAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        parentsResult.getOrCreateSubValidationResult(index(0), parents.get(0));

        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentAndValidChildWithEmptyResult() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        flatChildrenResult.getOrCreateSubValidationResult(index(0), flatChildren.get(0));

        mergeAndCheckValidationResultStructure();
    }

    // 1 родитель + 1 ребенок
    // ошибки в родительском результате на уровне родителя и на уровне дочерних элементов

    @Test
    public void parentWithErrorAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putParentError(0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentError(0, ERR_1);
    }

    @Test
    public void parentWithFieldErrorAndChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
    }

    @Test
    public void parentWithChildErrorAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildErrorToParentResult(0, 0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkChildError(0, 0, ERR_1);
    }

    @Test
    public void parentWithChildFieldErrorAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildFieldErrorToParentResult(0, 0, CHILD_FIELD_1, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_1);
    }

    @Test
    public void oneParentWithParentFieldErrorAndChildFieldErrorAndValidChild() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);
        putChildFieldErrorToParentResult(0, 0, CHILD_FIELD_1, ERR_2);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_2);
        checkParentErrorsCount(0, 2);
    }

    // 1 родитель + 1 ребенок
    // ошибки в дочернем результате из плоского списка

    @Test
    public void validParentAndChildWithError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildErrorToChildResult(0, 0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkChildError(0, 0, ERR_1);
    }

    @Test
    public void parentWithEmptyChildrenResultsAndChildWithError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        ValidationResult<?, Defect> parentResult =
                parentsResult.getOrCreateSubValidationResult(index(0), parents.get(0));
        parentResult.getOrCreateSubValidationResult(field(PROPERTY), singletonList(flatChildren.get(0)));

        putChildErrorToChildResult(0, 0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkChildError(0, 0, ERR_1);
    }

    @Test
    public void parentWithEmptyChildResultAndChildWithError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        ValidationResult<?, Defect> parentResult =
                parentsResult.getOrCreateSubValidationResult(index(0), parents.get(0));
        ValidationResult<?, Defect> childrenResult =
                parentResult.getOrCreateSubValidationResult(field(PROPERTY), singletonList(flatChildren.get(0)));
        childrenResult.getOrCreateSubValidationResult(index(0), flatChildren.get(0));

        putChildErrorToChildResult(0, 0, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkChildError(0, 0, ERR_1);
    }

    // 1 родитель + 1 ребенок
    // ошибки и в родительском результате на уровне дочернего элемента и в дочернем результате из плоского списка

    @Test
    public void parentWithChildErrorAndSameChildWithError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildErrorToChildResult(0, 0, ERR_1);
        putChildErrorToParentResult(0, 0, ERR_2);

        mergeAndCheckValidationResultStructure();
        checkChildError(0, 0, ERR_1);
        checkChildError(0, 0, ERR_2);
        checkParentErrorsCount(0, 2);
    }

    @Test
    public void parentWithChildFieldErrorAndChildWithSameFieldError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildFieldErrorToChildResult(0, 0, CHILD_FIELD_1, ERR_1);
        putChildFieldErrorToParentResult(0, 0, CHILD_FIELD_1, ERR_2);

        mergeAndCheckValidationResultStructure();
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_1);
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_2);
        checkParentErrorsCount(0, 2);
    }

    @Test
    public void parentWithChildFieldErrorAndChildWithOtherFieldError() {
        parentResults(1);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));

        putChildFieldErrorToChildResult(0, 0, CHILD_FIELD_1, ERR_1);
        putChildFieldErrorToParentResult(0, 0, CHILD_FIELD_2, ERR_2);

        mergeAndCheckValidationResultStructure();
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_1);
        checkChildFieldError(0, 0, CHILD_FIELD_2, ERR_2);
        checkParentErrorsCount(0, 2);
    }

    // один родитель и два дочерних элемента

    @Test
    public void validParentAndTwoValidChildren() {
        parentResults(1);
        flatChildrenResults(2);
        indexMaps(0, asList(0, 1));
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void parentWithFieldErrorAndTwoValidChildren() {
        parentResults(1);
        flatChildrenResults(2);
        indexMaps(0, asList(0, 1));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
    }

    @Test
    public void parentWithFieldErrorAndOneValidChildAndChildWithFieldError() {
        parentResults(1);
        flatChildrenResults(2);
        indexMaps(0, asList(0, 1));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);
        putChildFieldErrorToChildResult(0, 1, CHILD_FIELD_1, ERR_2);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkChildFieldError(0, 1, CHILD_FIELD_1, ERR_2);
        checkParentErrorsCount(0, 2);
    }

    @Test
    public void parentAndTwoChildrenWithErrorMix() {
        parentResults(1);
        flatChildrenResults(2);
        indexMaps(0, asList(0, 1));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);
        putChildFieldErrorToParentResult(0, 0, CHILD_FIELD_1, ERR_2);

        putChildErrorToChildResult(0, 0, ERR_3);
        putChildFieldErrorToChildResult(0, 0, CHILD_FIELD_1, ERR_4);
        putChildFieldErrorToChildResult(0, 1, CHILD_FIELD_2, ERR_5);

        mergeAndCheckValidationResultStructure();
        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkChildError(0, 0, ERR_3);
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_2);
        checkChildFieldError(0, 0, CHILD_FIELD_1, ERR_4);
        checkChildFieldError(0, 1, CHILD_FIELD_2, ERR_5);
        checkParentErrorsCount(0, 5);
    }

    // несколько родителей

    @Test
    public void validParentWithNullChildrenAndValidParentWithValidChild() {
        parentResults(2);
        flatChildrenResults(1);
        indexMaps(1, singletonList(0));
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentWithEmptyChildrenAndValidParentWithValidChild() {
        parentResults(2);
        flatChildrenResults(1);
        indexMaps(0, emptyList(), 1, singletonList(0));
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentWithValidChildAndValidParentWithNullChildren() {
        parentResults(2);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0));
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void validParentWithValidChildAndValidParentWithEmptyChildren() {
        parentResults(2);
        flatChildrenResults(1);
        indexMaps(0, singletonList(0), 1, emptyList());
        mergeAndCheckValidationResultStructure();
    }

    @Test
    public void twoValidParentsWithSomeValidChildren() {
        parentResults(2);
        flatChildrenResults(5);
        indexMaps(0, asList(0, 1, 2), 1, asList(3, 4));
        mergeAndCheckValidationResultStructure();
    }

    // несколько родителей с ошибками и в родителях и в дочерних элементах родителей и в плоских результатах детей

    @Test
    public void parentWithErrorAndParentWithoutErrorsWithValidChildren() {
        parentResults(2);
        flatChildrenResults(5);
        indexMaps(0, asList(0, 1, 2), 1, asList(3, 4));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);

        mergeAndCheckValidationResultStructure();

        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkParentErrorsCount(0, 1);
        checkParentErrorsCount(1, 0);
    }

    @Test
    public void twoParentsWithErrorsAndValidChildren() {
        parentResults(2);
        flatChildrenResults(5);
        indexMaps(0, asList(0, 1, 2), 1, asList(3, 4));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);
        putParentFieldError(1, PARENT_FIELD_2, ERR_2);

        mergeAndCheckValidationResultStructure();

        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkParentFieldError(1, PARENT_FIELD_2, ERR_2);
        checkParentErrorsCount(0, 1);
        checkParentErrorsCount(1, 1);
    }

    @Test
    public void twoParentsWithErrorsInChildrenAndChildrenWithErrors() {
        parentResults(2);
        flatChildrenResults(5);
        indexMaps(0, asList(0, 1, 2), 1, asList(3, 4));

        putParentFieldError(0, PARENT_FIELD_1, ERR_1);
        putChildFieldErrorToParentResult(0, 2, CHILD_FIELD_1, ERR_2);
        putChildFieldErrorToChildResult(0, 2, CHILD_FIELD_1, ERR_3);
        putChildErrorToChildResult(0, 2, ERR_4);

        putParentFieldError(1, PARENT_FIELD_2, ERR_5);
        putChildErrorToChildResult(1, 0, ERR_1);

        mergeAndCheckValidationResultStructure();

        checkParentFieldError(0, PARENT_FIELD_1, ERR_1);
        checkChildFieldError(0, 2, CHILD_FIELD_1, ERR_2);
        checkChildFieldError(0, 2, CHILD_FIELD_1, ERR_3);
        checkChildError(0, 2, ERR_4);

        checkParentFieldError(1, PARENT_FIELD_2, ERR_5);
        checkChildError(1, 0, ERR_1);

        checkParentErrorsCount(0, 4);
        checkParentErrorsCount(1, 2);
    }

    private void parentResults(int num) {
        parents = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            parents.add(new AdGroup());
        }
        parentsResult = new ValidationResult<>(parents);
    }

    private void flatChildrenResults(int num) {
        flatChildren = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            flatChildren.add(new Keyword());
        }
        flatChildrenResult = new ValidationResult<>(flatChildren);
    }

    private void emptyIndexMaps() {
        indexMap = MultimapBuilder.hashKeys().arrayListValues().build();
        parentIndexToChildrenMap = MultimapBuilder.hashKeys().arrayListValues().build();
    }

    private void indexMaps(int parent, List<Integer> children) {
        emptyIndexMaps();
        indexMap.putAll(parent, children);
        children.forEach(flatIndex -> parentIndexToChildrenMap.put(parent, flatChildren.get(flatIndex)));
    }

    private void indexMaps(int parent1, List<Integer> children1,
                           int parent2, List<Integer> children2) {
        indexMaps(parent1, children1);
        indexMap.putAll(parent2, children2);
        children2.forEach(flatIndex -> parentIndexToChildrenMap.put(parent2, flatChildren.get(flatIndex)));
    }

    private ValidationResult<?, Defect> getParentResult(int parent) {
        return parentsResult.getOrCreateSubValidationResult(index(parent), parents.get(parent));
    }

    private ValidationResult<?, Defect> getParentChildResult(int parent, int child) {
        List<Keyword> children = parentIndexToChildrenMap.get(parent);
        ValidationResult<?, Defect> parentResult = getParentResult(parent);
        ValidationResult<?, Defect> childrenResult =
                parentResult.getOrCreateSubValidationResult(field(PROPERTY), children);
        return childrenResult.getOrCreateSubValidationResult(index(child), children.get(child));
    }

    private ValidationResult<?, Defect> getFlatChildResult(int parent, int child) {
        List<Integer> childrenIndexes = indexMap.get(parent);
        int flatChildIndex = childrenIndexes.get(child);
        return flatChildrenResult.getOrCreateSubValidationResult(index(flatChildIndex),
                flatChildren.get(flatChildIndex));
    }

    private void putParentError(int parent, Defect error) {
        getParentResult(parent).addError(error);
    }

    private void putParentFieldError(int parent, String field, Defect error) {
        getParentResult(parent).getOrCreateSubValidationResult(field(field), 0L).addError(error);
    }

    private void putChildErrorToParentResult(int parent, int child, Defect error) {
        getParentChildResult(parent, child).addError(error);
    }

    private void putChildFieldErrorToParentResult(int parent, int child, String field, Defect error) {
        ValidationResult<?, Defect> childResult = getParentChildResult(parent, child);
        ValidationResult<?, Defect> fieldResult =
                childResult.getOrCreateSubValidationResult(field(field), 0L);
        fieldResult.addError(error);
    }

    private void putChildErrorToChildResult(int parent, int child, Defect error) {
        getFlatChildResult(parent, child).addError(error);
    }

    private void putChildFieldErrorToChildResult(int parent, int child, String field, Defect error) {
        ValidationResult<?, Defect> childResult = getFlatChildResult(parent, child);
        ValidationResult<?, Defect> fieldResult =
                childResult.getOrCreateSubValidationResult(field(field), 0L);
        fieldResult.addError(error);
    }

    private void mergeAndCheckValidationResultStructure() {
        mergeChildrenSubListValidationResults(parentsResult, flatChildrenResult, indexMap, parentIndexToChildrenMap,
                PROPERTY);
        checkValidationResultStructure();
    }

    @SuppressWarnings("unchecked")
    private void checkValidationResultStructure() {
        Set<Integer> childrenFlatIndexes = ImmutableSet.copyOf(intRange(0, flatChildren.size()));
        Set<Integer> childrenFlatIndexesInIndexMap = new HashSet<>();
        indexMap.asMap().values().forEach(childrenFlatIndexesInIndexMap::addAll);
        checkState(childrenFlatIndexes.equals(childrenFlatIndexesInIndexMap),
                "Тест невалиден: в индексной мапе присутствуют не все индексы детей в плоском списке");

        indexMap.asMap().forEach((parentIndex, flatChildrenIndexes) -> {
            List<Integer> flatChildrenIndexesList = (List) flatChildrenIndexes;

            if (flatChildrenIndexesList.isEmpty()) {
                return;
            }

            ValidationResult<?, Defect> parentResult =
                    parentsResult.getSubResults().get(index(parentIndex));

            assertThat(String.format("Результат валидации родительского объекта %s должен содержать "
                            + "в качестве значения соответствующего родителя", parentIndex),
                    parentResult.getValue(), sameInstance(parents.get(parentIndex)));


            ValidationResult<?, Defect> childrenResult =
                    parentResult.getSubResults().get(field(PROPERTY));

            if (childrenResult == null) {
                return;
            }

            assertThat("В родительском результате валидации саб-результат для "
                            + "списка дочерних объектов должен содержать в качестве значения "
                            + "список этих дочерних объектов",
                    childrenResult.getValue(), equalTo(parentIndexToChildrenMap.asMap().get(parentIndex)));

            for (int childIndex = 0; childIndex < flatChildrenIndexes.size(); childIndex++) {
                int flatChildIndex = flatChildrenIndexesList.get(childIndex);

                ValidationResult<?, Defect> childResult =
                        childrenResult.getSubResults().get(index(childIndex));

                if (childResult == null) {
                    continue;
                }

                String err = String.format("В родительском результате валидации %s "
                                + "в списке дочерних объектов под индексом %s "
                                + "ожидается дочерний объект из плоского списка под индексом %s",
                        parentIndex, childIndex, flatChildIndex);
                assertThat(err, childResult.getValue(), sameInstance(flatChildren.get(flatChildIndex)));
            }
        });
    }

    private void checkParentError(int parent, Defect error) {
        ValidationResult<?, Defect> parentResult = getParentValidationResultWithCheck(parent);
        assertThat(String.format("родитель %s не содержит искомой ошибки", parent),
                parentResult.getErrors().stream().anyMatch(err -> err == error), is(true));
    }

    private void checkParentFieldError(int parent, String field, Defect error) {
        ValidationResult<?, Defect> parentFieldResult =
                getParentFieldValidationResultWithCheck(parent, field);
        assertThat(String.format("родитель %s не содержит искомой ошибки в поле %s", parent, field),
                parentFieldResult.getErrors().stream().anyMatch(err -> err == error), is(true));
    }

    private void checkChildError(int parent, int child, Defect error) {
        ValidationResult<?, Defect> childResult = getChildValidationResultWithCheck(parent, child);
        assertThat(String.format("ребенок %s родителя %s не содержит искомой ошибки", child, parent),
                childResult.getErrors().stream().anyMatch(err -> err == error), is(true));
    }

    private void checkChildFieldError(int parent, int child, String field, Defect error) {
        ValidationResult<?, Defect> childFieldResult =
                getChildFieldValidationResultWithCheck(parent, child, field);
        assertThat(String.format("ребенок %s родителя %s не содержит искомой ошибки в поле %s", child, parent, field),
                childFieldResult.getErrors().stream().anyMatch(err -> err == error), is(true));
    }

    private ValidationResult<?, Defect> getParentValidationResultWithCheck(int parent) {
        ValidationResult<?, Defect> parentResult =
                parentsResult.getSubResults().get(index(parent));
        assertThat(String.format("родитель %s не содержит искомой ошибки, "
                        + "так как отсутствует результат валидации данного родителя", parent),
                parentResult, notNullValue());
        return parentResult;
    }

    private ValidationResult<?, Defect> getParentFieldValidationResultWithCheck(int parent, String field) {
        ValidationResult<?, Defect> parentResult = getParentValidationResultWithCheck(parent);

        ValidationResult<?, Defect> parentFieldResult =
                parentResult.getSubResults().get(field(field));
        assertThat(String.format("родитель %s не содержит искомой ошибки, "
                        + "так как отсутствует результат валидации соответствующего поля родителя", parent),
                parentFieldResult, notNullValue());

        return parentFieldResult;
    }

    private ValidationResult<?, Defect> getChildValidationResultWithCheck(int parent, int child) {
        ValidationResult<?, Defect> parentResult = getParentValidationResultWithCheck(parent);

        ValidationResult<?, Defect> childrenResult =
                parentResult.getSubResults().get(field(PROPERTY));
        assertThat(String.format("ребенок %s родителя %s не содержит искомой ошибки, "
                        + "так как отсутствует результат валидации списка детей", child, parent),
                childrenResult, notNullValue());

        ValidationResult<?, Defect> childResult =
                childrenResult.getSubResults().get(index(child));
        assertThat(String.format("ребенок %s родителя %s не содержит искомой ошибки, "
                        + "так как отсутствует результат валидации данного ребенка", child, parent),
                childResult, notNullValue());

        return childResult;
    }

    private ValidationResult<?, Defect> getChildFieldValidationResultWithCheck(
            int parent, int child, String field) {
        ValidationResult<?, Defect> childResult = getChildValidationResultWithCheck(parent, child);

        ValidationResult<?, Defect> childFieldResult =
                childResult.getSubResults().get(field(field));
        assertThat(String.format("ребенок %s родителя %s не содержит искомой ошибки, "
                        + "так как отсутствует результат валидации соответствующего поля ребенка", child, parent),
                childFieldResult, notNullValue());

        return childFieldResult;
    }

    private void checkParentErrorsCount(int parent, int expectedSize) {
        ValidationResult<?, Defect> parentResult = getParentValidationResultWithCheck(parent);
        assertThat(String.format("Ожидаемое количество ошибок у родителя %s не соответствует ожидаемому", parent),
                parentResult.flattenErrors(), hasSize(expectedSize));
    }
}
