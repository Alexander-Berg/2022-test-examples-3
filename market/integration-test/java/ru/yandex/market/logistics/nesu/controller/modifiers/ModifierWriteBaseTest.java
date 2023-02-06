package ru.yandex.market.logistics.nesu.controller.modifiers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.enums.NumericValueOperationType;
import ru.yandex.market.logistics.nesu.dto.modifier.DeliveryOptionModifierRequestDto;
import ru.yandex.market.logistics.nesu.dto.modifier.ModifierConditionRequestDto;
import ru.yandex.market.logistics.nesu.dto.modifier.NumericValueModificationRuleDto;
import ru.yandex.market.logistics.nesu.dto.modifier.NumericValueRangeDto;
import ru.yandex.market.logistics.nesu.model.ModelFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ValidationErrorDataBuilder;

import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DisplayName("Валидация модификаторов опций доставки")
abstract class ModifierWriteBaseTest extends AbstractContextualTest {
    private static final ErrorType NULL_BOUNDS_ERROR_TYPE = new ErrorType(
        "Range bounds must not be null",
        "ValidRange",
        Map.of("from", "min", "to", "max", "openRange", true)
    );
    private static final ErrorType INVALID_BOUNDS_ERROR_TYPE = new ErrorType(
        "Left bound of range must be less than right bound",
        "ValidRange",
        Map.of("from", "min", "to", "max", "openRange", true)
    );
    private static final ErrorType MINIMAL_CONDITION_ERROR_TYPE = new ErrorType(
        "At least one of conditions: price range, weight range, "
            + "chargeable weight range, item dimension range, delivery type, delivery option tag, "
            + "delivery services, directions must not be null",
        "MinimalModifierCondition"
    );
    private static final ErrorType MINIMAL_MODIFIER_ERROR_TYPE = new ErrorType(
        "At least one of rules: delivery service activation/deactivation, price rule, time rule,"
            + " paid by customer services must not be null",
        "MinimalModifierRule"
    );
    private static final ErrorType DIVISION_BY_ZERO_ERROR_TYPE = new ErrorType(
        "Division by zero error",
        "DivisionByZeroNumericModification"
    );
    private static final ErrorType SUBTRACTION_BELOW_ZERO_ERROR_TYPE = new ErrorType(
        "When modification is subtraction, minimal range must be positive or zero",
        "PositiveOrZeroNumericModification"
    );

    protected static final String PARTNER_VALIDATION_ERROR_MESSAGE = "Партнёры с идентификаторами [100] "
        + "с типами [DELIVERY, OWN_DELIVERY] недоступны для DAAS-магазинов.";

