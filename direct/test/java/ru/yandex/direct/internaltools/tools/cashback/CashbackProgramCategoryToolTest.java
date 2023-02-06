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
import ru.yandex.direct.core.entity.cashback.model.CashbackProgramCategory;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.cashback.model.CashbackProgramCategoryParams;
import ru.yandex.direct.internaltools.tools.cashback.tool.CashbackProgramCategoryTool;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConverter.getCategoryKey;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.isNull;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CashbackProgramCategoryToolTest {
    @Autowired
    private CashbackProgramCategoryTool cashbackProgramCategoryTool;

    @Autowired
    private CashbackSteps cashbackSteps;

    @Autowired
    private UserSteps userSteps;

    private String categoryKey;

    @Before
    public void before() {
        cashbackSteps.createTechnicalEntities();
        var category = new CashbackCategory()
                .withNameRu("Тест")
                .withNameEn("Test")
                .withDescriptionRu("Тестовое описание")
                .withDescriptionEn("Test description")
                .withButtonLink("test/button/link")
                .withButtonTextRu("Текст кнопки")
                .withButtonTextEn("Button text");

        cashbackSteps.createCategory(category);
        categoryKey = getCategoryKey(category.getId(), category.getNameRu());

        var program1 = new CashbackProgram()
                .withId(2L)
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(true)
                .withIsPublic(false)
                .withIsTechnical(false)
                .withIsNew(false);
        cashbackSteps.createProgram(program1);
        var program2 = new CashbackProgram()
                .withId(3L)
                .withPercent(BigDecimal.ONE)
                .withIsEnabled(true)
                .withIsPublic(false)
                .withIsTechnical(false)
                .withIsNew(false);
        cashbackSteps.createProgram(program2);

        var link = new CashbackProgramCategory()
                .withId(1L)
                .withCategoryId(2L)
                .withProgramId(3L)
                .withOrder(1L);
        cashbackSteps.createProgramCategoryLink(link);
    }

    @After
    public void after() {
        cashbackSteps.clear();
    }

    @Test
    public void testValidateShowWithoutParams() {
        var params = new CashbackProgramCategoryParams().withAction(CashbackProgramCategoryParams.Action.SHOW);
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateShowWithCategory() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.SHOW)
                .withCategoryKey(categoryKey);
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateShowWithProgram() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.SHOW)
                .withProgramKey("2 test");
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateShowWithCategoryAndProgram() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.SHOW)
                .withProgramKey("2 test")
                .withCategoryKey(categoryKey);
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateShowWithIncorrectCategory() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.SHOW)
                .withCategoryKey("Не существует");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateShowWithIncorrectProgram() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.SHOW)
                .withProgramKey("5 no test");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithCorrectParams() {
        var params = defaultCreateParams();
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateCreateWithNullCategory() {
        var params = defaultCreateParams().withCategoryKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithNullProgram() {
        var params = defaultCreateParams().withProgramKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithNullOrder() {
        var params = defaultCreateParams().withOrder(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("order")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithIncorrectCategory() {
        var params = defaultCreateParams().withCategoryKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithIncorrectProgram() {
        var params = defaultCreateParams().withProgramKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateWithIncorrectOrder() {
        var params = defaultCreateParams().withOrder("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("order")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateCreateExistingLink() {
        var params = defaultCreateParams().
                withCategoryKey(categoryKey)
                .withProgramKey("3 test");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithCorrectParams() {
        var params = defaultUpdateParams();
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateUpdateWithNullCategory() {
        var params = defaultUpdateParams().withCategoryKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithNullProgram() {
        var params = defaultUpdateParams().withProgramKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithNullOrder() {
        var params = defaultUpdateParams().withOrder(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("order")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithIncorrectCategory() {
        var params = defaultUpdateParams().withCategoryKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithIncorrectProgram() {
        var params = defaultUpdateParams().withProgramKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateWithIncorrectOrder() {
        var params = defaultUpdateParams().withOrder("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("order")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateUpdateNonExistingLink() {
        var params = defaultUpdateParams().withProgramKey("1 test");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteWithCorrectParams() {
        var params = defaultDeleteParams();
        var vr = cashbackProgramCategoryTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void testValidateDeleteWithNullCategory() {
        var params = defaultDeleteParams().withCategoryKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteWithNullProgram() {
        var params = defaultDeleteParams().withProgramKey(null);
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteWithNonNullOrder() {
        var params = defaultDeleteParams().withOrder("1");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("order")), isNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteWithIncorrectCategory() {
        var params = defaultDeleteParams().withCategoryKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteWithIncorrectProgram() {
        var params = defaultDeleteParams().withProgramKey("incorrect");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("program")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void testValidateDeleteNonExistingLink() {
        var params = defaultDeleteParams().withProgramKey("2 test");
        var vr = cashbackProgramCategoryTool.validate(params);
        var expected = validationError(path(field("category")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    private CashbackProgramCategoryParams defaultCreateParams() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.CREATE)
                .withProgramKey("2 test")
                .withCategoryKey(categoryKey)
                .withOrder("1");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }

    private CashbackProgramCategoryParams defaultUpdateParams() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.UPDATE)
                .withProgramKey("3 test")
                .withCategoryKey(categoryKey)
                .withOrder("1");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }

    private CashbackProgramCategoryParams defaultDeleteParams() {
        var params = new CashbackProgramCategoryParams()
                .withAction(CashbackProgramCategoryParams.Action.DELETE)
                .withProgramKey("3 test")
                .withCategoryKey(categoryKey);
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }
}
