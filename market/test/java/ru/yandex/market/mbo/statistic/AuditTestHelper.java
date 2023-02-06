package ru.yandex.market.mbo.statistic;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.time.Instant;
import java.util.Date;

/**
 * @author kravchenko-aa
 * @date 26/06/2019
 */
public class AuditTestHelper {
    private static long auditActionDateIncrement;

    private AuditTestHelper() {
    }

    public static AuditAction modelAuditAction(long entityId,
                                         AuditAction.EntityType entityType,
                                         AuditAction.ActionType actionType) {
        return createAuditAction(entityId, entityType, actionType, null, null,
            null, null, null, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction skuSingleParamAction(long paramId,
                                             String xslName,
                                             String oldValue,
                                             String newValue) {
        return skuParamAction(paramId, xslName, oldValue, newValue, AuditAction.ActionType.UPDATE);
    }

    public static AuditAction modelSingleParamAction(long paramId,
                                                     String xslName,
                                                     String oldValue,
                                                     String newValue) {
        return modelParamAction(paramId, xslName, oldValue, newValue, AuditAction.ActionType.UPDATE);
    }

    public static AuditAction modelParamAction(long paramId,
                                               String xslName,
                                               String oldValue,
                                               String newValue,
                                               AuditAction.ActionType actionType) {
        return createAuditAction(1L, AuditAction.EntityType.MODEL_PARAM, actionType,
            paramId, xslName, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction modelSingleParamHypothesisAction(long paramId,
                                                               String xslName,
                                                               String oldValue,
                                                               String newValue) {
        return modelParamHypothesisAction(paramId, xslName, oldValue, newValue, AuditAction.ActionType.UPDATE);
    }

    public static AuditAction modelParamHypothesisAction(long paramId,
                                                         String xslName,
                                                         String oldValue,
                                                         String newValue,
                                                         AuditAction.ActionType actionType) {
        return createAuditAction(1L, AuditAction.EntityType.PARAM_HYPOTHESIS, actionType,
            paramId, xslName, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction modelParamValueMetadataAction(long paramId,
                                                            String xslName,
                                                            String oldValue,
                                                            String newValue) {
        return createAuditAction(1L, AuditAction.EntityType.PARAM_METADATA, AuditAction.ActionType.UPDATE,
            paramId, xslName, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction modelPickerAction(long paramId,
                                          String option,
                                          String oldValue,
                                          String newValue) {
        return createAuditAction(1L, AuditAction.EntityType.MODEL_PICKER, AuditAction.ActionType.UPDATE,
            paramId, option, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction modelPictureAction(long paramId,
                                           String paramXslName,
                                           String oldValue,
                                           String newValue) {
        return createAuditAction(1L, AuditAction.EntityType.MODEL_PICTURE, AuditAction.ActionType.UPDATE,
            paramId, paramXslName, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction skuParamAction(long paramId,
                                       String xslName,
                                       String oldValue,
                                       String newValue,
                                       AuditAction.ActionType actionType) {
        return createAuditAction(1L, AuditAction.EntityType.SKU_PARAM, actionType,
            paramId, xslName, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    public static AuditAction optionAction(long optionId,
                                             long paramId,
                                             String property,
                                             String oldValue,
                                             String newValue,
                                             AuditAction.ActionType actionType) {
        return createAuditAction(optionId, AuditAction.EntityType.OPTION, actionType,
            paramId, property, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static AuditAction optionAction(long optionId,
                                           long paramId,
                                           long categoryId,
                                           String property,
                                           String oldValue,
                                           String newValue,
                                           AuditAction.ActionType actionType,
                                           long uid) {
        return createAuditAction(optionId, AuditAction.EntityType.OPTION, actionType,
            paramId, property, oldValue, newValue, categoryId, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM, uid);
    }

    public static AuditAction categoryAction(String property,
                                           String oldValue,
                                           String newValue,
                                           AuditAction.ActionType actionType) {
        return createAuditAction(1L, AuditAction.EntityType.OPTION, actionType,
            0L, property, oldValue, newValue, 1L, AuditAction.BillingMode.BILLING_MODE_FILL_CUSTOM);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public static AuditAction createAuditAction(long entityId,
                                         AuditAction.EntityType entityType,
                                         AuditAction.ActionType actionType,
                                         Long parameterId,
                                         String parameterXslName,
                                         String oldValue,
                                         String newValue,
                                         Long categoryId,
                                         AuditAction.BillingMode billingMode) {
        return createAuditAction(entityId, entityType, actionType,
            parameterId, parameterXslName, oldValue, newValue, categoryId,
            billingMode, null);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    public static AuditAction createAuditAction(long entityId,
                                         AuditAction.EntityType entityType,
                                         AuditAction.ActionType actionType,
                                         Long parameterId,
                                         String parameterXslName,
                                         String oldValue,
                                         String newValue,
                                         Long categoryId,
                                         AuditAction.BillingMode billingMode,
                                         Long userId) {
        AuditAction auditAction = new AuditAction();
        auditAction.setActionId(1L);
        auditAction.setEntityId(entityId);
        auditAction.setEntityType(entityType);
        auditAction.setActionType(actionType);
        auditAction.setParameterId(parameterId);
        auditAction.setPropertyName(parameterXslName);
        auditAction.setOldValue(oldValue);
        auditAction.setNewValue(newValue);
        auditAction.setCategoryId(categoryId);
        auditAction.setBillingMode(billingMode);
        auditAction.setUserId(userId);
        auditAction.setSource(AuditAction.Source.YANG_TASK);
        auditAction.setSourceId("1234");
        auditAction.setDate(Date.from(Instant.ofEpochSecond(auditActionDateIncrement++)));
        return auditAction;
    }

    public static MboParameters.Parameter createParameter(String xslName, boolean multivalue, long id) {
        return MboParameters.Parameter.newBuilder()
            .setXslName(xslName)
            .setMultivalue(multivalue)
            .setId(id)
            .build();
    }

    public static MboParameters.Parameter createLocalVendor(long id, long optionId, String name) {
        return MboParameters.Parameter.newBuilder()
            .setXslName(XslNames.VENDOR)
            .setMultivalue(false)
            .setId(id)
            .addOption(
                MboParameters.Option.newBuilder()
                    .setId(1L)
                    .setLocalVendorId(optionId)
                    .addName(
                        MboParameters.Word.newBuilder()
                            .setName(name).build()
                    ))
            .build();
    }

    public static void refreshTime() {
        auditActionDateIncrement = 0;
    }

    public static long refreshTimeToNow() {
        auditActionDateIncrement = Instant.now().getEpochSecond();
        return auditActionDateIncrement;
    }
}
