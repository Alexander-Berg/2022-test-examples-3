package ru.yandex.market.mbo.db.templates.generator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.TovarTreeDaoMock;
import ru.yandex.market.mbo.db.forms.ModelFormService;
import ru.yandex.market.mbo.db.params.MdmCategorySettingsService;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.param.SkuParameterMode;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplate;
import ru.yandex.market.mbo.gwt.models.visual.templates.OutputTemplateType;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.templates.generator.OutputTemplateAssert.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.07.2019
 */
public class ModelFormBasedTemplateGeneratorTest {

    private static final int CATEGORY_ID = 43464882;
    private static final OutputTemplateType TEMPLATE_TYPE = OutputTemplateType.PUT_MODEL;

    private ModelFormBasedTemplateGenerator generator;

    @Rule
    public final MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private ModelFormService modelFormService;
    @Mock
    private MdmCategorySettingsService mdmCategorySettingsService;
    @Mock
    private TemplateGenerator fallbackGenerator;

    private ParameterLoaderServiceStub parameterLoaderService;

    private CategoryParam color;
    private CategoryParam size;
    private CategoryParam volume;
    private CategoryParam xlPicture;
    private CategoryParam isPartner;

    @Before
    public void setUp() {
        color = param("color", "Цвет");
        size = param("size", "Размер");
        volume = param("volume", "Объем");
        xlPicture = param(XslNames.XL_PICTURE, XslNames.XL_PICTURE);
        isPartner = param(XslNames.IS_PARTNER, XslNames.IS_PARTNER);

        TovarCategory tovarCategory = new TovarCategory();
        tovarCategory.setHid(CATEGORY_ID);
        tovarCategory.setLeaf(true);
        TovarTreeDaoMock tovarTreeDao = new TovarTreeDaoMock(tovarCategory);
        parameterLoaderService = new ParameterLoaderServiceStub(
            new CategoryEntities(CATEGORY_ID, emptyList()));

        modelFormService = mock(ModelFormService.class);
        mdmCategorySettingsService = mock(MdmCategorySettingsService.class);

        generator = new ModelFormBasedTemplateGenerator(
            parameterLoaderService,
            TEMPLATE_TYPE,
            modelFormService,
            mdmCategorySettingsService,
            fallbackGenerator);
    }

    @Test
    public void renderBlock() {
        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                    .property(size.getXslName())
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size, volume);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие", color.getXslName(), size.getXslName(), volume.getXslName());
    }

    @Test
    public void renderBothTabs() {
        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                .endBlock()
            .endTab()
            .startTab("Умный дом")
                .startBlock("Умный дом размер")
                    .property(size.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size, volume);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие", color.getXslName())
            .contentContainsBlock("Умный дом размер")
            .contentContainsExactlyParamsInBlock("Умный дом размер", size.getXslName());
    }

    @Test
    public void skipService() {
        color.setService(true);

        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                    .property(size.getXslName())
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size, volume);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие", size.getXslName(), volume.getXslName());
    }

    @Test
    public void skipMissed() {
        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                    .property(size.getXslName())
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие", color.getXslName(), size.getXslName());

        assertThat(template.getGeneratorMessage()).isEqualTo("Не найден параметр volume");
    }

    @Test
    public void skipSku() {
        size.setUseInSku(true);

        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                    .property(size.getXslName())
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size, volume);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие", color.getXslName(), volume.getXslName());
    }

    @Test
    public void skipSkuModeAndRenderSkuDefiningForPsku() {
        color.setSkuParameterMode(SkuParameterMode.SKU_NONE);
        size.setSkuParameterMode(SkuParameterMode.SKU_DEFINING);
        volume.setSkuParameterMode(SkuParameterMode.SKU_INFORMATIONAL);

        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(color.getXslName())
                    .property(size.getXslName())
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(
            xlPicture, color, size, volume, isPartner);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentContainsBlock("Общие")
            .contentContainsExactlyParamsInBlock("Общие",
                isPartner.getXslName(), size.getXslName(), color.getXslName())
            .contentContainsExactlyPartnerParamsInBlock("Общие", true, size.getXslName());
    }

    @Test
    public void fallbackOnZeroTabs() {
        OutputTemplate fallbackTemplate = new OutputTemplate();

        when(fallbackGenerator.generate(CATEGORY_ID)).thenReturn(fallbackTemplate);
        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(new ModelForm());


        OutputTemplate template = generator.generate(CATEGORY_ID);


        Assertions.assertThat(template).isSameAs(fallbackTemplate);
        assertThat(template.getGeneratorMessage())
            .isEqualTo("Нет ни одной вкладки в шаблоне операторской карточки. Использован генератор по параметрам");
    }

    @Test
    public void fallbackOnMissedForm() {
        OutputTemplate fallbackTemplate = new OutputTemplate();
        when(fallbackGenerator.generate(CATEGORY_ID)).thenReturn(fallbackTemplate);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        Assertions.assertThat(template).isSameAs(fallbackTemplate);
        assertThat(template.getGeneratorMessage())
            .isEqualTo("У категории нет шаблона операторской карточки. Использован генератор по параметрам");
    }

    @Test
    public void skipEmptyBlock() {
        volume.setUseInSku(true);

        // @formatter:off
        ModelForm form = ModelFormBuilder.create()
            .startTab("Параметры модели")
                .startBlock("Общие")
                    .property(volume.getXslName())
                .endBlock()
            .endTab()
            .endForm();
        // @formatter:on

        when(modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR))
            .thenReturn(form);

        parameterLoaderService.addAllCategoryParams(xlPicture, volume);

        OutputTemplate template = generator.generate(CATEGORY_ID);

        assertThat(template).contentNotNullOrEmpty()
            .contentDoesntContainBlock("Общие");
    }

    private static CategoryParam param(String xslName, String name) {
        return CategoryParamBuilder.newBuilder()
            .setCategoryHid(CATEGORY_ID)
            .setXslName(xslName)
            .setName(name)
            .build();
    }
}
