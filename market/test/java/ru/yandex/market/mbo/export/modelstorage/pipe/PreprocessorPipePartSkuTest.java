package ru.yandex.market.mbo.export.modelstorage.pipe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.export.modelstorage.DummyPipePart;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.Video;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PreprocessorPipePartSkuTest {

    private ModelPipePart preprocessorPipePart;

    Map<Long, CategoryParam> parameters;
    private ModelStorage.Model protoParentModel;
    private ModelStorage.Model protoModification;
    private ModelStorage.Model protoSku1;
    private ModelStorage.Model protoSku2;
    private DummyPipePart output;

    @Before
    public void setUp() throws Exception {
        parameters = ParametersBuilder.startParameters()
            .startParameter()
                .id(1).type(Param.Type.ENUM).xsl("param1").name("Param1")
            .endParameter()
            .startParameter()
                .id(2).type(Param.Type.ENUM).xsl("param2").name("Param2")
            .endParameter()
            .startParameter()
                .id(3).type(Param.Type.STRING).xsl(XslNames.BAR_CODE).name("Barcode")
                .multifield(true)
                .level(CategoryParam.Level.MODEL)
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .startParameter()
                .id(4).type(Param.Type.STRING).xsl(XslNames.VENDOR_CODE).name("Vendor code")
                .multifield(true)
                .level(CategoryParam.Level.MODEL)
                .skuParameterMode(SkuParameterMode.SKU_INFORMATIONAL)
            .endParameter()
            .startParameter()
                .id(KnownIds.VENDOR_PARAM_ID).type(Param.Type.ENUM).xsl(XslNames.VENDOR).name("Vendor")
            .endParameter()
            .endParameters().stream()
            .collect(Collectors.toMap(CategoryParam::getId, Function.identity()));
        output = new DummyPipePart();
    }

    @Test
    public void testPickerImagesInheritanceFromModelToSku() throws IOException {
        setUpPickerModels();
        preprocessorPipePart = createPipePart(false, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(protoParentModel, Collections.emptyList(),
            Collections.singletonList(protoSku1));
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);

        CommonModel sku = output.getSkus().get(0);
        Map<Long, ParameterValue> parameterValueMap = sku.getParameterValues(1).stream()
            .collect(Collectors.toMap(ParameterValue::getOptionId, Function.identity()));
        PickerImage picker11 = parameterValueMap.get(11L).getPickerImage();
        PickerImage picker12 = parameterValueMap.get(12L).getPickerImage();
        PickerImage picker13 = parameterValueMap.get(13L).getPickerImage();
        PickerImage picker14 = parameterValueMap.get(14L).getPickerImage();
        PickerImage picker15 = parameterValueMap.get(15L).getPickerImage();

        assertThat(picker11).hasFieldOrPropertyWithValue("url", "url1_11");
        assertThat(picker12).hasFieldOrPropertyWithValue("url", "url1_12");
        assertThat(picker13).isNull();
        assertThat(picker14).hasFieldOrPropertyWithValue("url", "url1_14");
        assertThat(picker15).isNull();
    }

    @Test
    public void testPickerImagesInheritanceFromModificationToSku() throws IOException {
        setUpPickerModels();
        preprocessorPipePart = createPipePart(true, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(protoParentModel,
            Collections.singleton(protoModification),
            Collections.singleton(protoSku2));
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);

        CommonModel sku = output.getSkus().get(0);
        Map<Long, ParameterValue> parameterValueMap = sku.getParameterValues(1).stream()
            .collect(Collectors.toMap(ParameterValue::getOptionId, Function.identity()));
        PickerImage picker11 = parameterValueMap.get(11L).getPickerImage();
        PickerImage picker12 = parameterValueMap.get(12L).getPickerImage();
        PickerImage picker13 = parameterValueMap.get(13L).getPickerImage();
        PickerImage picker14 = parameterValueMap.get(14L).getPickerImage();
        PickerImage picker15 = parameterValueMap.get(15L).getPickerImage();

        assertThat(picker11).hasFieldOrPropertyWithValue("url", "url1_000");
        assertThat(picker12).hasFieldOrPropertyWithValue("url", "url1_12");
        assertThat(picker13).hasFieldOrPropertyWithValue("url", "url1_13");
        assertThat(picker14).isNull();
        assertThat(picker15).isNull();
    }

    @Test
    public void testPicturesInheritanceFromAllSkuToModel() throws IOException {
        List<Picture> uniquePictures = setUpSkuPicturesToModel();

        preprocessorPipePart = createPipePart(false, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoParentModel, Collections.emptyList(), Stream.of(protoSku1, protoSku2).collect(Collectors.toList())
        );
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);


        List<Picture> modelPictures = output.getModel().getPictures();

        // cause BigPicture equals XL-Picture
        assertEquals(modelPictures.size(), uniquePictures.size() + 1);

        List<String> pictureXslNameByIndex = Stream.of(
            XslNames.BIG_PICTURE,
            XslNames.XL_PICTURE,
            XslNames.XL_PICTURE + "_" + 2,
            XslNames.XL_PICTURE + "_" + 3
        ).collect(Collectors.toList());

        int uniquePicturesIterator = 0;

        for (int i = 0; i < modelPictures.size(); i++) {
            Picture expectedPicture = uniquePictures.get(uniquePicturesIterator);

            assertPicture(
                modelPictures.get(i),
                pictureXslNameByIndex.get(i),
                expectedPicture.getUrl(),
                expectedPicture.getOrigMd5()
            );

            if (i > 0) {
                uniquePicturesIterator += 1;
            }
        }
    }

    @Test
    public void testFilteringSkuPictures() throws IOException {
        List<Picture> uniquePicturesForSku = setUpFilteringSkuPictures();

        preprocessorPipePart = createPipePart(false, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoParentModel, Collections.emptyList(), Collections.singletonList(protoSku1)
        );
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);


        assertEquals(output.getSkus().size(), 1);

        List<Picture> skuPictures = output.getSkus().get(0).getPictures();

        // cause BigPicture and XL-Picture are same
        assertEquals(skuPictures.size(), uniquePicturesForSku.size() + 1);

        List<String> pictureXslNameByIndex = Stream.of(
            XslNames.BIG_PICTURE,
            XslNames.XL_PICTURE
        ).collect(Collectors.toList());

        for (int i = 0; i < skuPictures.size(); i++) {
            Picture expectedPicture = uniquePicturesForSku.get(0);

            assertPicture(
                skuPictures.get(i),
                pictureXslNameByIndex.get(i),
                expectedPicture.getUrl(),
                expectedPicture.getOrigMd5()
            );
        }
    }

    @Test
    public void testFilteringSkuVideos() throws IOException {
        List<Video> uniqueVideos = setUpFilteringSkuVideos();

        preprocessorPipePart = createPipePart(false, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoParentModel, Collections.emptyList(), Collections.singletonList(protoSku1)
        );
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);


        assertEquals(output.getSkus().size(), 1);

        List<Video> skuVideos = output.getSkus().get(0).getVideos();

        // cause BigPicture and XL-Picture are same
        assertEquals(skuVideos.size(), uniqueVideos.size());
        assertEquals(skuVideos.stream()
            .map(Video::getUrl)
            .collect(Collectors.toList()), uniqueVideos.stream()
            .map(Video::getUrl)
            .collect(Collectors.toList()));
    }

    @Test
    public void testSkuToModelParams() throws IOException {
        setUpSkuToModel(false);
        preprocessorPipePart = createPipePart(false, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(protoParentModel,
            Collections.emptyList(),
            Arrays.asList(protoSku1, protoSku2));
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);

        CommonModel model = output.getModel();
        assertThat(model.getFlatParameterValues()).containsExactlyInAnyOrder(
            createEnumValue(KnownIds.VENDOR_PARAM_ID, 1).setModificationSource(ModificationSource.OPERATOR_FILLED),
            createEnumValue(1, 1),
            createStringValue(3, "modelbarcode", "barcode3", "barcode4", "barcode5"),
            createStringValue(4, "vendor_code1", "vendor_code2", "vendor_code3", "vendor_code4")
        );
    }

    @Test
    public void testSkuToModificationParams() throws IOException {
        setUpSkuToModel(true);
        preprocessorPipePart = createPipePart(true, output);

        ModelPipeContext modelPipeContext = new ModelPipeContext(protoParentModel,
            Collections.singletonList(protoModification),
            Arrays.asList(protoSku1, protoSku2));
        preprocessorPipePart.acceptModelsGroup(modelPipeContext);

        CommonModel model = output.getModel();
        assertThat(model.getFlatParameterValues()).containsExactlyInAnyOrder(
            createEnumValue(KnownIds.VENDOR_PARAM_ID, 1).setModificationSource(ModificationSource.OPERATOR_FILLED),
            createEnumValue(1, 1),
            createStringValue(3, "modelbarcode")
        );

        CommonModel modification = output.getModifications().get(0);
        assertThat(modification.getFlatParameterValues()).containsExactlyInAnyOrder(
            createEnumValue(KnownIds.VENDOR_PARAM_ID, 1).setModificationSource(ModificationSource.OPERATOR_FILLED),
            createEnumValue(1, 2),
            createEnumValue(2, 3),
            createStringValue(3, "barcode1", "barcode2", "barcode3", "barcode4", "barcode5"),
            createStringValue(4, "vendor_code1", "vendor_code2", "vendor_code3", "vendor_code4")
        );
    }

    private ParameterValue createEnumValue(long paramId, long value) {
        return new ParameterValue(parameters.get(paramId), value);
    }

    private ParameterValue createStringValue(long paramId, String... values) {
        return new ParameterValue(parameters.get(paramId), WordUtil.defaultWords(values));
    }

    private ModelPipePart createPipePart(boolean groupCategory, ModelPipePart output) {
        TovarCategory tovarCategory = new TovarCategory(0);
        if (groupCategory) {
            tovarCategory.setGuruCategoryId(17);
            tovarCategory.setGroup(true);
            assertThat(ModelPipePart.isExtGrouped(tovarCategory)).isTrue();
        }

        CategoryInfo info = CategoryInfo.forCategory(
            tovarCategory, new TMTemplate("", "", null, "", "", ""),
            new ArrayList<>(parameters.values()), Collections.emptyList(), null, null
        );
        CategoryInfo category = info;
        return Pipe.simple(
            new PreprocessorPipePart(category),
            output
        );
    }

    private void setUpSkuToModel(boolean hasModification) {
        long skuParent = 1;
        protoParentModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .computeIf(!hasModification,
                o -> o.modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL))
            .computeIf(!hasModification,
                o -> o.modelRelation(22, 1, ModelRelation.RelationType.SKU_MODEL))
            .startParameterValue()
                .paramId(1).xslName("option1").optionId(1)
            .endParameterValue()
            .startParameterValue()
                .paramId(3).xslName(XslNames.BAR_CODE).words("modelbarcode")
            .endParameterValue()
            .getRawModel();
        if (hasModification) {
            skuParent = 2;
            protoModification = CommonModelBuilder.newBuilder(2, 1, 1)
                .parentModelId(1)
                .currentType(CommonModel.Source.GURU)
                .modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL)
                .modelRelation(22, 1, ModelRelation.RelationType.SKU_MODEL)
                .startParameterValue()
                    .paramId(1).xslName("option2").optionId(2)
                .endParameterValue()
                .startParameterValue()
                    .paramId(2).xslName("option3").optionId(3)
                .endParameterValue()
                .startParameterValue()
                    .paramId(3).xslName(XslNames.BAR_CODE).words("barcode1", "barcode2")
                .endParameterValue()
                .getRawModel();
        }
        protoSku1 = CommonModelBuilder.newBuilder(11, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(skuParent, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .startParameterValue()
                .paramId(3).xslName(XslNames.BAR_CODE).words("barcode3", "barcode4")
            .endParameterValue()
            .startParameterValue()
                .paramId(4).xslName(XslNames.VENDOR_CODE).words("vendor_code1", "vendor_code2")
            .endParameterValue()
            .getRawModel();
        protoSku2 = CommonModelBuilder.newBuilder(22, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(skuParent, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .startParameterValue()
                .paramId(3).xslName(XslNames.BAR_CODE).words("barcode5")
            .endParameterValue()
            .startParameterValue()
                .paramId(4).xslName(XslNames.VENDOR_CODE).words("vendor_code3", "vendor_code4")
            .endParameterValue()
            .getRawModel();
    }

    private void setUpPickerModels() {
        protoParentModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL)
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(11).pickerImage("url1_11")
            .endParameterValue()
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(12).pickerImage("url1_12")
            .endParameterValue()
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(14).pickerImage("url1_14")
            .endParameterValue()
            .getRawModel();
        protoModification = CommonModelBuilder.newBuilder(2, 1, 1)
            .parentModelId(1)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(22, 1, ModelRelation.RelationType.SKU_MODEL)
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(11).pickerImage("url1_000")
            .endParameterValue()
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(13).pickerImage("url1_13")
            .endParameterValue()
            .startParameterValueLink()
                .paramId(1).xslName("option1").optionId(14)
            .endParameterValue()
            .getRawModel();
        protoSku1 = CommonModelBuilder.newBuilder(11, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1, "option1", 11, 12, 13, 14, 15)
            .getRawModel();
        protoSku2 = CommonModelBuilder.newBuilder(22, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(2, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .parameterValues(1, "option1", 11, 12, 13, 14, 15)
            .getRawModel();
    }

    private List<Picture> setUpSkuPicturesToModel() {
        protoParentModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(22, 1, ModelRelation.RelationType.SKU_MODEL)
            .getRawModel();

        Picture picture1 = new Picture();
        picture1.setUrl("url1");
        picture1.setOrigMd5("md5_1");

        Picture picture2 = new Picture();
        picture2.setUrl("url1");
        picture2.setOrigMd5("md5_2");

        Picture picture3 = new Picture();
        picture3.setUrl("url2");
        picture3.setOrigMd5("md5_1");

        Picture picture4 = new Picture();
        picture4.setUrl("url3");
        picture4.setOrigMd5("md5_3");

        Picture picture5 = new Picture();
        picture5.setUrl("url5");

        Picture picture6 = new Picture();
        picture6.setUrl("url1");
        picture6.setOrigMd5("md5_6");

        Picture picture7 = new Picture();
        picture7.setUrl("url7");
        picture7.setOrigMd5("md5_3");

        // pictures that should be in the parent model
        List<Picture> uniquePictures = Stream.of(picture1, picture4, picture5).collect(Collectors.toList());

        protoSku1 = CommonModelBuilder.newBuilder(11, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .picture(picture1)
            .picture(picture4)
            .picture(picture6)
            .picture(picture7)
            .getRawModel();
        protoSku2 = CommonModelBuilder.newBuilder(22, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(2, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .picture(picture2)
            .picture(picture3)
            .picture(picture5)
            .getRawModel();

        return uniquePictures;
    }

    private void assertPicture(Picture picture, String xslName, String url, String md5) {
        assertEquals(xslName, picture.getXslName());
        assertEquals(url, picture.getUrl());
        assertEquals(md5, picture.getOrigMd5());
    }

    private List<Picture> setUpFilteringSkuPictures() {
        protoParentModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL)
            .getRawModel();

        Picture picture1 = new Picture();
        picture1.setUrl("url1");
        picture1.setOrigMd5("md5_1");

        Picture picture2 = new Picture();
        picture2.setUrl("url1");
        picture2.setOrigMd5("md5_2");

        Picture picture3 = new Picture();
        picture3.setUrl("url3");
        picture3.setOrigMd5("md5_1");

        protoSku1 = CommonModelBuilder.newBuilder(11, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .picture(picture1)
            .picture(picture2)
            .picture(picture3)
            .getRawModel();

        return Collections.singletonList(picture1);
    }

    private List<Video> setUpFilteringSkuVideos() {
        protoParentModel = CommonModelBuilder.newBuilder(1, 1, 1)
            .currentType(CommonModel.Source.GURU)
            .modelRelation(11, 1, ModelRelation.RelationType.SKU_MODEL)
            .getRawModel();

        Video video1 = new Video();
        video1.setUrl("url1");

        Video video2 = new Video();
        video2.setUrl("url1");

        Video video3 = new Video();
        video3.setUrl("url2");

        protoSku1 = CommonModelBuilder.newBuilder(11, 1, 1)
            .currentType(CommonModel.Source.SKU)
            .modelRelation(1, 1, ModelRelation.RelationType.SKU_PARENT_MODEL)
            .video(video1)
            .video(video2)
            .video(video3)
            .getRawModel();
        List<Video> videos = new ArrayList<>();
        videos.add(video1);
        videos.add(video3);
        return videos;
    }

}
