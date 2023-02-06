package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelation;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.service.client.PartnerRelationService;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;

public class DropshipHasOnlyOneRelationTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final PartnerRelationService RELATION_SERVICE = Mockito.mock(PartnerRelationService.class);
    private static final ValidationRule RULE = new DropshipHasOnlyOneRelationRule(
        RELATION_SERVICE
    );

    @BeforeAll
    public static void setUp() {
        given(RELATION_SERVICE.findActiveByPartnerId(anyLong())).willReturn(List.of());

        given(RELATION_SERVICE.findActiveByPartnerId(666L)).willReturn(List.of(
            new PartnerRelation()
        ));
        given(RELATION_SERVICE.findActiveByPartnerId(999L)).willReturn(List.of(
            new PartnerRelation(),
            new PartnerRelation()
        ));
    }

    @Override
    ValidationRule getRule() {
        return RULE;
    }

    @ParameterizedTest(name = "{index} : {1}.")
    @MethodSource("provideArguments")
    final void testWithoutProvider(PartnerRelationModel entity, ValidationStatus status, String error) {
        assertValidationResult(entity, status, error);
    }

    public static Stream<? extends Arguments> provideArguments() {
        return Stream.of(
            // Not a domain representation
            Arguments.arguments(
                new PartnerRelationDto(),
                ValidationStatus.OK,
                null
            ),

            // Not DROPSHIP
            Arguments.arguments(
                new PartnerRelation()
                    .setFromPartner(
                        new Partner()
                            .setId(999L)
                            .setPartnerType(PartnerType.DELIVERY)),
                ValidationStatus.OK,
                null
            ),

            // DROPSHIP without active relations
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setFromPartner(
                        new Partner()
                            .setId(0L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DROPSHIP)
                    ),
                ValidationStatus.OK,
                null
            ),

            // DROPSHIP with 1 active relations
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setFromPartner(
                        new Partner()
                            .setId(666L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DROPSHIP)
                    ),
                ValidationStatus.OK,
                null
            ),

            // WARN: DROPSHIP with 2 active relations
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setEnabled(Boolean.FALSE)
                    .setFromPartner(
                        new Partner()
                            .setId(999L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DROPSHIP)
                    ),
                ValidationStatus.WARN,
                "Dropship не может иметь больше одной активной связки"
            ),

            // FAILED: DROPSHIP with 2 active relations
            Arguments.arguments(
                new PartnerRelation()
                    .setId(1L)
                    .setEnabled(Boolean.TRUE)
                    .setFromPartner(
                        new Partner()
                            .setId(999L)
                            .setReadableName("Первый")
                            .setPartnerType(PartnerType.DROPSHIP)
                    ),
                ValidationStatus.FAILED,
                "Dropship не может иметь больше одной активной связки"
            )
        );
    }

}
