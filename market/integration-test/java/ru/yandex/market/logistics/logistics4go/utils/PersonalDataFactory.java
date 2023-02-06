package ru.yandex.market.logistics.logistics4go.utils;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.logistics4go.client.model.OrderAddressDto;
import ru.yandex.market.logistics.logistics4go.client.model.PersonName;
import ru.yandex.market.logistics.personal.model.Address;
import ru.yandex.market.personal.client.model.CommonType;
import ru.yandex.market.personal.client.model.FullName;
import ru.yandex.market.personal.client.model.GpsCoordV2;
import ru.yandex.market.personal.client.model.MultiTypeStoreResponseItem;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreRequest;
import ru.yandex.market.personal.client.model.PersonalMultiTypeStoreResponse;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class PersonalDataFactory {

    @Nonnull
    public static PersonalMultiTypeStoreRequest createStoreRequest(boolean isOnlyRequired, boolean isCourier) {
        PersonalMultiTypeStoreRequest request = createRecipientStoreRequest(isOnlyRequired);

        if (isCourier) {
            request.addItemsItem(createAddress(isOnlyRequired));
            if (!isOnlyRequired) {
                request.addItemsItem(createGpsCoords());
            }
        }

        return request;
    }

    @Nonnull
    public static PersonalMultiTypeStoreResponse createStoreResponse(boolean isOnlyRequired, boolean isCourier) {
        PersonalMultiTypeStoreResponse response = createRecipientStoreResponse(isOnlyRequired);

        if (isCourier) {
            response.addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createAddress(isOnlyRequired))
                    .id("address-id")
            );

            if (!isOnlyRequired) {
                response.addItemsItem(
                    new MultiTypeStoreResponseItem()
                        .value(createGpsCoords())
                        .id("gps-coords-id")
                );
            }
        }

        return response;
    }

    @Nonnull
    public static PersonalMultiTypeStoreRequest createC2CStoreRequest() {
        return createRecipientStoreRequest(false)
            .addItemsItem(createSenderFullName())
            .addItemsItem(createSenderPhone());
    }

    @Nonnull
    public static PersonalMultiTypeStoreResponse createC2CStoreResponse() {
        return createRecipientStoreResponse(false)
            .addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createSenderFullName())
                    .id("sender-full-name-id")
            )
            .addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createSenderPhone())
                    .id("sender-phone-id")
            );
    }

    @Nonnull
    public static PersonalMultiTypeStoreRequest createRecipientStoreRequest(boolean isOnlyRequired) {
        PersonalMultiTypeStoreRequest request = new PersonalMultiTypeStoreRequest()
            .addItemsItem(createRecipientFullName(isOnlyRequired))
            .addItemsItem(createRecipientPhone());

        if (!isOnlyRequired) {
            request.addItemsItem(createRecipientEmail());
        }

        return request;
    }

    @Nonnull
    public static PersonalMultiTypeStoreResponse createRecipientStoreResponse(boolean isOnlyRequired) {
        PersonalMultiTypeStoreResponse response = new PersonalMultiTypeStoreResponse()
            .addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createRecipientFullName(isOnlyRequired))
                    .id("recipient-full-name-id")
            )
            .addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createRecipientPhone())
                    .id("recipient-phone-id")
            );

        if (!isOnlyRequired) {
            response.addItemsItem(
                new MultiTypeStoreResponseItem()
                    .value(createRecipientEmail())
                    .id("recipient-email-id")
            );
        }

        return response;
    }

    @Nonnull
    private static CommonType createRecipientFullName(boolean isOnlyRequired) {
        PersonName personName = OrderFactory.recipientPersonName(isOnlyRequired);
        return new CommonType().fullName(
            new FullName()
                .forename(personName.getFirstName())
                .surname(personName.getLastName())
                .patronymic(personName.getMiddleName())
        );
    }

    @Nonnull
    private static CommonType createSenderFullName() {
        PersonName personName = OrderFactory.senderPersonName(false);
        return new CommonType().fullName(
            new FullName()
                .forename(personName.getFirstName())
                .surname(personName.getLastName())
                .patronymic(personName.getMiddleName())
        );
    }

    @Nonnull
    private static CommonType createRecipientPhone() {
        return new CommonType().phone(OrderFactory.recipientPhone(false).getNumber());
    }

    @Nonnull
    private static CommonType createSenderPhone() {
        return new CommonType().phone(OrderFactory.senderPhone(false).getNumber());
    }

    @Nonnull
    private static CommonType createRecipientEmail() {
        return new CommonType().email(OrderFactory.recipient(false).getEmail());
    }

    @Nonnull
    private static CommonType createAddress(boolean isOnlyRequired) {
        OrderAddressDto addressDto = OrderFactory.address(isOnlyRequired);
        Map<String, String> addressMap = new HashMap<>();
        addressMap.put(Address.GEO_ID_KEY, addressDto.getGeoId().toString());
        addressMap.put(Address.COUNTRY_KEY, addressDto.getCountry());
        addressMap.put(Address.REGION_KEY, addressDto.getRegion());
        addressMap.put(Address.LOCALITY_KEY, addressDto.getLocality());
        addressMap.put(Address.HOUSE_KEY, addressDto.getHouse());

        if (!isOnlyRequired) {
            addressMap.put(Address.SUB_REGION_KEY, addressDto.getSubRegion());
            addressMap.put(Address.STREET_KEY, addressDto.getStreet());
            addressMap.put(Address.HOUSING_KEY, addressDto.getHousing());
            addressMap.put(Address.BUILDING_KEY, addressDto.getBuilding());
            addressMap.put(Address.ROOM_KEY, addressDto.getApartment());
            addressMap.put(Address.ZIP_CODE_KEY, addressDto.getPostalCode());
        }

        return new CommonType().address(addressMap);
    }

    @Nonnull
    private static CommonType createGpsCoords() {
        OrderAddressDto addressDto = OrderFactory.address(false);
        return new CommonType().gps(new GpsCoordV2()
            .longitude(addressDto.getLongitude().toString())
            .latitude(addressDto.getLatitude().toString())
        );
    }
}
