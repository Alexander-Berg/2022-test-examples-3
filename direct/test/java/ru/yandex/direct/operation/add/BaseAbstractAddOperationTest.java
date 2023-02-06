package ru.yandex.direct.operation.add;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.operation.testing.entity.TextAdGroup;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class BaseAbstractAddOperationTest {

    protected static final long AD_GROUP_1_ID = 15;
    protected static final long AD_GROUP_2_ID = 30;

    protected AdGroup adGroup1;
    protected AdGroup adGroup2;
    protected AdGroup adGroup3;
    protected List<AdGroup> adGroups = new ArrayList<>();

    protected ValidationResult<List<AdGroup>, Defect> preValidationResult = new ValidationResult<>(adGroups);
    protected ValidationResult<List<AdGroup>, Defect> validationResult = new ValidationResult<>(adGroups);

    protected List<Long> idsToReturn = new ArrayList<>();
    protected Map<Integer, Long> mappingToReturn = new HashMap<>();

    private boolean setupDone;

    protected TestableSimpleAddOperation<AdGroup> createSimpleOperation(Applicability applicability) {
        checkState(setupDone, "setup models and validation result before creating operation");
        TestableSimpleAddOperation<AdGroup> operation = new TestableSimpleAddOperation<>(applicability, adGroups);
        TestableSimpleAddOperation<AdGroup> mockedOperation = spy(operation);
        when(mockedOperation.preValidate(any())).thenReturn(preValidationResult);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ValidationResult<List<AdGroup>, Defect> internalValidationResult = invocation.getArgument(0);
                internalValidationResult.merge(validationResult);
                return null;
            }
        }).when(mockedOperation).validate(any());
        when(mockedOperation.execute(any(List.class))).thenReturn(idsToReturn);
        return mockedOperation;
    }

    protected TestableAddOperation<AdGroup> createOperation(Applicability applicability) {
        checkState(setupDone, "setup models and validation result before creating operation");
        TestableAddOperation<AdGroup> operation = new TestableAddOperation<>(applicability, adGroups);
        TestableAddOperation<AdGroup> mockedOperation = spy(operation);
        when(mockedOperation.preValidate(any())).thenReturn(preValidationResult);
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                ValidationResult<List<AdGroup>, Defect> internalValidationResult = invocation.getArgument(0);
                internalValidationResult.merge(validationResult);
                return null;
            }
        }).when(mockedOperation).validate(any());
        when(mockedOperation.execute(any(Map.class))).thenReturn(mappingToReturn);
        return mockedOperation;
    }

    protected void oneValidObject() {
        oneObject();
        addValidResultForObject(validationResult, adGroup1, 0);
        idsToReturn.add(AD_GROUP_1_ID);
        mappingToReturn.put(0, AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void oneObjectWithError() {
        oneObject();
        addResultWithErrorForObject(validationResult, adGroup1, 0);
        setupDone = true;
    }

    protected void oneObjectWithWarning() {
        oneObject();
        addResultWithWarningForObject(validationResult, adGroup1, 0);
        idsToReturn.add(AD_GROUP_1_ID);
        mappingToReturn.put(0, AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void twoValidObjects() {
        twoObjects();
        addValidResultForObject(validationResult, adGroup1, 0);
        addValidResultForObject(validationResult, adGroup2, 1);
        idsToReturn.add(AD_GROUP_1_ID);
        mappingToReturn.put(0, AD_GROUP_1_ID);
        idsToReturn.add(AD_GROUP_2_ID);
        mappingToReturn.put(1, AD_GROUP_2_ID);
        setupDone = true;
    }

    protected void oneValidObjectAndOneWithError() {
        twoObjects();
        addValidResultForObject(validationResult, adGroup1, 0);
        addResultWithErrorForObject(validationResult, adGroup2, 1);
        idsToReturn.add(AD_GROUP_1_ID);
        mappingToReturn.put(0, AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void firstWithErrorSecondValid() {
        twoObjects();
        addResultWithErrorForObject(validationResult, adGroup1, 0);
        addValidResultForObject(validationResult, adGroup2, 1);
        idsToReturn.add(AD_GROUP_2_ID);
        mappingToReturn.put(1, AD_GROUP_2_ID);
        setupDone = true;
    }

    protected void twoObjectsWithErrors() {
        twoObjects();
        addResultWithErrorForObject(validationResult, adGroup1, 0);
        addResultWithErrorForObject(validationResult, adGroup2, 1);
        setupDone = true;
    }

    protected void onePreValidObject() {
        oneObject();
        addValidResultForObject(preValidationResult, adGroup1, 0);
        idsToReturn.add(AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void oneObjectWithPreError() {
        oneObject();
        addResultWithErrorForObject(preValidationResult, adGroup1, 0);
        setupDone = true;
    }

    protected void oneObjectWithPreWarning() {
        oneObject();
        addResultWithWarningForObject(preValidationResult, adGroup1, 0);
        idsToReturn.add(AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void twoPreValidObjects() {
        twoObjects();
        addValidResultForObject(preValidationResult, adGroup1, 0);
        addValidResultForObject(preValidationResult, adGroup2, 1);
        idsToReturn.add(AD_GROUP_1_ID);
        idsToReturn.add(AD_GROUP_2_ID);
        setupDone = true;
    }

    protected void onePreValidObjectAndOneWithPreError() {
        twoObjects();
        addValidResultForObject(preValidationResult, adGroup1, 0);
        addResultWithErrorForObject(preValidationResult, adGroup2, 1);
        idsToReturn.add(AD_GROUP_1_ID);
        setupDone = true;
    }

    protected void twoObjectsWithPreErrors() {
        twoObjects();
        addResultWithErrorForObject(preValidationResult, adGroup1, 0);
        addResultWithErrorForObject(preValidationResult, adGroup2, 1);
        setupDone = true;
    }

    protected void oneObjectWithPreErrorAndOneWithErrorAndOneValid() {
        threeObjects();
        addResultWithErrorForObject(preValidationResult, adGroup1, 0);
        addResultWithErrorForObject(validationResult, adGroup2, 1);
        addValidResultForObject(preValidationResult, adGroup3, 2);
        setupDone = true;
    }

    protected static <T> Matcher<ValidationResult<T, Defect>> errorMatcher(int objectIndex) {
        return hasDefectDefinitionWith(validationError(path(index(objectIndex), field("name")), new Defect<>(DefectIds.INVALID_VALUE)));
    }

    private void oneObject() {
        adGroups.add(adGroup1 = new TextAdGroup().withName("first adGroup"));
    }

    private void twoObjects() {
        adGroups.add(adGroup1 = new TextAdGroup().withName("first adGroup"));
        adGroups.add(adGroup2 = new TextAdGroup().withName("second adGroup"));
    }

    private void threeObjects() {
        adGroups.add(adGroup1 = new TextAdGroup().withName("first adGroup"));
        adGroups.add(adGroup2 = new TextAdGroup().withName("second adGroup"));
        adGroups.add(adGroup3 = new TextAdGroup().withName("third adGroup"));
    }

    protected void addValidResultForObject(ValidationResult<List<AdGroup>, Defect> vr, AdGroup adGroup,
                                           int index) {
        vr.getOrCreateSubValidationResult(index(index), adGroup);
    }

    protected void addResultWithErrorForObject(ValidationResult<List<AdGroup>, Defect> vr, AdGroup adGroup,
                                               int index) {
        ValidationResult<AdGroup, Defect> objValidationResult =
                vr.getOrCreateSubValidationResult(index(index), adGroup);
        ValidationResult<String, Defect> subValidationResult =
                objValidationResult.getOrCreateSubValidationResult(field("name"), null);
        subValidationResult.addError(new Defect<>(DefectIds.INVALID_VALUE));
    }

    private void addResultWithWarningForObject(ValidationResult<List<AdGroup>, Defect> vr, AdGroup adGroup,
                                               int index) {
        ValidationResult<AdGroup, Defect> objValidationResult =
                vr.getOrCreateSubValidationResult(index(index), adGroup);
        ValidationResult<AdGroup, Defect> subValidationResult =
                objValidationResult.getOrCreateSubValidationResult(field("name"), null);
        subValidationResult.addWarning(new Defect<>(DefectIds.INVALID_VALUE));
    }

    protected void executeReturns(Map<Integer, Long> mappingToReturn) {
        this.mappingToReturn = new HashMap<>(mappingToReturn);
    }
}
