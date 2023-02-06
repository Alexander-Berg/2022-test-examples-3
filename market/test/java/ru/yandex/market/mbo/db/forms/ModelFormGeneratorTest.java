package ru.yandex.market.mbo.db.forms;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.ImageType;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelFormBlock;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author s-ermakov
 */
public class ModelFormGeneratorTest {
    private ParameterLoaderServiceStub parameterLoaderService;

    private ModelFormGenerator modelFormGenerator;

    private static final long CATEGORY_ID = 100;

    @Before
    public void before() {
        parameterLoaderService = new ParameterLoaderServiceStub();
        modelFormGenerator = new ModelFormGeneratorImpl(parameterLoaderService);
    }

    @Test
    public void generateOperatorModelFormTest() {
        List<CategoryParam> params = createParams();
        parameterLoaderService.addAllCategoryParams(params);

        Assertions.assertThat(params.get(0).getName()).isNotNull();

        // @formatter:off
        ModelForm expected = new ModelFormBuilder()
            .startTab("Параметры модели")
                .startBlock("Общие характеристики")
                    .property("Price")
                    .property("Comment")
                    .property("Type")
                    .property("skuTest4")
                    .property("test")
                    .property("test2")
                .endBlock()
                .startBlock("Алиасы")
                    .property("aliases")
                .endBlock()
                .startBlock("Маркетинговое описание")
                    .property("description")
                    .property("draft_description")
                    .property("source_description")
                .endBlock()
                .startBlock("SKU параметры")
                    .property("skuTest1")
                    .property("skuTest2")
                    .property("skuTest3")
                .endBlock()
                .startBlock("Служебные поля")
                    .property("name")
                    .property("url")
                    .property("additional_url")
                    .property("BarCode")
                    .property("VendorCode")
                    .property("InstructionLink")
                    .property("IsSku")
                    .property("NotFound")
                    .property("DBFilledOK")
                .endBlock()
            .endTab()
            .startTab("Картинки")
                .startBlock("Картинки модели")
                    .property("XL-Picture")
                    .property("XLPictureUrl")
                    .property("XL-Picture_2")
                    .property("XLPictureUrl_2")
                    .property("XL-Picture_13")
                    .property("XLPictureUrl_13")
                .endBlock()
            .endTab()
            .startTab("Мастер данные")
                .startBlock("Мастер данные")
                    .property(XslNames.LIFE_SHELF)
                    .property(XslNames.WARRANTY_PERIOD)
                .endBlock()
            .endTab()
            .getModelForm();
        // @formatter:on

        ModelForm actual = modelFormGenerator.generateModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void generateModelFormWithLicensorBlockTest() {
        List<CategoryParam> params = Stream.of(
            param("licensor"),
            param("hero_global"),
            param("pers_model")
        ).map(CategoryParamBuilder::build).collect(Collectors.toList());
        parameterLoaderService.addAllCategoryParams(params);

        ModelForm expected = generateBaseOperatorModelForm();
        expected.getTab("Параметры модели").get().getBlocks()
            .add(1, new ModelFormBlock("Лицензиар, тема, персонаж", "licensor", "hero_global", "pers_model"));

        ModelForm actual = modelFormGenerator.generateModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void generateModelFormWithNotFullLicensorBlockTest() {
        List<CategoryParam> params = Stream.of(
            param("pers_model"),
            param("licensor")
        ).map(CategoryParamBuilder::build).collect(Collectors.toList());
        parameterLoaderService.addAllCategoryParams(params);

        ModelForm expected = generateBaseOperatorModelForm();
        expected.getTab("Параметры модели").get().getBlocks()
            .add(1, new ModelFormBlock("Лицензиар, тема, персонаж", "licensor", "pers_model"));

        ModelForm actual = modelFormGenerator.generateModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    private CategoryParamBuilder param(String xslName) {
        return CategoryParamBuilder.newBuilder()
            .setName(xslName)
            .setXslName(xslName)
            .setCategoryHid(CATEGORY_ID)
            .setUseForGuru(true);
    }


    private static ModelForm generateBaseOperatorModelForm() {
        // @formatter:off
        return new ModelFormBuilder()
            .startTab("Параметры модели")
                .startBlock("Общие характеристики")
                .endBlock()
                .startBlock("Алиасы")
                    .property("aliases")
                .endBlock()
                .startBlock("Маркетинговое описание")
                    .property("description")
                    .property("draft_description")
                    .property("source_description")
                .endBlock()
                .startBlock("Служебные поля")
                    .property("name")
                    .property("url")
                    .property("additional_url")
                    .property("BarCode")
                    .property("VendorCode")
                    .property("InstructionLink")
                    .property("IsSku")
                    .property("NotFound")
                    .property("DBFilledOK")
                .endBlock()
            .endTab()
            .startTab("Картинки")
                .startBlock("Картинки модели")
                .endBlock()
            .endTab()
            .startTab("Мастер данные")
                .startBlock("Мастер данные")
                .endBlock()
            .endTab()
            .getModelForm();
        // @formatter:on
    }

    @Test
    @SuppressWarnings("checkstyle:magicNumber")
    public void generateContentLabModelFormTest() {
        parameterLoaderService.addAllCategoryParams(createParams());

        // @formatter:off
        ModelForm expected = new ModelFormBuilder()
            .startTab("Параметры модели")
                .startBlock("Общие характеристики")
                    .property("Price")
                    .property("Comment")
                    .property("Type")
                    .property("skuTest4")
                    .property("test")
                    .property("test2")
                .endBlock()
                .startBlock("Маркетинговое описание")
                    .property("description")
                    .property("draft_description")
                    .property("source_description")
                .endBlock()
                .startBlock("SKU параметры")
                    .property("skuTest1")
                    .property("skuTest2")
                    .property("skuTest3")
                .endBlock()
                .startBlock("Служебные поля")
                    .property("name")
                    .property("url")
                    .property("additional_url")
                    .property("BarCode")
                    .property("VendorCode")
                    .property("InstructionLink")
                    .property("IsSku")
                .endBlock()
            .endTab()
            .startTab("Мастер данные")
                .startBlock("Мастер данные")
                    .property(XslNames.LIFE_SHELF)
                    .property(XslNames.WARRANTY_PERIOD)
                .endBlock()
            .endTab()
            .getModelForm();
        // @formatter:on

        ModelForm actual = modelFormGenerator.generateModelForm(CATEGORY_ID, FormType.CONTENT_LAB);

        Assertions.assertThat(actual).isEqualTo(expected);
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private List<CategoryParam> createParams() {
        return Stream.of(
            param("Comment").setAdvFilterIndex(2),
            param("Type").setAdvFilterIndex(3),
            param("vendor_line").setAdvFilterIndex(4).setHidden(true),
            param("Price").setAdvFilterIndex(0),
            param("picture").setAdvFilterIndex(5).setLevel(CategoryParam.Level.OFFER),
            param("test"),
            param("description"),
            param("draft_description"),
            // без source_description, проверить разные варианты
            param("test2"),
            param(XslNames.LIFE_SHELF).setMdmParameter(true),
            param(XslNames.WARRANTY_PERIOD).setMdmParameter(true),

            // vendor
            param("vendor").setService(false)
                .setLevel(CategoryParam.Level.MODEL)
                .setUseForGuru(true).setUseForGurulight(true)
                .setCommonFilterIndex(1).setAdvFilterIndex(1),

            //sku params
            param("skuTest1").setLevel(CategoryParam.Level.OFFER)
                .setSkuParameterMode(SkuParameterMode.SKU_DEFINING),
            param("skuTest2").setLevel(CategoryParam.Level.OFFER)
                .setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL).setMandatory(true),
            param("skuTest3").setLevel(CategoryParam.Level.OFFER)
                .setShowOnSkuTab(true),
            param("skuTest4").setLevel(CategoryParam.Level.MODEL)
                .setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL),

            // service
            param("NotFound").setService(true),
            param("InstructionLink").setService(true),
            param("DBFilledOK").setService(true),
            param("name").setService(true),
            param("IsSku").setService(true),

            // xl-pictures
            param("XL-Picture").setService(true),
            param(ImageType.XL_PICTURE.getHeightParamName("XL-Picture")).setService(true),
            param(ImageType.XL_PICTURE.getWidthParamName("XL-Picture")).setService(true),
            param(ImageType.XL_PICTURE.getUrlParamName("XL-Picture")).setService(true),
            param(ImageType.XL_PICTURE.getRawUrlParamName("XL-Picture")).setService(true),

            param("XL-Picture_2").setService(true),
            param(ImageType.XL_PICTURE.getHeightParamName("XL-Picture_2")).setService(true),
            param(ImageType.XL_PICTURE.getWidthParamName("XL-Picture_2")).setService(true),
            param(ImageType.XL_PICTURE.getUrlParamName("XL-Picture_2")).setService(true),
            param(ImageType.XL_PICTURE.getRawUrlParamName("XL-Picture_2")).setService(true),

            param("XL-Picture_13").setService(true),
            param(ImageType.XL_PICTURE.getHeightParamName("XL-Picture_13")).setService(true),
            param(ImageType.XL_PICTURE.getWidthParamName("XL-Picture_13")).setService(true),
            param(ImageType.XL_PICTURE.getUrlParamName("XL-Picture_13")).setService(true),
            param(ImageType.XL_PICTURE.getRawUrlParamName("XL-Picture_13")).setService(true),

            param("XL-Picture_8_mdata").setService(true)
        ).map(CategoryParamBuilder::build).collect(Collectors.toList());
    }
}
