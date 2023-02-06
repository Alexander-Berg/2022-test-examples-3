package ru.yandex.market.logistics.management.service.export.dynamic.validation.rule;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.domain.entity.PartnerRelationModel;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.PointType;
import ru.yandex.market.logistics.management.domain.entity.type.ValidationStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.LogisticsPointDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamTypeDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerExternalParamValueDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRelationDto;
import ru.yandex.market.logistics.management.service.export.dynamic.source.dto.PartnerRouteDto;
import ru.yandex.market.logistics.management.service.export.dynamic.validation.ValidationRule;

public class PartnerRouteExistsForPartnerRelationRuleTest extends AbstractValidationRuleTest<PartnerRelationModel> {

    private static final FeatureProperties FEATURE_PROPERTIES = new FeatureProperties();
    private static final ValidationRule VALIDATION_RULE = new PartnerRouteExistsForPartnerRelationRule(
        FEATURE_PROPERTIES
    );

    @AfterEach
    void tearDown() {
        FEATURE_PROPERTIES.setExpressRoutesValidationDisabled(false);
        FEATURE_PROPERTIES.setDropoffRoutesValidationDisabled(false);
    }

    @Override
    ValidationRule getRule() {
        return VALIDATION_RULE;
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
                            .setLocationId(2)
                            .setReadableName("Техномарт CrossDock")
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("DPD")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPartnerRoute(new PartnerRouteDto().setLocationFrom(2))
                    ),
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
                            .setLocationId(5)
                            .setReadableName("Техномарт CrossDock")
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(6L)
                            .setReadableName("DPD")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPartnerRoute(new PartnerRouteDto().setLocationFrom(7))
                    ),
                ValidationStatus.WARN,
                "не настроена магистраль для 'DPD' с регионом отправления 5 (домашний для 'Техномарт CrossDock')"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(7L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(8L)
                            .setReadableName("Техномарт CrossDock")
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(10L)
                            .setReadableName("DPD")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPartnerRoute(new PartnerRouteDto().setLocationFrom(11))
                    ),
                ValidationStatus.FAILED,
                "не настроена магистраль для 'DPD' с регионом отправления null (домашний для 'Техномарт CrossDock')"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Supplier")
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Fulfillment crossdock")
                            .setPartnerType(PartnerType.FULFILLMENT)
                    ),
                ValidationStatus.OK,
                null
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Техномарт CrossDock")
                            .setLocationId(1)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("DPD")
                            .setPartnerType(PartnerType.DELIVERY)
                            .addPartnerRoute(new PartnerRouteDto().setLocationFrom(213))
                    ),
                ValidationStatus.FAILED,
                "не настроена магистраль для 'DPD' с регионом отправления 1 (домашний для 'Техномарт CrossDock')"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Dropship")
                            .setLocationId(1)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(1006360L)
                            .setReadableName("GO Express")
                            .setPartnerType(PartnerType.DELIVERY)
                    ),
                ValidationStatus.FAILED,
                "не настроена магистраль для 'GO Express' с регионом отправления 1 (домашний для 'Dropship')"
            ),
            Arguments.arguments(
                new PartnerRelationDto()
                    .setId(1L)
                    .setEnabled(true)
                    .setFromPartner(
                        new PartnerDto()
                            .setId(1L)
                            .setReadableName("Dropship")
                            .setLocationId(1)
                            .setPartnerType(PartnerType.DROPSHIP)
                    )
                    .setToPartner(
                        new PartnerDto()
                            .setId(2L)
                            .setReadableName("Dropoff")
                            .setPartnerType(PartnerType.DELIVERY)
                    )
                    .setToPartnerLogisticsPoint(
                        new LogisticsPointDto()
                            .setId(20L)
                            .setType(PointType.PICKUP_POINT)
                    ),
                ValidationStatus.FAILED,
                "не настроена магистраль для 'Dropoff' с регионом отправления 1 (домашний для 'Dropship')"
            )
        );
    }

    @Test
    final void testExpressValidationDisabled() {
        FEATURE_PROPERTIES.setExpressRoutesValidationDisabled(true);
        PartnerRelationModel partnerRelationDto = new PartnerRelationDto()
            .setId(1L)
            .setEnabled(true)
            .setFromPartner(
                new PartnerDto()
                    .setId(1L)
                    .setReadableName("Dropship")
            )
            .setToPartner(
                new PartnerDto()
                    .setId(1006360L)
                    .setReadableName("GO Express")
                    .setPartnerType(PartnerType.DELIVERY)
            );
        assertValidationResult(partnerRelationDto, ValidationStatus.OK, null);
    }

    @Test
    final void testDropoffRouteValidationDisabled() {
        FEATURE_PROPERTIES.setDropoffRoutesValidationDisabled(true);
        PartnerRelationModel partnerRelationDto = getDropoffPartnerRelationDto(
            PartnerType.DROPSHIP,
            true
        );
        assertValidationResult(partnerRelationDto, ValidationStatus.OK, null);
    }

    @Test
    final void testDropoffRouteValidationDisabledNotDropofft() {
        FEATURE_PROPERTIES.setDropoffRoutesValidationDisabled(true);
        PartnerRelationModel partnerRelationDto = getDropoffPartnerRelationDto(
            PartnerType.DROPSHIP,
            false
        );
        assertValidationResult(
            partnerRelationDto,
            ValidationStatus.FAILED,
            "не настроена магистраль для 'Dropoff' с регионом отправления 1 (домашний для 'Dropship')"
        );
    }

    @Test
    final void testDropoffRouteValidationDisabledNotDropship() {
        FEATURE_PROPERTIES.setDropoffRoutesValidationDisabled(true);
        PartnerRelationModel partnerRelationDto = getDropoffPartnerRelationDto(
            PartnerType.SUPPLIER,
            true
        );
        assertValidationResult(
            partnerRelationDto,
            ValidationStatus.FAILED,
            "не настроена магистраль для 'Dropoff' с регионом отправления 1 (домашний для 'Dropship')"
        );
    }

    @Nonnull
    private PartnerRelationModel getDropoffPartnerRelationDto(PartnerType fromPartnerType, boolean isDropoff) {
        return new PartnerRelationDto()
            .setId(1L)
            .setEnabled(true)
            .setFromPartner(
                new PartnerDto()
                    .setId(1L)
                    .setReadableName("Dropship")
                    .setPartnerType(fromPartnerType)
                    .setLocationId(1)
            )
            .setToPartner(
                new PartnerDto()
                    .setId(2L)
                    .setReadableName("Dropoff")
                    .setPartnerType(PartnerType.DELIVERY)
                .setExternalParamValues(Set.of(
                    new PartnerExternalParamValueDto(
                        new PartnerExternalParamTypeDto(PartnerExternalParamType.IS_DROPOFF.name()),
                        isDropoff ? "1" : "0"
                    )
                ))
            )
            .setToPartnerLogisticsPoint(
                new LogisticsPointDto()
                    .setId(20L)
                    .setType(PointType.PICKUP_POINT)
            );
    }
}
