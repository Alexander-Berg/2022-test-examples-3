package ru.yandex.market.aliasmaker.cache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.protobuf.format.JsonFormat;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.cache.models.CategoryModelsCacheFactory;
import ru.yandex.market.aliasmaker.cache.models.ModelService;
import ru.yandex.market.aliasmaker.cache.models.ModelsShadowReloadingCache;
import ru.yandex.market.aliasmaker.cache.offers.OfferService;
import ru.yandex.market.aliasmaker.cache.offers.OffersCache;
import ru.yandex.market.aliasmaker.cache.offers.SupplierOffersCache;
import ru.yandex.market.aliasmaker.cache.tovar_tree.TovarTreeCachingService;
import ru.yandex.market.aliasmaker.cache.users.UserService;
import ru.yandex.market.aliasmaker.cache.vendors.GlobalVendorsCache;
import ru.yandex.market.aliasmaker.cache.vendors.GlobalVendorsCacheImpl;
import ru.yandex.market.aliasmaker.cache.yang.AssignmentServiceMock;
import ru.yandex.market.aliasmaker.http.AliasMakerServiceImpl;
import ru.yandex.market.aliasmaker.models.User;
import ru.yandex.market.aliasmaker.offers.GDOfferUtils;
import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.aliasmaker.offers.matching.MatchingService;
import ru.yandex.market.aliasmaker.offers.matching.OffersMatchingService;
import ru.yandex.market.ir.FormalizerContractor;
import ru.yandex.market.ir.dao.CategoryCreator;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.processor.FormalizationStringIndexer;
import ru.yandex.market.mbo.export.CategoryParametersAddOptionsServiceStub;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.GlobalVendorsService;
import ru.yandex.market.mbo.http.GuruCategoryService;
import ru.yandex.market.mbo.http.MboGuruService;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.mbo.http.YangTaskEnums;
import ru.yandex.market.ir.skutcher2.shard_worker.config.SkuRuntimeConfig;
import ru.yandex.market.robot.db.ParameterValueComposer;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

import static ru.yandex.market.aliasmaker.TestFileUtils.load;
import static ru.yandex.market.aliasmaker.TestFileUtils.loadBytes;
import static ru.yandex.market.mbo.http.ModelStorage.ParameterValueType.ENUM_VALUE;
import static ru.yandex.market.mbo.http.ModelStorage.ParameterValueType.STRING_VALUE;

/**
 * @author Dmitry Kotelnikov <a href="mailto:kotelnikov@yandex-team.ru"></a>
 * @date 08.11.17
 */
@SuppressWarnings("CheckStyle:*")
@RunWith(SpringRunner.class)
@ContextConfiguration(
        classes = {SkuRuntimeConfig.class}
)
public class KnowledgeServiceTest extends Assert {
    private static final String WORKER_ID = "worker_id";
    private static final String ASSIGNMENT_ID = "Assignment_Id";
    private static final String TASK_ID = "task_id";
    private static final YangTaskEnums.YangTask TASK_TYPE = YangTaskEnums.YangTask.YANG_TASK_BLUE_LOGS;
    private static final int CATEGORY_ID = 91491;
    private static AtomicInteger newModelIdSequence = new AtomicInteger(-1);
    private static AtomicInteger newParamIdSequence = new AtomicInteger(-1);
    private User testUser = new User();
    private OffersMatchingService offerMatchingService;
    private MatchingService matchingService;
    private ModelService modelService;
    private GuruCategoryService guruCategoryService;
    private ModelStorageServiceStub modelStorageService;
    private ModelsShadowReloadingCache modelsShadowReloadingCache;
    private TovarTreeCachingService tovarTreeCachingService;

    @Autowired
    private FormalizerContractor formalizerContractor;
    @Autowired
    private IndexedStringFactory indexedStringFactory;

    @Autowired
    private FormalizationStringIndexer stringIndexer;

    @Autowired
    private CategoryCreator categoryCreator;

    private Map<String, Offer> loadOffers() {
        try {
            OffersStorage.GetOffersResponse.Builder builder = OffersStorage.GetOffersResponse.newBuilder();
            JsonFormat.merge(
                    new InputStreamReader(
                            getClass().getResourceAsStream("/offers.json")
                    ),
                    builder
            );
            HashMap<String, Offer> offers = new HashMap<>();
            for (OffersStorage.GenerationDataOffer offer : builder.getOffersList()) {
                offers.put(
                        offer.getClassifierMagicId(),
                        new Offer(
                                offer, new ArrayList<>(),
                                GDOfferUtils.parseOfferParams(offer.getOfferParams())
                        )
                );
            }
            return offers;
        } catch (IOException ex) {
            throw new RuntimeException("Can't read offers", ex);
        }
    }

