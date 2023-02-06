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
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRouteDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class NotEmptyPartnerRoutesRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {
    private static final ValidationRule RULE = new NotEmptyPartnerRoutesRule();

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
                    .setToPartner(
                        new PartnerDto()
                            .addPartnerRoute(new PartnerRouteDto())
                            .setPartnerType(PartnerType.DELIVERY)
                    ),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(2L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("Илиор Латвия"))
                    .setToPartner(
                        new PartnerDto()
                            .setId(3L)
                            .setReadableName("Везу")
                            .setPartnerType(PartnerType.DELIVERY)
                    ),
                ValidationStatus.WARN,
                "не настроено ни одного календаря магистрали"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(4L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("Илиор Латвия")
                            .addPartnerRoute(new PartnerRouteDto()))
                    .setToPartner(
                        new PartnerDto()
                            .setId(5L)
                            .setReadableName("Везу")
                            .setPartnerType(PartnerType.DELIVERY)
                    ),
                ValidationStatus.FAILED,
                "не настроено ни одного календаря магистрали"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Supplier")
                            .addPartnerRoute(new PartnerRouteDto()))
                    .setToPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Crossdock FF")
                            .setPartnerType(PartnerType.FULFILLMENT)
                    ),
                ValidationStatus.OK,
                null
            )
        );
    }
}
