package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicTextAdTarget;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRule;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleKind;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.WebpageRuleType;
import ru.yandex.direct.validation.builder.Validator;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.dynamictextadtarget.service.validation.DynamicTextAdTargetDefects.invalidFormatWebpageCondition;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class AddDynamicTextAdTargetWebpageRuleValidationNegativeTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public DynamicTextAdTarget dynamicTextAdTarget;

    private static Validator<DynamicTextAdTarget, Defect> requestValidator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"правило отбора: Operand = DOMAIN + Operator = EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.DOMAIN))
                        )},
                {"правило отбора: Operand = DOMAIN + Operator = NOT_EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EQUALS)
                                        .withType(WebpageRuleType.DOMAIN))
                        )},
                {"правило отбора: Operand = URL + Operator = EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL))
                        )},
                {"правило отбора: Operand = URL + Operator = NOT_EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EQUALS)
                                        .withType(WebpageRuleType.URL))
                        )},
                {"правило отбора: Operand = TITLE + Operator = EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.TITLE))
                        )},
                {"правило отбора: Operand = TITLE + Operator = NOT_EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EQUALS)
                                        .withType(WebpageRuleType.TITLE))
                        )},
                {"правило отбора: Operand = CONTENT + Operator = EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.CONTENT))
                        )},
                {"правило отбора: Operand = CONTENT + Operator = NOT_EQUALS",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EQUALS)
                                        .withType(WebpageRuleType.CONTENT))
                        )},
                {"правило отбора: Operand = URL_PRODLIST + Operator = EXACT",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.URL_PRODLIST))
                        )},
                {"правило отбора: Operand = URL_PRODLIST + Operator = NOT_EXACT",
                        new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EXACT)
                                        .withType(WebpageRuleType.URL_PRODLIST))
                        )},

        });
    }

    @BeforeClass
    public static void setUp() {
        requestValidator = AddDynamicTextAdTargetValidationService.webpageRulesValidator();
    }

    @Test
    public void test() {
        ValidationResult<DynamicTextAdTarget, Defect> validationResult = requestValidator.apply(dynamicTextAdTarget);

        assertThat(validationResult).is(matchedBy(hasDefectWithDefinition(
                validationError(path(
                        field(DynamicTextAdTarget.CONDITION.name()),
                        index(0),
                        field(WebpageRule.KIND.name())),
                        invalidFormatWebpageCondition(1)))));
    }
}
