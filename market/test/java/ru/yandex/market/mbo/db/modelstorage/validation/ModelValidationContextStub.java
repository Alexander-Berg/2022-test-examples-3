package ru.yandex.market.mbo.db.modelstorage.validation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ru.yandex.market.mbo.db.modelstorage.StatsModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.validation.context.CachingModelValidationContext;
import ru.yandex.market.mbo.db.modelstorage.validation.dump.DumpValidationService;
import ru.yandex.market.mbo.db.params.guru.BaseGuruServiceImpl;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.licensor.MboLicensors;

/**
 * @author s-ermakov
 */
public class ModelValidationContextStub extends CachingModelValidationContext {
    private BaseGuruServiceImpl guruService;
    private StatsModelQueryService statsModelStorageService;
    private Map<Long, String> allParams = new HashMap<>();
    private Map<Long, String> mandatoryParamNames = new HashMap<>();
    private Map<Long, String> skuNoneParams = new HashMap<>();

    public ModelValidationContextStub(DumpValidationService dumpValidationService) {
        super(dumpValidationService);
    }

    public void setGuruService(BaseGuruServiceImpl guruService) {
        this.guruService = guruService;
    }

    @Override
    public StatsModelQueryService getModelQueryService() {
        return statsModelStorageService;
    }

    public void setStatsModelStorageService(StatsModelQueryService statsModelStorageService) {
        this.statsModelStorageService = statsModelStorageService;
    }

    @Override
    public boolean hasCategory(Long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, String> getMandatoryForSignatureParamNames(Long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean usedInCompatibilities(long modelId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean parameterMatchesDefinition(long categoryId, ParameterValue parameter) {
        String xslName = allParams.get(parameter.getParamId());
        return Objects.equals(xslName, parameter.getXslName());
    }

    @Override
    public NumericBounds getParameterBounds(long categoryId, String xslName) {
        return NumericBounds.notDefined();
    }

    @Override
    public Map<Long, String> getSkuParameterNamesWithMode(long categoryId, SkuParameterMode mode) {
        if (mode == SkuParameterMode.SKU_NONE) {
            return skuNoneParams;
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<Long, String> getMandatoryParamNames(long categoryId) {
        return mandatoryParamNames;
    }

    public void addParam(Long paramId, String xslName, boolean isMandatory, SkuParameterMode skuParameterMode) {
        allParams.put(paramId, xslName);
        if (isMandatory) {
            mandatoryParamNames.put(paramId, xslName);
        }

        if (skuParameterMode == SkuParameterMode.SKU_NONE) {
            skuNoneParams.put(paramId, xslName);
        }
    }

    @Override
    public Integer getStringParamMaxLength(long categoryId, String xslName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<Long, String> getOptionNames(long categoryId, long paramId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReadableParameterName(long categoryId, String xslName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReadableParameterName(long categoryId, long paramId) {
        return mandatoryParamNames.containsKey(paramId) ? "Param" + paramId : null;
    }

    @Override
    public Set<Long> getImagePickerParamIds(long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long getParamIdByXslName(long categoryId, String xslName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isGroupCategory(long categoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCategoryAllowNonWhiteFirstPicture(long categoryId) {
        return false;
    }

    @Override
    public OperationStats getStats() {
        return new OperationStats();
    }

    @Override
    public Map<Long, ModelValidationVendor> loadCategoryVendors(long categoryId) {
        return guruService.getVendors(categoryId);
    }

    @Override
    public Optional<CommonModel> getModel(long categoryId, long modelId, Collection<CommonModel> changedModels) {
        return changedModels.stream().filter(m -> m.getId() == modelId).findFirst();
    }

    @Override
    public RelatedModelsContainer getRelatedModelsForModels(long categoryId, List<CommonModel> models,
                                                            Collection<ModelRelation.RelationType> relationTypes,
                                                            Collection<CommonModel> updatedModels,
                                                            boolean takeOnlyLocalModels) {
        return super.getRelatedModelsForModels(categoryId, models, relationTypes, updatedModels, takeOnlyLocalModels);
    }

    @Override
    public Set<Long> getCategoryIdsUpToRoot(long categoryId) {
        return null;
    }

    @Override
    public List<MboLicensors.Licensor> getLicensorConstrains() {
        return null;
    }

    @Override
    public ParameterProperties getParameterProperties(long categoryId, long paramId) {
        return new ParameterProperties(false);
    }

    @Override
    public Map<Long, Long> getHidToParentHid() {
        return null;
    }

    @Override
    public void setHidToParentHid(Map<Long, Long> hidToParentHid) {

    }


}
