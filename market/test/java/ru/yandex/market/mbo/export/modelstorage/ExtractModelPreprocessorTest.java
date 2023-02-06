package ru.yandex.market.mbo.export.modelstorage;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageTestUtil;
import ru.yandex.market.mbo.export.modelstorage.pipe.CategoryInfo;
import ru.yandex.market.mbo.export.modelstorage.pipe.ModelPipeContext;
import ru.yandex.market.mbo.export.modelstorage.pipe.PreprocessorPipePart;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.Video;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.Unit;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.TMTemplate;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 26.01.2017
 */
@SuppressWarnings({"unchecked", "checkstyle:magicnumber", "checkstyle:methodlength"})
public class ExtractModelPreprocessorTest {

    private static final AtomicLong MODEL_IDS = new AtomicLong(0);
    private static final int CREATED_DATE = 10;
    private static final int GROUP_PICS_SIZE = 3;

    private PreprocessorPipePart preprocessor;
    private final CategoryParam nameParam = new Parameter();
    private final CategoryParam vendorParam = new Parameter();
    private final CategoryParam colorVendorParam = new Parameter();
    private final CategoryParam boolSkuParam = new Parameter();
    private final CategoryParam strSkuParam = new Parameter();
    private final CategoryParam numericSkuParam = new Parameter();
    private final CategoryParam isSkuParam = new Parameter();

    private final List<CategoryParam> params =
        Arrays.asList(nameParam, vendorParam, colorVendorParam, boolSkuParam, strSkuParam, numericSkuParam,
            isSkuParam);

    private ModelStorage.Model group;
    private ModelStorage.Model modif1;
    private ModelStorage.Model modif2;
    private ModelStorage.Model modif3;

    CommonModelBuilder<CommonModel> modelBuilder = CommonModelBuilder.newBuilder();

    private CategoryInfo enrichContext;

    @Before
    public void init() {
        initParams();
        preprocessor = new PreprocessorPipePart(enrichContext);
        group = model(0, WordUtil.defaultWord("group"));
        modif1 = model(0, WordUtil.defaultWord("modif1"))
            .toBuilder().setCreatedDate(0).build();
        modif2 = model(0, WordUtil.defaultWord("modif2"))
            .toBuilder().setCreatedDate(1).build();
        modif3 = model(0, WordUtil.defaultWord("modif3")) // the newest
            .toBuilder().setCreatedDate(CREATED_DATE).build();
    }

    @Test
    public void testPictureParams() throws Exception {
        ModelStorage.ParameterValue brokenXlPicture = stringParam(XslNames.XL_PICTURE, "http:///broken")
            .toBuilder().removeStrValue(0).build();

        ModelStorage.ParameterValue xlPicture = stringParam(XslNames.XL_PICTURE, "http:///xl");
        ModelStorage.ParameterValue xlPictureX = stringParam("XLPictureSizeX", "100");
        ModelStorage.ParameterValue xlPictureY = stringParam("XLPictureSizeY", "200");

        ModelStorage.ParameterValue xlPicture3 = stringParam("XL-Picture_3", "http:///xl3");
        ModelStorage.ParameterValue xlPictureX3 = stringParam("XLPictureSizeX_3", "103");
        ModelStorage.ParameterValue xlPictureY3 = stringParam("XLPictureSizeY_3", "203");

        ModelStorage.ParameterValue anotherXlPicture3 = stringParam("XL-Picture_3", "http:///xl3213");
        ModelStorage.ParameterValue anotherXlPictureX3 = stringParam("XLPictureSizeX_3", "333");
        ModelStorage.ParameterValue anotherXlPictureY3 = stringParam("XLPictureSizeY_3", "222");

        ModelStorage.ParameterValue xlPicture4 = stringParam("XL-Picture_4", "http:///xl4");
        ModelStorage.ParameterValue xlPictureX4 = stringParam("XLPictureSizeX_4", "104");
        ModelStorage.ParameterValue xlPictureY4 = stringParam("XLPictureSizeY_4", "105");

        ModelStorage.ParameterValue dublicateUrlXLPicture5 = stringParam("XL-Picture_5", "http:///xl3");
        ModelStorage.ParameterValue dublicateUrlXLPictureX5 = stringParam("XLPictureSizeX_5", "105");
        ModelStorage.ParameterValue dublicateUrlXLPictureY5 = stringParam("XLPictureSizeY_5", "205");

        assertThat("group size for single",
            !processAndGetGroupPics(group, Collections.emptyList()).hasGroupSize());

        assertThat("incorrect group size",
            processAndGetGroupPics(group, Arrays.asList(modif1, modif2)).getGroupSize() == 2);

        ModelStorage.Model groupWithPics = group.toBuilder()
            .addParameterValues(xlPicture3)
            .addParameterValues(xlPictureX3)
            .addParameterValues(xlPictureY3)
            .addParameterValues(xlPicture)
            .build();

        List<ModelStorage.ParameterValue> groupPics = processAndGetGroupPics(
            groupWithPics,
            Arrays.asList(modif1, modif2)
        ).getGroupPicturesList();

        assertThat("Pics from group", groupPics.containsAll(Arrays.asList(xlPicture3, xlPictureX3, xlPictureY3)));

        assertThat("Pics without x", !groupPics.contains(xlPicture));

        ModelStorage.Model groupWithBrokenPic = group.toBuilder()
            .addParameterValues(brokenXlPicture)
            .build();

        ModelStorage.Model unpublModif1WithPics = modif1.toBuilder()
            .setPublished(false)
            .addParameterValues(xlPicture3)
            .addParameterValues(xlPictureX3)
            .addParameterValues(xlPictureY3)
            .build();

        ModelStorage.Model modif2WithPics = modif2.toBuilder()
            .addParameterValues(anotherXlPicture3)
            .addParameterValues(anotherXlPictureX3)
            .addParameterValues(anotherXlPictureY3)
            .build();

        groupPics = processAndGetGroupPics(
            groupWithBrokenPic,
            Arrays.asList(unpublModif1WithPics, modif2WithPics)
        ).getGroupPicturesList();

        assertThat("contains only from published", groupPics.containsAll(
            Arrays.asList(anotherXlPicture3, anotherXlPictureX3, anotherXlPictureY3)) &&
            groupPics.size() == GROUP_PICS_SIZE);

        groupWithPics = group.toBuilder()
            .addParameterValues(xlPicture)
            .addParameterValues(xlPictureX)
            .addParameterValues(xlPictureY)
            .addParameterValues(xlPicture4)
            .addParameterValues(xlPictureY4)
            .build();

        ModelStorage.Model modif1WithPics = modif1.toBuilder()
            .addParameterValues(xlPicture3)
            .addParameterValues(xlPictureX3)
            .addParameterValues(xlPictureY3)
            .addParameterValues(dublicateUrlXLPicture5)
            .addParameterValues(dublicateUrlXLPictureX5)
            .addParameterValues(dublicateUrlXLPictureY5)
            .build();

        ModelStorage.Model modif3WithPics = modif3.toBuilder()
            .addParameterValues(xlPicture3)
            .addParameterValues(xlPictureX3)
            .addParameterValues(xlPictureY3)
            .addParameterValues(xlPictureX4)
            .build();

        groupPics = processAndGetGroupPics(
            groupWithPics,
            Arrays.asList(modif3WithPics, modif2WithPics, modif1WithPics)
        ).getGroupPicturesList();

        List<ModelStorage.ParameterValue> shouldContain = Arrays.asList(
            xlPicture,
            xlPictureX,
            xlPictureY,
            xlPicture3,
            xlPictureX3,
            xlPictureY3);

        assertThat("check all \n" + extractXslNamesPlusVals(groupPics)
                + "\n but should \n" + extractXslNamesPlusVals(shouldContain),
            groupPics.containsAll(shouldContain) && groupPics.size() == shouldContain.size());

    }

