package ru.yandex.market.mbo.db.forms;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.common.processing.OperationException;
import ru.yandex.market.mbo.common.validation.json.JsonSchemaValidator;
import ru.yandex.market.mbo.common.validation.json.JsonValidationException;
import ru.yandex.market.mbo.core.kdepot.api.KnowledgeDepotServiceMock;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.gwt.client.pages.model.editor.builder.ModelFormBuilder;
import ru.yandex.market.mbo.gwt.models.forms.model.ExtendedModelForm;
import ru.yandex.market.mbo.gwt.models.forms.model.FormType;
import ru.yandex.market.mbo.gwt.models.forms.model.ModelForm;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.server.utils.ModelFormConverter;
import ru.yandex.market.mbo.user.AutoUser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ModelFormServiceTest {
    private static final String VALID_XML_PATH = "/model_forms/valid_model_form.xml";
    private static final String INVALID_VALID_XML = "/model_forms/invalid_model_form.xml";
    private static final String BROKEN_VALID_XML = "/model_forms/broken_model_form.xml";
    private static final String SIMPLE_XML_PATH = "/model_forms/simple_model_form.xml";
    private static final String EMPTY_BLOCK_XML_PATH = "/model_forms/empty_block_model_form.xml";
    private static final String ONE_LINE_XML_PATH = "/model_forms/one_line_model_form.xml";

    private static final String MODEL_FORM_SCHEMA_PATH = "/ru/yandex/market/mbo/db/forms/ModelForm_schema.json";

    private static final long CATEGORY_ID = 100;

    private ModelFormServiceImpl modelFormService;
    private TovarTreeService tovarTreeService;
    private TovarCategory tovarCategory;
    private AutoUser autoUser = new AutoUser(100);

    @Before
    public void setUp() throws Exception {
        CategoryMappingServiceMock categoryMappingService = new CategoryMappingServiceMock();
        categoryMappingService.addMapping(CATEGORY_ID, CATEGORY_ID + 1);

        tovarCategory = new TovarCategory(0);
        tovarCategory.setHid(CATEGORY_ID);
        tovarCategory.setPublished(true);

        tovarTreeService = Mockito.mock(TovarTreeService.class);
        Mockito.when(tovarTreeService.getCategoryByHid(Mockito.eq(CATEGORY_ID))).thenReturn(tovarCategory);

        KnowledgeDepotServiceMock knowledgeDepotService = new KnowledgeDepotServiceMock();

        modelFormService = new ModelFormServiceImpl(knowledgeDepotService, categoryMappingService);
        modelFormService.setTovarTreeService(tovarTreeService);
    }

    @Test
    public void testValidXmlWillSaveSuccessfully() {
        String form = loadFileContent(VALID_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
    }

    @Test(expected = OperationException.class)
    public void testInvalidXmlWillFailedToSave() {
        String form = loadFileContent(INVALID_VALID_XML);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
    }

    @Test(expected = OperationException.class)
    public void testBrokenXmlWillFailedToSave() {
        String form = loadFileContent(BROKEN_VALID_XML);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
    }

    @Test
    public void testOneLineXmlWillSaveSuccessfully() {
        String form = loadFileContent(ONE_LINE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);

        // проверяем, что получение формы также работает корректно
        ModelForm actualModelForm = modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);
        ModelForm expectedModelForm = createSimpleModelForm();
        Assertions.assertThat(actualModelForm).isEqualTo(expectedModelForm);
    }

    @Test
    public void testBlockEmptyXmlWillSaveSuccessfully() {
        String form = loadFileContent(EMPTY_BLOCK_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);

        // проверяем, что получение формы также работает корректно
        ModelForm actualModelForm = modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);
        ModelForm expectedModelForm = ModelFormConverter.fromXml(form);
        Assertions.assertThat(actualModelForm).isEqualTo(expectedModelForm);
    }

    @Test
    public void modelFormWillBeCorrectParsed() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);

        ModelForm actualModelForm = modelFormService.getModelForm(CATEGORY_ID, FormType.MODEL_EDITOR);
        ModelForm expectedModelForm = createSimpleModelForm();
        Assertions.assertThat(actualModelForm).isEqualTo(expectedModelForm);
    }

    @Test
    public void testGenerationOfValidJson() throws JsonValidationException {
        String xml = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(xml, CATEGORY_ID, FormType.MODEL_EDITOR);

        String json = modelFormService.getModelFormJson(CATEGORY_ID, FormType.MODEL_EDITOR);

        JsonSchemaValidator jsonSchemaValidator = JsonSchemaValidator.createFromClasspath(MODEL_FORM_SCHEMA_PATH);
        jsonSchemaValidator.validate(json);

        Assertions.assertThat(json).isEqualTo(
            "{\"tabs\":[" +
                "{\"name\":\"Параметры модели\",\"blocks\":" +
                    "[{\"name\":\"Тип/ОС\",\"properties\":[\"Type\",\"SmartPhoneOS\"]}," +
                    "{\"name\":\"Фотокамера\",\"properties\":[\"dual_camera\",\"RAWsupport\"]}]}," +
                "{\"name\":\"Картинки\",\"blocks\":" +
                    "[{\"name\":\"Картинки модели\",\"properties\":" +
                    "[\"XL-Picture\",\"XLPictureUrl\",\"XL-Picture_2\",\"XLPictureUrl_2\"]}]}" +
            "]}");
    }

    @Test
    public void getPublishedModelFormTest() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);

        String publishedXml = modelFormService.getPublishedModelFormXml(CATEGORY_ID, FormType.MODEL_EDITOR);
        String publishedJson = modelFormService.getPublishedModelFormJson(CATEGORY_ID, FormType.MODEL_EDITOR);
        Assertions.assertThat(publishedXml).isNull();
        Assertions.assertThat(publishedJson).isNull();

        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        publishedXml = modelFormService.getPublishedModelFormXml(CATEGORY_ID, FormType.MODEL_EDITOR);
        publishedJson = modelFormService.getPublishedModelFormJson(CATEGORY_ID, FormType.MODEL_EDITOR);
        Assertions.assertThat(publishedXml).isEqualTo(form);
        Assertions.assertThat(publishedJson).isEqualTo(getSimpleJson());

        modelFormService.unpublishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        publishedXml = modelFormService.getPublishedModelFormXml(CATEGORY_ID, FormType.MODEL_EDITOR);
        publishedJson = modelFormService.getPublishedModelFormJson(CATEGORY_ID, FormType.MODEL_EDITOR);
        Assertions.assertThat(publishedXml).isNull();
        Assertions.assertThat(publishedJson).isNull();
    }

    @Test
    public void testFindModelWithDifferentFilters() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        ExtendedModelForm expected = ExtendedModelForm.from(createSimpleModelForm());
        expected.setCategoryId(CATEGORY_ID);
        expected.setPublished(true);

        List<ExtendedModelForm> forms1 =
            modelFormService.findForms(new ModelFormFilter()
                                           .setCategoryIds(Collections.singletonList(CATEGORY_ID)));
        Assertions.assertThat(forms1)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expected);

        List<ExtendedModelForm> forms2 =
            modelFormService.findForms(new ModelFormFilter()
                                           .setCategoryIds(Collections.singletonList(CATEGORY_ID))
                                           .setPublished(true));
        Assertions.assertThat(forms2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expected);

        List<ExtendedModelForm> forms3 =
            modelFormService.findForms(new ModelFormFilter()
                                           .setCategoryIds(Collections.singletonList(CATEGORY_ID))
                                           .setPublished(false));
        Assertions.assertThat(forms3).isEmpty();

        List<ExtendedModelForm> forms4 = modelFormService.findForms(new ModelFormFilter()
                                                                        .setPublished(true));
        Assertions.assertThat(forms4).isEmpty();

        List<ExtendedModelForm> forms5 = modelFormService.findForms(new ModelFormFilter());
        Assertions.assertThat(forms5).isEmpty();
    }

    @Test
    public void testFindModelFormsWontFailIfFilterContainsUnexistingCategoryIds() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);

        ExtendedModelForm expected = ExtendedModelForm.from(createSimpleModelForm());
        expected.setCategoryId(CATEGORY_ID);

        ModelFormFilter modelFormFilter = new ModelFormFilter()
            .setCategoryIds(Arrays.asList(CATEGORY_ID, CATEGORY_ID + 1));
        List<ExtendedModelForm> forms = modelFormService.findForms(modelFormFilter);

        Assertions.assertThat(forms)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(expected);
    }

    @Test
    public void testUpdateTovarCategoryOnFormPublish() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
        tovarCategory.setAcceptPartnerSkus(false);
        tovarCategory.setAcceptGoodContent(false);

        Mockito.doAnswer(args -> {
            TovarCategory after = args.getArgument(1);
            tovarCategory.setAcceptPartnerSkus(after.isAcceptPartnerSkus());
            return null;
        }).when(tovarTreeService).updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());

        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        Mockito.verify(tovarTreeService, Mockito.times(1))
            .updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());

        Assertions.assertThat(tovarCategory.isAcceptPartnerSkus()).isTrue();
        Assertions.assertThat(tovarCategory.isAcceptGoodContent()).isFalse();
    }

    @Test
    public void testNotChangedTovarCategoryOnFormPublish() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
        tovarCategory.setAcceptPartnerSkus(true);
        tovarCategory.setAcceptGoodContent(true);

        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        Mockito.verify(tovarTreeService, Mockito.times(0))
            .updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());
    }

    @Test
    public void testUpdateTovarCategoryOnFormUnpublish() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
        tovarCategory.setAcceptPartnerSkus(true);
        tovarCategory.setAcceptGoodContent(true);
        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        Mockito.doAnswer(args -> {
            TovarCategory after = args.getArgument(1);
            tovarCategory.setAcceptPartnerSkus(after.isAcceptPartnerSkus());
            return null;
        }).when(tovarTreeService).updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());

        modelFormService.unpublishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        Mockito.verify(tovarTreeService, Mockito.times(1))
            .updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());

        Assertions.assertThat(tovarCategory.isAcceptPartnerSkus()).isFalse();
        Assertions.assertThat(tovarCategory.isAcceptGoodContent()).isFalse();
    }

    @Test
    public void testNotChangedTovarCategoryOnFormUnpublish() {
        String form = loadFileContent(SIMPLE_XML_PATH);
        modelFormService.saveModelFormXml(form, CATEGORY_ID, FormType.MODEL_EDITOR);
        tovarCategory.setAcceptPartnerSkus(true);
        tovarCategory.setAcceptGoodContent(true);
        modelFormService.publishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());
        tovarCategory.setAcceptPartnerSkus(false);
        tovarCategory.setAcceptGoodContent(false);

        modelFormService.unpublishModelForm(CATEGORY_ID, FormType.MODEL_EDITOR, autoUser.getId());

        Mockito.verify(tovarTreeService, Mockito.times(0))
            .updateCategory(Mockito.any(), Mockito.any(), Mockito.anyLong());
    }

    private String loadFileContent(String resourceFilePath) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourceFilePath);
        return new BufferedReader(new InputStreamReader(inputStream))
            .lines().collect(Collectors.joining("\n"));
    }

    private static ModelForm createSimpleModelForm() {
       return new ModelFormBuilder()
            .startTab("Параметры модели")
                .startBlock("Тип/ОС")
                    .property("Type")
                    .property("SmartPhoneOS")
                .endBlock()
                .startBlock("Фотокамера")
                    .property("dual_camera")
                    .property("RAWsupport")
                .endBlock()
            .endTab()
            .startTab("Картинки")
                .startBlock("Картинки модели")
                    .property("XL-Picture")
                    .property("XLPictureUrl")
                    .property("XL-Picture_2")
                    .property("XLPictureUrl_2")
                .endBlock()
            .endTab()
            .getModelForm();
    }

    private static String getSimpleJson() {
        return "{\"tabs\":[" +
            "{\"name\":\"Параметры модели\",\"blocks\":" +
            "[{\"name\":\"Тип/ОС\",\"properties\":[\"Type\",\"SmartPhoneOS\"]}," +
            "{\"name\":\"Фотокамера\",\"properties\":[\"dual_camera\",\"RAWsupport\"]}]}," +
            "{\"name\":\"Картинки\",\"blocks\":" +
            "[{\"name\":\"Картинки модели\",\"properties\":" +
            "[\"XL-Picture\",\"XLPictureUrl\",\"XL-Picture_2\",\"XLPictureUrl_2\"]}]}" +
            "]}";
    }

}
