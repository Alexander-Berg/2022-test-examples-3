package ru.yandex.direct.internaltools.tools.cashback;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.model.CashbackCategory;
import ru.yandex.direct.core.entity.cashback.model.CashbackProgram;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams;
import ru.yandex.direct.internaltools.tools.cashback.model.InternalToolsCashbackProgram;
import ru.yandex.direct.internaltools.tools.cashback.tool.CashbackProgramsTool;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.steps.CashbackSteps.getTechnicalCategory;
import static ru.yandex.direct.core.testing.steps.CashbackSteps.getTechnicalProgram;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramKey.fromCashbackProgram;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams.Action.CREATE;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams.Action.SHOW;
import static ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramsParams.Action.UPDATE;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.PERCENT_MAX_VALUE;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.PERCENT_MIN_VALUE;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.PROGRAM_TOOLTIP_INFO_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.PROGRAM_TOOLTIP_LINK_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.PROGRAM_TOOLTIP_LINK_TEXT_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConverter.getCategoryKey;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CashbackProgramsToolTest {

    @Autowired
    private CashbackProgramsTool cashbackProgramsTool;

    @Autowired
    private CashbackSteps cashbackSteps;

    @Autowired
    private UserSteps userSteps;

    private CashbackCategory category;
    private CashbackProgram program;

    @Before
    public void before() {
        cashbackSteps.createTechnicalEntities();
        category = new CashbackCategory()
                .withNameRu("Тест")
                .withNameEn("Test")
                .withDescriptionRu("Тестовое описание")
                .withDescriptionEn("Test description")
                .withButtonLink("test/button/link")
                .withButtonTextRu("Текст кнопки")
                .withButtonTextEn("Button text");

        cashbackSteps.createCategory(category);

        program = new CashbackProgram()
                .withId(2L)
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(true)
                .withIsPublic(false)
                .withIsTechnical(false)
                .withIsNew(false)
                .withCategoryId(category.getId())
                .withCategoryNameRu(category.getNameRu());
        cashbackSteps.createProgram(program);
    }

    @After
    public void after() {
        cashbackSteps.clear();
    }

    @Test
    public void validateShow_categoryIsNotSelected() {
        var params = new CashbackProgramsParams()
                .withAction(SHOW);
        var vr = cashbackProgramsTool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateShow_categoryExists() {
        var category = getTechnicalCategory();
        var params = new CashbackProgramsParams()
                .withAction(SHOW)
                .withCategoryKey(getCategoryKey(category.getId(), category.getNameRu()));
        var vr = cashbackProgramsTool.validate(params);

        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateShow_categoryDoesntExist() {
        var params = new CashbackProgramsParams()
                .withAction(SHOW)
                .withCategoryKey("Doesn't exist");
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_success() {
        var vr = cashbackProgramsTool.validate(defaultCreateParams());
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateCreate_categoryIsNull_success() {
        var params = defaultCreateParams()
                .withCategoryKey(null);
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateCreate_categoryDoesntExist() {
        var params = defaultCreateParams()
                .withCategoryKey("this category doesn't exist");
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_percentIsNull() {
        var params = defaultCreateParams().withPercent(null);
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("percent")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_percentInvalid() {
        var params = defaultCreateParams()
                .withPercent("Oh, crap! It's not a decimal number in a string representation!");
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("percent value")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_percentOutOfRange_lowerLimit() {
        var params = defaultCreateParams()
                .withPercent("-0.1");
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("percent value")),
                inInterval(PERCENT_MIN_VALUE, PERCENT_MAX_VALUE));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_percentOutOfRange_upperLimit() {
        var params = defaultCreateParams()
                .withPercent("600.1");
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("percent value")),
                inInterval(PERCENT_MIN_VALUE, PERCENT_MAX_VALUE));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_withTooltip_success() {
        var params = withDefaultTooltipParams(defaultCreateParams());
        var vr = cashbackProgramsTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateCreate_tooltipWithoutLink() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLink(null);
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithEmptyLink() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLink("   ");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkCharacters() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLink("®");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkMaxLength() {
        var params = withDefaultTooltipParams(defaultCreateParams())
                .withTooltipLink("a".repeat(PROGRAM_TOOLTIP_LINK_MAX_LENGTH + 1));
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithoutInfoRu() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoRu(null);
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_ru")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithEmptyInfoRu() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoRu("   ");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_ru")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipInfoRuCharacters() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoRu("®");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_ru")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipInfoRuMaxLength() {
        var params = withDefaultTooltipParams(defaultCreateParams())
                .withTooltipInfoRu("a".repeat(PROGRAM_TOOLTIP_INFO_MAX_LENGTH + 1));
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_ru")),
                maxStringLength(PROGRAM_TOOLTIP_INFO_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithoutInfoEn() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoEn(null);
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_en")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithEmptyInfoEn() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoEn("   ");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_en")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipInfoEnCharacters() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipInfoEn("®");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_en")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipInfoEnMaxLength() {
        var params = withDefaultTooltipParams(defaultCreateParams())
                .withTooltipInfoEn("a".repeat(PROGRAM_TOOLTIP_INFO_MAX_LENGTH + 1));
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_info_en")),
                maxStringLength(PROGRAM_TOOLTIP_INFO_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithoutLinkTextEn() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextEn(null);
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_en")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithEmptyLinkTextEn() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextEn("   ");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_en")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkTextEnCharacters() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextEn("®");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_en")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkTextEnMaxLength() {
        var params = withDefaultTooltipParams(defaultCreateParams())
                .withTooltipLinkTextEn("a".repeat(PROGRAM_TOOLTIP_LINK_TEXT_MAX_LENGTH + 1));
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_en")),
                maxStringLength(PROGRAM_TOOLTIP_LINK_TEXT_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithoutLinkTextRu() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextRu(null);
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_ru")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipWithEmptyLinkTextRu() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextRu("   ");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_ru")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkTextRuCharacters() {
        var params = withDefaultTooltipParams(defaultCreateParams()).withTooltipLinkTextRu("®");
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_ru")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateCreate_tooltipLinkTextRuMaxLength() {
        var params = withDefaultTooltipParams(defaultCreateParams())
                .withTooltipLinkTextRu("a".repeat(PROGRAM_TOOLTIP_LINK_TEXT_MAX_LENGTH + 1));
        var vr = cashbackProgramsTool.validate(params);
        var expected = validationError(path(field("tooltip_link_text_ru")),
                maxStringLength(PROGRAM_TOOLTIP_LINK_TEXT_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateUpdate_success() {
        var params = defaultUpdateParams().withIsPublic(false);
        var vr = cashbackProgramsTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateUpdate_withTooltip_success() {
        var params = withDefaultTooltipParams(defaultUpdateParams()).withIsPublic(false);
        var vr = cashbackProgramsTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateUpdate_programDoesntExist() {
        var unknownProgram = new CashbackProgram()
                .withId(1234L)
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(false)
                .withIsPublic(false);
        var params = defaultUpdateParams()
                .withProgramKey(fromCashbackProgram(unknownProgram).toDisplayString());
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("program")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateUpdate_programIsTechnical() {
        var params = defaultUpdateParams()
                .withProgramKey(fromCashbackProgram(getTechnicalProgram()).toDisplayString());
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("program id")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validateUpdate_programIsPublicAndEnabled() {
        var params = defaultUpdateParams();
        var vr = cashbackProgramsTool.validate(params);

        var expected = validationError(path(field("isEnabled")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void getAllPrograms() {
        var params = new CashbackProgramsParams().withAction(SHOW);
        var result = cashbackProgramsTool.process(params);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(2); // Техническая и тестовая программы

        var programIds = mapList(result.getData(), InternalToolsCashbackProgram::getId);
        assertThat(programIds).contains(getTechnicalProgram().getId(), program.getId());
    }

    @Test
    public void getByCategory() {
        var params = new CashbackProgramsParams()
                .withAction(SHOW)
                .withCategoryKey(getCategoryKey(category.getId(), category.getNameRu()));
        var result = cashbackProgramsTool.process(params);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);

        var extractedProgramId = result.getData().get(0).getId();
        assertThat(extractedProgramId).isEqualTo(program.getId());
    }

    @Test
    public void create() {
        var params = defaultCreateParams();
        var result = cashbackProgramsTool.process(params);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);
    }

    private CashbackProgramsParams defaultCreateParams() {
        var params = new CashbackProgramsParams()
                .withAction(CREATE)
                .withCategoryKey(getCategoryKey(category.getId(), category.getNameRu()))
                .withPercent("12.5")
                .withNameRu("Название программы")
                .withNameEn("Program name")
                .withIsEnabled(true)
                .withIsPublic(true)
                .withIsTechnical(false)
                .withIsNew(false);
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }

    private CashbackProgramsParams defaultUpdateParams() {
        var params = new CashbackProgramsParams()
                .withAction(UPDATE)
                .withProgramKey(fromCashbackProgram(program).toDisplayString())
                .withIsPublic(true)
                .withIsEnabled(true);
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }

    private CashbackProgramsParams withDefaultTooltipParams(CashbackProgramsParams params) {
        return params.withTooltipInfoRu("Текст в всплывающем окне")
                .withTooltipInfoEn("Tooltip info")
                .withTooltipLinkTextRu("Ссылка в всплывающем окне")
                .withTooltipLinkTextEn("Tooltip link text")
                .withTooltipLink("https://direct.yandex.ru/dna/bonus?ulogin=zxczxc");
    }

}
