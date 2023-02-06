package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class WarehouseHasLogisticPointRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new WarehouseHasLogisticPointRule();

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource({
        "arguments1",
        "arguments2",
    })
    void test(PartnerRelationDto entity, ValidationStatus status, String error) {
        assertValidationResult(entity, status, error);
    }

    @Nonnull
    public static Stream<? extends Arguments> arguments1() {
        return Stream.of(
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(300))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.DROPSHIP)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(300))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(300))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(3L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(4L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.DELIVERY))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.FULFILLMENT))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.WARN,
                "не задана активная логистическая точка склада для партнера: 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setStatus(PartnerStatus.ACTIVE)
                            .setReadableName("РЦ Дзержинский")
                            .setPartnerType(PartnerType.DISTRIBUTION_CENTER)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(1))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Яндекс Маркет Самара")),
                ValidationStatus.OK,
                null
            )
        );
    }

    @Nonnull
    public static Stream<? extends Arguments> arguments2() {
        return Stream.of(
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.WARN,
                "не задана активная логистическая точка склада для партнера: 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(300))
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(3000))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.WARN,
                "не задана активная логистическая точка склада для партнера: 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(300))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE)
                                .setLocationId(3000))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.DROPSHIP)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.FAILED,
                "не задана активная логистическая точка склада для партнера: 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.DROPSHIP)
                            .addActiveWarehouse(new LogisticsPointDto()
                                .setType(PointType.WAREHOUSE))
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.FAILED,
                "не задана активная логистическая точка склада для партнера: 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setStatus(PartnerStatus.ACTIVE)
                            .setReadableName("РЦ Дзержинский")
                            .setPartnerType(PartnerType.DISTRIBUTION_CENTER)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Яндекс Маркет Самара")),
                ValidationStatus.FAILED,
                "не задана активная логистическая точка склада для партнера: 'РЦ Дзержинский'"
            )
        );
    }
}