    private KnowledgeService init() {
        Map<String, Offer> offers = loadOffers();

        KnowledgeService knowledgeService = new KnowledgeService();
        knowledgeService.setExecutorService(MoreExecutors.newDirectExecutorService());
        CategoryReloadingCache categoryCache = new CategoryReloadingCache();

        GlobalVendorsService globalVendorsService = Mockito.mock(GlobalVendorsService.class);
        Mockito.when(globalVendorsService.searchVendors(Mockito.any(MboVendors.SearchVendorsRequest.class)))
                .thenAnswer(invocation -> {
                    MboVendors.SearchVendorsRequest searchVendorsRequest =
                            invocation.getArgument(0, MboVendors.SearchVendorsRequest.class);
                    MboVendors.SearchVendorsResponse.Builder builder =
                            load("/vendors.json", MboVendors.SearchVendorsResponse.newBuilder());
                    int offset = searchVendorsRequest.getOffset();
                    if (offset >= builder.getVendorsCount()) {
                        builder.clearVendors();
                    } else {
                        List<MboVendors.GlobalVendor> vendors = builder.getVendorsList().subList(
                                offset,
                                Math.min(builder.getVendorsCount(), offset + searchVendorsRequest.getLimit())
                        );
                        builder.clearVendors();
                        builder.addAllVendors(vendors);
                    }
                    return builder.build();
                });
        GlobalVendorsCache globalVendorsCache = new GlobalVendorsCacheImpl(globalVendorsService);

        CategoryParametersServiceStub categoryParametersService = Mockito.mock(CategoryParametersServiceStub.class);
        Mockito.when(categoryParametersService.getParameters(Mockito.any())).thenAnswer(
                invocation -> {
                    String fileName = "/params.json";
                    MboParameters.GetCategoryParametersRequest req =
                            invocation.getArgument(0, MboParameters.GetCategoryParametersRequest.class);
                    if (req.getCategoryId() == 16309373) {
                        fileName = "/params_16309373.json";
                    }
                    return load(
                            fileName, MboParameters.GetCategoryParametersResponse.newBuilder()
                    ).build();
                }
        );
        CategoryParametersAddOptionsServiceStub categoryParametersAddOptionsService = Mockito
                .mock(CategoryParametersAddOptionsServiceStub.class);
        Mockito.when(categoryParametersAddOptionsService.addOptions(Mockito.any())).thenAnswer(
                invocation -> {
                    MboParameters.AddOptionsRequest request = invocation.getArgument(
                            0, MboParameters.AddOptionsRequest.class
                    );
                    List<MboParameters.ProcessedOption> processedOptions = new ArrayList<>();
                    for (int i = 0; i < request.getOptionCount(); i++) {
                        processedOptions.add(
                                MboParameters.ProcessedOption.newBuilder()
                                        .setOptionId(newParamIdSequence.decrementAndGet())
                                        .setParameterId(request.getParamId())
                                        .setVersionId(0)
                                        .build()
                        );
                    }
                    return MboParameters.AddOptionsResponse.newBuilder()
                            .setOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addAllOptions(processedOptions)
                            )
                            .build();
                }
        );
        Mockito.when(categoryParametersService.overrideOptions(Mockito.any())).thenAnswer(
                invocation -> {
                    MboParameters.OverrideOptionsRequest request = invocation.getArgument(
                            0, MboParameters.OverrideOptionsRequest.class
                    );
                    return MboParameters.OverrideOptionsResponse.newBuilder()
                            .addOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addOptions(
                                                    MboParameters.ProcessedOption.newBuilder()
                                                            .setVersionId(1)
                                                            .setParameterId(request.getParamId())
                                                            .setOptionId(request.getOptions(0).getOptionId())
                                            )
                            )
                            .build();
                }
        );
        Mockito.when(categoryParametersService.updateOption(Mockito.any())).thenAnswer(
                invocation -> {
                    MboParameters.UpdateOptionRequest request = invocation.getArgument(
                            0, MboParameters.UpdateOptionRequest.class
                    );
                    return MboParameters.UpdateOptionResponse.newBuilder()
                            .setOperationStatus(
                                    MboParameters.OperationStatus.newBuilder()
                                            .setStatus(MboParameters.OperationStatusType.OK)
                                            .addOptions(
                                                    MboParameters.ProcessedOption.newBuilder()
                                                            .setOptionId(request.getOptionId())
                                                            .setParameterId(request.getParamId())
                                                            .setVersionId(request.getExpectedVersionId() + 1)
                                            )
                            )
                            .build();
                }
        );
        guruCategoryService = Mockito.mock(GuruCategoryService.class);
        Mockito.when(guruCategoryService.updateGuruCategoryInternals(Mockito.any())).thenAnswer(invocation ->
                MboGuruService.UpdateGuruCategoryInternalsResponse.newBuilder()
                        .setStatus(MboGuruService.OperationStatusType.OK)
                        .build()
        );
        categoryCache.setGuruCategoryService(guruCategoryService);
        modelStorageService = Mockito.mock(ModelStorageServiceStub.class);
        Mockito.when(modelStorageService.findModels(Mockito.any())).thenAnswer(
                invocation -> {
                    ModelStorage.FindModelsRequest req = invocation.getArgument(0,
                            ModelStorage.FindModelsRequest.class);
                    if (req.getModelType() == ModelStorage.ModelType.SKU) {
                        return load(
                                "/skus.json", ModelStorage.GetModelsResponse.newBuilder()
                        ).build();
                    } else {
                        return load(
                                "/models.json", ModelStorage.GetModelsResponse.newBuilder()
                        ).build();
                    }
                }
        );
        Mockito.when(modelStorageService.getModels(Mockito.any())).thenAnswer(
                invocation -> load(
                        "/models.json", ModelStorage.GetModelsResponse.newBuilder()
                ).build()
        );
        Mockito.when(modelStorageService.saveModels(Mockito.any())).thenAnswer(
                invocation -> {
                    ModelStorage.SaveModelsRequest request = invocation.getArgument(
                            0, ModelStorage.SaveModelsRequest.class
                    );
                    ModelStorage.Model model = request.getModels(0);
                    long newModelId = model.getId();
                    if (newModelId == 0) {
                        newModelId = newModelIdSequence.getAndDecrement();
                    }
                    ModelStorage.Model.Builder modelBuilder = model.toBuilder();
                    modelBuilder.setModifiedTs(model.getModifiedTs() + 1);
                    modelBuilder.setId(newModelId);
                    return ModelStorage.OperationResponse.newBuilder()
                            .addStatuses(
                                    ModelStorage.OperationStatus.newBuilder()
                                            .setStatus(ModelStorage.OperationStatusType.OK)
                                            .setModel(modelBuilder)
                                            .setModelId(newModelId)
                                            .setType(model.getId() == 0 ? ModelStorage.OperationType.CREATE :
                                                    ModelStorage.OperationType.CHANGE)
                                            .build()
                            )
                            .build();
                }
        );
        Mockito.when(modelStorageService.saveModelsGroup(Mockito.any())).thenAnswer(
                invocation -> {
                    ModelCardApi.SaveModelsGroupRequest request = invocation.getArgument(
                            0, ModelCardApi.SaveModelsGroupRequest.class
                    );
                    ModelCardApi.SaveModelsGroupOperationResponse.Builder groupResponse =
                            ModelCardApi.SaveModelsGroupOperationResponse.newBuilder()
                                    .setStatus(ModelStorage.OperationStatusType.OK);

                    for (ModelStorage.Model model : request.getModelsRequest(0).getModelsList()) {
                        long newModelId = model.getId();
                        if (newModelId == 0) {
                            newModelId = newModelIdSequence.getAndDecrement();
                        }
                        ModelStorage.Model.Builder modelBuilder = model.toBuilder();
                        modelBuilder.setModifiedTs(model.getModifiedTs() + 1);
                        modelBuilder.setId(newModelId);
                        groupResponse.addRequestedModelsStatuses(
                                ModelStorage.OperationStatus.newBuilder()
                                        .setStatus(ModelStorage.OperationStatusType.OK)
                                        .setModel(modelBuilder)
                                        .setModelId(newModelId)
                                        .setType(model.getId() == 0 ? ModelStorage.OperationType.CREATE :
                                                ModelStorage.OperationType.CHANGE)
                                        .build()
                        );
                    }
                    return ModelCardApi.SaveModelsGroupResponse.newBuilder()
                            .addResponse(groupResponse)
                            .build();
                }
        );

        Mockito.when(modelStorageService.removeModels(Mockito.any())).thenAnswer(
                invocation -> {
                    ModelStorage.RemoveModelsRequest request = invocation.getArgument(
                            0, ModelStorage.RemoveModelsRequest.class
                    );
                    ModelStorage.Model model = request.getModels(0);
                    long newModelId = model.getId();
                    if (newModelId == 0) {
                        newModelId = newModelIdSequence.getAndDecrement();
                    }
                    ModelStorage.Model.Builder modelBuilder = model.toBuilder();
                    modelBuilder.setModifiedTs(model.getModifiedTs() + 1);
                    modelBuilder.setId(newModelId);
                    modelBuilder.setDeleted(true);
                    return ModelStorage.OperationResponse.newBuilder()
                            .addStatuses(
                                    ModelStorage.OperationStatus.newBuilder()
                                            .setStatus(ModelStorage.OperationStatusType.OK)
                                            .setModel(modelBuilder)
                                            .setModelId(newModelId)
                                            .setType(model.getId() == 0 ? ModelStorage.OperationType.CREATE :
                                                    ModelStorage.OperationType.CHANGE)
                                            .build()
                            )
                            .build();
                }
        );

        Mockito.when(modelStorageService.uploadDetachedImages(Mockito.any())).thenAnswer(
                invocation -> {
                    ModelStorage.UploadDetachedImagesRequest request = invocation.getArgument(0,
                            ModelStorage.UploadDetachedImagesRequest.class);
                    ModelStorage.UploadDetachedImagesResponse.Builder responseBuilder =
                            ModelStorage.UploadDetachedImagesResponse.newBuilder();

                    for (ModelStorage.ImageData imageData : request.getImageDataList()) {
                        ModelStorage.DetachedImageStatus.Builder image = ModelStorage.DetachedImageStatus.newBuilder();
                        if (!imageData.hasUrl()) {
                            responseBuilder.addUploadedImage(image
                                    .setStatus(ModelStorage.OperationStatus.newBuilder()
                                            .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                                            .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                                            .setStatusMessage("No url!")
                                    ));
                        } else {
                            boolean isValidImage = validateImage(imageData);
                            if (isValidImage) {
                                responseBuilder.addUploadedImage(image
                                        .setPicture(ModelStorage.Picture.newBuilder()
                                                .setUrlOrig(imageData.getUrl())
                                                .setWidth(1)
                                                .setHeight(1)
                                                .setUrl(imageData.getUrl() + "_uploaded"))
                                        .setStatus(ModelStorage.OperationStatus.newBuilder()
                                                .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                                                .setStatus(ModelStorage.OperationStatusType.OK)));
                            } else {
                                responseBuilder.addUploadedImage(image
                                        .setStatus(ModelStorage.OperationStatus.newBuilder()
                                                .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                                                .setStatus(ModelStorage.OperationStatusType.VALIDATION_ERROR)
                                                .setStatusMessage("Invalid image data!")
                                        ));
                            }
                        }
                    }
                    return responseBuilder.build();
                }
        );

        categoryCache.setGlobalVendorsCache(globalVendorsCache);
        categoryCache.setCategoryParametersService(categoryParametersService);
        categoryCache.setCategoryParametersAddOptionsService(categoryParametersAddOptionsService);
        globalVendorsCache.getGlobalVendors();

        OffersCache offersCache = Mockito.mock(OffersCache.class);
        Mockito.when(
                offersCache.getOffers(Mockito.anyInt(), Mockito.anyCollection())
        ).thenAnswer(invocation ->
                invocation.getArgument(1, Collection.class).stream()
                        .map(offers::get)
                        .collect(Collectors.toList())
        );
        SupplierOffersCache supplierOffersCache = Mockito.mock(SupplierOffersCache.class);
        Mockito.when(
                supplierOffersCache.getOffers(Mockito.anyCollection())
        ).thenAnswer(invocation ->
                invocation.getArgument(0, Collection.class).stream()
                        .map(String::valueOf)
                        .filter(i -> offers.containsKey(i))
                        .map(offers::get)
                        .collect(Collectors.toList())
        );
        OfferService offerService = new OfferService();
        offerService.setOffersCache(offersCache);
        offerService.setSupplierOffersCache(supplierOffersCache);

        modelsShadowReloadingCache = new ModelsShadowReloadingCache();
        modelsShadowReloadingCache.setModelStorageService(modelStorageService);
        modelsShadowReloadingCache.setCategoryCache(categoryCache);
        modelsShadowReloadingCache.setCacheFactory(new CategoryModelsCacheFactory(Collections.emptyList(), null, null));

        offerMatchingService = new OffersMatchingService();

        matchingService = new MatchingService(modelsShadowReloadingCache,
                globalVendorsCache,
                categoryCache,
                MoreExecutors.newDirectExecutorService(),
                formalizerContractor,
                indexedStringFactory,
                stringIndexer,
                categoryCreator,
                null);

        offerMatchingService.setMatchingService(matchingService);
        offerMatchingService.setOfferService(offerService);
        offerMatchingService.setGlobalVendorsCache(globalVendorsCache);
        offerMatchingService.setModelsCache(modelsShadowReloadingCache);

        modelService = new ModelService();
        modelService.setModelsCache(modelsShadowReloadingCache);
        modelService.setCategoryCache(categoryCache);
        modelService.setModelStorageService(modelStorageService);

        knowledgeService.setCategoryCache(categoryCache);
        knowledgeService.setGlobalVendorsCache(globalVendorsCache);
        knowledgeService.setOfferService(offerService);
        knowledgeService.setMatchingService(matchingService);
        knowledgeService.setModelService(modelService);
        ReflectionTestUtils.setField(matchingService, "skutcherMaxQueue", 2);
        return knowledgeService;
    }

    private boolean validateImage(ModelStorage.ImageData imageData) throws IOException {
        if (imageData.hasContentBytes()) {
            byte[] imageBytes = imageData.getContentBytes().toByteArray();
            ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
            ImageReader imageReader = ImageIO.getImageReaders(stream).next();
            return imageReader.getFormatName().toLowerCase().equals("jpeg");
        } else {
            return true;
        }
    }

    private void assertEqualsState(List<AliasMaker.OfferMatchState> expected, List<AliasMaker.OfferMatchState> actual) {
        if (expected.size() != actual.size()) {
            throw new ComparisonFailure(
                    "Match state wrong", toString(expected), toString(actual)
            );
        }
        for (int i = 0; i < expected.size(); i++) {
            AliasMaker.OfferMatchState expectedValue = expected.get(i);
            AliasMaker.OfferMatchState actualValue = actual.get(i);
            Assert.assertTrue(actualValue.hasOffer()); //check actual offer is returned
            if (!Objects.equals(expectedValue.getMatchType(), actualValue.getMatchType()) ||
                    !Objects.equals(getModel(expectedValue), getModel(actualValue)) ||
                    !Objects.equals(getVendor(expectedValue), getVendor(actualValue)) ||
                    !Objects.equals(getSku(expectedValue), getSku(actualValue))) {
                throw new ComparisonFailure(
                        "Match state wrong:" + i, toString(expected), toString(actual)
                );
            }
        }
    }

    private String toString(List<AliasMaker.OfferMatchState> states) {
        StringBuilder result = new StringBuilder();
        for (AliasMaker.OfferMatchState state : states) {
            result
                    .append("{\n\tmatch_type:")
                    .append(state.getMatchType()).append(",\n")
                    .append("\tmodelId:").append(getModel(state)).append(",\n")
                    .append("\tvendorId:").append(getVendor(state)).append(",\n")
                    .append("\tskuId:").append(getSku(state))
                    .append("\n};\n");
        }
        return result.toString();
    }

    private long getModel(AliasMaker.OfferMatchState value) {
        if (value.hasModel()) {
            return value.getModel().getId();
        }
        return 0;
    }

    private long getSku(AliasMaker.OfferMatchState value) {
        if (value.hasSku()) {
            return value.getSku().getId();
        }
        return 0;
    }

    private int getVendor(AliasMaker.OfferMatchState value) {
        if (value.hasVendor()) {
            return value.getVendor().getVendorId();
        }
        return 0;
    }

    private AliasMaker.OfferMatchState buildMatch(Matcher.MatchType matchType, int vendorId, long modelId) {
        return buildMatch(matchType, vendorId, modelId, 0);
    }

    private AliasMaker.OfferMatchState buildMatch(Matcher.MatchType matchType, int vendorId, long modelId, long skuId) {
        return AliasMaker.OfferMatchState.newBuilder()
                .setMatchType(matchType)
                .setModel(
                        ModelStorage.Model.newBuilder()
                                .setId(modelId)
                )
                .setVendor(
                        AliasMaker.Vendor.newBuilder()
                                .setVendorId(vendorId)
                )
                .setSku(
                        ModelStorage.Model.newBuilder()
                                .setId(skuId)
                )
                .build();
    }

    @Test
    public void testUpdateCutOffWords() {
        KnowledgeService knowledgeService = init();
        knowledgeService.updateCutOffWord(1, Arrays.asList("aeatk", "vepkj"), 0, testUser, TASK_ID);
        Mockito.verify(guruCategoryService).updateGuruCategoryInternals(
                Mockito.eq(MboGuruService.UpdateGuruCategoryInternalsRequest.newBuilder()
                        .setCategory(
                                MboGuruService.GuruCategoryInternals.newBuilder()
                                        .setCategoryId(1)
                                        .setVersionId(0)
                                        .addAllCutOffWord(Arrays.asList("aeatk", "vepkj"))
                                        .build()
                        )
                        .setTaskId(TASK_ID)
                        .setUid(testUser.getUserId())
                        .build()
                )
        );
    }

    @Test
    public void testCheckCutOffWordsSync() {
        KnowledgeService knowledgeService = new KnowledgeService() {
            @Override
            protected AliasMaker.CheckCutOffWordResponse doCheckCutOffWords(
                    AliasMaker.CheckCutOffWordRequest request) {
                return AliasMaker.CheckCutOffWordResponse.newBuilder()
                        .setOffersWithBadWordCount(13)
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .build();
            }
        };
        AtomicBoolean startInstantly = new AtomicBoolean(true);
        mockAsyncExecution(startInstantly, new ArrayList<>(), knowledgeService::setExecutorService);

        AliasMaker.CheckCutOffWordResponse response = knowledgeService.checkCutOffWord(
                AliasMaker.CheckCutOffWordRequest.newBuilder()
                        .setCategoryId(1)
                        .setLimit(10)
                        .setOffset(1)
                        .build());
        Assertions.assertThat(response.getOfferCount()).isEqualTo(2);
        Assertions.assertThat(response.getOffersWithBadWordCount()).isEqualTo(13);
    }

    @Test
    public void testCheckCutOffWordsAsyncIfThrownException() {
        AtomicBoolean throwException = new AtomicBoolean(true);
        KnowledgeService knowledgeService = new KnowledgeService() {
            @Override
            protected AliasMaker.CheckCutOffWordResponse doCheckCutOffWords(
                    AliasMaker.CheckCutOffWordRequest request) {
                if (throwException.get()) {
                    throw new RuntimeException("q");
                }
                return AliasMaker.CheckCutOffWordResponse.newBuilder()
                        .setOffersWithBadWordCount(13)
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .build();
            }
        };
        List<FutureTask<Void>> tasks = new ArrayList<>();
        AtomicBoolean startInstantly = new AtomicBoolean(false);
        mockAsyncExecution(startInstantly, tasks, knowledgeService::setExecutorService);

        AliasMaker.CheckCutOffWordRequest checkCutOffWordRequest = AliasMaker.CheckCutOffWordRequest.newBuilder()
                .setCategoryId(1)
                .setLimit(10)
                .setAsync(true)
                .build();
        AliasMaker.CheckCutOffWordResponse response = knowledgeService.checkCutOffWord(
                checkCutOffWordRequest);
        Assertions.assertThat(response.getReady()).isEqualTo(false);
        tasks.get(0).run();
        boolean exceptionThrown = false;
        try {
            knowledgeService.checkCutOffWord(checkCutOffWordRequest);
        } catch (Exception e) {
            exceptionThrown = true;
        }
        Assertions.assertThat(exceptionThrown).isTrue();
        throwException.set(false);
        startInstantly.set(true);
        response = knowledgeService.checkCutOffWord(
                checkCutOffWordRequest);
        Assertions.assertThat(response.getReady()).isEqualTo(true);
    }

    @Test(timeout = 2000)
    public void testCheckCutOffWordsAsync() {
        KnowledgeService knowledgeService = new KnowledgeService() {
            @Override
            protected AliasMaker.CheckCutOffWordResponse doCheckCutOffWords(
                    AliasMaker.CheckCutOffWordRequest request) {
                return AliasMaker.CheckCutOffWordResponse.newBuilder()
                        .setOffersWithBadWordCount(13)
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .addOffer(AliasMaker.Offer.getDefaultInstance())
                        .build();
            }
        };
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(new AtomicBoolean(false), tasks, knowledgeService::setExecutorService);
        AliasMaker.CheckCutOffWordRequest request = AliasMaker.CheckCutOffWordRequest.newBuilder()
                .setCategoryId(1)
                .setLimit(2)
                .setAsync(true)
                .build();

        AliasMaker.CheckCutOffWordResponse response = knowledgeService.checkCutOffWord(request);
        Assertions.assertThat(response.getReady()).isEqualTo(false);
        tasks.forEach(t -> t.run());

        response = knowledgeService.checkCutOffWord(request);
        Assertions.assertThat(response.getOfferCount()).isEqualTo(2);
        Assertions.assertThat(response.getOffersWithBadWordCount()).isEqualTo(13);
        Assertions.assertThat(response.getReady()).isEqualTo(true);

        response = knowledgeService.checkCutOffWord(request.toBuilder().setOffset(2).build());
        Assertions.assertThat(response.getOfferCount()).isEqualTo(1);
        Assertions.assertThat(response.getOffersWithBadWordCount()).isEqualTo(13);
        Assertions.assertThat(response.getReady()).isEqualTo(true);
    }

    @Test
    public void testUpdate() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "2"
        );
        /*
         * Проверяем начальное состояние
         */
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.NO_MATCH, 0, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );
        /*
         * Добавляем алиас вендора - предложение должно начать матчится до вендора
         */
        knowledgeService.updateVendor(
                AliasMaker.Vendor.newBuilder()
                        .setVendorId(153043)
                        .setCategoryId(91491)
                        .setVersionId(System.currentTimeMillis())
                        .addAlias(
                                MboParameters.Word.newBuilder()
                                        .setName("Эппл")
                        )
                        .build(),
                testUser,
                TASK_ID
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_VENDOR, 153043, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
        AliasMaker.GetModelsResponse response = modelService.getModels(
                AliasMaker.GetModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .addModelId(1)
                        .build()
        );
        ModelStorage.Model.Builder model = response.getModel(0).toBuilder();
        model.addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                        .setParamId(ParameterValueComposer.ALIASES_ID)
                        .setXslName(ParameterValueComposer.ALIASES)
                        .setTypeId(STRING_VALUE)
                        .addStrValue(
                                ModelStorage.LocalizedString.newBuilder()
                                        .setValue("iPhone 7 золотистый 128GB")
                        )
                        .addStrValue(
                                ModelStorage.LocalizedString.newBuilder()
                                        .setValue("iPhone 7 128GB")
                        )
        );
        /*
         * Добавляем алиас модели - предложение должно начать матчится до модели
         */
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(model.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
        offerIds = new ArrayList<>(offerIds);
        offerIds.add("3");
        /*
         * Пробуем добавить вендора
         */
        knowledgeService.updateVendor(
                AliasMaker.Vendor.newBuilder()
                        .setVendorId(152809)
                        .setCategoryId(91491)
                        .addAlias(
                                MboParameters.Word.newBuilder()
                                        .setName("Самсунг")
                        )
                        .build(),
                testUser,
                TASK_ID
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_VENDOR, 152809, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
        /*
         * Пробуем добавить модель
         */
        model = ModelStorage.Model.newBuilder()
                .setVendorId(152809)
                .addTitles(
                        ModelStorage.LocalizedString.newBuilder()
                                .setValue("Galaxy S7 128GB")
                )
                .setCurrentType(ModelStorage.ModelType.GURU.name())
                .setSourceType(ModelStorage.ModelType.GURU.name())
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setXslName(ParameterValueComposer.NAME)
                                .setParamId(ParameterValueComposer.NAME_ID)
                                .addStrValue(
                                        ModelStorage.LocalizedString.newBuilder()
                                                .setValue("Galaxy S7 128GB")
                                )
                )
                .setCategoryId(91491)
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setParamId(ParameterValueComposer.BARCODE_ID)
                                .setXslName(ParameterValueComposer.BARCODE)
                                .addStrValue(
                                        ModelStorage.LocalizedString.newBuilder()
                                                .setValue("12345678")
                                )
                )
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setParamId(ParameterValueComposer.ALIASES_ID)
                                .setXslName(ParameterValueComposer.ALIASES)
                                .setTypeId(STRING_VALUE)
                                .addStrValue(
                                        ModelStorage.LocalizedString.newBuilder()
                                                .setValue("Galaxy S7 128GB")
                                )
                );
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.ADD,
                Collections.singletonList(model.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.TASK, 152809, -1)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
        /*
         * Пробуем удалить модель
         */
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.REMOVE,
                Collections.singletonList(model.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1),
                        buildMatch(Matcher.MatchType.MATCH_VENDOR, 152809, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
    }

    @Test
    public void testSkutching() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(
                        ModelStorage.Model.newBuilder()
                                .setCategoryId(91491)
                                .setVendorId(153043)
                                .setCurrentType(ModelStorage.ModelType.SKU.name())
                                .setSourceType(ModelStorage.ModelType.SKU.name())
                                .addParameterValues(
                                        ModelStorage.ParameterValue.newBuilder()
                                                .setParamId(14871214)
                                                .setXslName("color_vendor")
                                                .setTypeId(ENUM_VALUE)
                                                .setOptionId(14896084)
                                                .setValueType(MboParameters.ValueType.ENUM)
                                )
                                .addRelations(
                                        ModelStorage.Relation.newBuilder()
                                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                                .setId(1)
                                                .setCategoryId(91491)
                                )
                                .setId(5)
                                .build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );
        /*
         * Проверяем skutching состояние
         */
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 5),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
        /*
         * Добавляем новое значение
         */
        AliasMaker.AddParamValueResponse response = knowledgeService.addParamValue(
                AliasMaker.AddParamValueRequest.newBuilder()
                        .setCategoryId(91491)
                        .setParamId(14871214)
                        .addOption(
                                MboParameters.Option.newBuilder()
                                        .setPublished(true)
                                        .addName(
                                                MboParameters.Word.newBuilder()
                                                        .setName("золотистый")
                                                        .setLangId(225)
                                        )
                                        .addAlias(
                                                MboParameters.EnumAlias.newBuilder()
                                                        .setType(MboParameters.EnumAlias.Type.GENERAL)
                                                        .setAlias(
                                                                MboParameters.Word.newBuilder()
                                                                        .setName("золотистый")
                                                                        .setLangId(225)
                                                        )
                                        )
                        )
                        .build(), testUser);
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(
                        ModelStorage.Model.newBuilder()
                                .setCategoryId(91491)
                                .setVendorId(153043)
                                .setCurrentType(ModelStorage.ModelType.SKU.name())
                                .setSourceType(ModelStorage.ModelType.SKU.name())
                                .addParameterValues(
                                        ModelStorage.ParameterValue.newBuilder()
                                                .setParamId(14871214)
                                                .setXslName("color_vendor")
                                                .setTypeId(ENUM_VALUE)
                                                .setOptionId(Math.toIntExact(response.getOption(0).getId()))
                                                .setValueType(MboParameters.ValueType.ENUM)
                                )
                                .addRelations(
                                        ModelStorage.Relation.newBuilder()
                                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                                .setId(1)
                                                .setCategoryId(91491)
                                )
                                .setId(6)
                                .build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 5),
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 6)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
    }

    @Test
    public void testSkutchingToIsSkuModel() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Collections.singletonList("1");

        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );

        AliasMaker.GetModelsRequest request = AliasMaker.GetModelsRequest.newBuilder()
                .setCategoryId(91491)
                .setVendorId(153043)
                .addModelId(1)
                .build();

        ModelStorage.Model model = modelService.getModels(request).getModel(0);

        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(model.toBuilder()
                        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                .setParamId(54234234)
                                .setXslName("IsSku")
                                .setValueType(MboParameters.ValueType.BOOLEAN)
                                .setBoolValue(true)
                        ).build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );

        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 1)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );

        //remove IsSku
        model = modelService.getModels(request).getModel(0);
        List<ModelStorage.ParameterValue> parameterValues = new ArrayList<>(model.getParameterValuesList());
        parameterValues.removeIf(pv -> pv.getXslName().equals("IsSku"));
        model = model.toBuilder()
                .clearParameterValues()
                .addAllParameterValues(parameterValues)
                .build();

        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(model),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );

        //Not skutched now
        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );
    }

    @Test
    public void testSkutchingToSkuFromAnotherModel() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Collections.singletonList("1");

        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );

        ModelStorage.Model.Builder skuBuilder = ModelStorage.Model.newBuilder()
                .setCategoryId(91491)
                .setVendorId(153043)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setParamId(ParameterValueComposer.BARCODE_ID)
                                .setXslName(ParameterValueComposer.BARCODE)
                                .addStrValue(
                                        ModelStorage.LocalizedString.newBuilder()
                                                .setValue("424242424242424242")
                                )
                )
                .addRelations(
                        ModelStorage.Relation.newBuilder()
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                .setId(123123213)
                                .setCategoryId(91491)
                )
                .setId(6);

        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Arrays.asList(
                        ModelStorage.Model.newBuilder()
                                .setCategoryId(91491)
                                .setVendorId(153043)
                                .addTitles(ModelStorage.LocalizedString.newBuilder()
                                        .setValue("QQ")
                                        .setIsoCode("ru")
                                )
                                .setCurrentType(ModelStorage.ModelType.GURU.name())
                                .addRelations(
                                        ModelStorage.Relation.newBuilder()
                                                .setType(ModelStorage.RelationType.SKU_MODEL)
                                                .setId(6)
                                                .setCategoryId(91491)
                                )
                                .setId(123123213)
                                .build(),
                        skuBuilder.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );

        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.BARCODE_MATCH, 153043, 123123213, 6)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );

        skuBuilder.clearParameterValues();

        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Collections.singletonList(skuBuilder.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );

        assertEqualsState(
                Collections.singletonList(buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0)),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );
    }

    @Test
    public void testSckutchingIfMatchingToVendor() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "2"
        );
        /*
         * Проверяем начальное состояние
         */
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0),
                        buildMatch(Matcher.MatchType.NO_MATCH, 0, 0, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, AliasMaker.ReloadState.getDefaultInstance())
        );
        /*
         * Добавляем алиас вендора - предложение должно начать матчится до вендора
         */
        knowledgeService.updateVendor(
                AliasMaker.Vendor.newBuilder()
                        .setVendorId(153043)
                        .setCategoryId(91491)
                        .setVersionId(System.currentTimeMillis())
                        .addAlias(
                                MboParameters.Word.newBuilder()
                                        .setName("Эппл")
                        )
                        .build(),
                testUser,
                TASK_ID
        );
        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0),
                        buildMatch(Matcher.MatchType.MATCH_VENDOR, 153043, 0, 0)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );

        ModelStorage.Model.Builder skuBuilder = ModelStorage.Model.newBuilder()
                .setCategoryId(91491)
                .setVendorId(153043)
                .setCurrentType(ModelStorage.ModelType.SKU.name())
                .setSourceType(ModelStorage.ModelType.SKU.name())
                .addParameterValues(
                        ModelStorage.ParameterValue.newBuilder()
                                .setParamId(ParameterValueComposer.VENDOR_CODE_ID)
                                .setXslName(ParameterValueComposer.VENDOR_CODE)
                                .addStrValue(
                                        ModelStorage.LocalizedString.newBuilder()
                                                .setValue("VendorCode111").setIsoCode("ru")
                                )
                )
                .addRelations(
                        ModelStorage.Relation.newBuilder()
                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                .setId(123123213)
                                .setCategoryId(91491)
                )
                .setId(6);

        /*
         * Добавляем sku c vendor code
         */
        knowledgeService.updateModelsGroup(
                AliasMaker.Action.UPDATE,
                Arrays.asList(
                        ModelStorage.Model.newBuilder()
                                .setCategoryId(91491)
                                .setVendorId(153043)
                                .addTitles(ModelStorage.LocalizedString.newBuilder()
                                        .setValue("QQ")
                                        .setIsoCode("ru")
                                )
                                // матчер теперь умеет в вендор коды
                                .addParameterValues(
                                        ModelStorage.ParameterValue.newBuilder()
                                                .setParamId(ParameterValueComposer.ALIASES_ID)
                                                .setXslName(ParameterValueComposer.ALIASES)
                                                .addStrValue(
                                                        ModelStorage.LocalizedString.newBuilder()
                                                                .setValue("VendorCode111").setIsoCode("ru")
                                                )
                                )
                                .setCurrentType(ModelStorage.ModelType.GURU.name())
                                .addRelations(
                                        ModelStorage.Relation.newBuilder()
                                                .setType(ModelStorage.RelationType.SKU_MODEL)
                                                .setId(6)
                                                .setCategoryId(91491)
                                )
                                .setId(123123213)
                                .build(),
                        skuBuilder.build()),
                true,
                testUser,
                TASK_TYPE,
                TASK_ID,
                false,
                false
        );

        assertEqualsState(
                Arrays.asList(
                        buildMatch(Matcher.MatchType.MATCH_OK, 153043, 1, 0),
                        buildMatch(Matcher.MatchType.SUPER_PARAM_MATCH, 153043, 123123213, 6)
                ),
                offerMatchingService.matchOffers(91491, offerIds, AliasMaker.OfferType.OFFER,
                        knowledgeService, null)
        );
    }

    @Test
    public void testMatchingToModification() {
        KnowledgeService knowledgeService = init();
        AliasMaker.OfferMatchState matchState = offerMatchingService.matchOffers(16309373,
                Collections.singletonList("5"), AliasMaker.OfferType.OFFER,
                knowledgeService, AliasMaker.ReloadState.getDefaultInstance()).get(0);

        assertEquals(Matcher.MatchType.MODIFICATION_TASK, matchState.getMatchType());
        assertEquals(1722737446, matchState.getModel().getId());
        assertEquals(503981082, matchState.getModification().getId());
    }

    @Test
    public void testAsyncReloadState() {
        init();
        mockAsyncExecution(new AtomicBoolean(), new ArrayList<>());
        //init finish
        AsyncReloadState asyncReloadState = matchingService.onCategoryChange(CATEGORY_ID);
        assertNotNull(asyncReloadState);
        assertEquals(CATEGORY_ID, asyncReloadState.getCategoryId());
        assertEquals(1, asyncReloadState.getFutures().size());
        assertFalse(asyncReloadState.getFutures().get(0).isDone());
        AliasMaker.ReloadState reloadState = asyncReloadState.getReloadState();
        assertNotNull(reloadState);
        assertFalse(reloadState.hasSkutcherReloadTimestamp());
        assertTrue(reloadState.getMatcherReloadTimestamp() > 0);

        asyncReloadState = matchingService.onCategoryChange(CATEGORY_ID);
        assertEquals(CATEGORY_ID, asyncReloadState.getCategoryId());
        assertEquals(1, asyncReloadState.getFutures().size());
        reloadState = asyncReloadState.getReloadState();
        assertTrue(reloadState.hasMatcherReloadTimestamp());

        asyncReloadState = matchingService.onCategoryChange(CATEGORY_ID);
        reloadState = asyncReloadState.getReloadState();
        assertEquals(1, asyncReloadState.getFutures().size());
        assertTrue(reloadState.getMatcherReloadTimestamp() > 0);
    }

    @Test
    public void testInvalidate() {
        init();
        AtomicBoolean runInstantly = new AtomicBoolean();
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);
        //init finish

        //init matcher and skutcher
        runInstantly.set(true);
        assertNotNull(matchingService.getCurrentSkutcher(CATEGORY_ID));
        Assertions.assertThat(matchingService.statisticByCategory()).isNotEmpty();
        matchingService.invalidate(List.of(CATEGORY_ID));
        Assertions.assertThat(matchingService.statisticByCategory()).isEmpty();
    }

    @Test
    public void testAsyncMatching() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        AtomicBoolean runInstantly = new AtomicBoolean();
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);
        //init finish

        //init matcher and skutcher
        runInstantly.set(true);
        assertNotNull(matchingService.getCurrentMatcher(CATEGORY_ID));
        assertNotNull(matchingService.getCurrentSkutcher(CATEGORY_ID));
        assertNotNull(matchingService.getUpdatedMatcherOrNull(CATEGORY_ID, null));
        assertNotNull(matchingService.getUpdatedSkutcherOrNull(CATEGORY_ID, null));

        runInstantly.set(false);
        AsyncReloadState asyncReloadState = matchingService.onCategoryChange(CATEGORY_ID);
        AliasMaker.ReloadState reloadState = asyncReloadState.getReloadState();
        assertNull(matchingService.getUpdatedMatcherOrNull(CATEGORY_ID, reloadState));
        assertNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds, AliasMaker.OfferType.OFFER,
                knowledgeService, reloadState));
        //nevertheless matching without reloadState works
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER,
                knowledgeService, AliasMaker.ReloadState.getDefaultInstance()));
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER, knowledgeService, null));

        //finishing matcher reload
        tasks.stream().filter(t -> !t.isDone()).forEach(FutureTask::run);
        assertNotNull(matchingService.getUpdatedMatcherOrNull(CATEGORY_ID, reloadState));
        assertNotNull(matchingService.getUpdatedMatcherOrNull(CATEGORY_ID, null));
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER,
                knowledgeService, reloadState));
    }

    @Test
    public void testAsyncSkutching() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        AtomicBoolean runInstantly = new AtomicBoolean();
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);
        //init finish

        //init matcher and skutcher
        runInstantly.set(true);
        assertNotNull(matchingService.getCurrentMatcher(CATEGORY_ID));
        assertNotNull(matchingService.getCurrentSkutcher(CATEGORY_ID));
        assertNotNull(matchingService.getUpdatedMatcherOrNull(CATEGORY_ID, null));
        assertNotNull(matchingService.getUpdatedSkutcherOrNull(CATEGORY_ID, null));

        runInstantly.set(false);
        AsyncReloadState asyncReloadState = matchingService.onCategoryChange(CATEGORY_ID);
        AliasMaker.ReloadState reloadState = asyncReloadState.getReloadState();
        assertNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds, AliasMaker.OfferType.OFFER,
                knowledgeService, reloadState));
        //nevertheless skutching without reloadState works
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER,
                knowledgeService, AliasMaker.ReloadState.getDefaultInstance()));
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER,
                knowledgeService, null));

        //finishing matcher reload
        tasks.stream().filter(t -> !t.isDone()).forEach(FutureTask::run);
        assertNotNull(matchingService.getUpdatedSkutcherOrNull(CATEGORY_ID, reloadState));
        assertNotNull(matchingService.getUpdatedSkutcherOrNull(CATEGORY_ID, null));
        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER,
                knowledgeService, reloadState));
    }

    @Test
    public void testAsyncUpdateWhenIncorrectState() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        AtomicBoolean runInstantly = new AtomicBoolean();
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);
        //init finish

        AliasMaker.ReloadState reloadState = AliasMaker.ReloadState.newBuilder()
                .setMatcherReloadTimestamp(System.currentTimeMillis())
                .setSkutcherReloadTimestamp(System.currentTimeMillis())
                .build();

        assertNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER, knowledgeService, reloadState));
        assertEquals(1, tasks.size());
        tasks.get(0).run();
        tasks.clear();

        assertNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER, knowledgeService, reloadState));
        assertEquals(1, tasks.size());
        tasks.get(0).run();

        assertNotNull(offerMatchingService.matchOffers(CATEGORY_ID, offerIds,
                AliasMaker.OfferType.OFFER, knowledgeService, reloadState));
    }

    @Test
    public void testAsyncUpdates() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        AtomicBoolean runInstantly = new AtomicBoolean();
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);

        UserService userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByWorkerId(Mockito.anyString())).thenReturn(testUser);

        AssignmentServiceMock assignmentService = new AssignmentServiceMock();
        assignmentService.addDefaultAssignmentInfo(ASSIGNMENT_ID, WORKER_ID);

        AliasMakerServiceImpl service = new AliasMakerServiceImpl(
                knowledgeService,
                modelService,
                null,
                offerMatchingService,
                userService, assignmentService, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, true, true);
        //init finish
        AliasMaker.UpdateVendorResponse resp = service.updateVendor(AliasMaker.UpdateVendorRequest.newBuilder()
                .setAsyncUpdateMatcher(true)
                .setVendor(AliasMaker.Vendor.newBuilder()
                        .setVendorId(153043)
                        .setCategoryId(CATEGORY_ID)
                        .setVersionId(System.currentTimeMillis())
                        .build())
                .addAllTaskOfferId(offerIds)
                .setAssignmentId(ASSIGNMENT_ID)
                .build()
        );
        AliasMaker.ReloadState reloadState = resp.getReloadState();
        assertEquals(0, resp.getTaskOfferCount());
        assertEquals(1, tasks.size());
        assertTrue(reloadState.getMatcherReloadTimestamp() > 0);
        assertFalse(reloadState.hasSkutcherReloadTimestamp());

        AliasMaker.UpdateTaskResponse updateTaskResp = service.updateTask(AliasMaker.UpdateTaskRequest.newBuilder()
                .addAllTaskOfferId(offerIds)
                .setCategoryId(CATEGORY_ID)
                .setReloadState(reloadState)
                .setOfferType(AliasMaker.OfferType.OFFER)
                .setAssignmentId(ASSIGNMENT_ID)
                .build());
        assertEquals(0, updateTaskResp.getTaskOfferCount());
        assertEquals(AliasMaker.OperationStatus.NOT_READY, updateTaskResp.getResult().getStatus());

        //Initial page load - no reloadState => Offers are returned
        updateTaskResp = service.updateTask(AliasMaker.UpdateTaskRequest.newBuilder()
                .addAllTaskOfferId(offerIds)
                .setCategoryId(CATEGORY_ID)
                .setOfferType(AliasMaker.OfferType.OFFER)
                .setAsync(true)
                .setAssignmentId(ASSIGNMENT_ID)
                .build());
        assertEquals(2, updateTaskResp.getTaskOfferCount());
        assertEquals(AliasMaker.OperationStatus.NOT_READY, updateTaskResp.getResult().getStatus());

        tasks.forEach(FutureTask::run);
        runInstantly.set(true);
        updateTaskResp = service.updateTask(AliasMaker.UpdateTaskRequest.newBuilder()
                .addAllTaskOfferId(offerIds)
                .setCategoryId(CATEGORY_ID)
                .setReloadState(reloadState)
                .setOfferType(AliasMaker.OfferType.OFFER)
                .setAssignmentId(ASSIGNMENT_ID)
                .build());

        assertNotEquals(AliasMaker.OperationStatus.NOT_READY, updateTaskResp.getResult().getStatus());
        assertEquals(2, updateTaskResp.getTaskOfferCount());
    }

    @Test
    public void testAsyncUpdatesOldClient() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        AtomicBoolean runInstantly = new AtomicBoolean(true);
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(runInstantly, tasks);

        UserService userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByWorkerId(Mockito.anyString())).thenReturn(testUser);

        AssignmentServiceMock assignmentService = new AssignmentServiceMock();
        assignmentService.addDefaultAssignmentInfo(ASSIGNMENT_ID, WORKER_ID);

        AliasMakerServiceImpl service = new AliasMakerServiceImpl(
                knowledgeService,
                modelService,
                null,
                offerMatchingService,
                userService, assignmentService, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, true, true);
        //init finish

        AliasMaker.UpdateVendorResponse resp = service.updateVendor(AliasMaker.UpdateVendorRequest.newBuilder()
                .setVendor(AliasMaker.Vendor.newBuilder()
                        .setVendorId(153043)
                        .setCategoryId(91491)
                        .setVersionId(System.currentTimeMillis())
                        .build())
                .setOfferType(AliasMaker.OfferType.OFFER)
                .addAllTaskOfferId(offerIds)
                .setAssignmentId(ASSIGNMENT_ID)
                .build()
        );
        assertFalse(resp.hasReloadState());
        assertEquals(2, resp.getTaskOfferCount());
    }

    @Test
    public void testAsyncUpdateTask() {
        KnowledgeService knowledgeService = init();
        List<String> offerIds = Arrays.asList(
                "1", "4"
        );
        List<FutureTask<Void>> tasks = new ArrayList<>();
        mockAsyncExecution(new AtomicBoolean(false), tasks);

        UserService userService = Mockito.mock(UserService.class);
        Mockito.when(userService.getUserByWorkerId(Mockito.anyString())).thenReturn(testUser);

        AssignmentServiceMock assignmentService = new AssignmentServiceMock();
        assignmentService.addDefaultAssignmentInfo(ASSIGNMENT_ID, WORKER_ID);

        AliasMakerServiceImpl service = new AliasMakerServiceImpl(
                knowledgeService,
                modelService,
                null,
                offerMatchingService,
                userService, assignmentService, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null, true, true);
        //init finish

        AliasMaker.UpdateTaskResponse response = service.updateTask(AliasMaker.UpdateTaskRequest.newBuilder()
                .setAssignmentId("q")
                .setAsync(true)
                .setCategoryId(CATEGORY_ID)
                .setOfferType(AliasMaker.OfferType.SUPPLIER_OFFER)
                .addAllTaskOfferId(offerIds)
                .setAssignmentId(ASSIGNMENT_ID)
                .build()
        );
        assertTrue(response.hasReloadState());
        assertEquals(response.getResult().getStatus(), AliasMaker.OperationStatus.NOT_READY);
    }

    private void mockAsyncExecution(AtomicBoolean runInstantly, List<FutureTask<Void>> tasks,
                                    Consumer<ExecutorService> executorConsumer) {
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        Mockito.when(executorService.submit(Mockito.any(Callable.class))).then(invocation -> {
            Callable<Void> callable = invocation.getArgument(0, Callable.class);
            FutureTask<Void> result = new FutureTask<>(callable);
            tasks.add(result);
            if (runInstantly.get()) {
                result.run();
            }
            return result;
        });
        executorConsumer.accept(executorService);
    }

    private void mockAsyncExecution(AtomicBoolean runInstantly, List<FutureTask<Void>> tasks) {
        mockAsyncExecution(runInstantly, tasks, matchingService::setUpdateExecutorService);
    }

    @Test
    public void testUploadPictures() {
        init();

        String image1 = new String(loadBytes("/image_base64_1.txt"));
        String image2 = new String(loadBytes("/image_base64_2.txt"));

        ModelStorage.UploadDetachedImagesResponse response = modelService.uploadImages(
                AliasMaker.UploadImagesRequest.newBuilder()
                        .addLocalImage(AliasMaker.LocalImage.newBuilder()
                                .setOriginalUrl("local1")
                                .setContentBase64(image1))
                        .addLocalImage(AliasMaker.LocalImage.newBuilder()
                                .setOriginalUrl("local2")
                                .setContentBase64(image2))
                        .addRemoteUrl("remote1")
                        .addRemoteUrl("remote2")
                        .build());
        Assert.assertEquals(4, response.getUploadedImageCount());

        Set<String> newUrls = response.getUploadedImageList().stream()
                .map(l -> l.getPicture().getUrlOrig())
                .collect(Collectors.toSet());

        Assert.assertTrue(newUrls.containsAll(
                Arrays.asList("local1", "local2", "remote1", "remote2")));

        List<ModelStorage.OperationStatus> errors = response.getUploadedImageList().stream()
                .map(ModelStorage.DetachedImageStatus::getStatus)
                .filter(status -> status.getStatus() != ModelStorage.OperationStatusType.OK)
                .collect(Collectors.toList());
        Assert.assertTrue(errors.isEmpty());
    }

    @Test
    public void testSearchModelsWithPictures() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .setModelType("GURU")
                        .setQuery("iPhone")
                        .setLimit(10)
                        .build());
        List<AliasMaker.SuggestedModel> models = response.getModelList().stream()
                .sorted(Comparator.comparing(AliasMaker.SuggestedModel::getId))
                .collect(Collectors.toList());
        Assertions.assertThat(models.size()).isEqualTo(3);
        Assertions.assertThat(models)
                .extracting(m -> m.hasParentId())
                .containsExactly(false, true, false);
        Assertions.assertThat(models.get(1).getParentId())
                .isEqualTo(234324L);
        String picture = models.get(0).getPicture();
        Assertions.assertThat(picture)
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/372220/img_id9222749140269300384.jpeg/orig");
    }

    @Test
    public void testSearchOnlyModels() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .setModelType("GURU")
                        .setQuery("iPhone")
                        .setOnlyModels(true)
                        .setLimit(10)
                        .build());
        Assertions.assertThat(response.getModelCount()).isEqualTo(2);
        Assertions.assertThat(response.getModelList())
                .extracting(m -> m.hasParentId())
                .containsExactly(false, false);

        Assertions.assertThat(response.getModelList().stream().filter(m -> m.getId() == 234324L)
                .findFirst().get().getModificationIdsList()).containsExactly(2L);
    }

    @Test
    public void testSearchModelsWithoutPictures() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(90702)
                        .setVendorId(7855522)
                        .setModelType("GURU")
                        .setQuery("")
                        .setLimit(10)
                        .build());

        // if model has no pictures, we take first SKU picture

        Assertions.assertThat(response.getModelCount()).isEqualTo(1);
        String picture = response.getModelList().get(0).getPicture();
        Assertions.assertThat(picture)
                .isEqualTo("//avatars.mds.yandex.net/get-mpic/1361544/img_id6943643846516795525.jpeg/orig");
    }

    @Test
    public void testSearchModelsNoPicturesNowhere() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(90702)
                        .setVendorId(1627868)
                        .setModelType("GURU")
                        .setQuery("")
                        .setLimit(10)
                        .build());
        // no pictures in model and no pictures in SKUs

        Assertions.assertThat(response.getModelCount()).isEqualTo(1);
        Assertions.assertThat(response.getModel(0).getPicture()).isEmpty();
    }

    @Test
    public void testSearchModelsEmptyQuery() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .setModelType("GURU")
                        .setQuery("")
                        .setLimit(10)
                        .build());

        Assertions.assertThat(response.getModelCount()).isEqualTo(5);
        Assertions.assertThat(response.getModelList())
                .extracting(AliasMaker.SuggestedModel::getName)
                .containsExactly("218313 53х26 см", "iPhone 7 128GB", "iPhone X 128GB", "iPhone Y 0GB", "QQ");
    }

    @Test
    public void testSearchModelsWithQuery() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .setModelType("GURU")
                        .setQuery("iPhone X")
                        .setLimit(10)
                        .build());

        Assertions.assertThat(response.getModelCount()).isEqualTo(1);
        Assertions.assertThat(response.getModelList())
                .extracting(AliasMaker.SuggestedModel::getName)
                .containsExactly("iPhone X 128GB");
    }

    @Test
    public void testSearchOnlyOperatorQualityModels() {
        init();

        AliasMaker.SearchModelsResponse response = modelService.searchModels(
                AliasMaker.SearchModelsRequest.newBuilder()
                        .setCategoryId(91491)
                        .setVendorId(153043)
                        .setModelType("GURU")
                        .setQuery("")
                        .setLimit(10)
                        .build());

        Assertions.assertThat(response.getModelCount()).isEqualTo(5);
        Assertions.assertThat(response.getModelList())
                .extracting(AliasMaker.SuggestedModel::getId)
                .doesNotContain(123454333L);
    }

    @Test
    public void testSearchParamByOptionId() {
        KnowledgeService service = init();

        int optionId = 14896084;
        AliasMaker.SearchParamValueResponse response = service.searchParamValue(
                AliasMaker.SearchParamValueRequest.newBuilder()
                        .setCategoryId(91491)
                        .setParamId(14871214)
                        .setOptionId(optionId)
                        .build());

        Assert.assertEquals(1, response.getParamValueCount());
        Assert.assertEquals("black", response.getParamValue(0).getName());
        Assert.assertEquals(optionId, response.getParamValue(0).getValueId());
    }

    @Test
    public void testSearchParamSuggest() {
        KnowledgeService service = init();

        // empty query
        AliasMaker.SearchParamValueResponse response = service.searchParamValue(
                AliasMaker.SearchParamValueRequest.newBuilder()
                        .setCategoryId(91491)
                        .setParamId(14871214)
                        .setQuery("")
                        .build());

        Assertions.assertThat(response.getParamValueCount()).isEqualTo(2);
        Assertions.assertThat(response.getParamValueList())
                .extracting(AliasMaker.ParamValue::getName)
                .containsExactly("black", "white"); // sorted by alphabet

        // with query
        response = service.searchParamValue(
                AliasMaker.SearchParamValueRequest.newBuilder()
                        .setCategoryId(91491)
                        .setParamId(14871214)
                        .setQuery("w")
                        .build());

        Assertions.assertThat(response.getParamValueCount()).isEqualTo(1);
        Assertions.assertThat(response.getParamValueList())
                .extracting(AliasMaker.ParamValue::getName)
                .containsExactly("white");
    }

    @Test
    public void testSearchVendors() {
        KnowledgeService service = init();
        AliasMaker.VendorsResponse response = service.searchVendors(AliasMaker.VendorsRequest.newBuilder()
                .setQuery("Sam")
                .build());

        Assert.assertEquals(1, response.getVendorCount());
        Assert.assertEquals("Samsung", response.getVendor(0).getName());
    }

    @Test
    public void testSearchVendorsEmptyQuery() {
        KnowledgeService service = init();
        AliasMaker.VendorsResponse response = service.searchVendors(AliasMaker.VendorsRequest.newBuilder()
                .setQuery("")
                .setLimit(10)
                .build());

        Assertions.assertThat(response.getVendorCount()).isEqualTo(3);
        Assertions.assertThat(response.getVendorList())
                .extracting(AliasMaker.Vendor::getName)
                .containsExactly("Apple", "Эппл с алиэкспресса", "Яблочко");
    }

    @Test
    public void testSearchVendorsOnlyLocal() {
        KnowledgeService service = init();
        AliasMaker.VendorsResponse response1 = service.searchVendors(AliasMaker.VendorsRequest.newBuilder()
                .setQuery("Sam")
                .setOnlyLocal(true)
                .setCategoryId(91491)
                .build());
        Assert.assertEquals(0, response1.getVendorCount());


        AliasMaker.VendorsResponse response2 = service.searchVendors(AliasMaker.VendorsRequest.newBuilder()
                .setQuery("App")
                .setOnlyLocal(true)
                .setCategoryId(91491)
                .build());
        Assert.assertEquals(1, response2.getVendorCount());
        Assert.assertEquals("Apple", response2.getVendor(0).getName());
    }

    @Test
    public void testSearchVendorsInheritedAreNotLocal() {
        KnowledgeService service = init();
        AliasMaker.VendorsResponse response = service.searchVendors(AliasMaker.VendorsRequest.newBuilder()
                .setCategoryId(91491)
                .setQuery("Xiaomi")
                .setLimit(10)
                .build());

        Assert.assertEquals(1, response.getVendorCount());
        Assert.assertEquals("Xiaomi", response.getVendor(0).getName());
        Assert.assertFalse(response.getVendor(0).getAddedToCategory()); // inherited vendors are not added to category
    }

    @Test
    public void testGetVendors() {
        KnowledgeService service = init();
        ImmutableList<Integer> vendors = ImmutableList.of(153043, 1530546);
        AliasMaker.VendorsResponse response = service.getVendors(AliasMaker.GetVendorsRequest.newBuilder()
                .setCategoryId(91491)
                .addAllVendorId(vendors)
                .build());
        Assert.assertEquals(2, response.getVendorCount());
        List<Integer> result = response.getVendorList().stream()
                .map(AliasMaker.Vendor::getVendorId)
                .collect(Collectors.toList());
        Assert.assertTrue(result.containsAll(vendors));
    }

    @Test
    public void testGetVendorsNotFound() {
        KnowledgeService service = init();
        AliasMaker.VendorsResponse response = service.getVendors(AliasMaker.GetVendorsRequest.newBuilder()
                .setCategoryId(91491)
                .addVendorId(12121212)
                .build());
        Assert.assertEquals(0, response.getVendorCount());
    }
}
