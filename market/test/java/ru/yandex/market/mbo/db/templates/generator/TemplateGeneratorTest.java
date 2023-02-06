package ru.yandex.market.mbo.db.templates.generator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.params.MdmCategorySettingsService;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryBuilder;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplate;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplateType;
import ru.yandex.market.mbo.gwt.utils.XslNames;

/**
 * Tests of {@link TemplateGeneratorImpl}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TemplateGeneratorTest {
    private static final long CATEGORY_ID = 100L;
    private static final long NON_LEAF_CATEGORY_ID = 101L;
    private static final TovarCategory CATEGORY = TovarCategoryBuilder.newBuilder(200, CATEGORY_ID)
        .create();
    private static final TovarCategory NON_LEAF_CATEGORY = TovarCategoryBuilder.newBuilder(201, NON_LEAF_CATEGORY_ID)
        .setLeaf(false)
        .create();

    private static final CategoryParam PARAM_NAME = CategoryParamBuilder
        .newBuilder(1, XslNames.NAME, Param.Type.STRING)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true)
        .build();
    private static final CategoryParam PARAM_VENDOR = CategoryParamBuilder
        .newBuilder(2, XslNames.VENDOR, Param.Type.ENUM)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setUseForGurulight(true)
        .setCommonFilterIndex(1).setAdvFilterIndex(1)
        .setHidden(true).setService(false)
        .build();
    private static final CategoryParam PARAM_COLOR_GLOB = CategoryParamBuilder
        .newBuilder(3, "color_glob", Param.Type.NUMERIC)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setUseForGurulight(true)
        .setCommonFilterIndex(2).setAdvFilterIndex(2)
        .build();
    private static final CategoryParam PARAM_TYPE = CategoryParamBuilder
        .newBuilder(4, "Type", Param.Type.ENUM)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(false)
        .setUseForGuru(true).setUseForGurulight(true)
        .setCommonFilterIndex(3).setAdvFilterIndex(4)
        .build();
    private static final CategoryParam PARAM_QUICK_CHARGE = CategoryParamBuilder
        .newBuilder(5, "QuickCharge", Param.Type.BOOLEAN)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setUseForGurulight(true)
        .setCommonFilterIndex(-1).setAdvFilterIndex(98)
        .build();
    private static final CategoryParam PARAM_COLOR_VENDOR = CategoryParamBuilder
        .newBuilder(6, "color_vendor", Param.Type.BOOLEAN)
        .setLevel(CategoryParam.Level.OFFER)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setUseForGurulight(true).setMultifield(true)
        .setCommonFilterIndex(-1).setAdvFilterIndex(-1)
        .build();
    private static final CategoryParam PARAM_XL_PICTURE = CategoryParamBuilder
        .newBuilder(8, "XL-Picture", Param.Type.STRING)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true)
        .setCommonFilterIndex(-1).setAdvFilterIndex(-1)
        .build();
    private static final CategoryParam PARAM_COMMENT = CategoryParamBuilder
        .newBuilder(9, XslNames.COMMENT, Param.Type.STRING)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setService(true)
        .setCommonFilterIndex(-1).setAdvFilterIndex(-1)
        .build();
    private static final CategoryParam PARAM_OFFER_TYPE = CategoryParamBuilder
        .newBuilder(10, "offer", Param.Type.STRING)
        .setLevel(CategoryParam.Level.OFFER)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setUseForGurulight(true)
        .setCommonFilterIndex(101).setAdvFilterIndex(101)
        .build();
    private static final CategoryParam PARAM_RAW_VENDOR = CategoryParamBuilder
        .newBuilder(11, XslNames.RAW_VENDOR, Param.Type.STRING)
        .setLevel(CategoryParam.Level.MODEL)
        .setCategoryHid(CATEGORY_ID).setPublished(true)
        .setUseForGuru(true).setHidden(true).setService(true)
        .build();

    private TemplateGeneratorImpl templateGenerator;
    private ParameterLoaderServiceStub parameterLoaderService;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private MdmCategorySettingsService mdmCategorySettingsService;

    @Mock
    private ModelFormService modelFormService;

    @Before
    public void setUp() throws Exception {
        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryParam(PARAM_NAME);
        parameterLoaderService.addCategoryParam(PARAM_VENDOR);
        parameterLoaderService.addCategoryParam(PARAM_COLOR_GLOB);
        parameterLoaderService.addCategoryParam(PARAM_TYPE);
        parameterLoaderService.addCategoryParam(PARAM_QUICK_CHARGE);
        parameterLoaderService.addCategoryParam(PARAM_COLOR_VENDOR);
        parameterLoaderService.addCategoryParam(PARAM_XL_PICTURE);
        parameterLoaderService.addCategoryParam(PARAM_COMMENT);
        parameterLoaderService.addCategoryParam(PARAM_OFFER_TYPE);
        parameterLoaderService.addCategoryParam(PARAM_RAW_VENDOR);

        templateGenerator = new TemplateGeneratorImpl(parameterLoaderService,
            mdmCategorySettingsService,
            modelFormService);
    }

    @Test
    public void testCorrectParamAreUsedDuringGeneration() {
        OutputTemplate template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentContainsExactlyParamsInBlock("Общие характеристики",
                "raw_vendor", "color_glob", "Type", "QuickCharge", "offer", "name", "color_vendor",
                "XL-Picture", "Comment");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_BRIEF_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentDoesntContainBlock("Общие характеристики")
            .contentContainsExactlyParamsInBlockInRaw("Технические характеристики",
                "color_glob", "Type");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_MICRO_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentDoesntContainBlock("Общие характеристики")
            .contentContainsExactlyParamsInBlockInRaw("Технические характеристики",
                "color_glob", "Type");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_MICRO_MODEL_SEARCH);
        OutputTemplateAssert.assertThat(template)
            .contentDoesntContainBlock("Общие характеристики")
            .contentContainsExactlyParamsInBlockInRaw("Технические характеристики",
                "color_glob", "Type");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_FRIENDLY_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentContainsExactlyParamsInBlock("Общие характеристики", "color_glob", "Type");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.SEO);
        OutputTemplateAssert.assertThat(template)
            .contentNullOrEmpty();

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.DESIGN_GROUP_PARAMS);
        OutputTemplateAssert.assertThat(template)
            .contentNullOrEmpty();
    }

    @Test
    public void testSkuDefinigParamsForPskuGeneration() {
        CategoryParam skuDefinigName = CategoryParamBuilder
            .newBuilder(1, XslNames.NAME, Param.Type.STRING)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(CATEGORY_ID).setPublished(true)
            .setUseForGuru(true)
            .setSkuParameterMode(SkuParameterMode.SKU_DEFINING)
            .build();
        parameterLoaderService.addCategoryParam(skuDefinigName);

        OutputTemplate template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentContainsExactlyParamsInBlock("Общие характеристики",
                XslNames.IS_PARTNER, "name", "raw_vendor", "color_glob", "Type", "QuickCharge", "offer",
                "color_vendor", "XL-Picture", "Comment", "name")
            .contentContainsExactlyPartnerParamsInBlock("Общие характеристики", true, "name")
            .contentContainsExactlyPartnerParamsInBlock("Общие характеристики", false, "name");

        template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_FRIENDLY_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentContainsExactlyParamsInBlock("Общие характеристики",
                "is_partner", "name", "color_glob", "Type")
            .contentContainsExactlyPartnerParamsInBlock("Общие характеристики", true, "name");
    }

    @Test
    public void testAdditionalBlockWithMdmParams() {
        parameterLoaderService.addCategoryParam(CategoryParamBuilder
            .newBuilder(10000, "ShelfService", Param.Type.NUMERIC)
            .setCategoryHid(CATEGORY_ID).build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder
            .newBuilder(10001, "WarrantyPeriod", Param.Type.NUMERIC)
            .setCategoryHid(CATEGORY_ID).build());
        parameterLoaderService.addCategoryParam(CategoryParamBuilder
            .newBuilder(10010, "LifeShelf", Param.Type.NUMERIC)
            .setCategoryHid(CATEGORY_ID).build());

        Mockito.when(
            mdmCategorySettingsService.isMdmTimeParamApplicableForCategory(Mockito.any(), Mockito.eq(CATEGORY_ID)))
            .thenReturn(true);

        OutputTemplate template = templateGenerator.generate(CATEGORY_ID, OutputTemplateType.PUT_MODEL);
        OutputTemplateAssert.assertThat(template)
            .contentContainsExactlyParamsInBlock("Дополнительно",
                "LifeShelf", "ShelfLife_Unit",
                "ShelfService", "ShelfService_Unit",
                "WarrantyPeriod", "WarrantyPeriod_Unit");
        Assert.assertTrue(template.getContent().contains("{hide_shelf_life#ifz}"));
        Assert.assertTrue(template.getContent().contains("{hide_life_time#ifz}"));
        Assert.assertTrue(template.getContent().contains("{hide_warranty_period#ifz}"));
    }

    @Test
    public void renderMdmTimeParamValueTest() {
        CategoryParam shelfServiceParam = CategoryParamBuilder
            .newBuilder(10000, "ShelfService", Param.Type.NUMERIC)
            .setCategoryHid(CATEGORY_ID).build();

        String shelfServiceResult = TemplateGeneratorHelper.renderMdmTimeParamValue(
            shelfServiceParam, OutputTemplateType.PUT_MODEL);

        String expected =
            "{ShelfService_Unit#ifnz}" +
                "{if ($ShelfService_Unit == \"не ограничен\") return \"не огран.\";" +
                "string unit = \"\";" +
                "if ($ShelfService_Unit == \"часы\") unit = \" ч\";" +
                "if ($ShelfService_Unit == \"дни\") unit = \" дн.\";" +
                "if ($ShelfService_Unit == \"недели\") unit = \" нед.\";" +
                "if ($ShelfService_Unit == \"месяцы\") unit = \" мес.\";" +
                "if ($ShelfService_Unit == \"годы\") " +
                "unit = ($ShelfService>=11) && ($ShelfService<=19) ? \" лет\" : " +
                "((($ShelfService % 10 >= 1) && ($ShelfService % 10 <= 4)) ? \" г.\" : \" лет\");" +
                "return $ShelfService + unit;#exec}{#else}{ShelfService} дн.{#endif}" +
                "{ShelfService_Comment#ifnz}, {ShelfService_Comment}{#endif}";

        Assert.assertEquals(expected, shelfServiceResult);
    }
}