    @Test
    public void testModificationsPictures() throws Exception {
        ModelStorage.Model groupWith2Pics = modelBuilder
            .startModel()
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .picture("XL-Picture", "http://xl")
            .picture("XL-Picture_2", "http://xl2")
            .getRawModel();

        ModelStorage.Model modif1With1Pic = modelBuilder
            .startModel()
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(2L)
            .parentModelId(1L)
            .published(true)
            .picture("XL-Picture_3", "http://xl3")
            .getRawModel();

        ModelStorage.Model modif2WithAnotherPic = modelBuilder
            .startModel()
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(3L)
            .parentModelId(1L)
            .published(true)
            .picture("XL-Picture_4", "http://xl4")
            .getRawModel();

        ModelPipeContext context = new ModelPipeContext(groupWith2Pics,
            Arrays.asList(modif1With1Pic, modif2WithAnotherPic), Collections.emptyList());

        preprocessor.acceptModelsGroup(context);

        int i = 0;
        List<ModelStorage.Picture> modelPictures = context.getModel().getPicturesList();
        assertPicture(modelPictures, i++, "BigPicture", "http://xl");
        assertPicture(modelPictures, i++, "XL-Picture", "http://xl");
        assertPicture(modelPictures, i++, "XL-Picture_2", "http://xl2");
        assertPicture(modelPictures, i++, "XL-Picture_3", "http://xl3");
        assertPicture(modelPictures, i, "XL-Picture_4", "http://xl4");

        Iterator<ModelStorage.Model.Builder> modifications = context.getModifications().iterator();

        i = 0;
        List<ModelStorage.Picture> modif1Pictures = modifications.next().getPicturesList();
        assertPicture(modif1Pictures, i++, "BigPicture", "http://xl");
        assertPicture(modif1Pictures, i++, "XL-Picture", "http://xl");
        assertPicture(modelPictures, i++, "XL-Picture_2", "http://xl2");
        assertPicture(modif1Pictures, i, "XL-Picture_3", "http://xl3");

        i = 0;
        List<ModelStorage.Picture> modif2Pictures = modifications.next().getPicturesList();
        assertPicture(modif1Pictures, i++, "BigPicture", "http://xl");
        assertPicture(modif2Pictures, i++, "XL-Picture", "http://xl");
        assertPicture(modelPictures, i++, "XL-Picture_2", "http://xl2");
        assertPicture(modif2Pictures, i, "XL-Picture_4", "http://xl4");
    }

