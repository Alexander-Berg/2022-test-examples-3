package ru.yandex.market.mbo.gwt.client.pages.model.editor.rpc;

import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.ModelData;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.model.SizeMeasureScaleInfo;
import ru.yandex.market.mbo.gwt.client.widgets.image.ImageFile;
import ru.yandex.market.mbo.gwt.models.GwtPair;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityModel;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CutOffWordsCheckResult;
import ru.yandex.market.mbo.gwt.models.modelstorage.EditorSaveInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelInfosOfVendorResult;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelPictureInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelTransition;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.BusinessSkuKey;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingChangeType;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.MappingUpdateStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.mbo_category.SupplierOffer;
import ru.yandex.market.mbo.gwt.models.modelstorage.models.MoveSkusGroup;
import ru.yandex.market.mbo.gwt.models.params.DependentOptionsDto;
import ru.yandex.market.mbo.gwt.models.params.Link;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplateView;
import ru.yandex.market.mbo.gwt.models.tt.GwtTaskStatus;
import ru.yandex.market.mbo.gwt.models.tt.GwtTaskType;
import ru.yandex.market.mbo.gwt.models.visual.CategoryWiki;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author gilmulla
 */
public class TestRpc implements Rpc {

    private CutOffWordsCheckResult checkCutOffWords;
    private Throwable checkCutOffWordsThrowable;

    private List<CommonModel> getModifications;
    private Throwable getModificationsThrowable;

    private CommonModel copyModelParams;
    private Throwable copyModelParamsThrowable;

    private boolean loadModelSetup;
    private CommonModel loadModel;
    private Throwable loadModelThrowable;

    private boolean loadModelsByBarcodeSetup;
    private List<CommonModel> loadModelsByBarcode;
    private Throwable loadModelsByBarcodeThrowable;

    private boolean loadModelDataSetup;
    private ModelData loadModelData;
    private Throwable loadModelDataThrowable;
    private Supplier<Error> loadModelFail;

    private boolean saveModelSetup;
    private boolean rewriteLoadModelOnSave = true;
    private CommonModel modelToSave;
    private List<ModelTransition> modelTransitionsToSave;
    private Long saveModelId;
    private Throwable saveModelThrowable;
    private Supplier<Error> saveModelFail;
    private List<CommonModel> relatedModelsToSave;

    private boolean sampleNameSetup;
    private List<String> sampleNames;
    private Throwable sampleNamesThrowable;

    private TMTemplateView tmTemplateView;
    private Throwable tmTemplateThrowable;

    private CategoryWiki categoryWiki;
    private Throwable categoryWikiThrowable;

    private boolean successOnCreateOptionRelation;
    private long optionIdOnCreateOptionRelation;
    private Throwable throwableOnCreateOptionRelation;

    private CommonModel deletePicturesModel;
    private Throwable deletePicturesThrowable;
    private Supplier<Error> deletePicturesSupplier;
    private Function<SupplierOffer, MappingUpdateStatus> skuMappingUpdateStatusProvider;
    private List<SupplierOffer> mappings = new ArrayList<>();

    private ModelInfo requestedSku = null;
    private ModelInfo requestedModel = null;

    private List<ModelPictureInfo> modelPictureInfos;
    private Throwable modelPictureInfosThrowable;

    public void setLoadModel(CommonModel model, Throwable throwable) {
        this.loadModelSetup = true;
        this.loadModelFail = null;
        this.loadModel = model;
        this.loadModelThrowable = throwable;
    }

    public void setLoadModelByBarcode(CommonModel model, Throwable throwable) {
        setLoadModelsByBarcode(
            model != null ? Collections.singletonList(model) : null,
            throwable);
    }

    public void setLoadModelsByBarcode(List<CommonModel> models, Throwable throwable) {
        this.loadModelsByBarcodeSetup = true;
        this.loadModelsByBarcode = models;
        this.loadModelsByBarcodeThrowable = throwable;
    }

    public void setLoadModelFail(Supplier<Error> loadModelFail) {
        this.loadModelDataSetup = true;
        this.loadModelFail = loadModelFail;
    }

    public void setLoadModelData(ModelData modelData, Throwable throwable) {
        this.loadModelDataSetup = true;
        this.loadModelData = modelData;
        this.loadModelDataThrowable = throwable;
    }

    public void setSaveModel(Long modelId, Throwable throwable) {
        this.saveModelSetup = true;
        this.saveModelFail = null;
        this.saveModelId = modelId;
        this.saveModelThrowable = throwable;
    }

    public void setSaveModelFail(Supplier<Error> saveModelFail) {
        this.saveModelSetup = true;
        this.saveModelFail = saveModelFail;
    }

