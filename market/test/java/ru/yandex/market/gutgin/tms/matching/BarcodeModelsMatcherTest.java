package ru.yandex.market.gutgin.tms.matching;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.entity.Model;
import ru.yandex.market.partner.content.common.entity.ParameterType;
import ru.yandex.market.partner.content.common.entity.ParameterValue;
import ru.yandex.market.partner.content.common.entity.Sku;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.message.Messages;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class BarcodeModelsMatcherTest {
    private static final int VENDOR_ID = 123;
    private static final long CATEGORY_ID = 1;
    private static final long ANOTHER_CATEGORY_ID = 2;
    private static final String CATEGORY_NAME = "CategoryName";

    private static final long BARCODE_MODEL_ID = 12;
    private static final long NOT_PUBLISHED_BARCODE_MODEL_ID = 13;
    private static final long MODEL_IN_ANOTHER_CATEGORY_ID = 14;
    private static final long BARCODE_SKU_ID = 15;
    private static final String BARCODE = "1234567890";
    private static final String NOT_PUBLISHED_BARCODE = "0000000000";
    private static final String ANOTHER_CATEGORY_BARCODE = "111111111";
    private static final String SKU_BARCODE = "01234567890";

    private static final ModelStorage.Model MODEL_WITH_BARCODE = ModelStorage.Model.newBuilder().setId(BARCODE_MODEL_ID)
        .setCategoryId(CATEGORY_ID).setCurrentType(ModelStorage.ModelType.GURU.name())
        .setDeleted(false).setPublished(true)
        .addParameterValues(ParameterValueComposer.buildStringParam(
            ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE, BARCODE,
            ModelStorage.ModificationSource.OPERATOR_FILLED))
        .build();

    private static final ModelStorage.Model NOT_PUBLISHED_MODEL_WITH_BARCODE = ModelStorage.Model.newBuilder()
        .setId(NOT_PUBLISHED_BARCODE_MODEL_ID).setCurrentType(ModelStorage.ModelType.GURU.name())
        .setCategoryId(CATEGORY_ID).setDeleted(false).setPublished(false)
        .addParameterValues(ParameterValueComposer.buildStringParam(
            ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE, NOT_PUBLISHED_BARCODE,
            ModelStorage.ModificationSource.OPERATOR_FILLED))
        .build();

    private static final ModelStorage.Model MODEL_IN_ANOTHER_CATEGORY = ModelStorage.Model.newBuilder()
        .setId(MODEL_IN_ANOTHER_CATEGORY_ID).setCurrentType(ModelStorage.ModelType.GURU.name())
        .setCategoryId(ANOTHER_CATEGORY_ID).setDeleted(false).setPublished(true)
        .addParameterValues(ParameterValueComposer.buildStringParam(
            ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE, ANOTHER_CATEGORY_BARCODE,
            ModelStorage.ModificationSource.OPERATOR_FILLED))
        .build();

    private static final ModelStorage.Model SKU_WITH_BARCODE = ModelStorage.Model.newBuilder().setId(BARCODE_SKU_ID)
        .setCategoryId(CATEGORY_ID).setCurrentType(ModelStorage.ModelType.SKU.name())
        .setDeleted(false).setPublished(true)
        .addParameterValues(ParameterValueComposer.buildStringParam(
            ParameterValueComposer.BARCODE_ID,
            ParameterValueComposer.BARCODE, SKU_BARCODE,
            ModelStorage.ModificationSource.OPERATOR_FILLED))
        .addRelations(ModelStorage.Relation.newBuilder()
            .setCategoryId(CATEGORY_ID).setId(BARCODE_MODEL_ID).setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
            .build())
        .build();


    private BarcodeModelsMatcher barcodeModelsMatcher;
    private ModelStorageServiceMock modelStorageService = new ModelStorageServiceMock();

    @Mock
    private CategoryDataHelper categoryDataHelper;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        modelStorageService.putModels(
            MODEL_WITH_BARCODE, NOT_PUBLISHED_MODEL_WITH_BARCODE, MODEL_IN_ANOTHER_CATEGORY, SKU_WITH_BARCODE
        );

        when(categoryDataHelper.getCategoryName(anyLong())).thenReturn(CATEGORY_NAME);

        ModelStorageHelper modelStorageHelper = new ModelStorageHelper(modelStorageService, modelStorageService);
        barcodeModelsMatcher = new BarcodeModelsMatcher(modelStorageHelper, categoryDataHelper);
    }

    @Test
    public void whenMatchModelsThenMatchByBarCode() {
        Model modelWithBarcode = createModelWithBarcode(BARCODE);

        List<Model> models = Collections.singletonList(modelWithBarcode);
        Multimap<Model, MessageInfo> messageInfos = ArrayListMultimap.create();
        barcodeModelsMatcher.matchModels(
            CATEGORY_ID,
            models,
            messageInfos::put
        );

        List<Model> matchedModels = models.stream()
            .filter(m -> m.getGuruModelId() != null)
            .collect(Collectors.toList());

        assertThat(messageInfos.isEmpty()).isTrue();
        assertThat(matchedModels).extracting(Model::getGuruModelId).containsExactly(BARCODE_MODEL_ID);
    }

    @Test
    public void whenMatchNotPublishedModelsThenNotMatchByBarCode() {
        Model partnerModelWithBarcode = createModelWithBarcode(NOT_PUBLISHED_BARCODE);

        List<Model> models = Collections.singletonList(partnerModelWithBarcode);
        Multimap<Model, MessageInfo> messageInfos = ArrayListMultimap.create();
        barcodeModelsMatcher.matchModels(
            CATEGORY_ID,
            models,
            messageInfos::put
        );
        List<Model> matchedModels = models.stream()
            .filter(m -> m.getGuruModelId() != null)
            .collect(Collectors.toList());

        assertThat(messageInfos.isEmpty());
        assertThat(matchedModels).isEmpty();
    }

    @Test
    public void whenMatchModelsInAnotherCategoryValidationFalse() {
        Model partnerModelWithBarcode = createModelWithBarcode(ANOTHER_CATEGORY_BARCODE);

        List<Model> models = Collections.singletonList(partnerModelWithBarcode);
        Multimap<Model, MessageInfo> messageInfos = ArrayListMultimap.create();
        barcodeModelsMatcher.matchModels(
            CATEGORY_ID,
            models,
            messageInfos::put
        );

        List<Model> matchedModels = models.stream()
            .filter(m -> m.getGuruModelId() != null)
            .collect(Collectors.toList());

        assertThat(messageInfos.values()).extracting(MessageInfo::toString)
            .containsExactlyInAnyOrder(
                Messages.get()
                    .modelExistInAnotherCategory(new String[0],
                        "model1",
                        MODEL_IN_ANOTHER_CATEGORY_ID,
                        CATEGORY_ID,
                        CATEGORY_NAME)
                    .toString()
            );
        assertThat(matchedModels).isEmpty();
    }

    @Test
    public void whenMatchModelsBySkuThenMatchByBarCode() {
        Model modelWithBarcode = createModelWithSkuWithBarcode(SKU_BARCODE);

        List<Model> models = Collections.singletonList(modelWithBarcode);
        Multimap<Model, MessageInfo> messageInfos = ArrayListMultimap.create();
        barcodeModelsMatcher.matchModels(
            CATEGORY_ID,
            models,
            messageInfos::put
        );

        List<Model> matchedModels = models.stream()
            .filter(m -> m.getGuruModelId() != null)
            .collect(Collectors.toList());

        assertThat(messageInfos.isEmpty()).isTrue();
        assertThat(matchedModels).extracting(Model::getGuruModelId).containsExactly(BARCODE_MODEL_ID);
        Model matchedModel = matchedModels.get(0);
        assertThat(matchedModel.getSkuList()).extracting(Sku::getSkutchedSkuId).containsExactly(BARCODE_SKU_ID);
    }


    private Model createModelWithBarcode(String barcode) {
        ParameterValue barcodeParam = new ParameterValue();
        barcodeParam.setType(ParameterType.STRING);
        barcodeParam.setParamId(ParameterValueComposer.BARCODE_ID);
        barcodeParam.setStringValue(barcode);
        ParameterValue vendorParam = new ParameterValue();
        vendorParam.setType(ParameterType.ENUM);
        vendorParam.setParamId(ParameterValueComposer.VENDOR_ID);
        vendorParam.setOptionId(VENDOR_ID);
        Model model = new Model();
        model.setName("model1");
        model.setCategoryId(1);
        model.setSourceId(2);
        model.setSkuList(Collections.emptyList());
        model.setParameterList(ImmutableList.of(vendorParam, barcodeParam));
        model.setAliases(Collections.emptyList());
        return model;
    }

    private Model createModelWithSkuWithBarcode(String barcode) {
        ParameterValue barcodeParam = new ParameterValue();
        barcodeParam.setType(ParameterType.STRING);
        barcodeParam.setParamId(ParameterValueComposer.BARCODE_ID);
        barcodeParam.setStringValue(barcode);
        ParameterValue vendorParam = new ParameterValue();
        vendorParam.setType(ParameterType.ENUM);
        vendorParam.setParamId(ParameterValueComposer.VENDOR_ID);
        vendorParam.setOptionId(VENDOR_ID);
        Sku sku = new Sku();
        sku.setParameterList(Collections.singletonList(barcodeParam));
        sku.setShopSku("shop_sku");
        Model model = new Model();
        model.setName("model1");
        model.setCategoryId(1);
        model.setSourceId(2);
        model.setSkuList(Collections.singletonList(sku));
        model.setParameterList(ImmutableList.of(vendorParam));
        model.setAliases(Collections.emptyList());
        return model;
    }

}
