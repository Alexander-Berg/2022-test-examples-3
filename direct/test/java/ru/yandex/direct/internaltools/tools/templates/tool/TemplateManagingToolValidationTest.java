package ru.yandex.direct.internaltools.tools.templates.tool;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.DirectTemplate;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateState;
import ru.yandex.direct.core.entity.internalads.repository.DirectTemplateRepository;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.templates.model.TemplateAction;
import ru.yandex.direct.internaltools.tools.templates.model.TemplateInput;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.ACTION_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.DIRECT_TEMPLATE_ID_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.FORMAT_NAME_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.PLACE_IDS_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.RESOURCES_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.model.TemplateInput.STATE_LABEL;
import static ru.yandex.direct.internaltools.tools.templates.tool.TemplateUtil.MAX_FORMAT_NAME_LENGTH;
import static ru.yandex.direct.internaltools.tools.templates.tool.TemplateUtil.MIN_NEW_TEMPLATE_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS;
import static ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.CANNOT_BE_EMPTY;
import static ru.yandex.direct.validation.defect.ids.StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX;
import static ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL;
import static ru.yandex.direct.validation.result.DefectIds.INVALID_FORMAT;
import static ru.yandex.direct.validation.result.DefectIds.INVALID_VALUE;
import static ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID;
import static ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateManagingToolValidationTest {
    private static final String FORMAT_NAME_VALID = "template";
    private static final String FORMAT_NAME_OVER_MAX_LENGTH = new String(new char[MAX_FORMAT_NAME_LENGTH + 1]);
    private static final String FORMAT_NAME_EMPTY = " 0 ";

    private static final Long TEMPLATE_ID_VALID_OLD = RandomNumberUtils.nextPositiveLong(MIN_NEW_TEMPLATE_ID);
    private static final Long TEMPLATE_ID_VALID_NEW = MIN_NEW_TEMPLATE_ID;
    private static final DirectTemplate DIRECT_TEMPLATE_OLD = new DirectTemplate()
            .withDirectTemplateId(TEMPLATE_ID_VALID_OLD)
            .withFormatName(FORMAT_NAME_VALID)
            .withState(DirectTemplateState.DEFAULT);

    private static final DirectTemplate DIRECT_TEMPLATE_NEW = new DirectTemplate()
            .withDirectTemplateId(TEMPLATE_ID_VALID_NEW)
            .withFormatName(FORMAT_NAME_VALID)
            .withState(DirectTemplateState.UNIFIED);

    private static final String PLACE_IDS_INVALID_STRING = "invalid";
    private static final String PLACE_IDS_INVALID_VALUES = "1195,1560,0";
    private static final String PLACE_IDS_EMPTY_VALID = "0";
    private static final String PLACE_IDS_VALID = "2, 3"; // Захардкожено в PlaceRepositoryMockUtils

    private static final String RESOURCES_INVALID_SYMBOLS_IN_STRING = "2020 invalid#2";
    private static final String RESOURCES_DUPLICATE_RESOURCE_NO = "2020 1\n2020 0";
    private static final String RESOURCES_MISSING_VALUE = "2020 1\n2005";
    private static final String RESOURCES_INVALID_REQUIRED_OPTION = "2000 10";
    private static final String RESOURCES_INVALID_RESOURCE_NO = "2000 1";
    private static final String RESOURCES_VALID = "7 1\n17 0"; // Захардкожено в TemplateResourceRepositoryMockUtils

    @Autowired
    private TemplateManagingTool templateManagingTool;

    @Autowired
    private DirectTemplateRepository directTemplateRepository;

    @Before
    public void before() {
        directTemplateRepository.delete(TEMPLATE_ID_VALID_OLD);
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_OLD);
    }

    @Test
    public void validate_WithoutAction_Negative() {
        var input = new TemplateInput();

        var validationResult = templateManagingTool.validate(input);
        assertThat("Пустое поле \"" + ACTION_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(ACTION_LABEL)), CANNOT_BE_NULL)));
    }

    // Тесты на валидацию копирования
    @Test
    public void validateCopy_WithoutId_Negative() {
        validateAction_WithoutId_Negative(TemplateAction.COPY);
    }

    @Test
    public void validateCopy_InvalidId_Negative() {
        validateAction_InvalidId_Negative(TemplateAction.COPY);
    }

    @Test
    public void validateCopy_ValidId_Positive() {
        validateAction_ValidId_Positive(TemplateAction.COPY);
    }

    // Тесты на валидацию удаления
    @Test
    public void validateDelete_WithoutId_Negative() {
        validateAction_WithoutId_Negative(TemplateAction.DELETE);
    }

    @Test
    public void validateDelete_InvalidId_Negative() {
        validateAction_InvalidId_Negative(TemplateAction.DELETE);
    }

    @Test
    public void validateDelete_ValidId_Positive() {
        validateAction_ValidId_Positive(TemplateAction.DELETE);
    }

    // Тесты на валидацию изменения
    @Test
    public void validateUpdate_WithoutId_Negative() {
        validateAction_WithoutId_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidId_Negative() {
        validateAction_InvalidId_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidStateForNewTemplates_Negative() {
        // Создаем через метод addOldTemplate, потому что нужно посетить значение id как минимум MIN_NEW_TEMPLATE_ID
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_NEW);

        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_VALID_NEW)
                .withState(DirectTemplateState.TRANSITIONAL);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.UPDATE.getActionName() + " шаблон: неправильное значение поля \"" + STATE_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(STATE_LABEL)), INVALID_VALUE)));

        directTemplateRepository.delete(TEMPLATE_ID_VALID_NEW);
    }

    @Test
    public void validateUpdate_FormatNameOverMaxLength_Negative() {
        validateAction_FormatNameOverMaxLength_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_FormatNameEmptyForNewTemplate_Negative() {
        // Создаем через метод addOldTemplate, потому что нужно посетить значение id = MIN_NEW_TEMPLATE_ID
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_NEW);

        validateAction_FormatNameEmptyForNewTemplate_Negative(TemplateAction.UPDATE, TEMPLATE_ID_VALID_NEW);

        directTemplateRepository.delete(TEMPLATE_ID_VALID_NEW);
    }

    @Test
    public void validateUpdate_FormatNameEmptyForOldTemplate_Positive() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD)
                .withFormatName(FORMAT_NAME_EMPTY);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.UPDATE.getActionName() + " шаблон: 0 в поле \"" + FORMAT_NAME_LABEL
                        + "\" для старых шаблонов принимается без ошибок валидации", validationResult,
                hasNoDefectsDefinitions());
    }

    @Test
    public void validateUpdate_ValidId_Positive() {
        validateAction_ValidId_Positive(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidPlaceIdsString_Negative() {
        validateAction_InvalidPlaceIdsString_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidPlaceIds_Negative() {
        validateAction_InvalidPlaceIds_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_ValidIdAndEmptyPlaceIds_Positive() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD)
                .withPlaceIds(PLACE_IDS_EMPTY_VALID);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.UPDATE.getActionName() + " шаблон: 0 в поле \"" + PLACE_IDS_LABEL
                + "\" принимается без ошибок валидации", validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validateUpdate_ValidIdAndPlaceIds_Positive() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD)
                .withPlaceIds(PLACE_IDS_VALID);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.UPDATE.getActionName() + " шаблон: существующее значение поля \"" + PLACE_IDS_LABEL
                + "\" принимается без ошибок валидации", validationResult, hasNoDefectsDefinitions());
    }

    @Test
    public void validateUpdate_InvalidSymbolsInResourcesString_Negative() {
        validateAction_InvalidSymbolsInResourcesString_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_DuplicateResourceNoInResourcesString_Negative() {
        validateAction_DuplicateResourceNoInResourcesString_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_MissingValueInResourcesString_Negative() {
        validateAction_MissingValueInResourcesString_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidRequiredOptionInResources_Negative() {
        validateAction_InvalidRequiredOptionInResources_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_InvalidResourceNoInResources_Negative() {
        validateAction_InvalidResourceNoInResources_Negative(TemplateAction.UPDATE);
    }

    @Test
    public void validateUpdate_ValidAllMeaningfulFields_Positive() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.UPDATE)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD)
                .withFormatName(FORMAT_NAME_VALID)
                .withState(DirectTemplateState.UNIFIED)
                .withPlaceIds(PLACE_IDS_VALID)
                .withResources(RESOURCES_VALID);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.UPDATE.getActionName() + " шаблон: правильные значения во всех полях " +
                "принимаются без ошибок валидации", validationResult, hasNoDefectsDefinitions());
    }

    // Тесты на валидацию создания
    @Test
    public void validateCreate_WithoutFormatName_Negative() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.CREATE.getActionName() + " шаблон: отсутствие поля \"" + FORMAT_NAME_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(FORMAT_NAME_LABEL)), CANNOT_BE_NULL)));
    }

    @Test
    public void validateCreate_EmptyFormatName_Negative() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE)
                .withFormatName("  ");

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.CREATE.getActionName() + " шаблон: пустое поле \"" + FORMAT_NAME_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(FORMAT_NAME_LABEL)), CANNOT_BE_EMPTY)));
    }

    @Test
    public void validateCreate_FormatNameOverMaxLength_Negative() {
        validateAction_FormatNameOverMaxLength_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_FormatNameEmptyForNewTemplate_Negative() {
        validateAction_FormatNameEmptyForNewTemplate_Negative(TemplateAction.CREATE, null);
    }

    @Test
    public void validateCreate_InvalidPlaceIdsString_Negative() {
        validateAction_InvalidPlaceIdsString_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_InvalidPlaceIds_Negative() {
        validateAction_InvalidPlaceIds_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_WithoutResources_Negative() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.CREATE.getActionName() + " шаблон: отсутствие поля \"" + RESOURCES_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)), CANNOT_BE_NULL)));
    }

    @Test
    public void validateCreate_EmptyResources_Negative() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE)
                .withResources("  ");

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.CREATE.getActionName() + " шаблон: пустое поле \"" + RESOURCES_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)), CANNOT_BE_EMPTY)));
    }

    @Test
    public void validateCreate_InvalidSymbolsInResourcesString_Negative() {
        validateAction_InvalidSymbolsInResourcesString_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_DuplicateResourceNoInResourcesString_Negative() {
        validateAction_DuplicateResourceNoInResourcesString_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_MissingValueInResourcesString_Negative() {
        validateAction_MissingValueInResourcesString_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_InvalidRequiredOptionInResources_Negative() {
        validateAction_InvalidRequiredOptionInResources_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_InvalidResourceNoInResources_Negative() {
        validateAction_InvalidResourceNoInResources_Negative(TemplateAction.CREATE);
    }

    @Test
    public void validateCreate_ValidAllMeaningfulFields_Positive() {
        var input = new TemplateInput()
                .withTemplateAction(TemplateAction.CREATE)
                .withFormatName(FORMAT_NAME_VALID)
                .withPlaceIds(PLACE_IDS_VALID)
                .withResources(RESOURCES_VALID);

        var validationResult = templateManagingTool.validate(input);
        assertThat(TemplateAction.CREATE.getActionName() + " шаблон: правильные значения во всех полях " +
                "принимаются без ошибок валидации", validationResult, hasNoDefectsDefinitions());
    }

    // Общие методы валидации
    private void validateAction_WithoutId_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: отсутствие поля \"" + DIRECT_TEMPLATE_ID_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(DIRECT_TEMPLATE_ID_LABEL)), CANNOT_BE_NULL)));
    }

    private void validateAction_InvalidId_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD + 1);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: несуществующее значение поля \"" + DIRECT_TEMPLATE_ID_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(DIRECT_TEMPLATE_ID_LABEL)), OBJECT_NOT_FOUND)));
    }

    private void validateAction_ValidId_Positive(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withDirectTemplateId(TEMPLATE_ID_VALID_OLD);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: существующее значение в поле \"" + DIRECT_TEMPLATE_ID_LABEL
                        + "\" принимается без ошибок валидации",
                validationResult, hasNoDefectsDefinitions());
    }

    private void validateAction_FormatNameOverMaxLength_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withFormatName(FORMAT_NAME_OVER_MAX_LENGTH);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: значение поля \"" + FORMAT_NAME_LABEL
                        + "\" превышает максимально разрешенное число символов", validationResult,
                hasDefectWithDefinition(validationError(path(field(FORMAT_NAME_LABEL)),
                        LENGTH_CANNOT_BE_MORE_THAN_MAX)));
    }

    private void validateAction_FormatNameEmptyForNewTemplate_Negative(TemplateAction action, Long templateId) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withDirectTemplateId(templateId)
                .withFormatName(FORMAT_NAME_EMPTY);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: значение поля \"" + FORMAT_NAME_LABEL
                        + "\" не может равняться 0 для новых шаблонов", validationResult,
                hasDefectWithDefinition(validationError(path(field(FORMAT_NAME_LABEL)), INVALID_VALUE)));
    }

    private void validateAction_InvalidPlaceIdsString_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withPlaceIds(PLACE_IDS_INVALID_STRING);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: невалидное значение поля \"" + PLACE_IDS_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(PLACE_IDS_LABEL)), MUST_BE_VALID_ID)));
    }

    private void validateAction_InvalidPlaceIds_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withPlaceIds(PLACE_IDS_INVALID_VALUES);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: несуществующее значение поля \"" + PLACE_IDS_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(PLACE_IDS_LABEL)), OBJECT_NOT_FOUND)));
    }

    private void validateAction_InvalidSymbolsInResourcesString_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withResources(RESOURCES_INVALID_SYMBOLS_IN_STRING);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: нечисловые символы в поле \"" + RESOURCES_LABEL
                        + "\" бросают ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)), INVALID_VALUE)));
    }

    private void validateAction_DuplicateResourceNoInResourcesString_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withResources(RESOURCES_DUPLICATE_RESOURCE_NO);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: неправильное значение поля \"" + RESOURCES_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)),
                        MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS)));
    }

    private void validateAction_MissingValueInResourcesString_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withResources(RESOURCES_MISSING_VALUE);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: неправильное значение поля \"" + RESOURCES_LABEL
                        + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)), INVALID_FORMAT)));
    }

    private void validateAction_InvalidRequiredOptionInResources_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withResources(RESOURCES_INVALID_REQUIRED_OPTION);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: неправильное значение обязательности ресурса в поле \""
                        + RESOURCES_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)),
                        MUST_BE_IN_THE_INTERVAL_INCLUSIVE)));
    }

    private void validateAction_InvalidResourceNoInResources_Negative(TemplateAction action) {
        var input = new TemplateInput()
                .withTemplateAction(action)
                .withResources(RESOURCES_INVALID_RESOURCE_NO);

        var validationResult = templateManagingTool.validate(input);
        assertThat(action.getActionName() + " шаблон: несуществующий номер единого ресурса в поле \""
                        + RESOURCES_LABEL + "\" бросает ошибку валидации", validationResult,
                hasDefectWithDefinition(validationError(path(field(RESOURCES_LABEL)), OBJECT_NOT_FOUND)));
    }
}
