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

public class FulfillmentHasLocationIdTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new FulfillmentHasLocationIdRule();

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource("provideArguments")
    final void test(PartnerRelationDto entity, ValidationStatus status, String error) {
        assertValidationResult(entity, status, error);
    }

    public static Stream<? extends Arguments> provideArguments() {
        return Stream.of(
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.FULFILLMENT)
                            .setLocationId(1))
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
                            .setLocationId(1))
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
                            .setLocationId(1))
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
                "не задан домашний регион у фулфилмента 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.SUPPLIER))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.WARN,
                "не задан домашний регион у фулфилмента 'Яндекс.Маркет (Ростов-на-Дону)'"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("Яндекс.Маркет (Ростов-на-Дону)")
                            .setPartnerType(PartnerType.DROPSHIP))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Стриж")),
                ValidationStatus.FAILED,
                "не задан домашний регион у фулфилмента 'Яндекс.Маркет (Ростов-на-Дону)'"
            )
        );
    }
}
