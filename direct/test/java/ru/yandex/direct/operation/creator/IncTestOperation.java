package ru.yandex.direct.operation.creator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import one.util.streamex.EntryStream;

import ru.yandex.direct.operation.OperationsUtils;
import ru.yandex.direct.operation.PartiallyApplicableOperation;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.builder.Constraint;
import ru.yandex.direct.validation.builder.ListValidationBuilder;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.ValidationResult.getValidItemsWithIndex;

// Тестовая инкрементирующая число операция с валидацией
class IncTestOperation implements PartiallyApplicableOperation<Integer> {
    static final Integer INVALID_VALUE = null;

    private final List<Integer> inputList;
    private ValidationResult<List<Integer>, Defect> validationResult;
    private Map<Integer, Integer> validModelsMap;
    private MassResult<Integer> result;

    IncTestOperation(List<Integer> inputList) {
        this.inputList = inputList;
        this.validModelsMap = emptyMap();
    }

    @Override
    public Optional<MassResult<Integer>> prepare() {
        ListValidationBuilder<Integer, Defect> vb = ListValidationBuilder.of(inputList, Defect.class)
                .checkEach(
                        Constraint.fromPredicateOfNullable(Objects::nonNull, new Defect<>(DefectIds.CANNOT_BE_NULL)));
        validationResult = vb.getResult();
        validModelsMap = getValidItemsWithIndex(validationResult);
        return Optional.empty();
    }

    @Override
    public Set<Integer> getValidElementIndexes() {
        return validModelsMap.keySet();
    }

    @Override
    public MassResult<Integer> apply(Set<Integer> elementIndexesToApply) {
        Map<Integer, Integer> validModelsMapToApply = EntryStream.of(validModelsMap)
                .filterKeys(elementIndexesToApply::contains)
                .toMap();
        Map<Integer, Integer> addedResults = OperationsUtils.applyForMapValues(validModelsMapToApply,
                listOfInteger -> mapList(listOfInteger, v -> v + 1));

        Set<Integer> canceledElements = Sets.difference(validModelsMap.keySet(), validModelsMapToApply.keySet());
        return createMassResult(addedResults, validationResult, canceledElements);
    }

    @Override
    public MassResult<Integer> apply() {
        Set<Integer> allIndexesSet = ContiguousSet.create(
                Range.closedOpen(0, inputList.size()), DiscreteDomain.integers());
        return apply(allIndexesSet);
    }

    private MassResult<Integer> createMassResult(Map<Integer, Integer> resultMap,
                                                 ValidationResult<List<Integer>, Defect> validationResult,
                                                 Set<Integer> canceledElementIndexes) {
        List<Integer> results = new ArrayList<>();
        for (int i = 0; i < inputList.size(); i++) {
            results.add(resultMap.get(i));
        }
        return MassResult.successfulMassAction(results, validationResult, canceledElementIndexes);
    }

    @Override
    public MassResult<Integer> cancel() {
        List<Integer> results = Collections.nCopies(inputList.size(), null);
        // все валидные элементы помечаем отменёнными, т.к. для них не выполнялась операция
        result = MassResult.successfulMassAction(results, validationResult, validModelsMap.keySet());
        return result;
    }

    @Override
    public Optional<MassResult<Integer>> getResult() {
        return Optional.ofNullable(result);
    }
}
