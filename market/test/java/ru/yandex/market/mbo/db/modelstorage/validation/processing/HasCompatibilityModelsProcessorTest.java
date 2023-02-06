package ru.yandex.market.mbo.db.modelstorage.validation.processing;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.compatibility.MboModelCompatibilities.DeleteCompatibilityRequest;
import ru.yandex.market.mbo.compatibility.MboModelCompatibilities.DeleteCompatibilityResponse;
import ru.yandex.market.mbo.compatibility.MboModelCompatibilities.DeleteCompatibilityStatus;
import ru.yandex.market.mbo.compatibility.ModelCompatibilityService;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationContextStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError.ErrorType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import io.qameta.allure.Issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 20.12.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class HasCompatibilityModelsProcessorTest {

    @Mock
    private ModelCompatibilityService modelCompatibilityService;

    private HasCompatibilityModelsProcessor processor;
    private static final long MODEL_ID = 11264102;
    private static final int COUNT = 1138631;

    @Before
    public void before() {
        processor = new HasCompatibilityModelsProcessor(modelCompatibilityService);
    }

    @Test
    @Issue("MBO-17714")
    public void deleteCompatibilities() {
        ModelValidationError error = new ModelValidationError(MODEL_ID,
            ErrorType.MODEL_HAS_COMPATIBILITIES, true, true);
        CommonModel model = CommonModelBuilder.newBuilder()
            .id(MODEL_ID)
            .setDeleted(true)
            .getModel();
        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model);
        when(modelCompatibilityService.deleteCompatibility(any())).thenReturn(DeleteCompatibilityResponse.newBuilder()
            .setStatus(DeleteCompatibilityStatus.newBuilder()
                .setCount(COUNT)
                .setModelId(MODEL_ID).build())
            .build());


        ValidationErrorProcessorResult result = processor.doProcess(new ModelValidationContextStub(null),
            saveGroup, error);


        assertThat(result.getError()).isSameAs(error);
        assertThat(result.getUpdatedModels()).isEmpty();

        ArgumentCaptor<DeleteCompatibilityRequest> requestArgument = ArgumentCaptor.forClass(
            DeleteCompatibilityRequest.class);

        verify(modelCompatibilityService).deleteCompatibility(requestArgument.capture());
        assertThat(requestArgument.getValue().getModelId()).isEqualTo(MODEL_ID);
    }
}
