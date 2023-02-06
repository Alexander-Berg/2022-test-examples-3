package ru.yandex.market.logistics.lrm.les;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.logistics.les.ff.FulfilmentBoxItemsReceivedEvent;
import ru.yandex.market.logistics.les.ff.dto.BoxItemDto;
import ru.yandex.market.logistics.les.ff.enums.UnitCountType;
import ru.yandex.market.logistics.lrm.LrmTest;
import ru.yandex.market.logistics.lrm.converter.EnumConverter;
import ru.yandex.market.logistics.lrm.converter.les.FulfilmentBoxItemsReceivedConverter;
import ru.yandex.market.logistics.lrm.service.meta.model.FulfilmentReceivedBoxMeta;

@ParametersAreNonnullByDefault
@DisplayName("Конвертация события LES в модель LRM")
class FulfilmentBoxItemsReceivedConverterTest extends LrmTest {

    private static final Instant FIXED_TIME = Instant.parse("2021-11-11T11:11:11.00Z");
    private static final List<String> ATTRIBUTES = List.of("attr-1", "attr-2");
    private static final Map<String, String> INSTANCES = Map.of("UIT", "123", "CIS", "345");
    private static final long ZERO_LONG = 0L;

    private final EnumConverter enumConverter = new EnumConverter();
    private final FulfilmentBoxItemsReceivedConverter converter = new FulfilmentBoxItemsReceivedConverter(
        enumConverter
    );

    @ParameterizedTest
    @EnumSource(UnitCountType.class)
    @DisplayName("Конвертация UnitCountType: LES -> LRM")
    void convertUnitCountType(UnitCountType unitCountType) {
        softly.assertThat(enumConverter.convert(unitCountType, FulfilmentReceivedBoxMeta.UnitCountType.class))
            .isNotNull();
    }

    @ParameterizedTest
    @EnumSource(FulfilmentReceivedBoxMeta.UnitCountType.class)
    @DisplayName("Конвертация UnitCountType: LRM -> LES")
    void convertUnitCountType(FulfilmentReceivedBoxMeta.UnitCountType unitCountType) {
        softly.assertThat(enumConverter.convert(unitCountType, UnitCountType.class))
            .isNotNull();
    }

    @Test
    @DisplayName("Конвертация события LES со всеми не заполненными полями")
    void convertEmptyEvent() {
        softly.assertThat(converter.toMeta(new FulfilmentBoxItemsReceivedEvent()))
            .usingRecursiveComparison()
            .isEqualTo(
                FulfilmentReceivedBoxMeta.builder()
                    .boxExternalId(StringUtils.EMPTY)
                    .warehousePartnerId(ZERO_LONG)
                    .timestamp(Instant.MIN)
                    .ffRequestId(ZERO_LONG)
                    .deliveryServicePartnerId(ZERO_LONG)
                    .items(List.of())
                    .build()
            );
    }

    @Test
    @DisplayName("Конвертация события о вторичной приёмке")
    void convertEvent() {
        softly.assertThat(converter.toMeta(lesEvent()))
            .usingRecursiveComparison()
            .isEqualTo(expectedMeta());
    }

    @Nonnull
    private FulfilmentReceivedBoxMeta expectedMeta() {
        return FulfilmentReceivedBoxMeta.builder()
            .boxExternalId("some-box-id")
            .orderExternalId("some-order-id")
            .ffRequestId(123L)
            .timestamp(FIXED_TIME)
            .warehousePartnerId(111L)
            .deliveryServicePartnerId(222L)
            .items(expectedItems())
            .build();
    }

    @Nonnull
    private List<FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta> expectedItems() {
        return List.of(
            FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta.builder()
                .supplierId(1L)
                .vendorCode("vendor-code")
                .stock(FulfilmentReceivedBoxMeta.UnitCountType.DEFECT)
                .attributes(ATTRIBUTES)
                .instances(INSTANCES)
                .build(),
            FulfilmentReceivedBoxMeta.ReceivedBoxItemMeta.builder()
                .supplierId(ZERO_LONG)
                .vendorCode(StringUtils.EMPTY)
                .stock(FulfilmentReceivedBoxMeta.UnitCountType.UNKNOWN)
                .attributes(List.of())
                .instances(Map.of())
                .build()
        );
    }

    @Nonnull
    private FulfilmentBoxItemsReceivedEvent lesEvent() {
        return new FulfilmentBoxItemsReceivedEvent(
            "some-box-id",
            "some-order-id",
            123L,
            FIXED_TIME,
            111L,
            222L,
            eventItems()
        );
    }

    @Nonnull
    private List<BoxItemDto> eventItems() {
        return List.of(
            new BoxItemDto(
                1L,
                "vendor-code",
                UnitCountType.DEFECT,
                ATTRIBUTES,
                INSTANCES
            ),
            new BoxItemDto()
        );
    }
}
