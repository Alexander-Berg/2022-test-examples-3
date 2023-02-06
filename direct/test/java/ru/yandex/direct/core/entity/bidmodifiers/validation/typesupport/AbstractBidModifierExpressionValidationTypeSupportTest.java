package ru.yandex.direct.core.entity.bidmodifiers.validation.typesupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpression;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierExpressionAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral;
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter;
import ru.yandex.direct.core.entity.bidmodifiers.expression.ParameterInfo;
import ru.yandex.direct.core.entity.bidmodifiers.service.CachingFeaturesProvider;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.qatools.allure.annotations.Description;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierExpression.EXPRESSION_ADJUSTMENTS;
import static ru.yandex.direct.core.entity.bidmodifier.BidModifierExpressionAdjustment.CONDITION;
import static ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator.EQ;
import static ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator.GE;
import static ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator.LE;
import static ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter.CLOUDNESS;
import static ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter.TEMP;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.expressionAdjustmentIsConstantForAnyValues;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.expressionConditionsIntersection;
import static ru.yandex.direct.core.entity.bidmodifiers.validation.BidModifiersDefects.invalidExpressionLiteral;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class AbstractBidModifierExpressionValidationTypeSupportTest {
    // Для тестов подойдёт любой тип из енума
    private static final BidModifierType TYPE = BidModifierType.GEO_MULTIPLIER;
    // и любой согласованный с ним тип кампании
    private static final CampaignType CAMPAIGN_TYPE = CampaignType.TEXT;

    private abstract static class BaseValidationTypeSupport extends AbstractBidModifierExpressionValidationTypeSupport {
        @Override
        public BidModifierType getType() {
            return TYPE;
        }
    }

    private static class BidModifierEx extends BidModifierExpression {
    }

    private static class AdjustmentEx extends BidModifierExpressionAdjustment {
    }

    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);

    @Test
    @Description("Простая корректировка без ошибок (int параметр)")
    public void validateSimpleIntParamNoErrors() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(5))))
                                                .withPercent(10)
                                        )
                                ),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    @Description("Простая корректировка без ошибок (string параметр)")
    public void validateSimpleStringParamNoErrors() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(CLOUDNESS, ParameterInfo.stringParameter(null, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(CLOUDNESS)
                                                                .withOperation(EQ)
                                                                .withValueString("some-str"))))
                                                .withPercent(10)
                                        )
                                ),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    @Description("Используется строковое значение для целочисленного параметра")
    public void validateIntParamStringValueError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueString("5"))))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION), index(0), index(0)),
                        invalidExpressionLiteral())));
    }

    @Test
    @Description("Используется недопустимый оператор")
    public void validateIntParamWrongOperationError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(GE)
                                                                .withValueInteger(5))))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION), index(0), index(0)),
                        invalidExpressionLiteral())));
    }

    @Test
    @Description("Значения параметра выходят за рамка min-max ограничений")
    public void validateIntParamValueOutOfMinMaxError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(asList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(0),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(-11),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(5),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(11)
                                                )))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION), index(0), index(1)),
                        invalidExpressionLiteral())));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION), index(0), index(3)),
                        invalidExpressionLiteral())));
    }

    @Test
    @Description("Выражение всегда принимает значение True, вне зависимости от значений параметров")
    public void validateIntParamAlwaysTrueError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ, LE, GE)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(asList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(0),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(LE)
                                                                .withValueInteger(-1),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(GE)
                                                                .withValueInteger(1)
                                                )))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION)),
                        expressionAdjustmentIsConstantForAnyValues())));
    }

    @Test
    @Description("Выражение всегда принимает значение False, вне зависимости от значений параметров")
    public void validateIntParamAlwaysFalseError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ, LE, GE)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(singletonList(
                                        new AdjustmentEx().withCondition(
                                                asList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(0)
                                                ), singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(1)
                                                )))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS), index(0), field(CONDITION)),
                        expressionAdjustmentIsConstantForAnyValues())));
    }

    @Test
    @Description("Корректировки в наборе пересекаются")
    public void validateIntParamIntersectionError() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ, LE, GE)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(asList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(asList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(0),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(1)
                                                )))
                                                .withPercent(10),
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(GE)
                                                                .withValueInteger(1)
                                                )))
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS)), expressionConditionsIntersection())));
    }

    @Test
    @Description("Корректировки в наборе не пересекаются (два параметра: int и enum)")
    public void validateIntAndEnumParamsNoIntersection() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ, LE, GE)));
                map.put(CLOUDNESS, ParameterInfo.enumParameter(Set.of("no", "little", "many"), Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(asList(
                                        new AdjustmentEx().withCondition(
                                                asList(asList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(0),
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(1)
                                                ), singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(CLOUDNESS)
                                                                .withOperation(EQ)
                                                                .withValueString("little")
                                                )))
                                                .withPercent(10),
                                        new AdjustmentEx().withCondition(
                                                asList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(GE)
                                                                .withValueInteger(1)
                                                        ), singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(CLOUDNESS)
                                                                .withOperation(EQ)
                                                                .withValueString("no")
                                                        )
                                                )
                                        )
                                                .withPercent(10))),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    @Description("Корректировки в наборе пересекаются (два параметра: int и enum)")
    public void validateIntAndStringParamsIntersection() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(TEMP, ParameterInfo.integerParameter(-10, 10, Set.of(EQ)));
                map.put(CLOUDNESS, ParameterInfo.stringParameter(null, Set.of(EQ)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        ValidationResult<BidModifierExpression, Defect> validationResult =
                typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                                .withExpressionAdjustments(asList(
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(TEMP)
                                                                .withOperation(EQ)
                                                                .withValueInteger(5))))
                                                .withPercent(10),
                                        new AdjustmentEx().withCondition(
                                                singletonList(singletonList(
                                                        new BidModifierExpressionLiteral()
                                                                .withParameter(CLOUDNESS)
                                                                .withOperation(EQ)
                                                                .withValueString("many-many"))))
                                                .withPercent(50)
                                        )
                                ),
                        CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
        Assert.assertThat(validationResult, hasDefectWithDefinition(
                validationError(path(field(EXPRESSION_ADJUSTMENTS)), expressionConditionsIntersection())));
    }

    @Test(expected = IllegalStateException.class)
    @Description("Enum-параметр с некорректной операцией GE (ошибочно внесённой в список допустимых)")
    public void validateEnumParamUnsupportedOperation() {
        class ValidationTypeSupport extends BaseValidationTypeSupport {
            @Override
            protected Map<BidModifierExpressionParameter, ParameterInfo> getParametersInfo() {
                Map<BidModifierExpressionParameter, ParameterInfo> map = new HashMap<>();
                map.put(CLOUDNESS, ParameterInfo.enumParameter(Set.of("no", "many"), Set.of(EQ, GE)));
                return map;
            }
        }
        ValidationTypeSupport typeSupport = new ValidationTypeSupport();
        typeSupport.validateAddStep1(new BidModifierEx().withType(TYPE)
                        .withExpressionAdjustments(singletonList(
                                new AdjustmentEx().withCondition(
                                        singletonList(singletonList(
                                                new BidModifierExpressionLiteral()
                                                        .withParameter(CLOUDNESS)
                                                        .withOperation(GE)
                                                        .withValueString("no"))))
                                        .withPercent(10)
                                )
                        ),
                CAMPAIGN_TYPE, null, CLIENT_ID, new CachingFeaturesProvider(null));
    }
}
