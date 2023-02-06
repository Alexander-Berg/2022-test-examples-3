package ru.yandex.market.logistics.lom.service.validation;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.common.util.collections.Quadruple;
import ru.yandex.market.logistics.lom.AbstractTest;
import ru.yandex.market.logistics.lom.converter.lms.PartnerExternalParamLmsConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.embedded.Cost;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichContext;
import ru.yandex.market.logistics.lom.jobs.processor.validation.ValidateAndEnrichResults;
import ru.yandex.market.logistics.lom.jobs.processor.validation.validator.AssessedValueTotalValidator;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DisplayName("Валидация оценочной стоимости")
@ParametersAreNonnullByDefault
class AssessedValueTotalValidatorTest extends AbstractTest {
    private static final long PARTNER_ID = 10L;

    private final AssessedValueTotalValidator assessedValueTotalValidator =
        new AssessedValueTotalValidator(new PartnerExternalParamLmsConverter());

    @DisplayName("Валидация того, что оценочная стоимость не меньше, чем итого с покупателя для некоторых партнеров")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    void validate(String caseName, Order order, ValidateAndEnrichContext context, Boolean validationPassed) {
        ValidateAndEnrichResults result = assessedValueTotalValidator.validateAndEnrich(order, context);
        softly.assertThat(result.isValidationPassed()).isEqualTo(validationPassed);
    }

    static Stream<Arguments> arguments() {
        return Stream.of(
            Quadruple.of(
                "У партнера есть флаг и проверка проходит",
                createOrder(BigDecimal.TEN, BigDecimal.ONE),
                createContextWithFlag(),
                true
            ),
            Quadruple.of(
                "У партнера есть флаг, но проверка не проходит",
                createOrder(BigDecimal.ONE, BigDecimal.TEN),
                createContextWithFlag(),
                false
            ),
            Quadruple.of(
                "У партнера нет флага, проверка проходит независимо от стоимостей заказа",
                createOrder(BigDecimal.TEN, BigDecimal.ONE),
                createContextWithNoFlag(),
                true
            ),
            Quadruple.of(
                "У партнера нет флага, проверка проходит независимо от стоимостей заказа",
                createOrder(BigDecimal.ONE, BigDecimal.TEN),
                createContextWithNoFlag(),
                true
            )
        )
            .map(q -> Arguments.of(q.getFirst(), q.getSecond(), q.getThird(), q.getFourth()));
    }

    @Nonnull
    private static Order createOrder(BigDecimal assessedValue, BigDecimal total) {
        return new Order()
            .setId(1L)
            .setCost(new Cost().setAssessedValue(assessedValue).setTotal(total));
    }

    @Nonnull
    private static ValidateAndEnrichContext createContextWithNoFlag() {
        return new ValidateAndEnrichContext()
            .setPartners(List.of(
                PartnerResponse.newBuilder().id(PARTNER_ID).partnerType(PartnerType.DELIVERY).build()
            ));
    }

    @Nonnull
    private static ValidateAndEnrichContext createContextWithFlag() {
        return new ValidateAndEnrichContext()
            .setPartners(
                List.of(
                    PartnerResponse.newBuilder()
                        .id(PARTNER_ID)
                        .partnerType(PartnerType.DELIVERY)
                        .params(List.of(new PartnerExternalParam(
                            "ASSESSED_VALUE_TOTAL_CHECK",
                            "Assessed value must be no less than total",
                            "1"
                        )))
                        .build())
            );
    }
}
