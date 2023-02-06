package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.CutoffDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

class NotEmptyCutoffRelationRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new NotEmptyCutoffRelationRule();

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
                            .setReadableName("Маршрут"))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Вестовой"))
                    .addCutoff(new CutoffDto()),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(2L)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("Маршрут"))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Вестовой"))
                    .setEnabled(false),
                ValidationStatus.WARN,
                "не настроено ни одного катофа"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(3L)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("Маршрут"))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Вестовой"))
                    .setEnabled(true),
                ValidationStatus.FAILED,
                "не настроено ни одного катофа"
            )
        );
    }
}