    @DisplayName("Валидация тела запроса")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource({
        "validationCondition",
        "validationRule",
    })
    void modifierRequestValidation(
        List<ValidationErrorDataBuilder> errors,
        Consumer<DeliveryOptionModifierRequestDto> modifierAdjuster
    ) throws Exception {
        DeliveryOptionModifierRequestDto request = minimalValidModifier();
        modifierAdjuster.accept(request);

        createOrUpdateModifier(request)
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                errors.stream()
                    .map(b -> b.forObject("deliveryOptionModifierRequestDto"))
                    .collect(Collectors.toList())
            ));
    }

    @Nonnull
    private static Stream<Arguments> validationCondition() {
        return Stream.<Pair<List<ValidationErrorDataBuilder>, Consumer<DeliveryOptionModifierRequestDto>>>of(
            Pair.of(
                List.of(fieldErrorBuilder("condition", MINIMAL_CONDITION_ERROR_TYPE)),
                rq -> rq.getCondition().setDestinations(null)
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.priceRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setPriceRange(createRange(null, -1L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.priceRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setPriceRange(createRange(-1L, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.priceRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.priceRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setPriceRange(createRange(null, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.priceRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.priceRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setPriceRange(createRange(1L, 0L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.pricePercent", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setPricePercent(BigDecimal.ONE.negate())
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.pricePercent", ErrorType.max(100))),
                rq -> rq.getCondition().setPricePercent(BigDecimal.valueOf(101))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.weightRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setWeightRange(createRange(null, -1L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.weightRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setWeightRange(createRange(-1L, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.weightRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.weightRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setWeightRange(createRange(null, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.weightRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.weightRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setWeightRange(createRange(1L, 0L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.chargeableWeightRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setChargeableWeightRange(createRange(null, -1L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.chargeableWeightRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setChargeableWeightRange(createRange(-1L, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.chargeableWeightRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.chargeableWeightRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setChargeableWeightRange(createRange(null, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.chargeableWeightRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.chargeableWeightRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setChargeableWeightRange(createRange(1L, 0L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.itemDimensionRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setItemDimensionRange(createRange(null, -1L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.itemDimensionRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.getCondition().setItemDimensionRange(createRange(-1L, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.itemDimensionRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.itemDimensionRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setItemDimensionRange(createRange(null, null))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("condition.itemDimensionRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("condition.itemDimensionRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.getCondition().setItemDimensionRange(createRange(1L, 0L))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.deliveryServiceIds", ErrorType.NOT_NULL_ELEMENTS)),
                rq -> rq.getCondition().setDeliveryServiceIds(Collections.singleton(null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("condition.destinations", ErrorType.NOT_NULL_ELEMENTS)),
                rq -> rq.getCondition().setDestinations(Collections.singleton(null))
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Nonnull
    private static Stream<Arguments> validationRule() {
        return Stream.<Pair<List<ValidationErrorDataBuilder>, Consumer<DeliveryOptionModifierRequestDto>>>of(
            Pair.of(
                List.of(objectErrorBuilder(MINIMAL_MODIFIER_ERROR_TYPE)),
                rq -> rq.setIsDeliveryServiceEnabled(null)
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule", DIVISION_BY_ZERO_ERROR_TYPE)),
                rq -> rq.setCostRule(createRule(NumericValueOperationType.DIVIDE, BigDecimal.ZERO, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule", SUBTRACTION_BELOW_ZERO_ERROR_TYPE)),
                rq -> rq.setCostRule(createRule(
                    NumericValueOperationType.SUBTRACT,
                    BigDecimal.TEN,
                    createRange(null, 10L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule.type", ErrorType.NOT_NULL)),
                rq -> rq.setCostRule(createRule(null, BigDecimal.ZERO, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule.value", ErrorType.NOT_NULL)),
                rq -> rq.setCostRule(createRule(NumericValueOperationType.ADD, null, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule.value", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setCostRule(createRule(NumericValueOperationType.DIVIDE, BigDecimal.ONE.negate(), null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule.resultRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setCostRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ZERO,
                    createRange(null, -1L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("costRule.resultRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setCostRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ZERO,
                    createRange(-1L, null)
                ))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("costRule.resultRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("costRule.resultRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.setCostRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ONE,
                    createRange(null, null)
                ))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("costRule.resultRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("costRule.resultRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.setCostRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ONE,
                    createRange(1L, 0L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule", DIVISION_BY_ZERO_ERROR_TYPE)),
                rq -> rq.setTimeRule(createRule(NumericValueOperationType.DIVIDE, BigDecimal.ZERO, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule", SUBTRACTION_BELOW_ZERO_ERROR_TYPE)),
                rq -> rq.setTimeRule(createRule(
                    NumericValueOperationType.SUBTRACT,
                    BigDecimal.TEN,
                    createRange(null, 10L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule.type", ErrorType.NOT_NULL)),
                rq -> rq.setTimeRule(createRule(null, BigDecimal.ZERO, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule.value", ErrorType.NOT_NULL)),
                rq -> rq.setTimeRule(createRule(NumericValueOperationType.ADD, null, null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule.value", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setTimeRule(createRule(NumericValueOperationType.DIVIDE, BigDecimal.ONE.negate(), null))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule.resultRange.max", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setTimeRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ZERO,
                    createRange(null, -1L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("timeRule.resultRange.min", ErrorType.POSITIVE_OR_ZERO)),
                rq -> rq.setTimeRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ZERO,
                    createRange(-1L, null)
                ))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("timeRule.resultRange.max", NULL_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("timeRule.resultRange.min", NULL_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.setTimeRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ONE,
                    createRange(null, null)
                ))
            ),
            Pair.of(
                List.of(
                    fieldErrorBuilder("timeRule.resultRange.max", INVALID_BOUNDS_ERROR_TYPE),
                    fieldErrorBuilder("timeRule.resultRange.min", INVALID_BOUNDS_ERROR_TYPE)
                ),
                rq -> rq.setTimeRule(createRule(
                    NumericValueOperationType.ADD,
                    BigDecimal.ONE,
                    createRange(1L, 0L)
                ))
            ),
            Pair.of(
                List.of(fieldErrorBuilder("paidByCustomerServices", ErrorType.NOT_NULL_ELEMENTS)),
                rq -> rq.setPaidByCustomerServices(Collections.singleton(null))
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }

    @Nonnull
    protected abstract ResultActions createOrUpdateModifier(DeliveryOptionModifierRequestDto request) throws Exception;

    @Nonnull
    private static DeliveryOptionModifierRequestDto minimalValidModifier() {
        DeliveryOptionModifierRequestDto modifierRequest = new DeliveryOptionModifierRequestDto();
        modifierRequest.setCondition(new ModifierConditionRequestDto().setDestinations(Set.of(1)))
            .setIsDeliveryServiceEnabled(false);
        return modifierRequest;
    }

    @Nonnull
    protected static NumericValueModificationRuleDto createRule(
        @Nullable NumericValueOperationType type,
        @Nullable BigDecimal value,
        @Nullable NumericValueRangeDto range
    ) {
        return new NumericValueModificationRuleDto()
            .setType(type)
            .setValue(value)
            .setResultRange(range);
    }

    @Nonnull
    protected static NumericValueRangeDto createRange(@Nullable Long min, @Nullable Long max) {
        return new NumericValueRangeDto()
            .setMin(Optional.ofNullable(min).map(BigDecimal::valueOf).orElse(null))
            .setMax(Optional.ofNullable(max).map(BigDecimal::valueOf).orElse(null));
    }

    @Nonnull
    protected static DeliveryOptionModifierRequestDto defaultModifier() {
        DeliveryOptionModifierRequestDto request = new DeliveryOptionModifierRequestDto();
        request.setActive(true).setCostRule(ModelFactory.createRuleDto());
        return request;
    }
}
