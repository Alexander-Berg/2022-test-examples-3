package ru.yandex.market.logistics.management.domain.converter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractTest;
import ru.yandex.market.logistics.management.domain.entity.Partner;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamType;
import ru.yandex.market.logistics.management.domain.entity.PartnerExternalParamValue;
import ru.yandex.market.logistics.management.domain.entity.type.PartnerType;
import ru.yandex.market.logistics.management.domain.entity.type.ShipmentType;
import ru.yandex.market.logistics.management.entity.type.ExtendedShipmentType;

import static ru.yandex.market.logistics.management.domain.entity.type.PartnerType.DROPSHIP;
import static ru.yandex.market.logistics.management.domain.entity.type.PartnerType.OWN_DELIVERY;

@DisplayName("Конвертация типа отгрузки")
public class ShipmentTypeConverterTest extends AbstractTest {
    private final ShipmentTypeConverter converter = new ShipmentTypeConverter(new EnumConverter());

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    void convert(
        @SuppressWarnings("unused") String name,
        boolean isExpress,
        PartnerType type,
        @Nullable ShipmentType before,
        @Nullable ExtendedShipmentType after
    ) {
        softly.assertThat(converter.convertShipmentType(partner(isExpress, type), before)).isEqualTo(after);
    }

    @Nonnull
    private static Stream<Arguments> convert() {
        return Stream.of(
            Arguments.of("Экспресс без типа", true, DROPSHIP, null, ExtendedShipmentType.EXPRESS),
            Arguments.of("Экспресс для tpl", true, DROPSHIP, ShipmentType.TPL, ExtendedShipmentType.EXPRESS),
            Arguments.of("Экспресс для самопривоща", true, DROPSHIP, ShipmentType.IMPORT, ExtendedShipmentType.EXPRESS),
            Arguments.of(
                "Экспресс не для дропшипа с забором",
                true,
                OWN_DELIVERY,
                ShipmentType.WITHDRAW,
                ExtendedShipmentType.WITHDRAW
            ),

            Arguments.of("Экспресс не для дропшипа без типа", true, OWN_DELIVERY, null, null),
            Arguments.of(
                "Экспресс не для дропшипа с tpl",
                true,
                OWN_DELIVERY,
                ShipmentType.TPL,
                ExtendedShipmentType.WITHDRAW
            ),
            Arguments.of(
                "Экспресс не для дропшипа с самопривозом",
                true,
                OWN_DELIVERY,
                ShipmentType.IMPORT,
                ExtendedShipmentType.IMPORT
            ),
            Arguments.of(
                "Экспресс не для дропшипа с забором",
                true,
                DROPSHIP,
                ShipmentType.WITHDRAW,
                ExtendedShipmentType.EXPRESS
            ),

            Arguments.of("Без типа для дропшипа", false, DROPSHIP, null, null),
            Arguments.of("TPL для дропшипа", false, DROPSHIP, ShipmentType.TPL, ExtendedShipmentType.WITHDRAW),
            Arguments.of("Самопривоз для дропшипа", false, DROPSHIP, ShipmentType.IMPORT, ExtendedShipmentType.IMPORT),
            Arguments.of("Забор для дропшипа", false, DROPSHIP, ShipmentType.WITHDRAW, ExtendedShipmentType.WITHDRAW),

            Arguments.of("Без типа не для дропшипа", false, OWN_DELIVERY, null, null),
            Arguments.of("Tpl не для дропшипа", false, OWN_DELIVERY, ShipmentType.TPL, ExtendedShipmentType.WITHDRAW),
            Arguments.of(
                "Самопривоз не для дропшипа",
                false,
                OWN_DELIVERY,
                ShipmentType.IMPORT,
                ExtendedShipmentType.IMPORT
            ),
            Arguments.of(
                "Забор не для дропшипа",
                false,
                OWN_DELIVERY,
                ShipmentType.WITHDRAW,
                ExtendedShipmentType.WITHDRAW
            )
        );
    }

    @Nonnull
    private Partner partner(boolean isExpress, PartnerType partnerType) {
        Partner partner = new Partner().setId(1L).setPartnerType(partnerType);
        PartnerExternalParamValue express = new PartnerExternalParamValue(
            new PartnerExternalParamType().setKey("DROPSHIP_EXPRESS").setId(1L),
            "1"
        );
        return isExpress
            ? partner.addExternalParam(express)
            : partner;
    }
}
