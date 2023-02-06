package ru.yandex.market.mbo.gwt.client.pages.model.editor.test;

import org.junit.Assert;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.EditorTabs;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelDataBuilder;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.events.PlaceShowEvent;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.test.model.EditorUrlStub;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.yandex.market.mbo.gwt.client.pages.model.editor.addon.ImageOrderChangeAddon.XL_PICTURES_BLOCK_NAME;
import static ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel.Source;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public abstract class AbstractImageTest extends AbstractTest {

    protected static final String XL_PICTURE_1 = XslNames.XL_PICTURE;
    protected static final String XL_PICTURE_2 = XslNames.XL_PICTURE + "_2";
    protected static final String XL_PICTURE_3 = XslNames.XL_PICTURE + "_3";
    protected static final String XL_PICTURE_4 = XslNames.XL_PICTURE + "_4";

    protected CommonModel createModelWithImages(Long modelId, List<Picture> pictures) {
        return createModelWithParentModelWithImages(modelId, null, pictures, new ArrayList<>());
    }

    protected CommonModel createModelWithParentModelWithImages(Long modificationId, Long parentModelId,
                                                               List<Picture> modificationPictures,
                                                               List<Picture> parentModelPictures) {
        ModelDataBuilder dataBuilder = ModelDataBuilder.modelData();
        ParametersBuilder<ModelDataBuilder> parametersBuilder = dataBuilder.startParameters().startParameter()
            .xsl(XslNames.VENDOR).type(Param.Type.ENUM).name("Производитель")
            .hidden(true)
            .option(1, "Vendor1")
            .option(2, "Vendor2")
            .option(3, "Vendor3")
            .endParameter();

        parametersBuilder = fillImageParams(parametersBuilder,
            modificationPictures.stream().map(Picture::getXslName).collect(Collectors.toList()));
        if (!parentModelPictures.isEmpty()) {
            parametersBuilder = fillImageParams(parametersBuilder,
                parentModelPictures.stream().map(Picture::getXslName).collect(Collectors.toList()));
        }
        dataBuilder = parametersBuilder.endParameters();

        CommonModelBuilder<ModelDataBuilder> modificationBuilder = dataBuilder.startModel()
            .id(modificationId)
            .category(2)
            .source(Source.GURU)
            .currentType(Source.GURU)
            .param(XslNames.VENDOR).setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED);
        for (Picture pic : modificationPictures) {
            modificationBuilder = addImageToModel(modificationBuilder, pic);
        }

        if (parentModelId != null) {
            CommonModelBuilder<CommonModelBuilder<ModelDataBuilder>> parentModelBuilder =
                modificationBuilder.startParentModel()
                .id(parentModelId)
                .category(2)
                .source(Source.GURU)
                .currentType(Source.GURU)
                .param(XslNames.VENDOR).setOption(1).modificationSource(ModificationSource.OPERATOR_FILLED);
            for (Picture pic : parentModelPictures) {
                parentModelBuilder = addImageToModel(parentModelBuilder, pic);
            }
            modificationBuilder = parentModelBuilder.endModel();
        }

        modificationBuilder.endModel()
            .startForm()
            .startTab()
            .name(EditorTabs.PICTURES.getDisplayName())
            .startBlock()
            .name(XL_PICTURES_BLOCK_NAME)

            .addAllProperties(modificationPictures.stream().map(pic -> pic.getXslName()).collect(Collectors.toList()))
            .addAllProperties(parentModelPictures.stream().map(pic -> pic.getXslName()).collect(Collectors.toList()))

            .addAllProperties(modificationPictures.stream().map(pic -> url(pic.getXslName())).collect(Collectors.toList()))
            .addAllProperties(parentModelPictures.stream().map(pic -> url(pic.getXslName())).collect(Collectors.toList()))

            .addAllProperties(modificationPictures.stream().map(pic -> x(pic.getXslName())).collect(Collectors.toList()))
            .addAllProperties(parentModelPictures.stream().map(pic -> x(pic.getXslName())).collect(Collectors.toList()))

            .addAllProperties(modificationPictures.stream().map(pic -> y(pic.getXslName())).collect(Collectors.toList()))
            .addAllProperties(parentModelPictures.stream().map(pic -> y(pic.getXslName())).collect(Collectors.toList()))

            .addAllProperties(modificationPictures.stream().map(pic -> orig(pic.getXslName())).collect(Collectors.toList()))
            .addAllProperties(parentModelPictures.stream().map(pic -> orig(pic.getXslName())).collect(Collectors.toList()))

            .endBlock()
            .endTab()
            .endForm()
            .startVendor()
            .source("http://source1", "ru", new Date())
            .source("http://source2", "en", new Date())
            .endVendor()
            .tovarCategory(1, 2);

        rpc.setLoadModel(dataBuilder.getModel(), null);
        rpc.setLoadModelData(dataBuilder.getModelData(), null);

        bus.fireEvent(
            new PlaceShowEvent(
                EditorUrlStub.of("modelEditor", String.format("entity-id=%d", modificationId))));

        return dataBuilder.getModel();
    }


    protected void compare(CommonModel orig, CommonModel second, String picXslName, boolean shouldEquals) {
        for (String xsl : Arrays.asList(picXslName, x(picXslName), y(picXslName), url(picXslName), orig(picXslName))) {
            ParameterValue origPV = orig.getSingleParameterValue(xsl);
            ParameterValue secondPV = second.getSingleParameterValue(xsl);
            Assert.assertTrue("contains " + xsl, secondPV != null);
            Assert.assertTrue("equality val " + xsl + " should " + shouldEquals,
                secondPV.valueEquals(origPV) == shouldEquals);
        }
    }

    protected void mutateParam(CommonModel model, String xslName, Consumer<ParameterValue> mutator) {
        ParameterValue value = model.getSingleParameterValue(xslName);
        value = new ParameterValue(value);
        mutator.accept(value);
        model.putParameterValues(ParameterValues.of(value));
    }

    protected Consumer<ParameterValue> numMutator(int numericValue) {
        return parameterValue -> {
            parameterValue.setNumericValue(new BigDecimal(numericValue));
        };
    }

    protected Consumer<ParameterValue> strMutator(String stringValue) {
        return parameterValue -> {
            parameterValue.setStringValue(WordUtil.defaultWords(Arrays.asList(stringValue)));
        };
    }

    protected String x(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getWidthParamName(paramPampam));
    }

    protected String y(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getHeightParamName(paramPampam));
    }

    protected String url(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getUrlParamName(paramPampam));
    }

    protected String orig(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getRawUrlParamName(paramPampam));
    }

    protected String color(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getColornessParamName(paramPampam));
    }

    protected String colorAvg(String paramPampam) {
        return dependXsl(paramPampam, t -> t.getColornessAvgParamName(paramPampam));
    }

    private CommonModelBuilder addImageToModel(CommonModelBuilder builder, Picture picture) {
        CommonModelBuilder result = builder;
        if (picture.getUrl() != null) {
            result = result.param(picture.getXslName()).setString(picture.getUrl())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getUrlSource() != null) {
            result = result.param(url(picture.getXslName())).setString(picture.getUrlSource())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getUrlOrig() != null) {
            result = result.param(orig(picture.getXslName())).setString(picture.getUrlOrig())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getWidth() != null) {
            result = result.param(x(picture.getXslName())).setNumeric(picture.getWidth())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getHeight() != null) {
            result = result.param(y(picture.getXslName())).setNumeric(picture.getHeight())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getColorness() != null) {
            result = result.param(color(picture.getXslName())).setNumeric(picture.getColorness())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        if (picture.getColornessAvg() != null) {
            result = result.param(colorAvg(picture.getXslName())).setNumeric(picture.getColornessAvg())
                           .modificationSource(ModificationSource.OPERATOR_FILLED);
        }
        return result;
    }

    private String dependXsl(String xsl, Function<ImageType, String> getter) {
        ImageType type = ImageType.getImageType(xsl);
        return getter.apply(type);
    }

    private ParametersBuilder<ModelDataBuilder> fillImageParams(ParametersBuilder<ModelDataBuilder> pb, List<String> paramBaseNames) {
        for (String paramXsl : paramBaseNames) {
            pb
                .startParameter()
                .xslAndName(paramXsl).type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                .xslAndName(x(paramXsl)).type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                .xslAndName(y(paramXsl)).type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                .xslAndName(url(paramXsl)).type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                .xslAndName(orig(paramXsl)).type(Param.Type.STRING)
                .endParameter()
                .startParameter()
                .xslAndName(color(paramXsl)).type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                .xslAndName(colorAvg(paramXsl)).type(Param.Type.NUMERIC)
                .endParameter();
        }
        return pb;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    protected static Picture picture(String xslName,
                                     Integer width,
                                     Integer height,
                                     String url,
                                     String urlSource,
                                     String urlOrig,
                                     Double colorness,
                                     Double colornessAvg,
                                     Boolean whiteBackground) {
        return PictureBuilder.newBuilder()
            .setXslName(xslName)
            .setWidth(width)
            .setHeight(height)
            .setUrl(url)
            .setUrlSource(urlSource)
            .setUrlOrig(urlOrig)
            .setColorness(colorness)
            .setColornessAvg(colornessAvg)
            .setIsWhiteBackground(whiteBackground)
            .build();
    }
}