    public void setSampleNames(List<String> sampleNames, Throwable throwable) {
        this.sampleNameSetup = true;
        this.sampleNames = sampleNames;
        this.sampleNamesThrowable = throwable;
    }

    public void setTMTemplateView(TMTemplateView tmTemplateView, Throwable throwable) {
        this.tmTemplateView = tmTemplateView;
        this.tmTemplateThrowable = throwable;
    }


    public void setCategoryWiki(CategoryWiki categoryWiki, Throwable throwable) {
        this.categoryWiki = categoryWiki;
        this.categoryWikiThrowable = throwable;
    }

    public void setCreateOptionRelation(boolean success, long optionId, Throwable throwable) {
        this.successOnCreateOptionRelation = success;
        this.optionIdOnCreateOptionRelation = optionId;
        this.throwableOnCreateOptionRelation = throwable;
    }

    public void setGetModifications(List<CommonModel> getModifications, Throwable throwable) {
        this.getModifications = new ArrayList<>(getModifications);
        this.getModificationsThrowable = throwable;
    }

    public void setDeletePictures(CommonModel deletePicturesModel, Throwable deletePicturesThrowable) {
        this.deletePicturesSupplier = null;
        this.deletePicturesModel = deletePicturesModel;
        this.deletePicturesThrowable = deletePicturesThrowable;
    }

    public void setDeletePicturesFail(Supplier<Error> deletePicturesSupplier) {
        this.deletePicturesSupplier = deletePicturesSupplier;
    }

    public void setSkuMappingUpdateStatusProvider(
        Function<SupplierOffer, MappingUpdateStatus> skuMappingUpdateStatusProvider) {
        this.skuMappingUpdateStatusProvider = skuMappingUpdateStatusProvider;
    }


    public void setRequestedSku(ModelInfo requestedSku) {
        this.requestedSku = requestedSku;
    }

    @Override
    public void checkCutOffWords(long modelId, ParameterValues cutOffWords,
                                 Consumer<CutOffWordsCheckResult> onSuccess,
                                 Consumer<Throwable> onFailure) {
        if (checkCutOffWords != null) {
            onSuccess.accept(checkCutOffWords);
        } else {
            onFailure.accept(checkCutOffWordsThrowable);
        }
    }

    @Override
    public void getModifications(long modelId,
                                 Consumer<List<CommonModel>> onSuccess,
                                 Consumer<Throwable> onFailure) {
        if (getModifications != null) {
            onSuccess.accept(getModifications);
        } else {
            onFailure.accept(getModificationsThrowable);
        }
    }

    @Override
    public void copyModelParams(long categoryId, long sourceId, long targetId,
                                boolean includePictures, Consumer<CommonModel> onSuccess,
                                Consumer<Throwable> onFailure) {
        if (copyModelParams != null) {
            onSuccess.accept(copyModelParams);
        } else {
            onFailure.accept(copyModelParamsThrowable);
        }
    }

    @Override
    public void getCategoryHidAndVendorId(long localVendorId,
                                          Consumer<GwtPair<Long, Long>> onSuccess,
                                          Consumer<Throwable> onFailure) {

    }

    @Override
    public void getSampleNames(long categoryId, long vendorId, long parentId,
                               Consumer<List<String>> onSuccess,
                               Consumer<Throwable> onFailure) {
        if (!sampleNameSetup) {
            return;
        }

        if (sampleNames != null) {
            onSuccess.accept(sampleNames);
        } else if (sampleNamesThrowable != null) {
            onFailure.accept(sampleNamesThrowable);
        }
    }

    @Override
    public void loadModel(long entityId, long categoryId,
                          Consumer<CommonModel> onSuccess, Consumer<Throwable> onFailure) {
        if (!loadModelSetup) {
            return;
        }

        if (loadModelFail != null) {
            throw loadModelFail.get();
        }

        if (loadModel != null) {
            onSuccess.accept(loadModel);
        } else if (loadModelThrowable != null) {
            onFailure.accept(loadModelThrowable);
        } else {
            onSuccess.accept(null);
        }
    }

    @Override
    public void loadModelsByBarcode(String barcode,
                                    long categoryId,
                                    Consumer<List<CommonModel>> onSuccess,
                                    Consumer<Throwable> onFailure) {
        if (!loadModelsByBarcodeSetup) {
            return;
        }

        if (loadModelsByBarcode != null) {
            onSuccess.accept(loadModelsByBarcode);
        } else if (loadModelsByBarcodeThrowable != null) {
            onFailure.accept(loadModelsByBarcodeThrowable);
        } else {
            onSuccess.accept(null);
        }
    }

