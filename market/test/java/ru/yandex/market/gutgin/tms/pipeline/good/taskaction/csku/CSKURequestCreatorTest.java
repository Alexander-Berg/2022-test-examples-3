package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.csku;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.primitives.Longs;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.gutgin.tms.utils.ModelSaveVisualization;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.MigrationUtils;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.csku.wrappers.pictures.PictureWrapper;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcSkuValidationDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.GcSkuTicketType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.BARCODE_STR;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateMModel;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateModel;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateMsku;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateSku;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateSkuWithPictures;
import static ru.yandex.market.gutgin.tms.base.ModelGeneration.generateSkuWithPicturesAndQuality;
import static ru.yandex.market.ir.autogeneration.common.helpers.ModelBuilderHelper.idFoSku;
import static ru.yandex.market.ir.autogeneration.common.helpers.ModelBuilderHelper.idForModel;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.NAME;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.USE_NAME_AS_TITLE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VENDOR_LINE;
import static ru.yandex.market.partner.content.common.csku.KnownParameters.VOLUME;
import static ru.yandex.market.robot.db.ParameterValueComposer.VENDOR_ID;

public class CSKURequestCreatorTest extends DBDcpStateGenerator {

    private static final long CATEGORY2_ID = 1235L;


    private final Judge judge = new Judge();
    private final ModelStorageHelper modelStorageHelper = mock(ModelStorageHelper.class);
    private final GcSkuValidationDao gcSkuValidationDao = mock(GcSkuValidationDao.class);
    private final MboPictureService mboPictureService = mock(MboPictureService.class);
    private CSKURequestCreator cskuRequestCreator;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        CategoryDataKnowledgeMock categoryDataKnowledge = new CategoryDataKnowledgeMock();
        CategoryData categoryData = spy(CategoryData.build(CATEGORY));
        categoryDataKnowledge.addCategoryData(CATEGORY_ID, categoryData);

