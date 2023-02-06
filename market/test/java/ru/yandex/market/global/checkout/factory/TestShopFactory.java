package ru.yandex.market.global.checkout.factory;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.mj.generated.server.model.AddressDto;
import ru.yandex.mj.generated.server.model.GeoPointDto;
import ru.yandex.mj.generated.server.model.GeoPolygonDto;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;
import ru.yandex.mj.generated.server.model.ShopExportDto;

@RequiredArgsConstructor
public class TestShopFactory {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestShopFactory.class).build();

    public static final double IN_DELIVERY_AREA_LAT = 32.05123863466847;
    public static final double IN_DELIVERY_AREA_LON = 34.79659455566249;

    private static final double SHOP_LAT = 32.013803675009164;
    private static final double SHOP_LON = 34.78775399475068;

    private static final GeoPolygonDto DELIVERY_AREA = new GeoPolygonDto().points(List.of(
            new GeoPointDto()
                    .lat(32.11693175006755)
                    .lon(34.78329079895013),
            new GeoPointDto()
                    .lat(32.01694821981134)
                    .lon(34.74003213195793),
            new GeoPointDto()
                    .lat(31.998518168317897)
                    .lon(34.80183022766106),
            new GeoPointDto()
                    .lat(32.10144445389724)
                    .lon(34.84955209045404),
            new GeoPointDto()
                    .lat(32.11693175006755)
                    .lon(34.78329079895013)
    ));

    private final Clock clock;

    public ShopExportDto buildShopDto() {
        return buildShopDto(CreateShopDtoBuilder.builder().build());
    }

    public ShopExportDto buildShopDto(CreateShopDtoBuilder builder) {
        OffsetDateTime now = OffsetDateTime.now(clock);

        String today = now.getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String yesterday = now.minus(1, ChronoUnit.DAYS).getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        String tomorrow = now.plus(1, ChronoUnit.DAYS).getDayOfWeek()
                .getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();

        ShopExportDto shop = builder.setupShop.apply(RANDOM.nextObject(ShopExportDto.class)
                .enabled(true)
                .hidden(false)
                .deliveryArea(DELIVERY_AREA)
                .orderProcessingMinutes(30)
                .schedule(List.of(
                        new ScheduleItemDto()
                                .day(yesterday)
                                .open(true)
                                .startAt("00:00:00")
                                .endAt("23:59:59"),
                        new ScheduleItemDto()
                                .day(today)
                                .open(true)
                                .startAt("00:00:00")
                                .endAt("23:59:59"),
                        new ScheduleItemDto()
                                .day(tomorrow)
                                .open(true)
                                .startAt("00:00:00")
                                .endAt("23:59:59")
                ))
        );

        AddressDto addressDto = builder.setupAddress.apply(RANDOM.nextObject(AddressDto.class)
                .coordinates(new GeoPointDto()
                        .lat(SHOP_LAT)
                        .lon(SHOP_LON)
                )
        );

        shop.setAddress(addressDto);
        return shop;
    }

    @Data
    @Builder
    public static class CreateShopDtoBuilder {
        @Builder.Default
        private Function<ShopExportDto, ShopExportDto> setupShop = Function.identity();

        @Builder.Default
        private Function<AddressDto, AddressDto> setupAddress = Function.identity();
    }
}
