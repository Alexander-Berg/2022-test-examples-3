package ru.yandex.market.mbo.db.modelstorage.audit;

import junitparams.converters.Nullable;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author anmalysh
 *
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelAuditServiceTest {

    private static final Date DATE = new Date();
    private static final Date NEW_DATE = new Date(DATE.getTime() + 100000);
    private static final Date DELETED_DATE = new Date(NEW_DATE.getTime() + 100000);
    private static final long OWNER_ID_1 = 1;
    private static final long OWNER_ID_2 = 2;
    private static final long OWNER_ID_3 = 3;

    private AuditServiceMock auditServiceMock;
    private ModelAuditServiceImpl service;
    private ModelAuditContext defaultContext = new DefaultModelAuditContext();
    private CommonModelBuilder<CommonModel> modelBuilder;

    @Before
    public void before() {
        modelBuilder = ParametersBuilder
            .startParameters(p -> CommonModelBuilder.builder(Function.identity()).parameters(p))
            .startParameter()
            .id(1L).xsl("param1").type(Param.Type.ENUM)
            .option(1L, "value1")
            .option(2L, "value2")
            .option(3L, "value3")
            .endParameter()
            .startParameter()
            .id(2L).xsl("param2").type(Param.Type.ENUM)
            .option(1L, "value1")
            .option(2L, "value2")
            .option(3L, "value3")
            .endParameter()
            .startParameter()
            .id(3L).xsl("name").type(Param.Type.STRING)
            .endParameter()
            .startParameter()
            .id(KnownIds.VENDOR_PARAM_ID).xsl("vendor").type(Param.Type.ENUM)
            .option(1L, "vendor1")
            .option(2L, "vendor2")
            .option(3L, "vendor3")
            .endParameter()
            .startParameter()
            .id(5L).xsl("strParam").type(Param.Type.STRING)
            .endParameter()
            .startParameter()
            .id(7L).xsl("Numeric").type(Param.Type.NUMERIC)
            .endParameter()
            .endParameters();

        auditServiceMock = new AuditServiceMock(false);
        service = new ModelAuditServiceImpl(auditServiceMock);
    }

    @Test
    public void testGuruModelOperations() {
        CommonModel model = createModel1(DATE);

        List<AuditAction> result = audit(null, model);

        assertCreate(result, AuditAction.EntityType.MODEL_GURU, DATE, true);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        assertUpdate(result, AuditAction.EntityType.MODEL_GURU, DATE, NEW_DATE, true);

        CommonModel deletedModel = createModel2(DATE, NEW_DATE);
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);

        assertDelete(result, AuditAction.EntityType.MODEL_GURU, NEW_DATE, DELETED_DATE, true);
    }

    @Test
    public void testSKUOperations() {
        CommonModel model = createModel1(DATE);
        model.setCurrentType(CommonModel.Source.SKU);

        List<AuditAction> result = audit(null, model);

        assertCreate(result, AuditAction.EntityType.MODEL_SKU, DATE, true);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);
        updatedModel.setCurrentType(CommonModel.Source.SKU);

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        assertUpdate(result, AuditAction.EntityType.MODEL_SKU, DATE, NEW_DATE, true);

        CommonModel deletedModel = createModel2(DATE, NEW_DATE);
        deletedModel.setCurrentType(CommonModel.Source.SKU);
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);

        assertDelete(result, AuditAction.EntityType.MODEL_SKU, NEW_DATE, DELETED_DATE, true);
    }

    @Test
    public void testHypothesisParamValueChanged() {
        CommonModel model = createBasicModel()
            .currentType(CommonModel.Source.PARTNER_SKU)
            .parameterValueHypothesis(1L, "first-xsl-name", Param.Type.ENUM, "a", "b", "c")
            .parameterValueHypothesis(2L, "second-xsl-name", Param.Type.ENUM, "1", "2", "3")
            .endModel();

        List<AuditAction> result = audit(null, model);

        Assertions.assertThat(result)
            .filteredOn(auditAction -> auditAction.getEntityType() == AuditAction.EntityType.PARAM_HYPOTHESIS)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, null, "a"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, null, "b"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, null, "c"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, null, "1"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, null, "2"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, null, "3")
            );

        CommonModel updatedModel = createBasicModel()
            .currentType(CommonModel.Source.PARTNER_SKU)
            .parameterValueHypothesis(1L, "first-xsl-name", Param.Type.ENUM, "a", "d")
            .parameterValueHypothesis(2L, "second-xsl-name", Param.Type.ENUM, "0", "3")
            .endModel();

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        Assertions.assertThat(result)
            .filteredOn(auditAction -> auditAction.getEntityType() == AuditAction.EntityType.PARAM_HYPOTHESIS)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "b", null),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "c", null),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, null, "d"),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, "1", null),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, "2", null),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.CREATE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, null, "0")
            );


        CommonModel deletedModel = createBasicModel()
            .currentType(CommonModel.Source.PARTNER_SKU)
            .parameterValueHypothesis(1L, "first-xsl-name", Param.Type.ENUM, "a")
            .parameterValueHypothesis(2L, "second-xsl-name", Param.Type.ENUM, "0")
            .endModel();
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);
        Assertions.assertThat(result)
            .filteredOn(auditAction -> auditAction.getEntityType() == AuditAction.EntityType.PARAM_HYPOTHESIS)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "first-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "d", null),
                createAuditAction(AuditAction.EntityType.PARAM_HYPOTHESIS, AuditAction.ActionType.DELETE,
                    "second-xsl-name", AuditAction.BillingMode.BILLING_MODE_NONE, 2L, "3", null)
            );
    }

    @Test
    public void testNotBilledOperations() {
        defaultContext.setBilledOperation(false);
        CommonModel model = createModel1(DATE);

        List<AuditAction> result = audit(null, model);

        assertCreate(result, AuditAction.EntityType.MODEL_GURU, DATE, false);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        assertUpdate(result, AuditAction.EntityType.MODEL_GURU, DATE, NEW_DATE, false);

        CommonModel deletedModel = createModel2(DATE, NEW_DATE);
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);

        assertDelete(result, AuditAction.EntityType.MODEL_GURU, NEW_DATE, DELETED_DATE, false);
    }

    @Test
    public void testNoChanges() {
        CommonModel model = createModel1(DATE);
        CommonModel model2 = createModel1(DATE);

        List<AuditAction> result = audit(model, model2);

        assertEquals(0, result.size());
    }

    @Test
    public void testCluster() {
        CommonModel model = createModel1(DATE);
        model.setCurrentType(CommonModel.Source.CLUSTER);

        List<AuditAction> result = audit(null, model);

        assertEquals(0, result.size());
    }

    @Test
    public void testPicturesMovedRemoved() {
        startDefaultSku()
            .picture(null, "http://picture1", ModificationSource.OPERATOR_FILLED)
            .picture(null, "http://picture2", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture3", ModificationSource.OPERATOR_FILLED)
            .picture(null, "http://picture4", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture5", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture6", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture7", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture8", ModificationSource.OPERATOR_COPIED)
            .endModel();

        CommonModel before = modelBuilder.getModel();

        startDefaultSku()
            .picture(null, "http://picture1", ModificationSource.OPERATOR_FILLED)
            .picture(null, "http://picture4", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture5", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture6", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture8", ModificationSource.OPERATOR_COPIED)
            .picture(null, "http://picture7", 500, 500, "http://picture7",
                "http://picture7", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_1)
            .endModel();

        CommonModel after = modelBuilder.getModel();

        List<AuditAction> actions = audit(before, after);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 10, 2L, 5L, 1L, "title", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.DELETE, "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL,
            null, "http://picture2", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.CREATE, "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, "http://picture4");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.DELETE, "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL,
            null, "http://picture3", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.CREATE, "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, "http://picture5");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.UPDATE, "4"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "http://picture4", "http://picture6");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.UPDATE, "5"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_MOVE,
            null, "http://picture5", "http://picture8");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.UPDATE, "6"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_MOVE,
            null, "http://picture6", "http://picture7");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.DELETE, "7"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "http://picture7", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.DELETE, "8"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "http://picture8", null);
    }

    @Test
    public void testPicturesOwnerIds() {
        startDefaultSku()
            .picture(null, "http://picture1", 500, 500, "http://picture1",
                "http://picture1", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null)
            .picture(null, "http://picture2", 500, 500, "http://picture2",
                "http://picture2", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_1)
            .picture(null, "http://picture3", 500, 500, "http://picture3",
                "http://picture3", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_1)
            .endModel();

        CommonModel before = modelBuilder.getModel();

        startDefaultSku()
            .picture(null, "http://picture1", 500, 500, "http://picture1",
                "http://picture1", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_1)
            .picture(null, "http://picture2", 500, 500, "http://picture2",
                "http://picture2", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null)
            .picture(null, "http://picture3", 500, 500, "http://picture3",
                "http://picture3", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_2)
            .endModel();

        CommonModel after = modelBuilder.getModel();

        List<AuditAction> actions = audit(before, after);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 3, 2L, 5L, 1L, "title", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_OWNER, AuditAction.ActionType.CREATE,
                "1"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, String.valueOf(OWNER_ID_1));
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_OWNER, AuditAction.ActionType.DELETE,
                "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, String.valueOf(OWNER_ID_1), null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_OWNER, AuditAction.ActionType.UPDATE,
                "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, String.valueOf(OWNER_ID_1), String.valueOf(OWNER_ID_2));
    }

    @Test
    public void testPicturesStatus() {
        startDefaultSku()
            .picture(null, "http://picture1", 500, 500, "http://picture1",
                "http://picture1", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null, Picture.PictureStatus.NEW)
            .picture(null, "http://picture2", 500, 500, "http://picture2",
                "http://picture2", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null, Picture.PictureStatus.NEW)
            .endModel();

        CommonModel before = modelBuilder.getModel();

        startDefaultSku()
            .picture(null, "http://picture1", 500, 500, "http://picture1",
                "http://picture1", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null, Picture.PictureStatus.APPROVED)
            .picture(null, "http://picture2", 500, 500, "http://picture2",
                "http://picture2", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, null, Picture.PictureStatus.DUPLICATE)
            .picture(null, "http://picture2", 500, 500, "http://picture2",
                "http://picture2", null, ModificationSource.OPERATOR_COPIED, null,
                null, null, null, OWNER_ID_3, Picture.PictureStatus.NEW)
            .endModel();

        CommonModel after = modelBuilder.getModel();

        List<AuditAction> actions = audit(before, after);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 5, 2L, 5L, 1L, "title", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_STATUS, AuditAction.ActionType.UPDATE,
                "1"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, Picture.PictureStatus.NEW.toString(), Picture.PictureStatus.APPROVED.toString());
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_STATUS, AuditAction.ActionType.UPDATE,
                "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, Picture.PictureStatus.NEW.toString(), Picture.PictureStatus.DUPLICATE.toString());
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE_STATUS, AuditAction.ActionType.CREATE,
                "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, Picture.PictureStatus.NEW.toString());

    }

    private CommonModelBuilder startDefaultSku() {
        return startDefaultModel(CommonModel.Source.SKU);
    }

    private CommonModelBuilder startDefaultGuru() {
        return startDefaultModel(CommonModel.Source.GURU);
    }

    private CommonModelBuilder startDefaultModel(CommonModel.Source type) {
        modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(type)
            .currentType(type)
            .param(3L)
            .setString("title")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modifiedUserId(5L)
            .modificationDate(DATE);
        return modelBuilder;
    }

    @Test
    public void testMultivalueEnumParams() {
        defaultContext = new DefaultModelAuditContext() {
            @Override
            public boolean isMultivalueParameter(Long categoryId, Long paramId) {
                return true;
            }
        };

        modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .param(1L)
            .setOption(1L)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(1L)
            .setOption(2L)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(3L)
            .setString("title")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .endModel();

        CommonModel model = modelBuilder.getModel();
        model.setModificationDate(DATE);

        modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .param(1L)
            .setOption(2L)
            .modificationSource(ModificationSource.OPERATOR_CONFIRMED)
            .param(1L)
            .setOption(3L)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(3L)
            .setString("title")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .endModel();

        CommonModel updatedModel = modelBuilder.getModel();
        updatedModel.setModificationDate(DATE);

        List<AuditAction> result = audit(model, updatedModel);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(result, 3, 2L, null, 1L, "title", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.DELETE, "param1"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 1L, "value1",  null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE, "param1"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_CHECK, 1L, "value2",  "value2");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.CREATE, "param1"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 1L, null,  "value3");
    }

    @Test
    public void testMultivalueStringParams() {
        defaultContext = new DefaultModelAuditContext() {
            @Override
            public boolean isMultivalueParameter(Long categoryId, Long paramId) {
                return true;
            }
        };

        modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .param(5L)
            .setString("value1", "value2")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(3L)
            .setString("title")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .endModel();

        CommonModel model = modelBuilder.getModel();
        model.setModificationDate(DATE);

        modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(CommonModel.Source.GURU)
            .currentType(CommonModel.Source.GURU)
            .param(5L)
            .setString("value2", "value3")
            .modificationSource(ModificationSource.OPERATOR_CONFIRMED)
            .param(3L)
            .setString("newTitle")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .endModel();

        CommonModel updatedModel = modelBuilder.getModel();
        updatedModel.setModificationDate(DATE);

        List<AuditAction> result = audit(model, updatedModel);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(result, 6, 2L, null, 1L, "newTitle", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.DELETE, "strParam"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 5L, "value1",  null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE, "strParam"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_CHECK, 5L, "value2",  "value2");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.CREATE, "strParam"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_CHECK, 5L, null,  "value3");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.DELETE, "name"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, "title",  null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.CREATE, "name"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 3L, null,  "newTitle");
    }

    @Test
    public void testMultidelete() {
        defaultContext = new DefaultModelAuditContext();

        CommonModel model = createBasicModel()
            .param(5L)
            .setString("value1")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(DATE)
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .param(5L)
            .setString("value1")
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(DATE)
            .modificationDate(NEW_DATE)
            .getModel();

        updatedModel.addRemovedParameterValues(5L, ModificationSource.OPERATOR_FILLED);
        updatedModel.addRemovedParameterValues(5L, ModificationSource.GENERALIZATION);

        updatedModel.removeAllParameterValues(5L);

        List<AuditAction> result = audit(model, updatedModel);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(result, 2, model.getCategoryId(), null, 1L, "title", NEW_DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE, "strParam"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 5L, "value1", null
        );
    }

    @Test
    public void testNumericParamPicker() {
        CommonModel model = createBasicModel()
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .startParameterValueLink()
            .paramId(7L)
            .num(100L)
            .pickerImage("http://picker2")
            .pickerImageSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        assertThat(result)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.CREATE,
                    "100", AuditAction.BillingMode.BILLING_MODE_COPY, 7L, null, "http://picker2")
            );
    }

    @Test
    public void testParamWithSameNameDifferentIds() {
        CommonModel model = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(1)
            .endParameterValue()
            .startParameterValue()
            .paramId(2L)
            .xslName("testParam")
            .num(2)
            .endParameterValue()
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(3)
            .endParameterValue()
            .startParameterValue()
            .paramId(2L)
            .xslName("testParam")
            .num(2)
            .endParameterValue()
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        assertThat(result)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE,
                    "testParam", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "1", "3")
            );
    }

    @Test
    public void testParamModelOperations() {
        CommonModel model = createModel1(DATE);
        model.setCurrentType(CommonModel.Source.PARTNER);

        List<AuditAction> result = audit(null, model);

        assertCreate(result, AuditAction.EntityType.MODEL_PARTNER, DATE, true);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);
        updatedModel.setCurrentType(CommonModel.Source.PARTNER);

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        assertUpdate(result, AuditAction.EntityType.MODEL_PARTNER, DATE, NEW_DATE, true);

        CommonModel deletedModel = createModel2(DATE, NEW_DATE);
        deletedModel.setCurrentType(CommonModel.Source.PARTNER);
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);

        assertDelete(result, AuditAction.EntityType.MODEL_PARTNER, NEW_DATE, DELETED_DATE, true);
    }

    @Test
    public void testPartnerSKUOperations() {
        CommonModel model = createModel1(DATE);
        model.setCurrentType(CommonModel.Source.PARTNER_SKU);

        List<AuditAction> result = audit(null, model);

        assertCreate(result, AuditAction.EntityType.MODEL_PARTNER_SKU, DATE, true);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);
        updatedModel.setCurrentType(CommonModel.Source.PARTNER_SKU);

        auditServiceMock.clearActions();
        result = audit(model, updatedModel);

        assertUpdate(result, AuditAction.EntityType.MODEL_PARTNER_SKU, DATE, NEW_DATE, true);

        CommonModel deletedModel = createModel2(DATE, NEW_DATE);
        deletedModel.setCurrentType(CommonModel.Source.PARTNER_SKU);
        deletedModel.setDeleted(true);
        deletedModel.setDeletedDate(DELETED_DATE);

        auditServiceMock.clearActions();
        result = audit(updatedModel, deletedModel);

        assertDelete(result, AuditAction.EntityType.MODEL_PARTNER_SKU, NEW_DATE, DELETED_DATE, true);
    }

    @Test
    public void testParameterValueMetadata() {
        CommonModel model = createModel1(DATE);
        model.setCurrentType(CommonModel.Source.PARTNER_SKU);

        CommonModel updatedModel = createModel2(DATE, NEW_DATE);
        auditServiceMock.clearActions();
        List<AuditAction> result = audit(model, updatedModel);
        AuditAction action = result.stream()
            .filter(it -> AuditAction.EntityType.PARAM_METADATA.equals(it.getEntityType()))
            .findFirst().get();
        assertTrue(action.hasNewValue());
        assertEquals(2L, (long) action.getParameterId());
        assertEquals(action.getBillingMode(), AuditAction.BillingMode.BILLING_MODE_FILL);
    }

    @Test
    public void testSupplierIdChanges() {
        CommonModelBuilder<CommonModel> builder = createBasicModel().currentType(CommonModel.Source.PARTNER_SKU);

        List<AuditAction> actions = audit(builder.endModel(), builder.supplierId(10L).endModel());
        Assertions.assertThat(actions)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARTNER_SKU, AuditAction.ActionType.UPDATE,
                    "ID поставщика", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, null, "10")
                    .setParameterId(null)
            );

        auditServiceMock.clearActions();
        actions = audit(builder.supplierId(10L).endModel(), builder.supplierId(11L).endModel());
        Assertions.assertThat(actions)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARTNER_SKU, AuditAction.ActionType.UPDATE,
                    "ID поставщика", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "10", "11")
                    .setParameterId(null)
            );

        auditServiceMock.clearActions();
        actions = audit(builder.supplierId(11L).endModel(), builder.supplierId(null).endModel());
        Assertions.assertThat(actions)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARTNER_SKU, AuditAction.ActionType.UPDATE,
                    "ID поставщика", AuditAction.BillingMode.BILLING_MODE_NONE, 1L, "11", null)
                    .setParameterId(null)
            );

        auditServiceMock.clearActions();
        actions = audit(builder.supplierId(12L).endModel(), builder.supplierId(0L).endModel());
        Assertions.assertThat(actions)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARTNER_SKU, AuditAction.ActionType.UPDATE,
                    "ID поставщика", AuditAction.BillingMode.BILLING_MODE_NONE, 12, "12", "0")
                    .setParameterId(null)
            );

        auditServiceMock.clearActions();
        actions = audit(builder.supplierId(0L).endModel(), builder.supplierId(13L).endModel());
        Assertions.assertThat(actions)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARTNER_SKU, AuditAction.ActionType.UPDATE,
                    "ID поставщика", AuditAction.BillingMode.BILLING_MODE_NONE, 12, "0", "13")
                    .setParameterId(null)
            );
    }

    @Test
    public void testNumericParamNotChanged() {
        defaultContext = new DefaultModelAuditContext();

        CommonModel model = createBasicModel()
            .param(7L)
            .setNumeric(new BigDecimal("1"))
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(DATE)
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .param(7L)
            .setNumeric(new BigDecimal("1.0"))
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(DATE)
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        assertThat(result).isEmpty();
    }

    @Test
    public void testNumericParamChanged() {
        defaultContext = new DefaultModelAuditContext();

        CommonModel model = createBasicModel()
            .param(7L)
            .setNumeric(new BigDecimal("1"))
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(DATE)
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .param(7L)
            .setNumeric(new BigDecimal("1.1"))
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .modificationDate(NEW_DATE)
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(result, 2, model.getCategoryId(), null, 1L, "title", NEW_DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE, "Numeric"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL, 7L, "1", "1.1"
        );
    }

    @Test
    public void testParamValueConfirmed() {
        CommonModel model = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(1)
            .modificationSource(ModificationSource.AUTO)
            .endParameterValue()
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(1)
            .modificationSource(ModificationSource.OPERATOR_CONFIRMED)
            .endParameterValue()
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        assertThat(result)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.CHECK,
                    "testParam", AuditAction.BillingMode.BILLING_MODE_CHECK, 1L, "1", "1")
            );
    }

    @Test
    public void testParamValueModificationSourceChanged() {
        CommonModel model = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(1)
            .modificationSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .getModel();

        CommonModel updatedModel = createBasicModel()
            .startParameterValue()
            .paramId(1L)
            .xslName("testParam")
            .num(1)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .endParameterValue()
            .getModel();

        List<AuditAction> result = audit(model, updatedModel);

        assertThat(result)
            .usingElementComparatorIgnoringFields("eventId", "actionId", "date")
            .containsExactlyInAnyOrder(
                createAuditAction(AuditAction.EntityType.MODEL_PARAM, AuditAction.ActionType.UPDATE,
                    "testParam", AuditAction.BillingMode.BILLING_MODE_FILL, 1L, "1", "1")
            );
    }

    @Test
    public void testVideosChanged() {
        startDefaultSku()
            .video("url1", ModificationSource.OPERATOR_FILLED, 5L, new Date(11L),
                "urlSource1", ModelStorage.VideoSource.YANDEX)
            .video("url2", ModificationSource.OPERATOR_COPIED, 5L, new Date(12L),
                "urlSource2", ModelStorage.VideoSource.YANDEX)
            .video("url3", ModificationSource.OPERATOR_FILLED, 5L, new Date(13L),
                "urlSource3", ModelStorage.VideoSource.YANDEX)
            .video("url4", ModificationSource.OPERATOR_COPIED, 5L, new Date(14L),
                "urlSource4", ModelStorage.VideoSource.YANDEX)
            .video("url5", ModificationSource.OPERATOR_COPIED, 5L, new Date(15L),
                "urlSource5", ModelStorage.VideoSource.YANDEX)
            .video("url6", ModificationSource.OPERATOR_COPIED, 5L, new Date(16L),
                "urlSource6", ModelStorage.VideoSource.YANDEX)
            .video("url7", ModificationSource.OPERATOR_COPIED, 5L, new Date(17L),
                "urlSource7", ModelStorage.VideoSource.YANDEX)
            .video("url8", ModificationSource.OPERATOR_COPIED, 5L, new Date(18L),
                "urlSource8", ModelStorage.VideoSource.YANDEX)
            .endModel();

        CommonModel before = modelBuilder.getModel();

        startDefaultSku()
            .video("url1", ModificationSource.OPERATOR_FILLED, 5L, new Date(11L),
                "urlSource1", ModelStorage.VideoSource.YANDEX)
            .video("url4", ModificationSource.OPERATOR_COPIED, 5L, new Date(14L),
                "urlSource4", ModelStorage.VideoSource.YANDEX)
            .video("url5", ModificationSource.OPERATOR_COPIED, 5L, new Date(15L),
                "urlSource5", ModelStorage.VideoSource.YANDEX)
            .video("url6", ModificationSource.OPERATOR_COPIED, 5L, new Date(16L),
                "urlSource6", ModelStorage.VideoSource.YANDEX)
            .video("url8", ModificationSource.OPERATOR_COPIED, 5L, new Date(18L),
                "urlSource8", ModelStorage.VideoSource.YANDEX)
            .video("url7", ModificationSource.OPERATOR_COPIED, 5L, new Date(13L),
                "urlSource7", ModelStorage.VideoSource.YANDEX)
            .endModel();

        CommonModel after = modelBuilder.getModel();

        List<AuditAction> actions = audit(before, after);

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 9, 2L, 5L, 1L, "title", DATE);

        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.DELETE, "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL,
            null, "url2", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.CREATE, "2"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, "url4");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.DELETE, "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_FILL,
            null, "url3", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.CREATE, "3"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, null, "url5");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.UPDATE, "4"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "url4", "url6");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.UPDATE, "5"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_MOVE,
            null, "url5", "url8");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.UPDATE, "6"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_MOVE,
            null, "url6", "url7");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.DELETE, "7"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "url7", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_VIDEO, AuditAction.ActionType.DELETE, "8"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE,
            null, "url8", null);
    }

    private List<AuditAction> audit(@Nullable CommonModel before, @Nonnull CommonModel after) {
        Map<Long, CommonModel> beforeMap = before == null
            ? Collections.emptyMap() : Collections.singletonMap(after.getId(), before);

        service.auditModels(Collections.singletonList(after), beforeMap, defaultContext);

        return auditServiceMock.loadAudit(0, Integer.MAX_VALUE, new AuditFilter());
    }

    private AuditAction createAuditAction(AuditAction.EntityType entityType, AuditAction.ActionType actionType,
                                          String propertyName, AuditAction.BillingMode billingMode, long paramId,
                                          String oldValue, String newValue) {
        AuditAction auditAction = new AuditAction();
        auditAction.setEntityType(entityType);
        auditAction.setEntityId(1L);
        auditAction.setCategoryId(2L);
        auditAction.setEntityName("title");
        auditAction.setActionType(actionType);
        auditAction.setPropertyName(propertyName);
        auditAction.setBillingMode(billingMode);
        auditAction.setParameterId(paramId);
        auditAction.setOldValue(oldValue);
        auditAction.setNewValue(newValue);
        auditAction.setSource(AuditAction.Source.MBO);
        return auditAction;
    }

    private CommonModelBuilder<CommonModel> createBasicModel() {
        return modelBuilder.startModel()
            .id(1L)
            .category(2L)
            .vendorId(3L)
            .source(CommonModel.Source.GENERATED)
            .currentType(CommonModel.Source.GURU)
            .param(3L)
            .setString("title")
            .modificationSource(ModificationSource.OPERATOR_FILLED);
    }

    private CommonModel createModel1(Date date) {
        createBasicModel()
            .param(1L)
            .setOption(1L)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .param(2L)
            .setOption(2L)
            .modificationSource(ModificationSource.OPERATOR_FILLED)
            .picture("pic1", "http://picture1", ModificationSource.OPERATOR_FILLED)
            .picture("pic2", "http://picture2", ModificationSource.OPERATOR_COPIED)
            .startParameterValueLink()
            .paramId(2L)
            .optionId(2L)
            .pickerImage("http://picker1")
            .pickerImageSource(ModificationSource.OPERATOR_FILLED)
            .endParameterValue()
            .valueAlias(2L, 2L, 3L)
            .startParameterValueLink()
            .paramId(2L)
            .optionId(3L)
            .pickerImage("http://picker2")
            .pickerImageSource(ModificationSource.OPERATOR_COPIED)
            .endParameterValue()
            .valueAlias(2L, 1L, 4L)
            .endModel();

        CommonModel model = modelBuilder.getModel();
        model.setParentModelId(1L);
        model.setSupplierId(10L);
        model.setPublished(true);
        model.setDeleted(false);
        model.setCreatedDate(date);
        model.setModifiedUserId(4L);
        model.setModificationDate(date);

        return model;
    }

    private CommonModel createModel2(Date date, Date newDate) {
        modelBuilder.startModel()
            .id(2L)
            .category(3L)
            .vendorId(1L)
            .source(CommonModel.Source.GENERATED)
            .currentType(CommonModel.Source.GURU)
            .param(1L)
            .setOption(2L)
            .modificationSource(ModificationSource.OPERATOR_CONFIRMED)
            .param(3L)
            .setString("newTitle")
            .modificationSource(ModificationSource.OPERATOR_COPIED)
            .picture("pic1", "http://picture3", ModificationSource.OPERATOR_FILLED)
            .startParameterValueLink()
            .paramId(2L)
            .optionId(2L)
            .pickerImage("http://picker3")
            .pickerImageSource(ModificationSource.OPERATOR_FILLED)
            .endParameterValue()
            .valueAlias(2L, 2L, 1L)
            .parameterMetadata(2L, ModificationSource.OPERATOR_FILLED)
            .endModel();

        CommonModel model = modelBuilder.getModel();
        model.setParentModelId(5L);
        model.setSupplierId(11L);
        model.setPublished(false);
        model.setDeleted(false);
        model.setCreatedDate(date);
        model.setModifiedUserId(5L);
        model.setBluePublished(true);
        model.setModificationDate(newDate);

        return model;
    }

    private void assertCreate(List<AuditAction> actions, AuditAction.EntityType type, Date date,
                              boolean billedOperation) {
        boolean isGuru = type == AuditAction.EntityType.MODEL_GURU;

        AuditAction.EntityType paramType;
        String currentModelType;
        switch (type) {
            case MODEL_GURU:
                paramType = AuditAction.EntityType.MODEL_PARAM;
                currentModelType = "Гуру-карточка";
                break;
            case MODEL_SKU:
                paramType = AuditAction.EntityType.SKU_PARAM;
                currentModelType = "Складская учетная единица";
                break;
            case MODEL_PARTNER:
                paramType = AuditAction.EntityType.PARTNER_PARAM;
                currentModelType = "Партнерская карточка";
                break;
            case MODEL_PARTNER_SKU:
                paramType = AuditAction.EntityType.PARTNER_SKU_PARAM;
                currentModelType = "Партнерская складская учетная единица";
                break;
            default:
                throw new IllegalStateException("Unexpected type: " + type);
        }

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 27, 2L, 4L, 1L, "title", date);

        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.CREATE, null),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation));
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "1");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID категории"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "2");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID производителя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "3");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID родительской модели"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "1");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID поставщика"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "10");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Имя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "title");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Исходный тип"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "Авто-карточка");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Текущий тип"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, currentModelType);
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Опубликована"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "true");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Удалена"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, "false");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Дата создания"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, date.toString());
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Дата редактирования"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null,  date.toString());
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID редактировавшего пользователя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null,  "4");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Опубликована на Синем"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null,  "false");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "vendor"),
            actionsMap,
            billedOperation ? AuditAction.BillingMode.BILLING_MODE_FILL : AuditAction.BillingMode.BILLING_MODE_NONE,
            KnownIds.VENDOR_PARAM_ID, null,  "vendor3");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "name"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            3L, null,  "title");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "param1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            1L, null,  "value1");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "param2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null,  "value2");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.CREATE,
                !isGuru ? "1" : "pic1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            null, null,  "http://picture1");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.CREATE,
                !isGuru ? "2" : "pic2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_COPY, billedOperation),
            null, null,  "http://picture2");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.CREATE,
                "value2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null,  "http://picker1");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.CREATE,
                "value3"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_COPY, billedOperation),
            2L, null,  "http://picker2");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS, AuditAction.ActionType.CREATE, "value2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null,  "value3");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS, AuditAction.ActionType.CREATE, "value1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null,  null);
    }

    private void assertUpdate(List<AuditAction> actions, AuditAction.EntityType type, Date date, Date newDate,
                              boolean billedOperation) {
        boolean isGuru = type == AuditAction.EntityType.MODEL_GURU;

        AuditAction.EntityType paramType;
        switch (type) {
            case MODEL_GURU:
                paramType = AuditAction.EntityType.MODEL_PARAM;
                break;
            case MODEL_SKU:
                paramType = AuditAction.EntityType.SKU_PARAM;
                break;
            case MODEL_PARTNER:
                paramType = AuditAction.EntityType.PARTNER_PARAM;
                break;
            case MODEL_PARTNER_SKU:
                paramType = AuditAction.EntityType.PARTNER_SKU_PARAM;
                break;
            default:
                throw new IllegalStateException("Unexpected type: " + type);
        }

        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 22, 3L, 5L, 2L, "newTitle", newDate);

        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "1", "2");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID категории"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "2", "3");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID производителя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "3", "1");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID родительской модели"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "1", "5");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID поставщика"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "10", "11");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Имя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "title", "newTitle");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Опубликована"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "true", "false");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Дата редактирования"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, date.toString(), newDate.toString());
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "ID редактировавшего пользователя"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "4", "5");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Опубликована на Синем"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "false", "true");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "vendor"),
            actionsMap,
            billedOperation ? AuditAction.BillingMode.BILLING_MODE_FILL : AuditAction.BillingMode.BILLING_MODE_NONE,
            KnownIds.VENDOR_PARAM_ID, "vendor3", "vendor1");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "name"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_COPY, billedOperation),
            3L, "title", "newTitle");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "param1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_CHECK, billedOperation),
            1L, "value1", "value2");
        assertAuditAction(
            new AuditActionKey(paramType, AuditAction.ActionType.UPDATE, "param2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, "value2", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.UPDATE,
                !isGuru ? "1" : "pic1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            null, "http://picture1", "http://picture3");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.DELETE,
                !isGuru ? "2" : "pic2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            null, "http://picture2", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.UPDATE, "value2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, "http://picker1", "http://picker3");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.DELETE, "value3"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, "http://picker2", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS, AuditAction.ActionType.DELETE, "value2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, "value3", null);
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS, AuditAction.ActionType.CREATE, "value2"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null, "value1");
        assertAuditAction(
            new AuditActionKey(AuditAction.EntityType.MODEL_ENUM_VALUE_ALIAS, AuditAction.ActionType.DELETE, "value1"),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation),
            2L, null,  null);
    }

    private void assertDelete(List<AuditAction> actions, AuditAction.EntityType type, Date date, Date deletedDate,
                              boolean billedOperation) {
        Map<AuditActionKey, List<AuditAction>> actionsMap =
            assertAuditActions(actions, 3, 3L, 5L, 2L, "newTitle", date);

        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.DELETE, null),
            actionsMap, effective(AuditAction.BillingMode.BILLING_MODE_FILL, billedOperation));
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Удалена"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, "false", "true");
        assertAuditAction(
            new AuditActionKey(type, AuditAction.ActionType.UPDATE, "Дата удаления"),
            actionsMap, AuditAction.BillingMode.BILLING_MODE_NONE, null, null, deletedDate.toString());
    }

    private AuditAction.BillingMode effective(AuditAction.BillingMode mode, boolean billedOperation) {
        return billedOperation ? mode : AuditAction.BillingMode.BILLING_MODE_NONE;
    }
    private Map<AuditActionKey, List<AuditAction>> assertAuditActions(List<AuditAction> result, int size,
                                                                      Long categoryId, Long userId,
                                                                      Long entityId, String entityName,
                                                                      Date date) {
        assertEquals(size, result.size());

        Map<AuditActionKey, List<AuditAction>> resultMap = result.stream().collect(
            Collectors.groupingBy(a -> new AuditActionKey(a.getEntityType(), a.getActionType(), a.getPropertyName())));

        resultMap.values().stream().flatMap(List::stream).forEach(a -> {
            assertEquals(categoryId, a.getCategoryId());
            assertEquals(userId, a.getUserId());
            assertEquals(entityId, a.getEntityId());
            assertEquals(entityName, a.getEntityName());
            assertEquals(date, a.getDate());
        });

        return resultMap;
    }

    private void assertAuditAction(AuditActionKey key, Map<AuditActionKey, List<AuditAction>> actions,
                                   AuditAction.BillingMode billingMode) {
        List<AuditAction> actionList = actions.get(key);
        assertNotNull(actionList);
        assertEquals(1, actionList.size());
        AuditAction action = actionList.get(0);
        assertEquals(billingMode, action.getBillingMode());
        assertNull(action.getOldValue());
        assertNull(action.getNewValue());
        assertNull(action.getParameterId());
    }

    private void assertAuditAction(AuditActionKey key, Map<AuditActionKey, List<AuditAction>> actions,
                                   AuditAction.BillingMode billingMode, Long parameterId,
                                   String oldValue, String newValue) {
        List<AuditAction> actionsList = actions.get(key);
        assertNotNull("Action not found for key " + key + " and values " + oldValue + "->" + newValue, actionsList);
        for (AuditAction action : actionsList) {
            if (Objects.equals(newValue, action.getNewValue()) &&
                Objects.equals(oldValue, action.getOldValue())) {
                assertEquals(parameterId, action.getParameterId());
                assertEquals(billingMode, action.getBillingMode());
                return;
            }
        }
        Assert.fail("Action not found for key " + key + " and values " + oldValue + "->" + newValue);
    }

    class AuditActionKey {
        AuditAction.EntityType entityType;
        AuditAction.ActionType actionType;
        String propertyName;

        AuditActionKey(AuditAction.EntityType entityType, AuditAction.ActionType actionType,
                       String propertyName) {
            this.entityType = entityType;
            this.actionType = actionType;
            this.propertyName = propertyName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AuditActionKey that = (AuditActionKey) o;
            return entityType == that.entityType &&
                actionType == that.actionType &&
                Objects.equals(propertyName, that.propertyName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entityType, actionType, propertyName);
        }

        @Override
        public String toString() {
            return "AuditActionKey{" +
                "entityType=" + entityType +
                ", actionType=" + actionType +
                ", propertyName='" + propertyName + '\'' +
                '}';
        }
    }

    class DefaultModelAuditContext implements ModelAuditContext {

        private boolean isBilledOperation = true;
        private SaveStats stats = new SaveStats();
        private AuditAction.Source source = AuditAction.Source.MBO;
        private String sourceId;

        @Override
        public boolean isBilledOperation() {
            return isBilledOperation;
        }

        @Override
        public void setBilledOperation(boolean billedOperation) {
            isBilledOperation = billedOperation;
        }

        @Override
        public SaveStats getStats() {
            return stats;
        }

        @Override
        public void setStats(SaveStats stats) {
            this.stats = stats;
        }

        @Override
        public Optional<Long> getParameterId(Long categoryId, String xslName) {
            return Optional.ofNullable(modelBuilder.getParamDescription(xslName)).map(ThinCategoryParam::getId);
        }

        @Override
        public Optional<String> getParameterOptionName(Long categoryId, Long paramId, Long optionId) {
            ThinCategoryParam param = modelBuilder.getParamDescription(paramId);
            if (param == null) {
                return null;
            }
            for (Option option : param.getOptions()) {
                if (option.getValueId() == optionId) {
                    return Optional.of(option.getName());
                }
            }
            return Optional.empty();
        }

        @Override
        public Optional<String> getParameterValueAsString(Long categoryId, ParameterValue value) {
            switch (value.getType()) {
                case ENUM:
                case NUMERIC_ENUM:
                    return getParameterOptionName(categoryId, value.getParamId(), value.getOptionId());
                case NUMERIC:
                    return Optional.of(String.valueOf(value.getNumericValue()));
                case BOOLEAN:
                    return Optional.of(String.valueOf(value.getBooleanValue()));
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        public boolean isBillableParameter(Long categoryId, Long paramId) {
            return true;
        }

        @Override
        public boolean isMultivalueParameter(Long categoryId, Long paramId) {
            return false;
        }

        @Override
        public boolean isCustomBillableParameter(Long categoryId, Long paramId) {
            return false;
        }

        @Override
        public AuditAction.Source getSource() {
            return source;
        }

        @Override
        public void setSource(AuditAction.Source source) {
            this.source = source;
        }

        @Override
        public String getSourceId() {
            return sourceId;
        }

        @Override
        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
    }
}