    @Test
    public void testPictures() throws Exception {
        // 1. Model with modifications and SKUs (all SKUs belong to modifications)
        ModelPipeContext context = createPipeContextWithModificationsAndSkus();
        preprocessor.acceptModelsGroup(context);

        int i = 0;
        List<ModelStorage.Picture> modelPictures = context.getModel().getPicturesList();
        assertPicture(modelPictures, i++, "BigPicture", "http://url1");
        assertPicture(modelPictures, i++, "XL-Picture", "http://url1");
        assertPicture(modelPictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(modelPictures, i++, "XL-Picture_3", "http://url9");
        assertPicture(modelPictures, i++, "XL-Picture_4", "http://url10");
        assertPicture(modelPictures, i++, "XL-Picture_5", "http://url2");
        assertPicture(modelPictures, i++, "XL-Picture_6", "http://url5");
        assertPicture(modelPictures, i++, "XL-Picture_7", "http://url13");
        Assert.assertEquals(i, modelPictures.size());

        i = 0;
        Iterator<ModelStorage.Model.Builder> modifIter = context.getModifications().iterator();
        List<ModelStorage.Picture> modif1Pictures = modifIter.next().getPicturesList();
        assertPicture(modif1Pictures, i++, "BigPicture", "http://url1");
        assertPicture(modif1Pictures, i++, "XL-Picture", "http://url1");
        assertPicture(modif1Pictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(modif1Pictures, i++, "XL-Picture_3", "http://url9");
        assertPicture(modif1Pictures, i++, "XL-Picture_4", "http://url10");
        assertPicture(modif1Pictures, i++, "XL-Picture_5", "http://url2");
        assertPicture(modif1Pictures, i++, "XL-Picture_6", "http://url5");
        assertPicture(modif1Pictures, i++, "XL-Picture_7", "http://url13");
        Assert.assertEquals(i, modif1Pictures.size());

        i = 0;
        List<ModelStorage.Picture> modif2Pictures = modifIter.next().getPicturesList();
        assertPicture(modif2Pictures, i++, "BigPicture", "http://url1");
        assertPicture(modif2Pictures, i++, "XL-Picture", "http://url1");
        assertPicture(modif2Pictures, i++, "XL-Picture_2", "http://url5");
        assertPicture(modif2Pictures, i++, "XL-Picture_3", "http://url8");
        assertPicture(modif2Pictures, i++, "XL-Picture_4", "http://url11");
        assertPicture(modif2Pictures, i++, "XL-Picture_5", "http://url7");
        assertPicture(modif2Pictures, i++, "XL-Picture_6", "http://url12");
        assertPicture(modif2Pictures, i++, "XL-Picture_7", "http://url13");
        Assert.assertEquals(i, modif2Pictures.size());

        i = 0;
        List<ModelStorage.Picture> sku1Pictures = context.getSkus().get(0).getPicturesList();
        assertPicture(sku1Pictures, i++, "BigPicture", "http://url1");
        assertPicture(sku1Pictures, i++, "XL-Picture", "http://url1");
        assertPicture(sku1Pictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(sku1Pictures, i++, "XL-Picture_3", "http://url2");
        assertPicture(sku1Pictures, i++, "XL-Picture_4", "http://url5");
        assertPicture(sku1Pictures, i++, "XL-Picture_5", "http://url13");
        Assert.assertEquals(i, sku1Pictures.size());

        i = 0;
        List<ModelStorage.Picture> sku2Pictures = context.getSkus().get(1).getPicturesList();
        assertPicture(sku2Pictures, i++, "BigPicture", "http://url1");
        assertPicture(sku2Pictures, i++, "XL-Picture", "http://url1");
        assertPicture(sku2Pictures, i++, "XL-Picture_2", "http://url5");
        assertPicture(sku2Pictures, i++, "XL-Picture_3", "http://url7");
        assertPicture(sku2Pictures, i++, "XL-Picture_4", "http://url12");
        assertPicture(sku2Pictures, i++, "XL-Picture_5", "http://url13");
        Assert.assertEquals(i, sku2Pictures.size());

        i = 0;
        List<ModelStorage.Picture> sku3Pictures = context.getSkus().get(2).getPicturesList();
        assertPicture(sku3Pictures, i++, "BigPicture", "http://url8");
        assertPicture(sku3Pictures, i++, "XL-Picture", "http://url8");
        assertPicture(sku3Pictures, i++, "XL-Picture_2", "http://url5");
        assertPicture(sku3Pictures, i++, "XL-Picture_3", "http://url7");
        assertPicture(sku3Pictures, i++, "XL-Picture_4", "http://url12");
        assertPicture(sku3Pictures, i++, "XL-Picture_5", "http://url13");
        Assert.assertEquals(i, sku3Pictures.size());

        i = 0;
        List<ModelStorage.Picture> sku4Pictures = context.getSkus().get(3).getPicturesList();
        assertPicture(sku4Pictures, i++, "BigPicture", "http://url9");
        assertPicture(sku4Pictures, i++, "XL-Picture", "http://url9");
        assertPicture(sku4Pictures, i++, "XL-Picture_2", "http://url10");
        assertPicture(sku4Pictures, i++, "XL-Picture_3", "http://url2");
        assertPicture(sku4Pictures, i++, "XL-Picture_4", "http://url5");
        assertPicture(sku4Pictures, i++, "XL-Picture_5", "http://url13");
        Assert.assertEquals(i, sku4Pictures.size());

        i = 0;
        List<ModelStorage.Picture> sku5Pictures = context.getSkus().get(4).getPicturesList();
        assertPicture(sku5Pictures, i++, "BigPicture", "http://url11");
        assertPicture(sku5Pictures, i++, "XL-Picture", "http://url11");
        assertPicture(sku5Pictures, i++, "XL-Picture_2", "http://url5");
        assertPicture(sku5Pictures, i++, "XL-Picture_3", "http://url7");
        assertPicture(sku5Pictures, i++, "XL-Picture_4", "http://url12");
        assertPicture(sku5Pictures, i++, "XL-Picture_5", "http://url13");
        Assert.assertEquals(i, sku5Pictures.size());

        // 2. Model without modifications (SKUs belong to the model)
        context = createPipeContextWithSkus();
        preprocessor.acceptModelsGroup(context);

        i = 0;
        modelPictures = context.getModel().getPicturesList();
        assertPicture(modelPictures, i++, "BigPicture", "http://url1");
        assertPicture(modelPictures, i++, "XL-Picture", "http://url1");
        assertPicture(modelPictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(modelPictures, i++, "XL-Picture_3", "http://url9");
        assertPicture(modelPictures, i++, "XL-Picture_4", "http://url8");
        assertPicture(modelPictures, i++, "XL-Picture_5", "http://url11");
        assertPicture(modelPictures, i++, "XL-Picture_6", "http://url2");
        assertPicture(modelPictures, i++, "XL-Picture_7", "http://url5");
        Assert.assertEquals(i, modelPictures.size());

        i = 0;
        sku1Pictures = context.getSkus().get(0).getPicturesList();
        assertPicture(sku1Pictures, i++, "BigPicture", "http://url1");
        assertPicture(sku1Pictures, i++, "XL-Picture", "http://url1");
        assertPicture(sku1Pictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(sku1Pictures, i++, "XL-Picture_3", "http://url9");
        assertPicture(sku1Pictures, i++, "XL-Picture_4", "http://url2");
        assertPicture(sku1Pictures, i++, "XL-Picture_5", "http://url5");
        assertPicture(sku1Pictures, i++, "XL-Picture_6", "http://url11");
        Assert.assertEquals(i, sku1Pictures.size());

        i = 0;
        sku2Pictures = context.getSkus().get(1).getPicturesList();
        assertPicture(sku2Pictures, i++, "BigPicture", "http://url9");
        assertPicture(sku2Pictures, i++, "XL-Picture", "http://url9");
        assertPicture(sku2Pictures, i++, "XL-Picture_2", "http://url8");
        assertPicture(sku2Pictures, i++, "XL-Picture_3", "http://url2");
        assertPicture(sku2Pictures, i++, "XL-Picture_4", "http://url5");
        assertPicture(sku2Pictures, i++, "XL-Picture_5", "http://url11");
        Assert.assertEquals(i, sku2Pictures.size());

        i = 0;
        sku3Pictures = context.getSkus().get(2).getPicturesList();
        assertPicture(sku3Pictures, i++, "BigPicture", "http://url11");
        assertPicture(sku3Pictures, i++, "XL-Picture", "http://url11");
        assertPicture(sku3Pictures, i++, "XL-Picture_2", "http://url6");
        assertPicture(sku3Pictures, i++, "XL-Picture_3", "http://url2");
        assertPicture(sku3Pictures, i++, "XL-Picture_4", "http://url9");
        assertPicture(sku3Pictures, i++, "XL-Picture_5", "http://url5");
        Assert.assertEquals(i, sku3Pictures.size());
    }

    @Test
    public void testVideos() {
        ModelPipeContext context = createPipeContextWithSkusWithVideos();
        preprocessor.acceptModelsGroup(context);

        List<ModelStorage.Video> modelVideo = context.getModel().getVideosList();
        assertEquals("http://url3", modelVideo.get(0).getUrl());
        assertEquals("http://url4", modelVideo.get(1).getUrl());
        assertEquals("http://url5", modelVideo.get(2).getUrl());
        assertEquals("http://url7", modelVideo.get(3).getUrl());
        assertEquals("http://url6", modelVideo.get(4).getUrl());
        assertEquals("http://url1", modelVideo.get(5).getUrl());
        assertEquals("http://url2", modelVideo.get(6).getUrl());
        Assert.assertEquals(7, modelVideo.size());

        List<ModelStorage.Video> sku1Video = context.getSkus().get(0).getVideosList();
        assertEquals("http://url3", sku1Video.get(0).getUrl());
        assertEquals("http://url4", sku1Video.get(1).getUrl());
        assertEquals("http://url1", sku1Video.get(2).getUrl());
        assertEquals("http://url2", sku1Video.get(3).getUrl());
        Assert.assertEquals(4, sku1Video.size());

        List<ModelStorage.Video> sku2Video = context.getSkus().get(1).getVideosList();
        assertEquals("http://url5", sku2Video.get(0).getUrl());
        assertEquals("http://url1", sku2Video.get(1).getUrl());
        assertEquals("http://url2", sku2Video.get(2).getUrl());
        assertEquals("http://url3", sku2Video.get(3).getUrl());
        Assert.assertEquals(4, sku2Video.size());

        List<ModelStorage.Video> sku3Video = context.getSkus().get(2).getVideosList();
        assertEquals("http://url7", sku3Video.get(0).getUrl());
        assertEquals("http://url6", sku3Video.get(1).getUrl());
        assertEquals("http://url1", sku3Video.get(2).getUrl());
        assertEquals("http://url2", sku3Video.get(3).getUrl());
        assertEquals("http://url3", sku3Video.get(4).getUrl());
        Assert.assertEquals(5, sku3Video.size());
    }

    @Test
    public void testIsSkuModelWithXlPicture() {
        ModelStorage.Model isSkuModel = createIsSkuModel(1L, 0, "http://url2")
            .getRawModel();

        ModelPipeContext context = new ModelPipeContext(
            isSkuModel, Collections.emptyList(), Collections.emptyList());
        preprocessor.acceptModelsGroup(context);

        List<ModelStorage.Picture> pictures = context.getModel().getPicturesList();

        assertEquals(2, pictures.size());
        assertPicture(pictures, 0, "BigPicture", "http://url2");
        assertPicture(pictures, 1, "XL-Picture", "http://url2");
    }

    @Test
    public void testIsSkuModificationsPictures() {
        ModelStorage.Model parentModel = CommonModelBuilder.newBuilder(1L, 10L, 20L).getRawModel();
        ModelStorage.Model isSkuModification1 = createIsSkuModel(2L, 1L, "http://url2")
            .getRawModel();
        ModelStorage.Model isSkuModification2 = createIsSkuModel(3L, 1L, "http://url3")
            .getRawModel();
        ModelStorage.Model modification3 = createIsSkuModel(4L, 1L, "http://url5")
            .picture("XL-Picture_2", "http://url6")
            .param(XslNames.IS_SKU).setBoolean(false)
            .getRawModel();
        ModelStorage.Model modification4 = createIsSkuModel(5L, 1L, "http://url7")
            .picture("XL-Picture_3", "http://url7")
            .getRawModel();

        ModelPipeContext context = new ModelPipeContext(parentModel,
            Arrays.asList(isSkuModification1, isSkuModification2, modification3, modification4),
            Collections.emptyList());
        preprocessor.acceptModelsGroup(context);

        Iterator<ModelStorage.Model.Builder> modifIter = context.getModifications().iterator();

        List<ModelStorage.Picture> modification1Pictures = modifIter.next().getPicturesList();
        assertEquals(2, modification1Pictures.size());
        assertPicture(modification1Pictures, 0, "BigPicture", "http://url2");
        assertPicture(modification1Pictures, 1, "XL-Picture", "http://url2");

        List<ModelStorage.Picture> modification2Pictures = modifIter.next().getPicturesList();
        assertEquals(2, modification2Pictures.size());
        assertPicture(modification2Pictures, 0, "BigPicture", "http://url3");
        assertPicture(modification2Pictures, 1, "XL-Picture", "http://url3");

        List<ModelStorage.Picture> modification3Pictures = modifIter.next().getPicturesList();
        assertEquals(3, modification3Pictures.size());
        assertPicture(modification3Pictures, 0, "BigPicture", "http://url5");
        assertPicture(modification3Pictures, 1, "XL-Picture", "http://url5");
        assertPicture(modification3Pictures, 2, "XL-Picture_2", "http://url6");

        List<ModelStorage.Picture> modification4Pictures = modifIter.next().getPicturesList();
        assertEquals(2, modification4Pictures.size());
        assertPicture(modification4Pictures, 0, "BigPicture", "http://url7");
        assertPicture(modification4Pictures, 1, "XL-Picture", "http://url7");
    }

    @Test
    public void testClusterSkippedAndGuruNotSkipped() {
        preprocessor = Mockito.mock(PreprocessorPipePart.class);
        Mockito.doCallRealMethod().when(preprocessor).acceptModelsGroup(Mockito.any());

        ModelStorage.Model cluster = CommonModelBuilder.newBuilder()
            .id(1L).category(2L).vendorId(3L).currentType(CommonModel.Source.CLUSTER)
            .getRawModel();
        ModelPipeContext clusterContext = new ModelPipeContext(cluster,
            Collections.emptyList(), Collections.emptyList());

        preprocessor.acceptModelsGroup(clusterContext);
        Mockito.verify(preprocessor, Mockito.never()).processGroupModel(Mockito.any());

        ModelStorage.Model guruModel = CommonModelBuilder.newBuilder()
            .id(1L).category(2L).vendorId(3L).currentType(CommonModel.Source.GURU)
            .getRawModel();
        ModelPipeContext guruContext = new ModelPipeContext(guruModel,
            Collections.emptyList(), Collections.emptyList());

        preprocessor.acceptModelsGroup(guruContext);
        Mockito.verify(preprocessor, Mockito.times(1)).processGroupModel(Mockito.eq(guruContext));
    }

    private void assertPicture(List<ModelStorage.Picture> pictures, int i, String xslName, String url) {
        assertThat("have enough pictures", pictures.size() > i);

        ModelStorage.Picture pic = pictures.get(i);
        assertEquals(xslName, pic.getXslName());
        assertEquals(url, pic.getUrl());
    }

    private ModelStorage.Model.Builder processAndGetGroupPics(ModelStorage.Model model,
                                                              Collection<ModelStorage.Model> modifications) {
        ModelPipeContext context = new ModelPipeContext(
            model,
            modifications,
            Collections.emptyList());
        preprocessor.processGroupModel(context);
        return context.getModel();
    }

    private ModelStorage.ParameterValue stringParam(String xslName, String value) {
        return ModelStorageTestUtil.stringParamValue(1L, xslName, value);
    }

    private Collection<String> extractXslNames(Collection<ModelStorage.Picture> list) {
        return list.stream()
            .map(ModelStorage.Picture::getXslName)
            .sorted()
            .collect(Collectors.toList());
    }

    private Collection<String> extractXslNamesPlusVals(Collection<ModelStorage.ParameterValue> list) {
        return list.stream().map(
            pv -> pv.getXslName() + ":" + stringValues(pv)
        ).sorted().collect(Collectors.toList());
    }

    private String stringValues(ModelStorage.ParameterValue str) {
        return str.getStrValueList().stream().map(ls -> ls.getValue()).collect(Collectors.joining(","));
    }

    private ModelStorage.Model model(long vendorId, Word... names) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.setId(MODEL_IDS.incrementAndGet());
        builder.setPublished(true);
        builder.setVendorId(vendorId);

        ModelStorage.ParameterValue.Builder nameBuilder = ModelStorage.ParameterValue
            .newBuilder()
            .setXslName("name");

        for (Word name : names) {
            nameBuilder.addStrValue(ModelProtoConverter.fromWord(name));
        }

        builder.addParameterValues(nameBuilder);

        // add more test values
        builder.addParameterValues(
            ModelStorage.ParameterValue
                .newBuilder()
                .setXslName("aliases")
                .addStrValue(ModelProtoConverter.fromWord(name("superAlias", Language.RUSSIAN)))
                .addStrValue(ModelProtoConverter.fromWord(name("superAlias BEL", Language.BELARUSSIAN)))
        );
        builder.addParameterValues(
            ModelStorage.ParameterValue
                .newBuilder()
                .setXslName("coreCount")
                .setNumericValue("42")
        );
        builder.addParameterValues(
            ModelStorage.ParameterValue
                .newBuilder()
                .setXslName("picture")
                .addStrValue(ModelProtoConverter.fromWord(name("http://mdata...", Language.RUSSIAN)))
        );

        return builder.build();
    }

    private OptionImpl vendor(long vendorId, Word... names) {
        OptionImpl vendor = new OptionImpl(Option.OptionType.VENDOR);
        Option globalVendor = new OptionImpl(vendorId);
        globalVendor.setNames(Arrays.asList(names));
        vendor.setParent(globalVendor);
        return vendor;
    }

    private Word name(String name, Language language) {
        return new Word(language.getId(), name);
    }


    private Matcher<ModelStorage.LocalizedString> isLocalizedString(String word, Language language) {
        return allOf(
            hasProperty(ModelStorage.LocalizedString::hasIsoCode, "isoCode"),
            hasProperty(ModelStorage.LocalizedString::hasValue, "value"),
            propertyValueEquals(ModelStorage.LocalizedString::getIsoCode, "isoCode", language.getIsoCode()),
            propertyValueEquals(ModelStorage.LocalizedString::getValue, "value", word)
        );
    }

    private <Item, Value> Matcher<Item> propertyValueEquals(
        Function<Item, Value> getter, String propertyName, Value expect) {

        return new FeatureMatcher<Item, Value>(is(expect), propertyName, propertyName) {
            @Override
            protected Value featureValueOf(Item actual) {
                return getter.apply(actual);
            }
        };
    }


    private <Item> Matcher<Item> hasProperty(Predicate<Item> hasPropertyMethod, String propertyName) {
        return new CustomTypeSafeMatcher<Item>("should have property '" + propertyName + "'") {
            @Override
            protected boolean matchesSafely(Item item) {
                return hasPropertyMethod.apply(item);
            }

            @Override
            protected void describeMismatchSafely(Item item, Description mismatchDescription) {
                mismatchDescription.appendText("didn't have ");
            }
        };
    }

    private ModelPipeContext createPipeContextWithModificationsAndSkus() {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .published(true)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_4", "http://url13")
            .vendorId(11L)
            .getRawModel();

        ModelStorage.Model contextModif1 = modelBuilder
            .startModel()
            .title("MODIF1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(2L)
            .parentModelId(contextModel.getId())
            .published(true)
            .modelRelation(4, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(7, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(9, 0, ModelRelation.RelationType.SKU_MODEL)
            .picture("XL-Picture_2", "http://url5")
            .vendorId(11L)
            .createdDate(new Date(1518685980548L))
            .getRawModel();

        ModelStorage.Model contextModif2 = modelBuilder
            .startModel()
            .title("MODIF2")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(3L)
            .published(false)
            .parentModelId(contextModel.getId())
            .modelRelation(5, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(6, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(8, 0, ModelRelation.RelationType.SKU_MODEL)
            .picture("XL-Picture", "http://url5")
            .picture("XL-Picture_2", "http://url7")
            .picture("XL-Picture_3", "http://url12")
            .vendorId(12L)
            .createdDate(new Date(1518685980547L))
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url6")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue1")
            .param(numericSkuParam)
            .setNumeric(15)
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(5L)
            .vendorId(11L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url5")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue2")
            .getRawModel();

        ModelStorage.Model contextSku3 = modelBuilder
            .startModel()
            .title("SKU3")
            .id(6L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url8")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue5")
            .getRawModel();

        ModelStorage.Model contextSku4 = modelBuilder
            .startModel()
            .title("SKU4")
            .id(7L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url9")
            .picture(null, "http://url10")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue6")
            .getRawModel();

        ModelStorage.Model contextSku5 = modelBuilder
            .startModel()
            .title("SKU5")
            .id(8L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif2.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url11")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue7")
            .getRawModel();

        ModelStorage.Model contextSku6 = modelBuilder
            .startModel()
            .title("SKU6")
            .id(9L)
            .vendorId(12L)
            .published(false)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModif1.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url14")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue8")
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Arrays.asList(contextModif1, contextModif2),
            Arrays.asList(contextSku1, contextSku2, contextSku3, contextSku4, contextSku5, contextSku6));
    }

    private ModelPipeContext createPipeContextWithSkus() {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .published(true)
            .modelRelation(4, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(6, 0, ModelRelation.RelationType.SKU_MODEL)
            .modelRelation(8, 0, ModelRelation.RelationType.SKU_MODEL)
            .picture("XL-Picture", "http://url2")
            .picture("XL-Picture_2", "http://url9")
            .picture("XL-Picture_3", "http://url5")
            .picture("XL-Picture_4", "http://url11")
            .vendorId(11L)
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url1")
            .picture(null, "http://url6")
            .picture(null, "http://url9")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue1")
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(6L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url9")
            .picture(null, "http://url8")
            .param(colorVendorParam)
            .setOption(102L)
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue5")
            .getRawModel();

        ModelStorage.Model contextSku3 = modelBuilder
            .startModel()
            .title("SKU3")
            .id(8L)
            .vendorId(12L)
            .published(true)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .picture(null, "http://url11")
            .picture(null, "http://url6")
            .param(colorVendorParam)
            .setOption(101L)
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue7")
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Collections.emptyList(),
            Arrays.asList(contextSku1, contextSku2, contextSku3));
    }

    private ModelPipeContext createPipeContextWithSkusWithVideos() {
        ModelStorage.Model contextModel = modelBuilder
                .startModel()
                .title("MODEL1")
                .currentType(CommonModel.Source.GURU)
                .source(CommonModel.Source.GURU)
                .id(1L)
                .published(true)
                .modelRelation(4, 0, ModelRelation.RelationType.SKU_MODEL)
                .modelRelation(6, 0, ModelRelation.RelationType.SKU_MODEL)
                .modelRelation(8, 0, ModelRelation.RelationType.SKU_MODEL)
                .picture("XL-Picture", "http://url2")
                .picture("XL-Picture_2", "http://url9")
                .picture("XL-Picture_3", "http://url5")
                .picture("XL-Picture_4", "http://url11")
                .video(getVideo("http://url1", "http://url1_source"))
                .video(getVideo("http://url2", "http://url2_source"))
                .video(getVideo("http://url3", "http://url3_source"))
                .vendorId(11L)
                .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
                .startModel()
                .title("SKU1")
                .id(4L)
                .vendorId(12L)
                .published(true)
                .currentType(CommonModel.Source.SKU)
                .source(CommonModel.Source.SKU)
                .startModelRelation()
                .id(contextModel.getId())
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
                .picture(null, "http://url1")
                .picture(null, "http://url6")
                .picture(null, "http://url9")
                .video(getVideo("http://url3", "http://url3_source"))
                .video(getVideo("http://url4", "http://url4_source"))
                .param(colorVendorParam)
                .setOption(102L)
                .param(boolSkuParam)
                .setBoolean(true)
                .param(strSkuParam)
                .setString("StrValue1")
                .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
                .startModel()
                .title("SKU2")
                .id(6L)
                .vendorId(12L)
                .published(true)
                .currentType(CommonModel.Source.SKU)
                .source(CommonModel.Source.SKU)
                .startModelRelation()
                .id(contextModel.getId())
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
                .picture(null, "http://url9")
                .picture(null, "http://url8")
                .video(getVideo("http://url5", "http://url5_source"))
                .param(colorVendorParam)
                .setOption(102L)
                .param(boolSkuParam)
                .setBoolean(true)
                .param(strSkuParam)
                .setString("StrValue5")
                .getRawModel();

        ModelStorage.Model contextSku3 = modelBuilder
                .startModel()
                .title("SKU3")
                .id(8L)
                .vendorId(12L)
                .published(true)
                .currentType(CommonModel.Source.SKU)
                .source(CommonModel.Source.SKU)
                .startModelRelation()
                .id(contextModel.getId())
                .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
                .endModelRelation()
                .picture(null, "http://url11")
                .picture(null, "http://url6")
                .video(getVideo("http://url7", "http://url7_source"))
                .video(getVideo("http://url6", "http://url6_source"))
                .param(colorVendorParam)
                .setOption(101L)
                .param(boolSkuParam)
                .setBoolean(false)
                .param(strSkuParam)
                .setString("StrValue7")
                .getRawModel();

        return new ModelPipeContext(
                contextModel,
                Collections.emptyList(),
                Arrays.asList(contextSku1, contextSku2, contextSku3));
    }

    @NotNull
    private Video getVideo(String url, String urlSource) {
        Video video = new Video();
        video.setUrl(url);
        video.setUrlSource(urlSource);
        return video;
    }

    private ModelPipeContext createPipeContextWithSkusWithSizes(CategoryParam sizeParam, CategoryParam scaleParam) {
        ModelStorage.Model contextModel = modelBuilder
            .startModel()
            .title("MODEL1")
            .currentType(CommonModel.Source.GURU)
            .source(CommonModel.Source.GURU)
            .id(1L)
            .vendorId(11L)
            .getRawModel();

        ModelStorage.Model contextSku1 = modelBuilder
            .startModel()
            .title("SKU1")
            .id(4L)
            .vendorId(12L)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .param(boolSkuParam)
            .setBoolean(true)
            .param(strSkuParam)
            .setString("StrValue101")
            .param(sizeParam)
            .setOption(55432L)
            .param(scaleParam)
            .setOption(55430L)
            .getRawModel();

        ModelStorage.Model contextSku2 = modelBuilder
            .startModel()
            .title("SKU2")
            .id(6L)
            .vendorId(12L)
            .currentType(CommonModel.Source.SKU)
            .source(CommonModel.Source.SKU)
            .startModelRelation()
            .id(contextModel.getId())
            .type(ModelRelation.RelationType.SKU_PARENT_MODEL)
            .endModelRelation()
            .param(boolSkuParam)
            .setBoolean(false)
            .param(strSkuParam)
            .setString("StrValue102")
            .param(sizeParam)
            .setOption(55435L)
            .param(scaleParam)
            .setOption(55431L)
            .getRawModel();

        return new ModelPipeContext(
            contextModel,
            Collections.emptyList(),
            Arrays.asList(contextSku1, contextSku2));
    }

    private void initParams() {
        nameParam.setId(100000L);
        nameParam.setXslName("name");
        nameParam.addName(WordUtil.defaultWord("Name"));
        nameParam.setType(Param.Type.STRING);

        vendorParam.setId(123456L);
        vendorParam.setXslName("vendor");
        vendorParam.addName(WordUtil.defaultWord("Vendor"));
        vendorParam.setType(Param.Type.ENUM);

        Option vendorOption1 = new OptionImpl();
        vendorOption1.setId(11L);
        vendorOption1.setNames(WordUtil.defaultWords("VENDOR1"));

        Option vendorOption2 = new OptionImpl();
        vendorOption2.setId(12L);
        vendorOption2.setNames(WordUtil.defaultWords("VENDOR2"));

        vendorParam.addOption(vendorOption1);
        vendorParam.addOption(vendorOption2);

        colorVendorParam.setId(14871214L);
        colorVendorParam.setXslName("color_vendor");
        colorVendorParam.addName(WordUtil.defaultWord("Color vendor"));
        colorVendorParam.setType(Param.Type.ENUM);
        colorVendorParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        colorVendorParam.setModelFilterIndex(0);

        Option color1 = new OptionImpl();
        color1.setId(101L);
        color1.setNames(WordUtil.defaultWords("Red"));

        Option color2 = new OptionImpl();
        color2.setId(102L);
        color2.setNames(WordUtil.defaultWords("Blue"));

        colorVendorParam.addOption(color1);
        colorVendorParam.addOption(color2);

        boolSkuParam.setId(123632L);
        boolSkuParam.setXslName("bool_sku_param");
        boolSkuParam.addName(WordUtil.defaultWord("Boolean SKU param"));
        boolSkuParam.setType(Param.Type.BOOLEAN);
        boolSkuParam.addOption(new OptionImpl(1, "TRUE"));
        boolSkuParam.addOption(new OptionImpl(2, "FALSE"));
        boolSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        boolSkuParam.setModelFilterIndex(-1);

        strSkuParam.setId(123639L);
        strSkuParam.setXslName("str_sku_param");
        strSkuParam.addName(WordUtil.defaultWord("String SKU param"));
        strSkuParam.setType(Param.Type.STRING);
        strSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        strSkuParam.setModelFilterIndex(5);

        numericSkuParam.setId(43759L);
        numericSkuParam.setXslName("numeric_sku_param");
        numericSkuParam.addName(WordUtil.defaultWord("Numeric SKU param"));
        numericSkuParam.setType(Param.Type.NUMERIC);
        numericSkuParam.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        numericSkuParam.setUnit(new Unit("Сантиметр", "см", BigDecimal.ONE, 1234L, 24898L));
        numericSkuParam.setModelFilterIndex(-1);

        isSkuParam.setId(43770L);
        isSkuParam.setXslName(XslNames.IS_SKU);
        isSkuParam.setType(Param.Type.BOOLEAN);
        isSkuParam.addOption(new OptionImpl(1, "TRUE"));
        isSkuParam.addOption(new OptionImpl(2, "FALSE"));
        isSkuParam.addName(WordUtil.defaultWord("Model is SKU"));

        CategoryInfo info = CategoryInfo.forCategory(
            new TovarCategory(0), new TMTemplate(), params, Collections.emptyList(), null, null
        );
        enrichContext = info;
    }

    private CommonModelBuilder<CommonModel> createIsSkuModel(long id, long parentId, String xlPictureUrl) {
        return CommonModelBuilder.newBuilder()
            .id(id)
            .category(10L)
            .vendorId(20L)
            .parentModelId(parentId)
            .parameters(params)
            .param(XslNames.IS_SKU).setBoolean(true)
            .computeIf(xlPictureUrl != null,
                builder -> builder.picture("XL-Picture", xlPictureUrl));
    }
}
