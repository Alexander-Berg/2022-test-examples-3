package ru.yandex.market.logistics.nesu.client.utils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.nesu.client.enums.PickupPointType;
import ru.yandex.market.logistics.nesu.client.enums.ShopPickupPointStatus;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.CreateShopPickupPointMetaResponse;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointAddressDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointMetaRequest;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointPhoneDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointScheduleDayDto;
import ru.yandex.market.logistics.nesu.client.model.shoppickuppoints.ShopPickupPointTariffDto;

@UtilityClass
public class ShopPickupPointFactory {
    @Nonnull
    public ShopPickupPointMetaRequest shopPickupPointMetaRequest() {
        return new ShopPickupPointMetaRequest()
            .setMbiId(1234567890L)
            .setStatus(ShopPickupPointStatus.ACTIVE)
            .setPickupPoint(shopPickupPoint())
            .setTariff(shopPickupPointTariff());
    }

    @Nonnull
    public CreateShopPickupPointMetaResponse createShopPickupPointMetaResponse() {
        var response = new CreateShopPickupPointMetaResponse();
        response.setAlreadyCreated(false)
            .setId(2)
            .setShopId(1)
            .setMbiId(1234567890L)
            .setStatus(ShopPickupPointStatus.ACTIVE)
            .setPickupPoint(shopPickupPoint())
            .setTariff(shopPickupPointTariff());
        return response;
    }

    @Nonnull
    public ShopPickupPointMetaDto shopPickupPointMeta() {
        return new ShopPickupPointMetaDto()
            .setId(2)
            .setShopId(1)
            .setMbiId(1234567890L)
            .setStatus(ShopPickupPointStatus.ACTIVE)
            .setPickupPoint(shopPickupPoint())
            .setTariff(shopPickupPointTariff());
    }

    @Nonnull
    public ShopPickupPointTariffDto shopPickupPointTariff() {
        ShopPickupPointTariffDto shopPickupPointTariff = new ShopPickupPointTariffDto();
        shopPickupPointTariff
            .setTarifficatorId(1000003000L)
            .setOrderBeforeHour(15)
            .setDaysFrom(6)
            .setDaysTo(6);
        return shopPickupPointTariff;
    }

    @Nonnull
    public ShopPickupPointDto shopPickupPoint() {
        ShopPickupPointDto shopPickupPoint = new ShopPickupPointDto();
        shopPickupPoint
            .setLmsId(1000002000L)
            .setExternalId("externalId-2")
            .setName("Тестовая точка externalId-2")
            .setType(PickupPointType.PICKUP_POINT)
            .setOwnerPartnerId(51L)
            .setIsMain(true)
            .setAddress(
                new ShopPickupPointAddressDto()
                    .setLocationId(213)
                    .setLatitude(new BigDecimal("55.725146"))
                    .setLongitude(new BigDecimal("37.64693"))
                    .setPostalCode("109012")
                    .setRegion("Москва")
                    .setLocality("Москва")
                    .setStreet("Красная площадь")
                    .setHouse("1")
            )
            .setSchedule(
                Arrays.stream(DayOfWeek.values())
                    .map(
                        dayOfWeek -> new ShopPickupPointScheduleDayDto()
                            .setDay(dayOfWeek.getValue())
                            .setTimeFrom(LocalTime.of(8, 0))
                            .setTimeTo(LocalTime.of(21, 0))
                    )
                    .collect(Collectors.toList())
            )
            .setWorksOnNationalHolidays(true)
            .setPhones(List.of(new ShopPickupPointPhoneDto().setPhoneNumber("79459998877").setInternalNumber("123")))
            .setStoragePeriod(10L);
        return shopPickupPoint;
    }
}
