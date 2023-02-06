package ru.yandex.direct.operation.add;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

public class AbstractAddOperationExecutionTest extends BaseAbstractAddOperationTest {
    // вызов метода execute(Map) со списком валидных моделей

    @Test
    public void apply_OneValidItem_CallsExecuteWithValidObject() {
        oneValidObject();
        Map<Integer, AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, hasEntry(0, adGroup1));
    }

    @Test
    public void apply_OneItemWithWarning_CallsExecuteWithValidObject() {
        oneObjectWithWarning();
        Map<Integer, AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, hasEntry(0, adGroup1));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_TwoValidItems_CallsExecuteWithValidObjects() {
        twoValidObjects();
        Map<Integer, AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, hasEntry(0, adGroup1));
        assertThat(validModels, hasEntry(1, adGroup2));
    }

    @Test
    public void apply_OneValidAndOneInvalidItem_CallsExecuteWithValidObjects() {
        oneValidObjectAndOneWithError();
        Map<Integer, AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, hasEntry(0, adGroup1));
    }

    @Test
    public void apply_FirstInvalidAndSecondValidItem_CallsExecuteWithValidObjects() {
        firstWithErrorSecondValid();
        Map<Integer, AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, hasEntry(1, adGroup2));
    }

    private Map<Integer, AdGroup> callApplyAndExtractModelsPassedToExecute() {
        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        ArgumentCaptor<Map<Integer, AdGroup>> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        operation.apply();
        verify(operation).execute(argumentCaptor.capture());

        return argumentCaptor.getValue();
    }

    // возвращение результата со списком id

    @Test
    public void apply_PartialYes_OneValidItem_ReturnsValidResult() {
        oneValidObject();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> massResult = operation.apply();
        assertThat("результат должен состоять из одного элемента", massResult.getResult(), hasSize(1));
        assertThat("результат элемента должен быть положительный",
                massResult.getResult().get(0).isSuccessful(), is(true));
        assertThat("id элемента не соответствует ожидаемому",
                massResult.getResult().get(0).getResult(), equalTo(AD_GROUP_1_ID));
    }

    @Test
    public void apply_PartialYes_TwoValidItems_ReturnsValidResult() {
        twoValidObjects();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> massResult = operation.apply();
        assertThat("результат должен состоять из двух элементов", massResult.getResult(), hasSize(2));

        assertThat("результат 1 элемента должен быть положительный",
                massResult.getResult().get(0).isSuccessful(), is(true));
        assertThat("id 1 элемента не соответствует ожидаемому",
                massResult.getResult().get(0).getResult(), equalTo(AD_GROUP_1_ID));

        assertThat("результат 2 элемента должен быть положительный",
                massResult.getResult().get(1).isSuccessful(), is(true));
        assertThat("id 2 элемента не соответствует ожидаемому",
                massResult.getResult().get(1).getResult(), equalTo(AD_GROUP_2_ID));
    }

    @Test
    public void apply_PartialYes_OneValidItemAndOneWithError_ReturnsValidResult() {
        oneValidObjectAndOneWithError();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        MassResult<Long> massResult = operation.apply();
        assertThat("результат должен состоять из двух элементов", massResult.getResult(), hasSize(2));

        assertThat("результат 1 элемента должен быть положительный",
                massResult.getResult().get(0).isSuccessful(), is(true));
        assertThat("id 1 элемента не соответствует ожидаемому",
                massResult.getResult().get(0).getResult(), equalTo(AD_GROUP_1_ID));

        assertThat("результат 2 элемента должен быть отрицательный",
                massResult.getResult().get(1).isSuccessful(), is(false));
    }

    // вызывает метод onExecuted()

    @Test
    public void apply_PartialYes_OneValidItem_CallsMethodOnExecuted() {
        oneValidObject();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        operation.apply();

        verify(operation).onExecuted(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_PartialYes_OneValidItemAndOneWithError_CallsMethodOnExecutedWithValidItemOnly() {
        oneValidObjectAndOneWithError();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        operation.apply();

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(operation).onExecuted(argumentCaptor.capture());

        Map<Integer, AdGroup> actualValidItemsMap = (Map<Integer, AdGroup>) argumentCaptor.getValue();
        assertThat(actualValidItemsMap.keySet(), hasSize(1));
        assertThat(actualValidItemsMap.get(0), sameInstance(adGroup1));
    }

    // падает, если execute возвращает map неправильного размера

    @Test(expected = IllegalStateException.class)
    public void apply_MethodExecuteReturnsMapWithInvalidSize_ThrowsException() {
        twoValidObjects();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        mappingToReturn.clear();
        mappingToReturn.put(1, AD_GROUP_2_ID);
        operation.apply();
    }

    // падает, если execute возвращает map c неверным keySet

    @Test(expected = IllegalStateException.class)
    public void apply_MethodExecuteReturnsMapWithInvalidKeySet_ThrowsException() {
        twoValidObjects();

        TestableAddOperation<AdGroup> operation = createOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(),
                "в данном случае метод prepare() не должен возвращать результат");

        mappingToReturn.clear();
        mappingToReturn.put(1, AD_GROUP_1_ID);
        mappingToReturn.put(2, AD_GROUP_2_ID);

        operation.apply();
    }
}
