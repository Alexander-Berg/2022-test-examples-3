package ru.yandex.market.markup2.tasks.fill_param_values.yang;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.tasks.fill_param_values.AbstractParamValuesResultCollector;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesModelIdentity;
import ru.yandex.market.markup2.tasks.fill_param_values.FillParamValuesResultCollectorTestBase;
import ru.yandex.market.markup2.utils.email.EmailService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 16.06.2017
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class FillParamValuesYangResultCollectorTest extends FillParamValuesResultCollectorTestBase
    <FillParamValuesModelIdentity, FillParamValuesYangDataItemPayload, FillParamValuesYangResponse> {

    @Captor
    private ArgumentCaptor<String> emailTitleCaptor;

    @Captor
    private ArgumentCaptor<String> emailTextCaptor;

    @Override
    protected AbstractParamValuesResultCollector<FillParamValuesModelIdentity, FillParamValuesYangDataItemPayload,
        FillParamValuesYangResponse> createResultCollector() {
        return new FillParamValuesYangResultCollector();
    }

    @Override
    protected FillParamValuesYangResponse createResponse(int number) {
        Map<String, Object> fillCharacteristics;
        Map<String, Object> checkCharacteristics;
        switch (number % 4) {
            case 0:
                fillCharacteristics = generateCharacteristics().get(0);
                checkCharacteristics = generateCharacteristics().get(0);
                break;
            case 1:
                fillCharacteristics = generateCharacteristics().get(0);
                checkCharacteristics = fillCharacteristics;
                break;
            case 2:
                fillCharacteristics = generateCharacteristics().get(0);
                checkCharacteristics = generateCharacteristics().get(0);
                fillCharacteristics.put(ParamUtils.DONE_PARAM, ParamUtils.CANNOT_STATUS);
                break;
            default:
                fillCharacteristics = generateCharacteristics().get(0);
                checkCharacteristics = generateCharacteristics().get(0);
                checkCharacteristics.put(ParamUtils.DONE_PARAM, ParamUtils.CANNOT_STATUS);
        }

        return new FillParamValuesYangResponse(
            number,
            new FillParamValuesYangResult(fillCharacteristics, String.valueOf(number)),
            new FillParamValuesYangResult(checkCharacteristics, "Checker" + number));
    }

    protected Set<Long> getExpectedSavedModelIds(long firstModelId,
        Collection<TaskDataItem<FillParamValuesYangDataItemPayload, FillParamValuesYangResponse>> items) {
        return items.stream()
            .map(TaskDataItem::getInputData)
            .map(payload -> payload.getDataIdentifier().getModelId())
            .filter(id -> (id + 1 - firstModelId) % 4 != 0)
            .collect(Collectors.toSet());
    }

    protected ModelStorage.ModificationSource getExpectedModificationSource() {
        return ModelStorage.ModificationSource.ASSESSOR;
    }

    @Override
    @Test
    public void modelsFromResponseSaved() {
        super.modelsFromResponseSaved();

        // Yang report is sent.
        verify(emailService).sendSafe(anyString(), anyString(), eq(EmailService.YANG_REPORTS));
    }

    @Override
    @Test
    public void replaceOnlyTolokerValues() {
        super.replaceOnlyTolokerValues();
    }

    @Override
    protected Collection<TaskDataItem<FillParamValuesYangDataItemPayload, FillParamValuesYangResponse>> generateItems(
        int count) {

        Collection<TaskDataItem<FillParamValuesYangDataItemPayload, FillParamValuesYangResponse>> result =
            new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            FillParamValuesYangDataItemPayload payload =
                new FillParamValuesYangDataItemPayload(models.get(i).getId(), null);
            FillParamValuesYangResponse response = createResponse(i);
            TaskDataItem<FillParamValuesYangDataItemPayload, FillParamValuesYangResponse> item =
                new TaskDataItem<>(i, payload);
            item.setResponseInfo(response);

            result.add(item);
        }

        return result;
    }
}