        when(categoryData.getParamById(anyLong())).thenAnswer(new Answer<MboParameters.Parameter>() {
            @Override
            public MboParameters.Parameter answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Long id = (Long) args[0];
                return MboParameters.Parameter.newBuilder()
                        .setId(id)
                        .build();
            }
        });

        MboParameters.Category category2 = MboParameters.Category.newBuilder()
                .setHid(CATEGORY2_ID)
                .addName(MboParameters.Word.newBuilder().setName("Category " + CATEGORY2_ID).setLangId(225))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(VENDOR_ID).setXslName(ParameterValueComposer.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .addName(MboParameters.Word.newBuilder().setLangId(225).setName("производитель"))
                        .addOption(MboParameters.Option.newBuilder().addName(
                                        MboParameters.Word.newBuilder().setLangId(225).setName("производитель-1"))
                                .setId(1000)
                                .build())
                        .setMandatoryForPartner(true)
                )
                .build();
        CategoryData categoryData2 = spy(CategoryData.build(category2));
        when(categoryData2.getParamById(anyLong())).thenAnswer(new Answer<MboParameters.Parameter>() {
            @Override
            public MboParameters.Parameter answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Long id = (Long) args[0];
                return MboParameters.Parameter.newBuilder()
                        .setId(id)
                        .build();
            }
        });
        categoryDataKnowledge.addCategoryData(CATEGORY2_ID, categoryData2);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledge, null);
        doReturn(HashMultimap.create()).when(modelStorageHelper).getModelIdsByBarcodes(any());
        this.cskuRequestCreator = new CSKURequestCreator(
                judge,
                categoryDataHelper,
                modelStorageHelper,
                gcSkuValidationDao,
                mboPictureService);
    }

    @Test
    public void createRequest() {
        doReturn(Lists.emptyList()).when(mboPictureService).getValidPictures(any());
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, 110), generateModel(110, 100), false, generateTicket(100)),
                new TicketWrapper(generateSku(101, 111), generateModel(111, 101), true, generateTicket(101)),
                new TicketWrapper(generateSku(102, 112), generateModel(112, 102), true, generateTicket(102))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);

        assertThat(request).isNotNull();
        //берем только валидные модели по 1 ModelsRequest на каждый SkuWrapper
        assertThat(request).extracting(ModelCardApi.SaveModelsGroupRequest::getModelsRequestCount).isEqualTo(2);
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model
        assertThat(modelsRequestList).extracting(ModelStorage.SaveModelsRequest::getModelsCount).containsOnly(2);
        //первым идет модель
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getId()).containsOnly(111L, 112L);
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getId()).containsOnly(101L, 102L);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void updateSkuAndModelCategoryWhenOfferCategoryChanged() {
        long offerCategoryId = CATEGORY2_ID;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, 110), generateModel(110, 100), true,
                        generateTicketWithCategory(100, offerCategoryId))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);

        assertThat(request).isNotNull();
        assertThat(request).extracting(ModelCardApi.SaveModelsGroupRequest::getModelsRequestCount).isEqualTo(1);
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model
        assertThat(modelsRequestList).extracting(ModelStorage.SaveModelsRequest::getModelsCount).containsOnly(2);

        ModelStorage.Model model = modelsRequestList.get(0).getModels(0);
        assertThat(model).extracting(ModelStorage.Model::getCategoryId).isEqualTo(offerCategoryId);
        assertModelHasRelations(model, CATEGORY2_ID, 100);

        ModelStorage.Model sku = modelsRequestList.get(0).getModels(1);
        assertThat(sku).extracting(ModelStorage.Model::getCategoryId).isEqualTo(offerCategoryId);
        assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
        assertSkuRelation(sku.getRelationsList().get(0), 110, offerCategoryId);

        //в sku barcode = 12 в офере barcode = 123, берем из скю
        ModelStorage.ParameterValue barcode = sku.getParameterValuesList().stream()
                .filter(parameterValue -> parameterValue.getParamId() == KnownParameters.BARCODE.getId())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(barcode).extracting(parameterValue -> parameterValue.getStrValue(0).getValue())
                .isEqualTo(BARCODE_STR);

        //в model barcode = 12 в офере barcode = 123, берем из модели
        ModelStorage.ParameterValue modelBarcode = sku.getParameterValuesList().stream()
                .filter(parameterValue -> parameterValue.getParamId() == KnownParameters.BARCODE.getId())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
        assertThat(modelBarcode).extracting(parameterValue -> parameterValue.getStrValue(0).getValue())
                .isEqualTo(BARCODE_STR);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void whenCategoryIsChangedAndModelHasOtherSkusThenCreateNewModel() {
        long offerCategoryId = CATEGORY2_ID;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, 110), generateModel(110, 100, 101), true,
                        generateTicketWithCategory(100, offerCategoryId))
        );
        Long ticketId = ticketWrappers.get(0).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);

        assertThat(request).isNotNull();
        assertThat(request).extracting(ModelCardApi.SaveModelsGroupRequest::getModelsRequestCount).isEqualTo(1);
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только 1 sku и 1 старая модель и 1 новая модель
        assertThat(modelsRequestList).extracting(ModelStorage.SaveModelsRequest::getModelsCount).containsOnly(3);

        ModelStorage.Model oldModel = modelsRequestList.get(0).getModels(0);
        assertThat(oldModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertModelHasRelations(oldModel, CATEGORY_ID, 101);

        ModelStorage.Model sku = modelsRequestList.get(0).getModels(1);
        assertThat(sku).extracting(ModelStorage.Model::getCategoryId).isEqualTo(offerCategoryId);
        assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
        assertSkuRelation(sku.getRelationsList().get(0), -ticketId * 10, offerCategoryId);

        ModelStorage.Model newModel = modelsRequestList.get(0).getModels(2);
        assertThat(newModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(offerCategoryId);
        assertModelHasRelations(newModel, offerCategoryId, 100);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createSkuAndModelForOfferWithoutMapping() {
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(null, null, true, generateTicketWith()),
                new TicketWrapper(null, null, true, generateTicketWith())
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);

        assertThat(request).isNotNull();
        //берем только валидные модели по 1 ModelsRequest на каждый SkuWrapper
        assertThat(request).extracting(ModelCardApi.SaveModelsGroupRequest::getModelsRequestCount).isEqualTo(2);
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model
        assertThat(modelsRequestList).extracting(ModelStorage.SaveModelsRequest::getModelsCount).containsOnly(2);

        long ticketId1 = ticketWrappers.get(0).getTicket().getId();
        long ticketId2 = ticketWrappers.get(1).getTicket().getId();
        //первым идет модель c id тикета с минусом * 10
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getId())
                .containsOnly(idForModel(ticketId1), idForModel(ticketId2));
        //вторым идет sku c id тикета с минусом
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getId())
                .containsOnly(idFoSku(ticketId1), idFoSku(ticketId2));

        //assert параметры которые должны быть в новых сущностях
        //в модели
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getPublished()).containsOnly(true);
        long businessId = PARTNER_SHOP_ID;
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getSupplierId()).containsOnly(businessId);
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getCategoryId()).containsOnly(CATEGORY_ID);
        assertThat(modelsRequestList).extracting(r -> r.getModels(0).getRelationsList().get(0).getId())
                .containsOnly(-ticketId1, -ticketId2);
        //в скю
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPublished()).containsOnly(true);
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getSupplierId()).containsOnly(businessId);
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getCategoryId()).containsOnly(CATEGORY_ID);
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getRelationsList().get(0).getId())
                .containsOnly(-ticketId1 * 10, -ticketId2 * 10);
        assertAllModelsHaveModelQualityParameter(request);

    }

    //     model1(200)   model2(2000)                    model1(200)     model2(2000) - deleted
    //    /   |    \         |                          /   |   |   \
    //  sku1 sku2 sku3      sku4          ====>>>>   sku1 sku2 sku3  sku4
    //        |     |        |
    //      offer1 offer2  offer3
    //             group = 1
    @Test
    public void testRequestCreationForAGroupMappedToSkusWithDifferentModels() {
        long modelId1 = 200L;
        long modelId2 = 2000L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), generateModel(modelId1, 100, 101, 102),
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1),
                                generateSku(102, modelId1)
                        ), true, generateTicket(100)),
                new TicketWrapper(generateSku(1000, modelId2), generateModel(modelId2, 1000),
                        List.of(
                                generateSku(1000, modelId2)
                        ), true, generateTicket(1000))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(6);// 4 sku + 1 model + 1 model на удаление
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(1);
        assertThat(modelsRequest.getTransition(0).getOldEntityId()).isEqualTo(modelId2);
        assertThat(modelsRequest.getTransition(0).getOldEntityDeleted()).isEqualTo(true);
        assertThat(modelsRequest.getTransition(0).getNewEntityId()).isEqualTo(modelId1);

        //Проверяем флаг published
        modelsRequest.getModelsList().forEach(model -> assertThat(model.getPublished()).isTrue());

        //assert model
        ModelStorage.Model model = modelsRequest.getModels(0);
        assertThat(model.getId()).isEqualTo(modelId1);
        assertModelHasRelations(model, CATEGORY_ID, 100L, 101L, 102L, 1000L);

        ModelStorage.Model modelToDelete =
                takeModelWithId(modelId2, modelsRequest);
        assertThat(modelToDelete).extracting(ModelStorage.Model::getDeleted).isEqualTo(true);

        //параметр из модели modelId2 перешел в модель model1 (в каждую модель добавляется параметр с совпадающим id)
        GutginAssertions.assertThat(model).containParameterValues(modelId1, modelId2);
        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(model, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        for (int i = 1; i < 5; i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
            assertThat(sku.getRelations(0)).extracting(ModelStorage.Relation::getId).isEqualTo(modelId1);
        }
        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    @Test
    public void testPublishedFlagForAGroupWithMixedFlags() {
        long modelId1 = 200L;
        ModelStorage.Model parentModel = generateModel(modelId1, 100, 101, 102);
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), parentModel,
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1).toBuilder().setPublished(false).build(),
                                generateSku(102, modelId1)
                        ), true, generateTicket(100)),
                new TicketWrapper(generateSku(101, modelId1), parentModel,
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1),
                                generateSku(102, modelId1)
                        ), true, generateTicket(101))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(4);// 3 sku + 1 model

        //Проверяем флаг published
        modelsRequest.getModelsList().forEach(model -> assertThat(model.getPublished()).isTrue());

        //assert model
        ModelStorage.Model model = modelsRequest.getModels(0);
        assertThat(model.getId()).isEqualTo(modelId1);
        assertModelHasRelations(model, CATEGORY_ID, 100L, 101L, 102L);
        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    @Test
    public void testPublishedFlagForAGroupWithHiddenSku() {
        long modelId1 = 200L;
        ModelStorage.Model parentModel = generateModel(modelId1, 100, 101, 102);
        ModelStorage.Model hiddenSku = generateSku(101, modelId1).toBuilder().setPublished(false).build();
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), parentModel,
                        List.of(
                                generateSku(100, modelId1),
                                hiddenSku,
                                generateSku(102, modelId1)
                        ), true, generateTicket(100)),
                new TicketWrapper(hiddenSku, parentModel,
                        List.of(
                                generateSku(100, modelId1),
                                hiddenSku,
                                generateSku(102, modelId1)
                        ), true, generateTicket(101))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(4);// 3 sku + 1 model

        //Проверяем флаг published
        modelsRequest.getModelsList()
                .stream()
                .filter(model -> model.getId() == hiddenSku.getId())
                .forEach(model -> assertThat(model.getPublished()).isFalse());

        modelsRequest.getModelsList()
                .stream()
                .filter(model -> model.getId() != hiddenSku.getId())
                .forEach(model -> assertThat(model.getPublished()).isTrue());

        //assert model
        ModelStorage.Model model = modelsRequest.getModels(0);
        assertThat(model.getId()).isEqualTo(modelId1);
        assertModelHasRelations(model, CATEGORY_ID, 100L, 101L, 102L);
        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    private ModelStorage.ParameterValue extractParam(ModelStorage.Model model, KnownParameters name) {
        return model.getParameterValuesList().stream()
                .filter(parameterValue -> name.getXslName().equals(parameterValue.getXslName()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    //     model1(200)   model2(2000) model3(20_000)            model1(200)      model2(2000)  model3(20_000) - deleted
    //    /   |    \         |   \         |                 /   |   |    \   \         |
    //  sku1 sku2 sku3      sku4 sku5     sku6     ===>>> sku1 sku2 sku3 sku4 sku6     sku5
    //        |     |        |           /
    //      offer1 offer2  offer3  offer4
    //             group = 1
    //             conflict in (sku3 and sku5)
    @Test
    public void testRequestCreationForAGroupMappedToSkusWithDifferentModelsWithConflictedSkus() {
        long model1 = 200L;
        long model2 = 2000L;
        long model3 = 20_000L;
        long sku3 = 102L;
        long sku4 = 1000L;
        long sku5 = 1001L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, model1), generateModel(model1, 100, 101, sku3),
                        List.of(
                                generateSku(100, model1),
                                generateSku(101, model1),
                                generateSku(sku3, model1)
                        ), true, generateTicket(100)),
                new TicketWrapper(generateSku(sku4, model2), generateModel(model2, sku4, sku5),
                        List.of(
                                generateSku(sku4, model2),
                                generateSku(sku5, model2)
                        ), true, generateTicket(sku4)),
                new TicketWrapper(generateSku(10_000, model3), generateModel(model3, 10_000),
                        List.of(
                                generateSku(10_000, model3)
                        ), true, generateTicket(10_000))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku3, sku5));

        ModelSaveVisualization.visualizeInputData(ticketWrappers);
        ModelSaveVisualization.visualizeFinalRequest(requestForGroup);

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(9);// 6 sku + 2 models + 1 model на удаление
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(1);
        assertThat(modelsRequest.getTransition(0).getOldEntityId()).isEqualTo(model3);
        assertThat(modelsRequest.getTransition(0).getOldEntityDeleted()).isEqualTo(true);
        assertThat(modelsRequest.getTransition(0).getNewEntityId()).isEqualTo(model1);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model1);
        assertModelHasRelations(targetModel, CATEGORY_ID, 100L, 101L, sku3, 10_000L);
        //параметр из модели modelId2 перешел в модель model1 (в каждую модель добавляется параметр с совпадающим id)
        GutginAssertions.assertThat(targetModel).containParameterValues(model1, model3);
        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(targetModel, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        ModelStorage.Model otherModel = takeModelWithId(model2, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku4, sku5);

        ModelStorage.Model modelToDelete =
                takeModelWithId(model3, modelsRequest);
        assertThat(modelToDelete).extracting(ModelStorage.Model::getDeleted).isEqualTo(true);

        List<Long> skusForModel1 = List.of(100L, 101L, sku3, 10_000L);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model2 || sku.getId() == model3) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1
                assertSkuHasRelationWithModel(model1, sku);
            } else {
                //model2
                assertSkuHasRelationWithModel(model2, sku);
            }
        }
        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    /**
     * MCR-3811
     * content_quality should be cleared when we are changing first image
     */
    @Test
    public void createClearingContentQualityTest() {
        PictureWrapper wrapper = buildSizedPictureWrapper("1234", "1234",
                ModelStorage.ModificationSource.VENDOR_OFFICE, 1000, 1000);
        doReturn(Collections.singletonList(
                new Pair("test", wrapper)))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildSizedPictureWrapper("123", "123", ModelStorage.ModificationSource.VENDOR_OFFICE,
                400, 400));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPicturesAndQuality(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model
        //вторым идет sku
        assertFalse(modelsRequestList.get(0).getModels(1)
                .getParameterValuesList()
                .stream().anyMatch(it -> Objects.equals(it.getParamId(), KnownParameters.CONTENT_QUALITY.getId()))
        );
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createRequestPicturesTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapper("1234", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapperWithOwnerId("123", "123", ModelStorage.ModificationSource.VENDOR_OFFICE, 222L));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(2);
        assertThat(modelsRequestList.get(0).getModels(1).getPicturesList().get(1).getPictureStatus())
                .isEqualTo(ModelStorage.PictureStatus.NEW);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createRequestPicturesTheSameUrlTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapper("123", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapper("123", "123", ModelStorage.ModificationSource.VENDOR_OFFICE));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model]
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(1);
    }

    @Test
    public void createRequestPicturesNoModifiableTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapper("123", "1234", ModelStorage.ModificationSource.OPERATOR_FILLED))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapper("123", "123", ModelStorage.ModificationSource.VENDOR_OFFICE));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model]
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(1);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createRequestPicturesSameMD5ModifiableTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapper("1234", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapper("123", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model]
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(1);
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createRequestWithNewPictureAndOwnerIdTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapper(
                        "1234", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapperWithOwnerId("123", "12345", ModelStorage.ModificationSource.VENDOR_OFFICE, 222L));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model]
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(2);
        assertThat(modelsRequestList.get(0).getModels(1).getPicturesList().get(1).getPictureStatus())
                .isEqualTo(ModelStorage.PictureStatus.NEW);
        assertEquals(123, modelsRequestList.get(0).getModels(1).getPicturesList().get(1).getOwnerId());
        assertAllModelsHaveModelQualityParameter(request);
    }

    @Test
    public void createRequestWithApprovedPictureTest() {
        doReturn(Collections.singletonList(
                new Pair("test", buildPictureWrapperWithOwnerId(
                        "1234", "1234", ModelStorage.ModificationSource.VENDOR_OFFICE, 123L))))
                .when(mboPictureService).getValidPictures(any());

        List<PictureWrapper> list = new LinkedList<>();
        list.add(buildPictureWrapperWithOwnerId(
                "123", "12345", ModelStorage.ModificationSource.VENDOR_OFFICE, 123L));
        List<TicketWrapper> ticketWrappers = Collections.singletonList(
                new TicketWrapper(generateSkuWithPictures(100, 110, list),
                        generateSku(110, 100),
                        true,
                        generateTicket(100))
        );

        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        assertThat(request).isNotNull();
        List<ModelStorage.SaveModelsRequest> modelsRequestList = request.getModelsRequestList();
        //внутри ModelsRequest только одна 1 sku и 1 model]
        //вторым идет sku
        assertThat(modelsRequestList).extracting(r -> r.getModels(1).getPicturesList().size()).containsOnly(1);
        assertThat(modelsRequestList.get(0).getModels(1).getPicturesList().get(0).getPictureStatus())
                .isEqualTo(ModelStorage.PictureStatus.APPROVED);
        assertEquals(123, modelsRequestList.get(0).getModels(1).getPicturesList().get(0).getOwnerId());
        assertAllModelsHaveModelQualityParameter(request);
    }


    //   model1(2000)                      model1(2000)
    //         |                           /      |
    //        sku1       ====>>>>      sku1     sku2 (NEW)
    //            \
    //    offer1  offer2
    //      group = 1
    @Test
    public void requestForAGroupWithOfferWithoutMapping() {
        long modelId1 = 2000L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(null, null, Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(generateSku(1000, modelId1), generateModel(modelId1, 1000),
                        List.of(
                                generateSku(1000, modelId1)
                        ), true, generateTicket(1000))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(3);// 2 sku + 1 model
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model model = modelsRequest.getModels(0);
        assertThat(model.getId()).isEqualTo(modelId1);

        long expectedNewSkuId = -ticketWrappers.get(0).getTicket().getId();
        assertModelHasRelations(model, CATEGORY_ID, 1000L, expectedNewSkuId);
        //параметр из model1 остался (в каждую модель добавляется параметр с совпадающим id)
        GutginAssertions.assertThat(model).containParameterValues(modelId1);

        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(model, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
            assertThat(sku.getRelations(0)).extracting(ModelStorage.Relation::getId).isEqualTo(modelId1);
            assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
        }
        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //                                      model1 (NEW)
    //                                     /      \
    //     нет маппингов  ====>>>>     sku1 (NEW) sku2 (NEW)
    //    offer1  offer2
    //      group = 1
    @Test
    public void requestForAGroupWithAllOfferWithoutMapping() {
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(null, null, Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(null, null, Collections.emptyList(), true, ticketWithoutMapping())
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(3);// 2 sku + 1 model
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model model = modelsRequest.getModels(0);
        long expectedNewModelId = -ticketWrappers.get(ticketWrappers.size() - 1).getTicket().getId() * 10;
        assertThat(model.getId()).isEqualTo(expectedNewModelId);

        assertModelHasRelations(model, CATEGORY_ID,
                -ticketWrappers.get(1).getTicket().getId(),
                -ticketWrappers.get(0).getTicket().getId());

        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(model, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
            assertThat(sku.getRelations(0)).extracting(ModelStorage.Relation::getId).isEqualTo(expectedNewModelId);
            assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //     model1(200)   model2(2000)                     model2(2000)  model2(200)    model3(new)
    //    /   |              |   \                         /   |         /   |             |
    //  sku1 sku2           sku3 sku4          ===>>>  sku3 sku4      sku1  sku2      sku5(new from offer2)
    //        |              |
    //      offer1 offer2  offer3
    //             group = 1
    //             conflict in (sku1 and sku5(new from offer2))
    @Test
    public void newSkuAndConflict() {
        long model1 = 200L;
        long model2 = 2000L;

        long sku1 = 100L;
        long sku3 = 1001L;
        long sku4 = 1002L;

        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(101, model1), generateModel(model1, sku1, 101),
                        List.of(
                                generateSku(sku1, model1),
                                generateSku(101, model1)
                        ), true, generateTicket(101)),
                new TicketWrapper(null, null, Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(generateSku(sku3, model2), generateModel(model2, sku3, sku4),
                        List.of(
                                generateSku(sku3, model2),
                                generateSku(sku4, model2)
                        ), true, generateTicket(sku3))
        );

        Long tickedIdNew = ticketWrappers.get(1).getTicket().getId();


        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, -tickedIdNew));

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(8);// 5 sku + 3 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model2);
        assertModelHasRelations(targetModel, CATEGORY_ID, sku3, sku4);
        //параметр из модели modelId2 перешел в модель model1 (в каждую модель добавляется параметр с совпадающим id)
        GutginAssertions.assertThat(targetModel).containParameterValues(model2);
        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(targetModel, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        ModelStorage.Model otherModel = takeModelWithId(model1, modelsRequest);
        ModelStorage.Model newModel = takeModelWithId(-tickedIdNew * 10, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku1, 101L);

        List<Long> skusForModel2 = List.of(sku3, sku4);
        List<Long> skusForNewModel = List.of(-tickedIdNew);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model1 || sku.getId() == newModel.getId()) {
                //skipping non target models, check is for sku
                continue;
            }
            if (skusForModel2.contains(sku.getId())) {
                //model2(target)
                assertSkuHasRelationWithModel(model2, sku);
            } else if (skusForNewModel.contains(sku.getId())) {
                assertSkuHasRelationWithModel(newModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            } else {
                //model1
                assertSkuHasRelationWithModel(model1, sku);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }


    //     model1(200)   model2(2000)                     model1(200)  model2(2000)
    //    /   |              |   \                        /     |          /   |
    //  sku1 sku2           sku3 sku4          ===>>> sku1    sku2       sku3 sku4
    //        |              |
    //      offer1         offer3
    //             group = 1
    //             conflict in (sku1 and sku3)
    @Test
    public void dontTouchAllSkusIfOneSkuOfParentModelHasConflicts() {
        long model1 = 200L;
        long model2 = 2000L;

        long sku1 = 100L;
        long sku3 = 1001L;
        long sku4 = 1002L;

        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(101, model1), generateModel(model1, sku1, 101),
                        List.of(
                                generateSku(sku1, model1),
                                generateSku(101, model1)
                        ), true, generateTicket(101)),
                new TicketWrapper(generateSku(sku3, model2), generateModel(model2, sku3, sku4),
                        List.of(
                                generateSku(sku3, model2),
                                generateSku(sku4, model2)
                        ), true, generateTicket(sku3))
        );


        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, sku3)
        );

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(6);// 4 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model2);
        assertModelHasRelations(targetModel, CATEGORY_ID, sku3, sku4);
        //параметр из модели modelId2 перешел в модель model1 (в каждую модель добавляется параметр с совпадающим id)
        GutginAssertions.assertThat(targetModel).containParameterValues(model2);
        //флаг singleOfferGroupMode указывает, что мы должны взять имя параметра для модели из офера
        ModelStorage.ParameterValue nameParam = extractParam(targetModel, NAME);
        assertThat(nameParam).extracting(pv -> pv.getStrValue(0).getValue()).isEqualTo("group_name_from_offer");

        ModelStorage.Model otherModel = takeModelWithId(model1, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku1, 101L);

        List<Long> skusForModel1 = List.of(sku1, 101L);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model1) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                assertSkuHasRelationWithModel(model1, sku);
            } else {
                assertSkuHasRelationWithModel(model2, sku);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }


    //     model1(200)                                                  model1(200)    model2(NEW)
    //     /   |                                                        /    |   \       |
    //   sku1 sku2                                   ===>>>           sku1 sku2 sku4   sku3(NEW)
    //         |
    //      offer1 offer2 offer3
    //         group = 1
    //             conflict in (sku1 and sku3(new from offer2))
    @Test
    public void autoResolveConflictInsideTargetModelByCreatingNewModel() {
        long model1 = 200L;
        long sku1 = 101;
        long sku2 = 102;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku2, model1), generateModel(model1, sku1, sku2),
                        List.of(
                                generateSku(sku1, model1),
                                generateSku(sku2, model1)
                        ), true, generateTicket(sku1)),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping())
        );

        Long tickedIdNew1 = ticketWrappers.get(1).getTicket().getId();
        Long tickedIdNew2 = ticketWrappers.get(2).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, -tickedIdNew1));

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(6);// 4 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model1);


        assertModelHasRelations(targetModel, CATEGORY_ID, sku1, sku2, -tickedIdNew2);

        ModelStorage.Model otherModel =
                takeModelWithId(-tickedIdNew1 * 10, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(-tickedIdNew1);
        assertThat(otherModel.getCategoryId()).isEqualTo(CATEGORY_ID);

        List<Long> skusForModel1 = List.of(sku1, sku2, -tickedIdNew2);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model1 || sku.getCurrentType().equals("GURU")) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1
                assertSkuHasRelationWithModel(model1, sku);
            } else {
                //new model
                assertSkuHasRelationWithModel(otherModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //     model1(200)                                                  model1(200)       model2(NEW)
    //     /   |    \                                                    /    |   \         /     \
    //   sku1 sku2  sku3                                 ===>>>         sku1 sku2 sku3 sku4(NEW) sku5(NEW)
    //         |
    //      offer1 offer2 offer3
    //         group = 1
    //             conflict in (sku1, sku3, sku4(new from offer2), sku5(new from offer3))
    @Test
    public void autoResolveConflictWithMultipleNewSkusTargetModelByCreatingNewModel() {
        long model1 = 200L;
        long sku1 = 101;
        long sku2 = 102;
        long sku3 = 103;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku2, model1), generateModel(model1, sku1, sku2, sku3),
                        List.of(
                                generateSku(sku1, model1),
                                generateSku(sku2, model1),
                                generateSku(sku3, model1)
                        ), true, generateTicket(sku2)),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping())
        );

        Long ticketIdNew1 = ticketWrappers.get(1).getTicket().getId();
        Long ticketIdNew2 = ticketWrappers.get(2).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, sku3, -ticketIdNew1, -ticketIdNew2));

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(7);// 5 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model1);


        assertModelHasRelations(targetModel, CATEGORY_ID, sku1, sku2, sku3);

        ModelStorage.Model otherModel =
                takeModelWithId(-ticketIdNew1 * 10, modelsRequest);
        assertThat(otherModel.getRelationsList())
                .extracting(ModelStorage.Relation::getId)
                .containsOnly(-ticketIdNew1, -ticketIdNew2);
        assertThat(otherModel.getCategoryId()).isEqualTo(CATEGORY_ID);

        List<Long> skusForModel1 = List.of(sku1, sku2, sku3);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model1 || sku.getCurrentType().equals("GURU")) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1
                assertSkuHasRelationWithModel(model1, sku);
            } else {
                //new model
                assertSkuHasRelationWithModel(otherModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //     model1(200)      model2(2000)                             model2(2000)  model1(200)  model3(NEW)
    //     /   |              |    \                                  /   |   \       |            |
    //   sku1 sku2          sku3  sku4               ===>>>        sku2 sku3 sku4    sku1         sku5(NEW)
    //         |             |
    //      offer1 offer2 offer3
    //         group = 1
    //             conflict in (sku1 and sku4 and sku5(new from offer2))
    @Test
    public void conflictInsideModelAndInOtherModel() {
        long model1Id = 200L;
        long model2Id = 2000L;
        long sku1Id = 101;
        long sku2Id = 102;
        long sku3Id = 103;
        long sku4Id = 104;
        ModelStorage.Model sku2 = generateSku(sku2Id, model1Id).toBuilder()
                .setStrictChecksRequired(true)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VOLUME.getId())
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .setXslName(VOLUME.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString("1"))
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VENDOR_LINE.getId())
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("VENDOR_LINE").build())
                        .setOwnerId(123)
                        .build())
                .build();
        ModelStorage.Model model1 = generateModel(model1Id, sku1Id, sku2Id)
                .toBuilder()
                .setStrictChecksRequired(true)
                .build();
        ModelStorage.Model sku1 = generateSku(sku1Id, model1Id);
        ModelStorage.Model sku3 = generateSku(sku3Id, model2Id);
        ModelStorage.Model sku4 = generateSku(sku4Id, model2Id)
                .toBuilder()
                .setStrictChecksRequired(true)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VOLUME.getId())
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .setXslName(VOLUME.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString("3"))
                        .setValueSource(ModelStorage.ModificationSource.TOOL)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VENDOR_LINE.getId())
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("VENDOR_LINE").build())
                        .setOwnerId(123)
                        .build())
                .build();
        ModelStorage.Model model2 = generateModel(model2Id, sku3Id, sku4Id)
                .toBuilder()
                .setStrictChecksRequired(true)
                .build();
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(sku2, model1,
                        List.of(sku1, sku2), true, generateTicket(sku1Id)),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping()),
                new TicketWrapper(sku3, model2,
                        List.of(sku3, sku4), true, generateTicket(sku3Id))
        );
        Long tickedIdNew1 = ticketWrappers.get(1).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1Id, sku4Id, -tickedIdNew1));

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(8);// 5 sku + 3 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(model2Id);
        assertModelHasRelations(targetModel, CATEGORY_ID, sku3Id, sku4Id);

        ModelStorage.Model newModel =
                takeModelWithId(-tickedIdNew1 * 10, modelsRequest);
        assertThat(newModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(-tickedIdNew1);
        assertThat(newModel.getCategoryId()).isEqualTo(CATEGORY_ID);

        ModelStorage.Model oldModel =
                takeModelWithId(model1Id, modelsRequest);
        assertThat(oldModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku1Id, sku2Id);

        List<Long> skusForModel1 = List.of(sku1Id, sku2Id);
        List<Long> skusForModel2 = List.of(sku3Id, sku4Id);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == model2Id || sku.getCurrentType().equals("GURU")) {
                if (sku.getId() == model2Id) {
                    assertThat(sku.hasStrictChecksRequired() && sku.getStrictChecksRequired()).isFalse();
                } else if (sku.getId() == model1Id) {
                    assertThat(sku.hasStrictChecksRequired() && sku.getStrictChecksRequired()).isFalse();
                }
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1Id
                assertSkuHasRelationWithModel(model1Id, sku);
            } else if (skusForModel2.contains(sku.getId())) {
                //model2Id
                assertSkuHasRelationWithModel(model2Id, sku);
            } else {
                //new model
                assertSkuHasRelationWithModel(newModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            }
        }
        //Оффер для sku2 был, флажок снимаем, параметры подчищаем
        modelsRequest.getModelsList().stream().filter(model -> model.getId() == sku2Id)
                .forEach(sku -> {
                    GutginAssertions.assertThat(sku).containParameterValues(VOLUME.getId());
                    GutginAssertions.assertThat(sku).doesNotContainParameterValues(VENDOR_LINE.getId());
                    assertThat(sku.getStrictChecksRequired()).isFalse();
                });

        //Оффера на sku4 не было, не трогаем
        modelsRequest.getModelsList().stream().filter(model -> model.getId() == sku4Id)
                .forEach(sku -> {
                    GutginAssertions.assertThat(sku).containParameterValues(VOLUME.getId(), VENDOR_LINE.getId());
                    assertThat(sku.getStrictChecksRequired()).isTrue();
                });

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    @Test
    public void throwExceptionWhenCategoryIsChangedAndConflictsArePresent() {
        long model1 = 200L;
        long sku1 = 101;
        long sku2 = 102;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku2, model1), generateModel(model1, sku1, sku2),
                        List.of(
                                generateSku(sku1, model1),
                                generateSku(sku2, model1)
                        ), true, generateTicketWithCategory(sku1, CATEGORY2_ID))
        );

        assertThrows(IllegalStateException.class, () -> cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1)));
    }

    //     model1(200)     model2(2000)                         model1(200)  model3(NEW)  model2(2000) - DELETED
    //     /   |     \        |                                  /   |        /    \
    //   sku3 sku2  sku1    sku4                ===>>>        sku3 sku2    sku1    sku4
    //               |        |                                             new category
    //             offer1   offer2
    //                 group = 1
    @Test
    public void categoryChangeNewModelAndTransitionForEmptyModels() {
        long modelId1 = 200L;
        long modelId2 = 2000L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), generateModel(modelId1, 100, 101, 102),
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1),
                                generateSku(102, modelId1)
                        ), true, generateTicketWithCategory(100, CATEGORY2_ID)),
                new TicketWrapper(generateSku(1000, modelId2), generateModel(modelId2, 1000),
                        List.of(
                                generateSku(1000, modelId2)
                        ), true, generateTicketWithCategory(1000, CATEGORY2_ID))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());


        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(5);// 2 sku + 3 model (скю без оферов не трогаем)
        //assert transition
        long expectedTargetModelId = ticketWrappers.get(1).getTicket().getId() * -10;
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(1);
        assertThat(modelsRequest.getTransition(0).getOldEntityId()).isEqualTo(modelId2);
        assertThat(modelsRequest.getTransition(0).getOldEntityDeleted()).isEqualTo(true);
        assertThat(modelsRequest.getTransition(0).getNewEntityId()).isEqualTo(expectedTargetModelId);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel).extracting(ModelStorage.Model::getId).isEqualTo(expectedTargetModelId);
        assertThat(targetModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertModelHasRelations(targetModel, CATEGORY2_ID, 100L, 1000L);
        //старого параметра нет - модель пересоздалась
        GutginAssertions.assertThat(targetModel).doesNotContainParameterValues(targetModel.getId());

        ModelStorage.Model oldModel1 = takeModelWithId(modelId1, modelsRequest);
        assertThat(oldModel1).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertModelHasRelations(oldModel1, CATEGORY_ID, 101, 102);

        ModelStorage.Model oldModel2 = takeModelWithId(modelId2, modelsRequest);
        assertThat(oldModel2).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertThat(oldModel2).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(0);
        assertThat(oldModel2).extracting(ModelStorage.Model::getDeleted).isEqualTo(true);

        //assert skus
        ModelStorage.Model sku1 = modelsRequest.getModels(1);
        assertThat(sku1).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(expectedTargetModelId, sku1, CATEGORY2_ID);

        ModelStorage.Model sku2 = modelsRequest.getModels(2);
        assertThat(sku2).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(expectedTargetModelId, sku2, CATEGORY2_ID);

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //     model1(200)     model2(2000)                         model1(200)  model3(NEW)     model2(2000)
    //     /   |     \        |   \                                /   |       /   |  \            |
    //   sku3 sku2  sku1    sku4  sku5              ===>>>       sku3 sku2  sku1  sku4 sku6(NEW)  sku5
    //               |        |                                               new category
    //             offer1   offer2  offer3
    //                    group = 1
    @Test
    public void categoryChangeNewModelAndLeaveModelUntouched() {
        long modelId1 = 200L;
        long modelId2 = 2000L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), generateModel(modelId1, 100, 101, 102),
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1),
                                generateSku(102, modelId1)
                        ), true, generateTicketWithCategory(100, CATEGORY2_ID)),
                new TicketWrapper(generateSku(1000, modelId2), generateModel(modelId2, 1000, 1001),
                        List.of(
                                generateSku(1000, modelId2),
                                generateSku(1001, modelId2)
                        ), true, generateTicketWithCategory(1000, CATEGORY2_ID)),
                new TicketWrapper(null, null,
                        Collections.emptyList(), true, ticketWithoutMapping(CATEGORY2_ID))
        );
        long expectedNewSkuId = -ticketWrappers.get(2).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(6);// 3 sku + 3 model (скю без оферов не трогаем)
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        long expectedTargetModelId = ticketWrappers.get(2).getTicket().getId() * -10;
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel).extracting(ModelStorage.Model::getId).isEqualTo(expectedTargetModelId);
        assertThat(targetModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertModelHasRelations(targetModel, CATEGORY2_ID, 100L, 1000L, expectedNewSkuId);
        //старого параметра нет - модель пересоздалась
        GutginAssertions.assertThat(targetModel).doesNotContainParameterValues(targetModel.getId());

        ModelStorage.Model oldModel1 = takeModelWithId(modelId1, modelsRequest);
        assertThat(oldModel1).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertModelHasRelations(oldModel1, CATEGORY_ID, 101, 102);

        ModelStorage.Model oldModel2 = takeModelWithId(modelId2, modelsRequest);
        assertThat(oldModel2).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertModelHasRelations(oldModel2, CATEGORY_ID, 1001);

        //assert skus
        ModelStorage.Model sku1 = modelsRequest.getModels(1);
        assertThat(sku1).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(expectedTargetModelId, sku1, CATEGORY2_ID);

        ModelStorage.Model sku2 = modelsRequest.getModels(2);
        assertThat(sku2).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(expectedTargetModelId, sku2, CATEGORY2_ID);

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    private ModelStorage.Model takeModelWithId(long modelId,
                                               ModelStorage.SaveModelsRequest modelsRequest) {
        return modelsRequest.getModelsList().stream()
                .filter(model -> model.getId() == modelId).findFirst()
                .orElseThrow(() -> new IllegalStateException("Expecting other model beside targetModel"));
    }

    //     model1(200)                   model1(200)(NEW WITH OLD ID)
    //     /     |                            /                \
    //   sku1   sku2          ===>>>    sku1(NEW WITH OLD ID) sku2(NEW WITH OLD ID)
    //    |      |                               new category
    //  offer1  offer2
    //     group = 1
    @Test
    public void categoryChangeWithReuseOfCurrentModel() {
        long modelId1 = 200L;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(100, modelId1), generateModel(modelId1, 100, 101),
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1)
                        ), true, generateTicketWithCategory(100, CATEGORY2_ID)),
                new TicketWrapper(generateSku(101, modelId1), generateModel(modelId1, 100, 101),
                        List.of(
                                generateSku(100, modelId1),
                                generateSku(101, modelId1)
                        ), true, generateTicketWithCategory(101, CATEGORY2_ID))
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        assertThat(requestForGroup.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestForGroup.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(3);// 2 sku + 1 model
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(modelId1);
        assertThat(targetModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertModelHasRelations(targetModel, CATEGORY2_ID, 100L, 101L);
        //старого параметра нет - модель пересоздалась
        GutginAssertions.assertThat(targetModel).doesNotContainParameterValues(targetModel.getId());

        //assert skus
        ModelStorage.Model sku1 = modelsRequest.getModels(1);
        assertThat(sku1).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(modelId1, sku1, CATEGORY2_ID);

        ModelStorage.Model sku2 = modelsRequest.getModels(2);
        assertThat(sku2).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY2_ID);
        assertSkuHasRelationWithModel(modelId1, sku2, CATEGORY2_ID);

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    @Test
    public void testVendorParameter() {
        long modelId1 = 200L;
        ModelStorage.Model model = generateModel(modelId1, -100);
        // вендор модели недоступен для изменения
        model = model.toBuilder()
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(7893318)
                        .setTypeId(1)
                        .setOptionId(16644882)
                        .setXslName("vendor")
                        .setOwnerId(-1)
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build()).build();
        GcSkuTicket ticket = generateTicketWithCategory(-100, CATEGORY_ID);
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(
                        generateSku(-100, modelId1),
                        model,
                        List.of(
                                generateSku(-100, modelId1)
                        ),
                        true,
                        ticket
                )
        );

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                Collections.emptyList());

        //проверяем, что значение вендора пришло в скю из модели
        ModelStorage.ParameterValue parameterValue = requestForGroup.getModelsRequest(0)
                .getModelsList().stream().filter(sku -> sku.getId() == -100)
                .findFirst().get()
                .getParameterValuesList().stream().filter(p -> p.getParamId() == 7893318)
                .findFirst().get();
        assertThat(parameterValue.getOptionId()).isEqualTo(16644882);

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    @Test
    public void whenTargetCategoryChangeThenLeaveUnmodifiableParams() {
        long modelId1 = 200L;
        ModelStorage.Model sku = generateSku(100, modelId1).toBuilder()
                .setStrictChecksRequired(true)
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VOLUME.getId())
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .setXslName(VOLUME.getXslName())
                        .addStrValue(LocalizedStringUtils.defaultString("1"))
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build())
                .addParameterValues(ModelStorage.ParameterValue.newBuilder()
                        .setParamId(VENDOR_LINE.getId())
                        .addStrValue(ModelStorage.LocalizedString.newBuilder().setValue("VENDOR_LINE").build())
                        .setOwnerId(123)
                        .build())
                .addPictures(ModelStorage.Picture.newBuilder().setOrigMd5("orig").setUrl("url")
                        .setValueSource(ModelStorage.ModificationSource.OPERATOR_FILLED)
                        .build()
                )
                .addPictures(ModelStorage.Picture.newBuilder().setOrigMd5("orig2").setUrl("url2")
                        .setValueSource(ModelStorage.ModificationSource.VENDOR_OFFICE)
                        .build()
                )
                .build();

        ModelStorage.Model model = generateModel(modelId1, 100).toBuilder()
                .setStrictChecksRequired(true)
                .build();
        TicketWrapper ticketWrapper = new TicketWrapper(sku, model, true,
                generateTicketWithCategory(100, CATEGORY_ID));

        ModelCardApi.SaveModelsGroupRequest singleSkuRequest = cskuRequestCreator.createRequest(List.of(ticketWrapper));

        assertThat(singleSkuRequest.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = singleSkuRequest.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(2);// 1 sku + 1 model
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(modelId1);
        assertThat(targetModel).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertModelHasRelations(targetModel, CATEGORY_ID, 100L);
        //старого параметра нет - модель пересоздалась
        GutginAssertions.assertThat(targetModel).doesNotContainParameterValues(targetModel.getId());

        //assert sku
        ModelStorage.Model resultSku = modelsRequest.getModels(1);
        assertThat(resultSku).extracting(ModelStorage.Model::getCategoryId).isEqualTo(CATEGORY_ID);
        assertSkuHasRelationWithModel(modelId1, resultSku, CATEGORY_ID);
        List<ModelStorage.ParameterValue> volumePV = resultSku.getParameterValuesList().stream()
                .filter(parameterValue -> VOLUME.getId().equals(parameterValue.getParamId()))
                .collect(Collectors.toList());
        assertThat(volumePV).hasSize(1);
        assertThat(volumePV.get(0).getValueSource()).isEqualTo(ModelStorage.ModificationSource.OPERATOR_FILLED);

        GutginAssertions.assertThat(resultSku).doesNotContainParameterValues(VENDOR_LINE.getId());
        GutginAssertions.assertThat(resultSku).containsUrlPicturesExactlyInOrder("url");

        assertAllModelsHaveModelQualityParameter(singleSkuRequest);
    }

    //     model1(200)                                                  model2(NEW)    model1(200)
    //     /   |                                                             |              |
    //   sku1 sku2                                   ===>>>                 sku1           sku2
    //     |
    //  offer1
    //
    //             sku1 is updated to conflict with sku2
    @Test
    public void whenConflictedSkusFromDiffPartnersUnderTargetModelThenMoveOneToNewModel() {
        long modelId = 200L;
        long sku1 = 101;
        long sku2 = 102;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku1, modelId), generateModel(modelId, sku1, sku2),
                        List.of(
                                generateSku(sku1, modelId),
                                generateSku(sku2, modelId)
                        ), true, generateTicket(sku1))
        );

        Long tickedId = ticketWrappers.get(0).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, sku2));
        ModelStorageHelper.SaveGroupResponse requestPhase2 =
                new ModelStorageHelper.SaveGroupResponse(requestForGroup, null);
        ModelSaveVisualization.visualizeInputData(ticketWrappers);
        ModelSaveVisualization.visualizeFinalRequest(requestPhase2.getRequest());
        ModelCardApi.SaveModelsGroupRequest requestPhase3 =
                cskuRequestCreator.requestBasedOnOtherRequest(ticketWrappers,
                        requestPhase2, List.of(sku1, sku2));
        ModelSaveVisualization.visualizeFinalRequest(requestPhase3);


        assertThat(requestPhase3.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestPhase3.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(4);// 2 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(modelId);


        assertModelHasRelations(targetModel, CATEGORY_ID, sku2);

        ModelStorage.Model otherModel =
                takeModelWithId(-tickedId * 10, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku1);
        assertThat(otherModel.getCategoryId()).isEqualTo(CATEGORY_ID);

        List<Long> skusForModel1 = List.of(sku2);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == modelId || sku.getCurrentType().equals("GURU")) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1
                assertSkuHasRelationWithModel(modelId, sku);
            } else {
                //new model
                assertSkuHasRelationWithModel(otherModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }

    //     model1(200)                                                  model2(NEW)    model1(200)
    //     /   |   \   \                                                   |             |    |    \
    //   sku1 sku2 sku3 sku4                                ===>>>      sku1           sku2  sku3  sku4
    //     |
    //  offer1
    //
    //             sku1 is updated to conflict with sku2, and sku3 and sku4 already in conflict
    @Test
    public void whenConflictAlreadyUnderTargetAndNewConflictFromUpdateThenResolveNewConflict() {
        long modelId = 200L;
        long sku1 = 101;
        long sku2 = 102;
        long sku3 = 103;
        long sku4 = 104;
        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku1, modelId), generateModel(modelId, sku1, sku2, sku3, sku4),
                        List.of(
                                generateSku(sku1, modelId),
                                generateSku(sku2, modelId),
                                generateSku(sku3, modelId),
                                generateSku(sku4, modelId)
                        ), true, generateTicket(sku1))
        );

        Long tickedId = ticketWrappers.get(0).getTicket().getId();

        ModelCardApi.SaveModelsGroupRequest requestForGroup = cskuRequestCreator.createRequestForGroup(ticketWrappers,
                List.of(sku1, sku2, sku3, sku4));
        ModelStorageHelper.SaveGroupResponse requestPhase2 =
                new ModelStorageHelper.SaveGroupResponse(requestForGroup, null);
        ModelSaveVisualization.visualizeInputData(ticketWrappers);
        ModelSaveVisualization.visualizeFinalRequest(requestPhase2.getRequest());
        ModelCardApi.SaveModelsGroupRequest requestPhase3 =
                cskuRequestCreator.requestBasedOnOtherRequest(ticketWrappers,
                        requestPhase2, List.of(sku1, sku2, sku3, sku4));
        ModelSaveVisualization.visualizeFinalRequest(requestPhase3);


        assertThat(requestPhase3.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = requestPhase3.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(6);// 4 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert model
        ModelStorage.Model targetModel = modelsRequest.getModels(0);
        assertThat(targetModel.getId()).isEqualTo(modelId);


        assertModelHasRelations(targetModel, CATEGORY_ID, sku2, sku3, sku4);

        ModelStorage.Model otherModel =
                takeModelWithId(-tickedId * 10, modelsRequest);
        assertThat(otherModel.getRelationsList()).extracting(ModelStorage.Relation::getId).containsOnly(sku1);
        assertThat(otherModel.getCategoryId()).isEqualTo(CATEGORY_ID);

        List<Long> skusForModel1 = List.of(sku2, sku3, sku4);
        for (int i = 1; i < modelsRequest.getModelsCount(); i++) {
            ModelStorage.Model sku = modelsRequest.getModels(i);
            if (sku.getId() == modelId || sku.getCurrentType().equals("GURU")) {
                continue;
            }
            if (skusForModel1.contains(sku.getId())) {
                //model1
                assertSkuHasRelationWithModel(modelId, sku);
            } else {
                //new model
                assertSkuHasRelationWithModel(otherModel.getId(), sku);
                assertThat(sku.getCategoryId()).isEqualTo(CATEGORY_ID);
            }
        }

        assertAllModelsHaveModelQualityParameter(requestForGroup);
    }


    @Test
    public void dontChangeNameInMSKU() {
        long modelId = 200L;
        long sku1 = 101;

        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateMsku(sku1, modelId), generateMModel(modelId, sku1),
                        List.of(
                                generateMsku(sku1, modelId)
                        ), true, generateTicket(sku1))
        );


        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        ModelStorage.SaveModelsRequest modelsRequest = request.getModelsRequest(0);
        ModelStorage.Model resultMmodel = modelsRequest.getModels(0);
        ModelStorage.Model resultMsku = modelsRequest.getModels(1);

        GutginAssertions.assertThat(resultMmodel).containParameterWithValue(NAME.getId(), String.valueOf(modelId));
        GutginAssertions.assertThat(resultMmodel).doesNotContainParameterValues(USE_NAME_AS_TITLE.getId());

        GutginAssertions.assertThat(resultMsku).containParameterWithValue(NAME.getId(), String.valueOf(sku1));
        GutginAssertions.assertThat(resultMsku).doesNotContainParameterValues(USE_NAME_AS_TITLE.getId());
    }

    @Test
    public void changeNameInPSKU() {
        long modelId = 200L;
        long sku1 = 101;

        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateSku(sku1, modelId), generateModel(modelId, sku1),
                        List.of(
                                generateSku(sku1, modelId)
                        ), true, generateTicket(sku1))
        );


        ModelCardApi.SaveModelsGroupRequest request = cskuRequestCreator.createRequest(ticketWrappers);
        ModelStorage.SaveModelsRequest modelsRequest = request.getModelsRequest(0);
        ModelStorage.Model resultMmodel = modelsRequest.getModels(0);
        ModelStorage.Model resultMsku = modelsRequest.getModels(1);

        GutginAssertions.assertThat(resultMmodel).containParameterWithValue(NAME.getId(), "group_name_from_offer");
        GutginAssertions.assertThat(resultMmodel).containParameterWithValue(USE_NAME_AS_TITLE.getId(), true);

        GcSkuTicket ticket = ticketWrappers.get(0).getTicket();
        GutginAssertions.assertThat(resultMsku).containParameterWithValue(NAME.getId(),
                "Name for Offer(bizId = " + ticket.getPartnerShopId() +
                        ", offerId = " + ticket.getShopSku() + ")");
        GutginAssertions.assertThat(resultMmodel).containParameterWithValue(USE_NAME_AS_TITLE.getId(), true);
    }


    //     M_model1(200)  M_model2(201)                    M_model1(200)  M_model2(201)
    //        |              |                                |              |
    //      M_sku1        M_sku2              ===>>>        M_sku1        M_sku2
    //        |              |
    //      offer1        offer2
    //              group = 1
    @Test
    public void whenMskuGroupDoNotUniteMultipleModels() {
        long modelId1 = 200L;
        long modelId2 = 201L;
        long skuId1 = 101;
        long skuId2 = 102;

        List<TicketWrapper> ticketWrappers = List.of(
                new TicketWrapper(generateMsku(skuId1, modelId1), generateMModel(modelId1, skuId1),
                        List.of(
                                generateMsku(skuId1, modelId1)
                        ), true, generateTicket(skuId1)),
                new TicketWrapper(generateMsku(skuId2, modelId2), generateMModel(modelId2, skuId2),
                        List.of(
                                generateMsku(skuId2, modelId2)
                        ), true, generateTicket(skuId2))
        );
        ticketWrappers.forEach(ticketWrapper -> ticketWrapper.getTicket().setType(GcSkuTicketType.CSKU_MSKU));

        ModelCardApi.SaveModelsGroupRequest request =
                cskuRequestCreator.createRequestForGroup(ticketWrappers, Collections.emptyList());

        assertThat(request.getModelsRequestCount()).isEqualTo(1);
        ModelStorage.SaveModelsRequest modelsRequest = request.getModelsRequest(0);
        assertThat(modelsRequest.getModelsCount()).isEqualTo(4);// 2 sku + 2 models
        //assert transition
        assertThat(modelsRequest.getTransitionCount()).isEqualTo(0);
        //assert sku
        ModelStorage.Model sku1 = modelsRequest.getModels(0);
        assertTrue(MigrationUtils.isMSku(sku1));
        assertThat(sku1).extracting(ModelStorage.Model::getId).isEqualTo(skuId1);
        assertSkuHasRelationWithModel(modelId1, sku1);

        ModelStorage.Model sku2 = modelsRequest.getModels(1);
        assertTrue(MigrationUtils.isMSku(sku2));
        assertThat(sku2).extracting(ModelStorage.Model::getId).isEqualTo(skuId2);
        assertSkuHasRelationWithModel(modelId2, sku2);

        //assert models
        assertTrue(MigrationUtils.isMModel(modelsRequest.getModels(2)));
        assertTrue(MigrationUtils.isMModel(modelsRequest.getModels(3)));

        ModelStorage.Model model1 = takeModelWithId(modelId1, modelsRequest);
        assertModelHasRelations(model1, CATEGORY_ID, skuId1);
        ModelStorage.Model model2 = takeModelWithId(modelId2, modelsRequest);
        assertModelHasRelations(model2, CATEGORY_ID, skuId2);
    }

    private void assertSkuHasRelationWithModel(long model, ModelStorage.Model sku) {
        assertSkuHasRelationWithModel(model, sku, CATEGORY_ID);
    }

    private void assertSkuHasRelationWithModel(long model, ModelStorage.Model sku, long categoryId) {
        assertThat(sku).extracting(ModelStorage.Model::getRelationsCount).isEqualTo(1);
        assertThat(sku).extracting(_sku -> _sku.getRelations(0).getId()).isEqualTo(model);
        assertThat(sku).extracting(_sku -> _sku.getRelations(0).getCategoryId()).isEqualTo(categoryId);
    }

    private GcSkuTicket generateTicket(long skuId) {
        return generateTicketWithCategory(skuId, CATEGORY_ID);
    }

    private GcSkuTicket generateTicketWithCategory(long skuId, long categoryId) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(categoryId))
                    .withGroupId(1)
                    .withGroupName("group_name_from_offer")
                    .withStringParam(100, "asd")
                    .withBarCodes("123")
                    .build();
        });
        GcSkuTicket gcSkuTicket = gcSkuTickets.get(0);
        gcSkuTicket.setCategoryId(categoryId);
        gcSkuTicket.setExistingMboPskuId(skuId);
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets.get(0);
    }

    private void assertModelHasRelations(ModelStorage.Model model, long categoryId, long... skuIds) {
        List<Long> expectedSkuIds = Longs.asList(skuIds);
        assertThat(model.getRelationsList()).hasSize(expectedSkuIds.size());
        assertThat(model.getRelationsList()).extracting(ModelStorage.Relation::getId)
                .containsExactlyInAnyOrderElementsOf(expectedSkuIds);
        assertThat(model.getRelationsList()).extracting(ModelStorage.Relation::getCategoryId)
                .containsOnly(categoryId);
        assertThat(model.getRelationsList()).extracting(ModelStorage.Relation::getType)
                .containsOnly(ModelStorage.RelationType.SKU_MODEL);
    }

    private GcSkuTicket ticketWithoutMapping() {
        return ticketWithoutMapping(CATEGORY_ID);
    }

    private GcSkuTicket ticketWithoutMapping(long category) {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(category))
                    .withGroupId(1)
                    .withGroupName("group_name_from_offer")
                    .withStringParam(100, "asd")
                    .build();
        });
        gcSkuTickets.forEach(ticket -> ticket.setCategoryId(category));
        gcSkuTicketDao.update(gcSkuTickets);
        return gcSkuTickets.get(0);
    }

    private GcSkuTicket generateTicketWith() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialStateNew(1, datacampOffers -> {
            datacampOffers.get(0).getDcpOfferBuilder()
                    .withCategory(Math.toIntExact(CATEGORY_ID))
                    .withStringParam(1111, "1111value")
                    .build();
        });
        GcSkuTicket gcSkuTicket = gcSkuTickets.get(0);
        gcSkuTicket.setDcpGroupId(null);
        gcSkuTicketDao.update(gcSkuTicket);
        return gcSkuTickets.get(0);
    }



    private PictureWrapper buildSizedPictureWrapper(String url, String md5, ModelStorage.ModificationSource type,
                                                    int width, int height) {
        ModelStorage.Picture picture = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5)
                .setWidth(width)
                .setHeight(height)
                .setHasText(false)
                .build();
        // needed because of hasText = false
        return PictureWrapper.forSku(picture, null);
    }



    private PictureWrapper buildPictureWrapper(String url, String md5, ModelStorage.ModificationSource type) {
        ModelStorage.Picture picture = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5)
                .build();
        return PictureWrapper.forOffer(picture, 0);
    }

    private PictureWrapper buildPictureWrapperWithOwnerId(String url,
                                                          String md5,
                                                          ModelStorage.ModificationSource type,
                                                          Long ownerId
    ) {
        ModelStorage.Picture picture = ModelStorage.Picture.newBuilder()
                .setUrlOrig(url)
                .setValueSource(type)
                .setOrigMd5(md5)
                .setOwnerId(ownerId)
                .build();
        return PictureWrapper.forOffer(picture, 0);
    }

    private void assertSkuRelation(ModelStorage.Relation skuRelation, long modelId, long categoryId) {
        assertThat(skuRelation).extracting(ModelStorage.Relation::getId).isEqualTo(modelId);
        assertThat(skuRelation).extracting(ModelStorage.Relation::getCategoryId).isEqualTo(categoryId);
        assertThat(skuRelation).extracting(ModelStorage.Relation::getType)
                .isEqualTo(ModelStorage.RelationType.SKU_PARENT_MODEL);
    }

    private void assertAllModelsHaveModelQualityParameter(ModelCardApi.SaveModelsGroupRequest request) {
        request.getModelsRequestList().forEach(saveModelsRequest -> {
            saveModelsRequest.getModelsList().stream()
                    .filter(MigrationUtils::isPModel20)
                    .filter(model -> !model.getDeleted())
                    .forEach(model -> GutginAssertions.assertThat(model)
                            .containParameterValues(KnownParameters.MODEL_QUALITY.getId())
                    );
        });
    }
}
