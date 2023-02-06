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


class NotEmptyShipmentTypeRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new NotEmptyShipmentTypeRule();

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource("provideArguments")
    final void test(PartnerRelationDto entity, ValidationStatus status, String error) {
        assertValidationResult(entity, status, error);
    }

    private static Stream<? extends Arguments> provideArguments() {
        return Stream.of(
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setFromPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.FULFILLMENT)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DELIVERY)),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(2L)
                    .setFromPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DROPSHIP)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DELIVERY)),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(3L)
                    .setFromPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.FULFILLMENT)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER)),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(4L)
                    .setFromPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DROPSHIP)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER))
                    .setEnabled(false),
                ValidationStatus.WARN,
                NotEmptyShipmentTypeRule.WARN_MESSAGE
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(5L)
                    .setFromPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.DROPSHIP)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setPartnerType(PartnerType.SORTING_CENTER))
                    .setEnabled(true),
                ValidationStatus.FAILED,
                NotEmptyShipmentTypeRule.WARN_MESSAGE
            )
        );
    }
}
