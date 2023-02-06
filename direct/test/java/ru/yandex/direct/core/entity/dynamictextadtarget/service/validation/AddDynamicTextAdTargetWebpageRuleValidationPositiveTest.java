package ru.yandex.direct.core.entity.dynamictextadtarget.service.validation;

import java.util.Collection;
import java.util.List;

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

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class AddDynamicTextAdTargetWebpageRuleValidationPositiveTest {

    @Parameterized.Parameter
    public String description;

    @Parameterized.Parameter(1)
    public List<DynamicTextAdTarget> request;

    private static Validator<DynamicTextAdTarget, Defect> requestValidator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"правило отбора: Operand = DOMAIN + Operator = EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.DOMAIN))
                        ))},
                {"правило отбора: Operand = DOMAIN + Operator = NOT_EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EXACT)
                                        .withType(WebpageRuleType.DOMAIN))
                        ))},
                {"правило отбора: Operand = URL + Operator = EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.URL))
                        ))},
                {"правило отбора: Operand = URL + Operator = NOT_EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EXACT)
                                        .withType(WebpageRuleType.URL))
                        ))},
                {"правило отбора: Operand = TITLE + Operator = EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.TITLE))
                        ))},
                {"правило отбора: Operand = TITLE + Operator = NOT_EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EXACT)
                                        .withType(WebpageRuleType.TITLE))
                        ))},
                {"правило отбора: Operand = CONTENT + Operator = EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EXACT)
                                        .withType(WebpageRuleType.CONTENT))
                        ))},
                {"правило отбора: Operand = CONTENT + Operator = NOT_EXACT",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EXACT)
                                        .withType(WebpageRuleType.CONTENT))
                        ))},
                {"правило отбора: Operand = URL_PRODLIST + Operator = EQUALS",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST))
                        ))},
                {"правило отбора: Operand = URL_PRODLIST + Operator = NOT_EQUALS",
                        singletonList(new DynamicTextAdTarget().withCondition(
                                singletonList(new WebpageRule().withKind(WebpageRuleKind.NOT_EQUALS)
                                        .withType(WebpageRuleType.URL_PRODLIST))
                        ))},

        });
    }

    @BeforeClass
    public static void setUp() {
        requestValidator = AddDynamicTextAdTargetValidationService.webpageRulesValidator();
    }

    @Test
    public void test() {
        ValidationResult<DynamicTextAdTarget, Defect> validationResult = requestValidator.apply(request.get(0));

        assertThat(validationResult.hasAnyErrors()).isEqualTo(false);
    }
}
