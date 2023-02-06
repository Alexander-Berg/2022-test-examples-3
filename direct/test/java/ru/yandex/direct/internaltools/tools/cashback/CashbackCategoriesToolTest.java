package ru.yandex.direct.internaltools.tools.cashback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.cashback.service.CashbackCategoriesService;
import ru.yandex.direct.core.testing.steps.CashbackSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.cashback.model.CashbackCategoriesParams;
import ru.yandex.direct.internaltools.tools.cashback.tool.CashbackCategoriesTool;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.CATEGORY_BUTTON_LINK_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.CATEGORY_BUTTON_TEXT_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.CATEGORY_DESCRIPTION_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConstants.CATEGORY_NAME_MAX_LENGTH;
import static ru.yandex.direct.internaltools.tools.cashback.tool.InternalToolsCashbackConverter.getCategoryKey;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.maxStringLength;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.defect.StringDefects.admissibleChars;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CashbackCategoriesToolTest {

    @Autowired
    private CashbackCategoriesTool cashbackCategoriesTool;

    @Autowired
    private CashbackCategoriesService cashbackCategoriesService;

    @Autowired
    private CashbackSteps steps;

    @Autowired
    private UserSteps userSteps;

    @Test
    public void validate_nameRuNotNull() {
        var params = defaultParams().withNameRu(null);

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameRu")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameEnNotNull() {
        var params = defaultParams().withNameEn(null);

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameEn")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameRuNotBlank() {
        var params = defaultParams().withNameRu("");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameRu")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameEnNotBlank() {
        var params = defaultParams().withNameEn("");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameEn")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameRuMaxLength() {
        var params = defaultParams().withNameRu("a".repeat(CATEGORY_NAME_MAX_LENGTH + 1));

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameRu")),
                maxStringLength(CATEGORY_NAME_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameEnMaxLength() {
        var params = defaultParams().withNameEn("a".repeat(CATEGORY_NAME_MAX_LENGTH + 1));

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameEn")),
                maxStringLength(CATEGORY_NAME_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameRuCharacters() {
        var params = defaultParams().withNameRu("®");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameRu")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_nameEnCharacters() {
        var params = defaultParams().withNameEn("®");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("nameEn")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionRuNotNull() {
        var params = defaultParams().withDescriptionRu(null);

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionRu")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionEnNotNull() {
        var params = defaultParams().withDescriptionEn(null);

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionEn")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionRuNotBlank() {
        var params = defaultParams().withDescriptionRu("");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionRu")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionEnNotBlank() {
        var params = defaultParams().withDescriptionEn("");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionEn")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionRuMaxLength() {
        var params = defaultParams().withDescriptionRu("a".repeat(CATEGORY_DESCRIPTION_MAX_LENGTH + 1));

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionRu")),
                maxStringLength(CATEGORY_DESCRIPTION_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionEnMaxLength() {
        var params = defaultParams().withDescriptionEn("a".repeat(CATEGORY_DESCRIPTION_MAX_LENGTH + 1));

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionEn")),
                maxStringLength(CATEGORY_DESCRIPTION_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionRuCharacters() {
        var params = defaultParams().withDescriptionRu("®");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionRu")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_descriptionEnCharacters() {
        var params = defaultParams().withDescriptionEn("®");

        var vr = cashbackCategoriesTool.validate(params);

        var expected = validationError(path(field("descriptionEn")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonLinkNotNull() {
        var params = defaultParams().withButtonLink(null);
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonLink")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonLinkNotBlank() {
        var params = defaultParams().withButtonLink("   ");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonLink")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonLinkMaxLength() {
        var params = defaultParams().withButtonLink("a".repeat(CATEGORY_BUTTON_LINK_MAX_LENGTH + 1));
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonLink")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonLinkCharacters() {
        var params = defaultParams().withButtonLink("®");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonLink")), invalidValue());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextRuNotNull() {
        var params = defaultParams().withButtonTextRu(null);
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextRu")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextRuNotBlank() {
        var params = defaultParams().withButtonTextRu("   ");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextRu")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextRuMaxLength() {
        var params = defaultParams().withButtonTextRu("a".repeat(CATEGORY_BUTTON_TEXT_MAX_LENGTH + 1));
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextRu")),
                maxStringLength(CATEGORY_BUTTON_TEXT_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextRuCharacters() {
        var params = defaultParams().withButtonTextRu("®");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextRu")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextEnNotNull() {
        var params = defaultParams().withButtonTextEn(null);
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextEn")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextEnNotBlank() {
        var params = defaultParams().withButtonTextEn("   ");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextEn")), notEmptyString());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextEnMaxLength() {
        var params = defaultParams().withButtonTextEn("a".repeat(CATEGORY_BUTTON_TEXT_MAX_LENGTH + 1));
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextEn")),
                maxStringLength(CATEGORY_BUTTON_TEXT_MAX_LENGTH));
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_buttonTextEnCharacters() {
        var params = defaultParams().withButtonTextEn("®");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("buttonTextEn")), admissibleChars());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_campaignWithoutButton() {
        var params = defaultParams().withButtonTextRu(null).withButtonTextEn(null).withButtonLink(null);
        var vr = cashbackCategoriesTool.validate(params);
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_updateWithWrongCategory() {
        var params = defaultParams().withCategoryKey("Incorrect");
        var vr = cashbackCategoriesTool.validate(params);
        var expected = validationError(path(field("category")), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void getAllCategories() {
        var params = new CashbackCategoriesParams()
                .withNameRu("Моя категория")
                .withNameEn("My category")
                .withDescriptionRu("блаблабла")
                .withDescriptionEn("blablabla");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        cashbackCategoriesTool.process(params);

        var result = cashbackCategoriesTool.processWithoutInput();

        assertThat(result).isNotNull();
        assertThat(result.getData()).isNotEmpty();
    }

    @Test
    public void createNewCategory() {
        var params = new CashbackCategoriesParams()
                .withNameRu("Новая категория")
                .withNameEn("New category")
                .withDescriptionRu("блаблабла")
                .withDescriptionEn("blablabla");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        var result = cashbackCategoriesTool.process(params);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(1);

        var createdCategory = result.getData().get(0);
        var dbCategory = steps.getCategory(createdCategory.getId());

        assertThat(dbCategory).isNotNull();
        assertThat(dbCategory.getNameRu()).isEqualTo(createdCategory.getNameRu());
        assertThat(dbCategory.getNameEn()).isEqualTo(createdCategory.getNameEn());
        assertThat(dbCategory.getDescriptionRu()).isEqualTo(createdCategory.getDescriptionRu());
        assertThat(dbCategory.getDescriptionEn()).isEqualTo(createdCategory.getDescriptionEn());
    }

    @Test
    public void updateCategory() {
        var params = new CashbackCategoriesParams()
                .withNameRu("Ещё кагетория")
                .withNameEn("One more category")
                .withDescriptionRu("блаблабла")
                .withDescriptionEn("blablabla");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        cashbackCategoriesTool.process(params);

        var categoryId = cashbackCategoriesService.getCategoryByName(params.getNameRu()).getId();
        var updateParams = new CashbackCategoriesParams()
                .withNameRu(params.getNameRu())
                .withCategoryKey(getCategoryKey(categoryId, params.getNameRu()));
        updateParams.setOperator(operator.getUser());
        var updateResult = cashbackCategoriesTool.process(updateParams);

        assertThat(updateResult).isNotNull();
        assertThat(updateResult.getData()).hasSize(1);

        var updatedCategory = updateResult.getData().get(0);
        var dbCategory = steps.getCategory(updatedCategory.getId());

        assertThat(dbCategory).isNotNull();
        assertThat(dbCategory.getNameRu()).isEqualTo(updatedCategory.getNameRu());
    }

    private CashbackCategoriesParams defaultParams() {
        var params = new CashbackCategoriesParams()
                .withNameRu("Имя")
                .withNameEn("Name")
                .withDescriptionRu("Описание")
                .withDescriptionEn("Description")
                .withButtonLink("button/link")
                .withButtonTextRu("Текст кнопки")
                .withButtonTextEn("Button text");
        var operator = userSteps.createDefaultUser();
        params.setOperator(requireNonNull(operator.getUser()));
        return params;
    }

}
