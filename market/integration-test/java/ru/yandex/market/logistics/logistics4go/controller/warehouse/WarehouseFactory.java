package ru.yandex.market.logistics.logistics4go.controller.warehouse;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import lombok.AllArgsConstructor;

import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.logistics4go.client.model.ScheduleDayDto;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseAddressDto;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseContactDto;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseDto;
import ru.yandex.market.logistics.logistics4go.client.model.WarehouseResponse;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;

@AllArgsConstructor
@ParametersAreNonnullByDefault
public class WarehouseFactory {
    private String warehouseExternalId;
    private long partnerId;
    private String suffix;
    private boolean onlyRequired;

    @Nonnull
    public LogisticsPointCreateRequest logisticsPointCreateRequest() {
        return logisticsPointCreateRequest(warehouseExternalId, partnerId, suffix, onlyRequired);
    }

    @Nonnull
    public static LogisticsPointCreateRequest logisticsPointCreateRequest(
        String externalId,
        long partnerId,
        String suffix,
        boolean onlyRequired
    ) {
        LogisticsPointCreateRequest.Builder builder = LogisticsPointCreateRequest.newBuilder()
            .externalId(String.valueOf(externalId))
            .partnerId(partnerId)
            .type(PointType.WAREHOUSE)
            .name(addSuffix("warehouse-name", suffix))
            .address(address(suffix, onlyRequired))
            .active(true)
            .isFrozen(false)
            .schedule(Set.of(scheduleDayResponse(1)));

        if (!onlyRequired) {
            builder
                .contact(contact(suffix))
                .phones(Set.of(phone(suffix)));
        }

        return builder.build();
    }

    @Nonnull
    public LogisticsPointResponse logisticsPointResponse(long id) {
        return logisticsPointResponse(id, warehouseExternalId, partnerId, suffix, onlyRequired);
    }

    @Nonnull
    public static LogisticsPointResponse logisticsPointResponse(
        long id,
        String externalId,
        long partnerId,
        String suffix,
        boolean onlyRequired
    ) {
        LogisticsPointResponse.LogisticsPointResponseBuilder builder = LogisticsPointResponse.newBuilder()
            .id(id)
            .externalId(String.valueOf(externalId))
            .partnerId(partnerId)
            .type(PointType.WAREHOUSE)
            .name(addSuffix("warehouse-name", suffix))
            .address(address(suffix, onlyRequired))
            .active(true)
            .isFrozen(false)
            .schedule(Set.of(scheduleDayResponse(1)));

        if (!onlyRequired) {
            builder
                .contact(contact(suffix))
                .phones(Set.of(phone(suffix)));
        }

        return builder.build();
    }

    @Nonnull
    public WarehouseDto createWarehouseRequest() {
        return createWarehouseRequest(warehouseExternalId, suffix, onlyRequired);
    }

    @Nonnull
    public static WarehouseDto createWarehouseRequest(
        String externalId,
        String suffix,
        boolean onlyRequired
    ) {
        WarehouseDto result = new WarehouseDto()
            .externalId(externalId)
            .name(addSuffix("warehouse-name", suffix))
            .address(warehouseAddress(suffix, onlyRequired))
            .schedule(List.of(scheduleDay(1)));

        if (!onlyRequired) {
            result.contact(warehouseContact(suffix));
        }

        return result;
    }

    @Nonnull
    public WarehouseResponse warehouseResponse(long id) {
        return warehouseResponse(id, warehouseExternalId, suffix, onlyRequired);
    }

    @Nonnull
    public static WarehouseResponse warehouseResponse(long id, String externalId, String suffix, boolean onlyRequired) {
        WarehouseResponse result = new WarehouseResponse()
            .id(id)
            .externalId(externalId)
            .name(addSuffix("warehouse-name", suffix))
            .address(warehouseAddress(suffix, onlyRequired))
            .schedule(List.of(scheduleDay(1)));

        if (!onlyRequired) {
            result.contact(warehouseContact(suffix));
        }

        return result;
    }

    @Nonnull
    public static Address address(String suffix, boolean onlyRequired) {
        Address.AddressBuilder builder = Address.newBuilder()
            .locationId(1)
            .region(addSuffix("region", suffix))
            .settlement(addSuffix("settlement", suffix))
            .street(addSuffix("street", suffix))
            .house(addSuffix("house", suffix))
            .postCode(addSuffix("postalcode", suffix));

        if (!onlyRequired) {
            builder
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.ONE)
                .country(addSuffix("country", suffix))
                .subRegion(addSuffix("subregion", suffix))
                .housing(addSuffix("housing", suffix))
                .building(addSuffix("building", suffix))
                .apartment(addSuffix("apartment", suffix))
                .comment(addSuffix("comment", suffix));
        }
        return builder.build();
    }

    @Nonnull
    public static Contact contact(String suffix) {
        return new Contact(
            addSuffix("first-name", suffix),
            addSuffix("last-name", suffix),
            addSuffix("middle-name", suffix)
        );
    }

    @Nonnull
    public static Phone phone(String suffix) {
        return new Phone(
            addSuffix("phone-number", suffix),
            addSuffix("internal-number", suffix),
            null,
            PhoneType.PRIMARY
        );
    }

    @Nonnull
    public static ScheduleDayResponse scheduleDayResponse(int day) {
        return new ScheduleDayResponse(
            null,
            day,
            LocalTime.MIN,
            LocalTime.MAX
        );
    }

    @Nonnull
    public static WarehouseAddressDto warehouseAddress(String suffix, boolean onlyRequired) {
        WarehouseAddressDto result = new WarehouseAddressDto()
            .geoId(1)
            .region(addSuffix("region", suffix))
            .locality(addSuffix("settlement", suffix))
            .street(addSuffix("street", suffix))
            .house(addSuffix("house", suffix))
            .postalCode(addSuffix("postalcode", suffix));

        if (!onlyRequired) {
            result
                .latitude(BigDecimal.ONE)
                .longitude(BigDecimal.ONE)
                .country(addSuffix("country", suffix))
                .subRegion(addSuffix("subregion", suffix))
                .housing(addSuffix("housing", suffix))
                .building(addSuffix("building", suffix))
                .apartment(addSuffix("apartment", suffix))
                .comment(addSuffix("comment", suffix));
        }

        return result;
    }

    @Nonnull
    public static WarehouseContactDto warehouseContact(String suffix) {
        return new WarehouseContactDto()
            .name(
                new PersonName()
                    .firstName(addSuffix("first-name", suffix))
                    .lastName(addSuffix("last-name", suffix))
                    .middleName(addSuffix("middle-name", suffix))
            )
            .phone(
                new ru.yandex.market.logistics.logistics4go.client.model.Phone()
                    .number(addSuffix("phone-number", suffix))
                    .extension(addSuffix("internal-number", suffix))
            );
    }

    @Nonnull
    public static ScheduleDayDto scheduleDay(int day) {
        return new ScheduleDayDto()
            .day(day)
            .timeFrom(LocalTime.MIN)
            .timeTo(LocalTime.MAX);
    }

    @Nonnull
    public static String addSuffix(String str, String suffix) {
        return str + "-" + suffix;
    }
}