    @Override
    public void loadModelData(CommonModel model, long vendorId,
                              Consumer<ModelData> onSuccess, Consumer<Throwable> onFailure) {
        if (!loadModelDataSetup) {
            return;
        }

        if (loadModelData != null) {
            onSuccess.accept(loadModelData);
        } else if (loadModelDataThrowable != null) {
            onFailure.accept(loadModelDataThrowable);
        } else {
            onSuccess.accept(null);
        }
    }

    public void setRewriteLoadModelOnSave(boolean rewriteLoadModel) {
        this.rewriteLoadModelOnSave = rewriteLoadModel;
    }

    @Override
    public void loadVendorValueLinksForOfferParameters(ModelData modelData, Long categoryHid,
                                                       Consumer<List<ValueLink>> onSuccess,
                                                       Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList());
    }

    @Override
    public void loadSizeMeasureScaleInfos(ModelData modelData, Long categoryHid,
                                          Consumer<List<SizeMeasureScaleInfo>> onSuccess,
                                          Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList());
    }

    @Override
    public void loadParamLinks(ModelData modelData,
                               Consumer<List<Link>> onSuccess,
                               Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList());
    }

    @Override
    public void loadDependentOptions(ModelData modelData,
                                     Consumer<Map<Long, List<DependentOptionsDto>>> onSuccess,
                                     Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyMap());
    }

    @Override
    public void saveDependentOptions(List<DependentOptionsDto> options,
                                     Consumer<Void> onSuccess,
                                     Consumer<Throwable> onFailure) {

    }

    @Override
    @SuppressWarnings("checkstyle:ParameterNumber")
    public void saveModel(CommonModel model, List<CommonModel> relatedModels,
                          List<ModelTransition> modelTransitionsToSave,
                          Long taskId, boolean force, GeneralizationStrategy strategy,
                          Consumer<EditorSaveInfo> onSuccess, Consumer<Throwable> onFailure) {

        if (!saveModelSetup) {
            return;
        }

        if (saveModelFail != null) {
            throw saveModelFail.get();
        }

        this.modelToSave = model;
        this.modelTransitionsToSave = modelTransitionsToSave;
        this.relatedModelsToSave = relatedModels;
        if (this.rewriteLoadModelOnSave) {
            this.setLoadModel(new CommonModel(model), null);
        }

        if (saveModelId != null) {
            onSuccess.accept(new EditorSaveInfo(Collections.emptyList(),
                Collections.singletonList(new ModelChanges(null, model))));
        } else if (saveModelThrowable != null) {
            onFailure.accept(saveModelThrowable);
        } else {
            onSuccess.accept(null);
        }
    }

    @Override
    public void moveSkus(MoveSkusGroup moveSkusGroup, Consumer<Void> onSuccess, Consumer<Throwable> onFailure) {

    }

    public void createNewParamOption(String parameterValueName, Long paramId, Long hid,
                                     Consumer<Option> onSuccess,
                                     Consumer<Throwable> onFailure) {
        if (successOnCreateOptionRelation) {
            Option option = new OptionImpl(optionIdOnCreateOptionRelation, parameterValueName);
            option.setParamId(paramId);
            onSuccess.accept(option);
        } else {
            onFailure.accept(new Exception("message"));
        }
    }

    @Override
    public void saveModelCompatibilities(CommonModel model,
                                         List<CompatibilityModel> compatibilityModels, Runnable onSuccess,
                                         Consumer<Throwable> onFailure) {
    }

    @Override
    public void getModelCompatibilities(CommonModel model,
                                        Consumer<List<CompatibilityModel>> onSuccess,
                                        Consumer<Throwable> onFailure) {
    }

    @Override
    public void getAvailableTaskActions(long taskId, long userId,
                                        Consumer<List<GwtTaskStatus>> onSuccess,
                                        Consumer<Throwable> onFailure) {
    }

    @Override
    public void getTaskListType(long taskListId,
                                Consumer<GwtTaskType> onSuccess, Consumer<Throwable> onFailure) {

    }

    @Override
    public void updateTaskStatus(long taskId, GwtTaskStatus status,
                                 Consumer<Boolean> onSuccess, Consumer<Throwable> onFailure) {

    }

    public CommonModel getSavedModel() {
        return modelToSave;
    }

    public List<ModelTransition> getModelTransitionsToSave() {
        return modelTransitionsToSave;
    }

    @Override
    public void getCategoryWiki(long categoryId, Consumer<CategoryWiki> onSuccess,
                                Consumer<Throwable> onFailure) {
        if (categoryWiki != null) {
            onSuccess.accept(categoryWiki);
        } else if (categoryWikiThrowable != null) {
            onFailure.accept(categoryWikiThrowable);
        } else {
            onSuccess.accept(new CategoryWiki());
        }
    }

    @Override
    public void getModelMatchedOffers(long modelId, @Nullable Integer limit,
                                      Consumer<List<OfferData>> onSuccess, Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList()); // Mock it!
    }

    @Override
    public void getModificationMatchedOffers(long modificationId, @Nullable Integer limit,
                                             Consumer<List<OfferData>> onSuccess, Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList()); // Mock it!
    }

    @Override
    public void uploadImageFromUrl(
        String sourceUrl, String targetDir, Consumer<ImageFile> onSuccess,
        Consumer<Throwable> onFailure) {
        onSuccess.accept(new ImageFile());
    }

    @Override
    public void findEffectiveSku(long skuId, Consumer<ModelInfo> onSuccess, Consumer<Throwable> onFailure) {
        onSuccess.accept(requestedSku);
    }

    @Override
    public void findEffectiveSkus(long categoryId, long vendorId,
                                  boolean withPartnerModels,
                                  Consumer<List<ModelInfo>> onSuccess,
                                  Consumer<Throwable> onFailure) {
        onSuccess.accept(Collections.emptyList());
    }

    @Override
    public void getModelInfo(long modelId,
                             List<CommonModel.Source> types,
                             Consumer<ModelInfo> onSuccess,
                             Consumer<Throwable> onFailure) {
        onSuccess.accept(requestedModel);
    }

    @Override
    public void findGuruModelsOfVendor(long categoryId,
                                       long vendorId,
                                       Consumer<ModelInfosOfVendorResult> onSuccess,
                                       Consumer<Throwable> onFailure) {
        ModelInfosOfVendorResult result = new ModelInfosOfVendorResult(
            Collections.emptyList(), false, 0);
        onSuccess.accept(result);
    }

    @Override
    public void getMappings(long modelID, boolean isGuruModel,
                            Consumer<List<SupplierOffer>> onSuccess,
                            Consumer<Throwable> onFailure) {
        onSuccess.accept(mappings.stream()
            .filter(m -> m.getApprovedMapping().getSkuId() == modelID)
            .collect(Collectors.toList()));
    }

    @Override
    public void updateSkuMappings(List<SupplierOffer> mappingsToUpdate,
                                  MappingChangeType changeType,
                                  List<String> tickets,
                                  Consumer<List<MappingUpdateStatus>> onSuccess,
                                  Consumer<Throwable> onFailure) {
        onSuccess.accept(mappingsToUpdate.stream()
            .map(mapping -> {
                MappingUpdateStatus status = skuMappingUpdateStatusProvider.apply(mapping);
                if (!status.isFailure()) {
                    BusinessSkuKey updatedKey = BusinessSkuKey.from(mapping);
                    List<SupplierOffer> newMappings = mappings.stream()
                        .map(m -> {
                            BusinessSkuKey key = BusinessSkuKey.from(m);
                            if (updatedKey.equals(key)) {
                                return mapping;
                            } else {
                                return m;
                            }
                        }).collect(Collectors.toList());
                    mappings.clear();
                    mappings.addAll(newMappings);
                }
                return status;
            })
            .collect(Collectors.toList()));
    }

    public void setMappings(List<SupplierOffer> mappings) {
        this.mappings = new ArrayList<>(mappings);
    }

    public List<CommonModel> getRelatedModelsToSave() {
        return relatedModelsToSave;
    }

    @Override
    public void getAllChildrenModelPictureInfo(long categoryId, long modelId,
                                               Consumer<List<ModelPictureInfo>> onSuccess,
                                               Consumer<Throwable> onFailure) {
        if (modelPictureInfos != null) {
            onSuccess.accept(modelPictureInfos);
        } else if (modelPictureInfosThrowable != null) {
            onFailure.accept(modelPictureInfosThrowable);
        }
    }

    public List<ModelPictureInfo> getModelPictureInfos() {
        return modelPictureInfos;
    }

    public void setModelPictureInfos(List<ModelPictureInfo> modelPictureInfos) {
        this.modelPictureInfos = modelPictureInfos;
    }

    public void setModelPictureInfosThrowable(Throwable modelPictureInfosThrowable) {
        this.modelPictureInfosThrowable = modelPictureInfosThrowable;
    }

    @Override
    public void getEncodedModels(CommonModel model, List<CommonModel> relatedModels,
                                 Consumer<List<String>> onSuccess, Consumer<Throwable> onFailure) {

    }

    @Override
    public void saveEncodedModels(String modelJson, boolean asGroup, boolean force, Consumer<String> onSuccess,
                                  Consumer<Throwable> onFailure) {
    }
}
