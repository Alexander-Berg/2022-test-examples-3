package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.LogisticsPoint;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class WarehouseHasOnlyOneActiveLogisticsPointTest extends AbstractValidationRuleTest<Object> {

    private static final WarehouseHasOnlyOneActiveLogisticsPointRule RULE =
        new WarehouseHasOnlyOneActiveLogisticsPointRule();

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource("provideArguments")
    final void testWithoutProvider(Object object, ValidationStatus status, String error) {
        assertValidationResult(object, status, error);
    }

    public static Stream<? extends Arguments> provideArguments() {
        return Stream.of(
            // A wrong type
            Arguments.arguments(
                new Object(),
                ValidationStatus.OK,
                null
            ),

            // Validation works only for a warehouse partner type
            Arguments.arguments(
                new Partner()
                    .setPartnerType(PartnerType.DELIVERY)
                    .addLogisticsPoint(
                        new LogisticsPoint()
                            .setId(1L)
                            .setActive(true)
                    )
                    .addLogisticsPoint(
                        new LogisticsPoint()
                            .setId(2L)
                            .setActive(true)
                    ),
                ValidationStatus.OK,
                null
            ),

            // FAILED
            Arguments.arguments(
                new Partner()
                    .setPartnerType(PartnerType.FULFILLMENT)
                    .addLogisticsPoint(
                        new LogisticsPoint()
                            .setId(1L)
                            .setActive(true)
                    )
                    .addLogisticsPoint(
                        new LogisticsPoint()
                            .setId(2L)
                            .setActive(true)
                    ),
                ValidationStatus.FAILED,
                "у склада может быть только одна активная логистическая точка"
            ),

            // OK
            Arguments.arguments(
                new Partner()
                    .setPartnerType(PartnerType.DELIVERY)
                    .addLogisticsPoint(
                        new LogisticsPoint()
                            .setId(1L)
                            .setActive(true)
                    ),
                ValidationStatus.OK,
                null
            )
        );
    }

}
