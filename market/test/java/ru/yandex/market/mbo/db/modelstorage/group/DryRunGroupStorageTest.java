package ru.yandex.market.mbo.db.modelstorage.group;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.TovarTreeDao;
import ru.yandex.market.mbo.db.TovarTreeProtoServiceMock;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.group.engine.BaseGroupStorageUpdatesTest;
import ru.yandex.market.mbo.db.modelstorage.validation.CategoryIdValidator;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ExporterModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.context.ModelValidationContext;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
public class DryRunGroupStorageTest extends BaseGroupStorageUpdatesTest {

    @Mock
    TovarTreeDao tovarTreeDao;

    @Override
    protected ModelValidationService createModelValidationService() {
        ModelValidationService baseService = super.createModelValidationService(
            Collections.singletonList(new CategoryIdValidator())
        );
        return baseService;
    }

    @Test
    public void testValidationFailed() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, CommonModel.NO_ID, 1)
            .getModel();

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.VALIDATION_ERROR);
    }

    @Test
    public void testValidationSucceeded() {
        CommonModel model1 = CommonModelBuilder.newBuilder(CommonModel.NO_ID, 1, 1)
            .getModel();

        ModelSaveGroup saveGroup = ModelSaveGroup.fromModels(model1);
        GroupOperationStatus groupOperationStatus = storage.saveModels(saveGroup, context);

        assertThat(groupOperationStatus.getStatus()).isEqualTo(OperationStatusType.OK);
    }

    @Override
    protected ModelValidationContext createModelValidationContext() {
        return new ExporterModelValidationContext(
            null, categoryParametersServiceClient, null, null,
            new TovarTreeProtoServiceMock(tovarTreeDao), null
        );
    }
}
