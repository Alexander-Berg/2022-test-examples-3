package ru.yandex.market.markup2.tasks.fill_param_values;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 16.06.2017
 */
@RunWith(MockitoJUnitRunner.class)
public class FillParamValuesResultCollectorTest extends FillParamValuesResultCollectorTestBase
    <FillParamValuesIdentity, FillParamValuesDataItemPayload, FillParamValuesResponse> {


    @Override
    protected AbstractParamValuesResultCollector<FillParamValuesIdentity,
        FillParamValuesDataItemPayload, FillParamValuesResponse> createResultCollector() {

        return new FillParamValuesResultCollector();
    }

    @Override
    protected FillParamValuesResponse createResponse(int number) {
        return new FillParamValuesResponse(number, generateCharacteristics(), new ArrayList<>());
    }

    @Override
    @Test
    public void modelsFromResponseSaved() {
        super.modelsFromResponseSaved();
    }

    @Override
    @Test
    public void replaceOnlyTolokerValues() {
        super.replaceOnlyTolokerValues();
    }

    @Override
    protected Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> generateItems(
        int count) {

        Collection<TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse>> result = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            FillParamValuesDataItemPayload payload =
                new FillParamValuesDataItemPayload(models.get(i).getId(), Collections.emptySet(), null);
            FillParamValuesResponse response = createResponse(i);
            TaskDataItem<FillParamValuesDataItemPayload, FillParamValuesResponse> item = new TaskDataItem<>(i, payload);
            item.setResponseInfo(response);

            result.add(item);
        }

        return result;
    }
}
