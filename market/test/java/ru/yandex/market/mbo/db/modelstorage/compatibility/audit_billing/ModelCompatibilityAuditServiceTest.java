package ru.yandex.market.mbo.db.modelstorage.compatibility.audit_billing;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModel;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModelBuilder;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelCompatibilityAuditServiceTest {

    private static final Long UID = 1L;
    private static final long MODEL_ID_1 = 1L;
    private static final long MODEL_ID_2 = 2L;

    private static final CommonModel MODEL = CommonModelBuilder.newBuilder(MODEL_ID_1, 100L, 1000L)
            .getModel();

    private static final CommonModel OLD_MODEL = CommonModelBuilder.newBuilder(MODEL_ID_2, 100L, 1000L)
            .getModel();

    private static final CompatibilityModel OLD_COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(MODEL_ID_2, "Model title")
            .setDirection(CompatibilityModel.Direction.BOTH)
            .create();

    private static final CompatibilityModel COMPATIBILITY_MODEL = CompatibilityModelBuilder.newBuilder()
            .setModel(MODEL_ID_2, "Model title")
            .setDirection(CompatibilityModel.Direction.FORWARD)
            .create();

    private ModelStorageService modelStorageService;

    private AuditServiceMock auditService;
    private ModelCompatibilityAuditService modelCompatibilityAuditService;

    @Before
    public void setUp() throws Exception {
        auditService = new AuditServiceMock();
        modelStorageService = new ModelStorageServiceStub();
        modelCompatibilityAuditService = new ModelCompatibilityAuditService(auditService, modelStorageService);
    }

    @Test
    public void compatibilityCreate() throws Exception {
        modelCompatibilityAuditService.onModelCompatibilitiesUpdated(UID, MODEL,
                Collections.emptyList(), Collections.singletonList(COMPATIBILITY_MODEL));

        List<AuditAction> auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());
        assertAction(auditActions, AuditAction.ActionType.CREATE, MODEL_ID_1, MODEL_ID_2);
    }

    @Test
    public void compatibilityUpdate() throws Exception {
        modelStorageService.saveModel(OLD_MODEL, new ModelSaveContext(UID));

        modelCompatibilityAuditService.onModelCompatibilitiesUpdated(UID, MODEL,
                Collections.singletonList(OLD_COMPATIBILITY_MODEL), Collections.singletonList(COMPATIBILITY_MODEL));

        List<AuditAction> auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());
        assertAction(auditActions, AuditAction.ActionType.UPDATE, MODEL_ID_1, MODEL_ID_2);
    }

    @Test
    public void compatibilityDelete() throws Exception {
        modelStorageService.saveModel(OLD_MODEL, new ModelSaveContext(UID));

        modelCompatibilityAuditService.onModelCompatibilitiesUpdated(UID, MODEL,
                Collections.singletonList(OLD_COMPATIBILITY_MODEL), Collections.emptyList());

        List<AuditAction> auditActions = auditService.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());
        assertAction(auditActions, AuditAction.ActionType.DELETE, MODEL_ID_1, MODEL_ID_2);
    }

    @Test
    public void compatibilityCreateUpdateAndDelete() throws Exception {
        // configure
        CommonModel commonModel1 = CommonModelBuilder.newBuilder(1L, 11L, 111L).getModel();
        CommonModel commonModel2 = CommonModelBuilder.newBuilder(2L, 11L, 111L).getModel();
        CommonModel commonModel3 = CommonModelBuilder.newBuilder(3L, 11L, 111L).getModel();
        CompatibilityModel compatibility2 = CompatibilityModelBuilder.newBuilder()
                .setModel(2L, "Model title")
                .setDirection(CompatibilityModel.Direction.FORWARD)
                .create();
        CompatibilityModel compatibility3 = CompatibilityModelBuilder.newBuilder()
                .setModel(3L, "Model title")
                .setDirection(CompatibilityModel.Direction.BACKWARD)
                .create();
        CompatibilityModel compatibility3New = CompatibilityModelBuilder.newBuilder()
                .setModel(3L, "Model title")
                .setDirection(CompatibilityModel.Direction.FORWARD)
                .create();
        CompatibilityModel compatibility4New = CompatibilityModelBuilder.newBuilder()
                .setModel(4L, "Model title")
                .setDirection(CompatibilityModel.Direction.BOTH)
                .create();
        modelStorageService.saveModel(commonModel2, new ModelSaveContext(UID));
        modelStorageService.saveModel(commonModel3, new ModelSaveContext(UID));

        // run
        modelCompatibilityAuditService.onModelCompatibilitiesUpdated(UID, commonModel1,
                Arrays.asList(compatibility2, compatibility3), Arrays.asList(compatibility3New, compatibility4New));

        // validate
        List<AuditAction> createActions = auditService.loadAudit(0, Integer.MAX_VALUE,
                createFilter(AuditAction.ActionType.CREATE));
        assertAction(createActions, AuditAction.ActionType.CREATE, 1L, 4L);

        List<AuditAction> updateActions = auditService.loadAudit(0, Integer.MAX_VALUE,
                createFilter(AuditAction.ActionType.UPDATE));
        assertAction(updateActions, AuditAction.ActionType.UPDATE, 1L, 3L);

        List<AuditAction> deleteActions = auditService.loadAudit(0, Integer.MAX_VALUE,
                createFilter(AuditAction.ActionType.DELETE));
        assertAction(deleteActions, AuditAction.ActionType.DELETE, 1L, 2L);
    }

    private void assertAction(List<AuditAction> actions, AuditAction.ActionType actionType,
                              long modelId1, long modelId2) {
        assertEquals(2, actions.size());
        AuditAction auditAction1 = actions.get(0);
        AuditAction auditAction2 = actions.get(1);

        assertEquals(actionType, auditAction1.getActionType());
        assertEquals(actionType, auditAction2.getActionType());

        boolean firstMatch = auditAction1.getEntityId() == modelId1 && auditAction2.getEntityId() == modelId2;
        boolean secondMatch = auditAction2.getEntityId() == modelId1 && auditAction1.getEntityId() == modelId2;

        if (!firstMatch && !secondMatch) {
            throw new AssertionError(String.format("Model ids don't match with action entities ids!\n" +
                    "Action1: %s, action2: %s, modelID1: %d, modelId2: %d",
                    auditAction1, auditAction2, modelId1, modelId2));
        }
    }

    private AuditFilter createFilter(AuditAction.ActionType actionType) {
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.setActionType(actionType);
        return auditFilter;
    }
}
