package ru.yandex.market.mbo.db.modelstorage.data.group;

import org.junit.Before;
import ru.yandex.market.mbo.db.modelstorage.StatsModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class BaseModelSaveGroupTest {
    private static final long USER_ID = 100500;
    private static final long CATEGORY_ID = 1;
    private static final long VENDOR_ID = 100;

    protected StatsModelStorageServiceStub storageService;

    protected CommonModel createdModel1;
    protected CommonModel updatedModel1;
    protected CommonModel deletedModel1;
    protected CommonModel createdModel2;
    protected CommonModel updatedModel2;
    protected CommonModel deletedModel2;

    @Before
    public void setUp() throws Exception {
        storageService = new StatsModelStorageServiceStub();

        createdModel1 = createModel();
        updatedModel1 = createAndSaveModel();
        deletedModel1 = createAndSaveModel();
        deletedModel1.setDeleted(true);

        createdModel2 = createModel();
        updatedModel2 = createAndSaveModel();
        deletedModel2 = createAndSaveModel();
        deletedModel2.setDeleted(true);
    }

    protected CommonModel createModel() {
        return CommonModelBuilder.newBuilder(0, CATEGORY_ID, VENDOR_ID).getModel();
    }

    protected CommonModel createAndSaveModel() {
        CommonModel model = CommonModelBuilder.newBuilder(0, CATEGORY_ID, VENDOR_ID).getModel();

        GroupOperationStatus status = storageService.saveModel(model, USER_ID);
        model.setId(status.getSingleModelStatus().getModelId());

        return model;
    }

    protected static List<Long> getRequestedModelIds(ModelSaveGroup group) {
        GroupOperationStatus groupStatus = group.generateOverallStatus();
        return groupStatus.getRequestedModelStatuses().stream()
            .map(OperationStatus::getModelId)
            .collect(Collectors.toList());
    }

    protected static List<Long> getAdditionalModelIds(ModelSaveGroup group) {
        GroupOperationStatus groupStatus = group.generateOverallStatus();
        return groupStatus.getAdditionalModelStatues().stream()
            .map(OperationStatus::getModelId)
            .collect(Collectors.toList());
    }

    protected static List<OperationType> getRequestedOperationTypes(ModelSaveGroup group) {
        GroupOperationStatus groupStatus = group.generateOverallStatus();
        return groupStatus.getRequestedModelStatuses().stream()
            .map(OperationStatus::getType)
            .collect(Collectors.toList());
    }

    protected static List<OperationType> getAdditionalOperationTypes(ModelSaveGroup group) {
        GroupOperationStatus groupStatus = group.generateOverallStatus();
        return groupStatus.getAdditionalModelStatues().stream()
            .map(OperationStatus::getType)
            .collect(Collectors.toList());
    }
}
