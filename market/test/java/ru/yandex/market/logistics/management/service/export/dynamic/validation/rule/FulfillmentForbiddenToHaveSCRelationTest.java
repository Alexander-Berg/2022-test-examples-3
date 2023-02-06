package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class FulfillmentForbiddenToHaveSCRelationTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new FulfillmentForbiddenToHaveSortingCenterRelationRule();

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
            // from FF to DD
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
            // from DD to FF
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("СД")
                            .setPartnerType(PartnerType.DELIVERY)
                            .setLocationId(1))
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            // from DD to SC
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("СД")
                            .setPartnerType(PartnerType.DELIVERY)
                            .setLocationId(1))
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER)
                            .setReadableName("Стриж")),
                ValidationStatus.OK,
                null
            ),
            // from FF to SC {Enabled}
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(Boolean.TRUE)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("СД")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setLocationId(1))
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER)
                            .setReadableName("Стриж")),
                ValidationStatus.FAILED,
                "ФФ не может иметь связку с СЦ"
            ),
            // from FF to SC {Disabled}
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(Boolean.FALSE)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("СД")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setLocationId(1))
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER)
                            .setReadableName("Стриж")),
                ValidationStatus.FAILED,
                "ФФ не может иметь связку с СЦ"
            )
        );
    }

}
