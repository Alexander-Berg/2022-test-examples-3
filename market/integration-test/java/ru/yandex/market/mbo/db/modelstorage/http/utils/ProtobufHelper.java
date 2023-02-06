package ru.yandex.market.mbo.db.modelstorage.http.utils;

import org.jetbrains.annotations.NotNull;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Arrays;
import java.util.List;

/**
 * @author amaslak
 * @timestamp 2/27/16 6:18 PM
 */
public class ProtobufHelper {

    private static final int ISO_CODE_RU = 225;

    public static final Long VENDOR_PARAM_ID = KnownIds.VENDOR_PARAM_ID;
    public static final Long NAME_PARAM_ID = KnownIds.NAME_PARAM_ID;
    public static final Long OPERATOR_COMMENT_PARAM_ID = 7351758L;
    public static final Long DESCRIPTION_PARAM_ID = 100500123L; //Fake ID as we currently don't have description parm

    private ProtobufHelper() {
    }

    @NotNull
    public static ModelStorage.LocalizedString.Builder toLocalizedString(String s) {
        return ModelStorage.LocalizedString.newBuilder()
                .setIsoCode(CommonModel.DEFAULT_LANG.getIsoCode())
                .setValue(s);
    }

    @NotNull
    public static ModelCardApi.OpenSessionRequest getSessionOpen(String sessionId) {
        return ModelCardApi.OpenSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .build();
    }

    @NotNull
    public static ModelCardApi.OpenCategorySessionRequest getCategoryOpen(int categoryId, String sessionId,
                                                                          String scSessionId) {
        return ModelCardApi.OpenCategorySessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setCategoryId(categoryId)
                .setScSessionId(scSessionId)
                .build();
    }

    @NotNull
    public static ModelCardApi.CloseSessionRequest getSessionClose(String sessionId) {
        return ModelCardApi.CloseSessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setTotalCategoryCount(1)
                .setSuccessCategoryCount(1)
                .build();
    }

    @NotNull
    public static ModelCardApi.UpdateCategorySessionStatusRequest getSessionStatusUpdate(int categoryId,
                 String sessionId, ModelCardApi.CategorySessionStatus status) {
        return ModelCardApi.UpdateCategorySessionStatusRequest.newBuilder()
                .setClusterizerSessionId(sessionId)
                .setCategoryId(categoryId)
                .setStatus(status)
                .setEvironment("development")
                .setHost("localhost")
                .build();
    }

    @NotNull
    public static ModelCardApi.CloseCategorySessionRequest getCategoryClose(int categoryId, String sessionId) {
        return ModelCardApi.CloseCategorySessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setCategoryId(categoryId)
                .setStatus(ModelCardApi.CategorySessionStatus.FINISHED_SUCCESS)
                .build();
    }

    @NotNull
    public static ModelCardApi.ImportModelsV2Request getImportV2(int categoryId, String sessionId,
                                                                 List<ModelCardApi.CreateOrUpdateModelDiff> createDiffs,
                                                                 List<ModelCardApi.CreateOrUpdateModelDiff> updateDiffs,
                                                                 List<ModelCardApi.DeleteModelDiff> deleteDiffs,
                                                                 List<ModelCardApi.PublishModelDiff> publishDiffs) {
        return ModelCardApi.ImportModelsV2Request.newBuilder()
                .setCategoryId(categoryId)
                .setClusterizerSessionId(sessionId)
                .addAllCreateDiffs(createDiffs)
                .addAllUpdateDiffs(updateDiffs)
                .addAllDeleteDiffs(deleteDiffs)
                .addAllPublishDiffs(publishDiffs)
                .build();
    }

    @NotNull
    public static ModelCardApi.ImportModelsV3Request getImportV3(int categoryId, String sessionId,
                                                                 List<ModelCardApi.CreateOrUpdateModelDiff> createDiffs,
                                                                 List<ModelCardApi.CreateOrUpdateModelDiff> updateDiffs,
                                                                 List<ModelCardApi.PublishModelDiff> publishDiffs) {
        return ModelCardApi.ImportModelsV3Request.newBuilder()
                .setCategoryId(categoryId)
                .setClusterizerSessionId(sessionId)
                .addAllCreateDiffs(createDiffs)
                .addAllUpdateDiffs(updateDiffs)
                .addAllPublishDiffs(publishDiffs)
                .build();
    }


    public static ModelStorage.ParameterValue.Builder createParameter(Long id, String xslName, Long date, Long uid) {
        ModelStorage.ParameterValue.Builder paramBuilder = ModelStorage.ParameterValue.newBuilder();
        paramBuilder.setParamId(id);
        paramBuilder.setXslName(xslName);
        paramBuilder.setValueSource(ModelStorage.ModificationSource.AUTO);
        paramBuilder.setModificationDate(date);
        paramBuilder.setUserId(uid);
        return paramBuilder;
    }

    @NotNull
    public static ModelCardApi.ImportModelsRequest importModelRequest(int categoryId, String sessionId,
                                                                      List<ModelCardApi.ModelDiff> modelDiffs) {
        return ModelCardApi.ImportModelsRequest.newBuilder()
                .setCategoryId(categoryId)
                .setClusterizerSessionId(sessionId)
                .addAllModels(modelDiffs)
                .build();
    }

    public static ModelStorage.LocalizedString fromString(String name) {
        return ModelStorage.LocalizedString.newBuilder()
                .setValue(name)
                .setIsoCode(Language.forId(ISO_CODE_RU).getIsoCode())
                .build();
    }

    public static ModelStorage.ParameterValue.Builder createValueBuilder(String xslName, long paramId,
                                                                         Param.Type type) {
        return ModelStorage.ParameterValue.newBuilder()
                .setXslName(xslName)
                .setParamId(paramId)
                .setTypeId(type.ordinal());
    }

    public static ModelStorage.GetModelsRequest.Builder getModelsRequestBuilder(long categoryId, Long... modelIds) {
        ModelStorage.GetModelsRequest.Builder getModelsRequest = ModelStorage.GetModelsRequest
                .newBuilder()
                .setCategoryId(categoryId)
                .addAllModelIds(Arrays.asList(modelIds));
        return getModelsRequest;
    }

    public static ModelStorage.RemoveModelsRequest.Builder removeModelsRequestBuilder(ModelStorage.Model... models) {
        ModelStorage.RemoveModelsRequest.Builder removeModelsRequest = ModelStorage.RemoveModelsRequest
                .newBuilder()
                .addAllModels(Arrays.asList(models));
        return removeModelsRequest;
    }
}
