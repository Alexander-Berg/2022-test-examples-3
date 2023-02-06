package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.PlatformClient;
import ru.yandex.market.logistics.management.domain.entity.PlatformClientPartner;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class FulfillmentHasTheSamePlatformTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new FulfillmentHasTheSamePlatformRule();

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource("provideArguments")
    final void test(PartnerRelationModel entity, ValidationStatus status, String error) {
        assertValidationResult(entity, status, error);
    }

    public static Stream<? extends Arguments> provideArguments() {
        return Stream.of(
            // do not verify DTO representation
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Склад")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setLocationId(1))
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DELIVERY)
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelation()
                    .setToPartner(
                        new Partner()
                            .setPartnerType(PartnerType.DELIVERY)),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setEnabled(Boolean.FALSE)
                    .setFromPartner(
                        new Partner()
                            .setId(1L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(1L)
                                    )
                            )
                    )
                    .setToPartner(
                        new Partner()
                            .setId(2L)
                            .setReadableName("Второй")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(2L)
                                    )
                            )
                    ),
                ValidationStatus.WARN,
                "партнеры 1 и 2 не имеют общей платформы"
            ),
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setEnabled(Boolean.TRUE)
                    .setFromPartner(
                        new Partner()
                            .setId(1L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(1L)
                                    )
                            )
                    )
                    .setToPartner(
                        new Partner()
                            .setId(2L)
                            .setReadableName("Второй")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(2L)
                                    )
                            )
                    ),
                ValidationStatus.FAILED,
                "партнеры 1 и 2 не имеют общей платформы"
            ),
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setEnabled(Boolean.TRUE)
                    .setFromPartner(
                        new Partner()
                            .setId(1L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(2L)
                                    )
                            )
                    )
                    .setToPartner(
                        new Partner()
                            .setId(2L)
                            .setReadableName("Второй")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .addPlatformClient(
                                new PlatformClientPartner()
                                    .setStatus(PartnerStatus.ACTIVE)
                                    .setPlatformClient(
                                        new PlatformClient()
                                            .setId(2L)
                                    )
                            )
                    ),
                ValidationStatus.OK,
                null
            )
        );
    }

}
