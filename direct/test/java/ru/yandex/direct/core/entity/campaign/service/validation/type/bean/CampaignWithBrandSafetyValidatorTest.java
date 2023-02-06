package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.HashSet;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithBrandSafety;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.NumberDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithBrandSafetyValidatorTest {
    private CampaignWithBrandSafetyValidator validator;

    @Before
    public void init() {
        validator = new CampaignWithBrandSafetyValidator(new HashSet<>());
    }

    @Test
    public void testValidBrandSafetyCategoryIds() {
        ValidationResult vr = validator.apply(packCategories(List.of(
                Goal.BRANDSAFETY_LOWER_BOUND + 1,
                Goal.BRANDSAFETY_LOWER_BOUND + 2
        )));
        assertThat(vr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    @Parameters(method = "invalidPhrasesParameters")
    @TestCaseName("[{index}] {2}")
    public void testInvalidBrandSafetyCategoryIds(List<Long> categoryIds, Defect expectedDefect,
                                        @SuppressWarnings("unused")  String description) {
        Path expectedPath = path(field(CampaignWithBrandSafety.BRAND_SAFETY_CATEGORIES), index(1));
        ValidationResult vr = validator.apply(packCategories(categoryIds));
        assertThat(vr).is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
    }

    @SuppressWarnings("unused") // See @Parameters annotations usages here
    private static Object[] invalidPhrasesParameters() {
        return new Object[][]{
                {List.of(Goal.BRANDSAFETY_UPPER_BOUND - 1, Goal.BRANDSAFETY_UPPER_BOUND - 1),
                        CollectionDefects.duplicatedElement(),
                        "не должно быть дублирующихся категорий"},
                {List.of(Goal.BRANDSAFETY_UPPER_BOUND - 1, Goal.BRANDSAFETY_UPPER_BOUND),
                        NumberDefects.inInterval(Goal.BRANDSAFETY_LOWER_BOUND, Goal.BRANDSAFETY_UPPER_BOUND - 1),
                        "все id из диапазона для brandsafety категорий"}
        };
    }

    private static CampaignWithBrandSafety packCategories(List<Long> categoryIds) {
        return new TextCampaign().withBrandSafetyCategories(categoryIds);
    }
}
