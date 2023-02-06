package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.ProductRatingDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class NotEmptyProductRatingsRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final ValidationRule RULE = new NotEmptyProductRatingsRule();

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
                    .addProductRating(new ProductRatingDto()),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(2L)
                    .setEnabled(false)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("ВелоШоп CrossDock"))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Почта России")),
                ValidationStatus.WARN,
                "не настроено ни одного продуктового рейтинга"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(3L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setReadableName("ВелоШоп CrossDock"))
                    .setToPartner(
                        new PartnerDto()
                            .setReadableName("Почта России")),
                ValidationStatus.FAILED,
                "не настроено ни одного продуктового рейтинга"
            )
        );
    }
}
