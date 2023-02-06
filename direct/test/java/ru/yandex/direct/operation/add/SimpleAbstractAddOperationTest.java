package ru.yandex.direct.operation.add;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.testing.entity.AdGroup;
import ru.yandex.direct.result.MassResult;

import static com.google.common.base.Preconditions.checkState;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

public class SimpleAbstractAddOperationTest extends BaseAbstractAddOperationTest {
// вызов метода execute(List) со списком валидных моделей

    @Test
    public void apply_OneValidItem_CallsExecuteWithValidObject() {
        oneValidObject();
        List<AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, contains(sameInstance(adGroup1)));
    }

    @Test
    public void apply_OneItemWithWarning_CallsExecuteWithValidObject() {
        oneObjectWithWarning();
        List<AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, contains(sameInstance(adGroup1)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void apply_TwoValidItems_CallsExecuteWithValidObjects() {
        twoValidObjects();
        List<AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, contains(sameInstance(adGroup1), sameInstance(adGroup2)));
    }

    @Test
    public void apply_OneValidAndOneInvalidItem_CallsExecuteWithValidObjects() {
        oneValidObjectAndOneWithError();
        List<AdGroup> validModels = callApplyAndExtractModelsPassedToExecute();
        assertThat(validModels, contains(sameInstance(adGroup1)));
    }

    private List<AdGroup> callApplyAndExtractModelsPassedToExecute() {
        TestableSimpleAddOperation<AdGroup> operation = createSimpleOperation(Applicability.PARTIAL);

        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        ArgumentCaptor<List<AdGroup>> argumentCaptor = ArgumentCaptor.forClass(List.class);
        operation.apply();
        verify(operation).execute(argumentCaptor.capture());

        return argumentCaptor.getValue();
    }

    // падает, если execute возвращает список id неправильного размера

    @Test(expected = IllegalStateException.class)
    public void apply_MethodExecuteReturnsListOfIdsWithInvalidSize_ThrowsException() {
        twoValidObjects();

        TestableSimpleAddOperation<AdGroup> operation = createSimpleOperation(Applicability.PARTIAL);
        Optional<MassResult<Long>> optionalResult = operation.prepare();
        checkState(!optionalResult.isPresent(), "в данном случае метод prepare() не должен возвращать результат");

        idsToReturn.clear();
        idsToReturn.add(AD_GROUP_1_ID);
        operation.apply();
    }
}
