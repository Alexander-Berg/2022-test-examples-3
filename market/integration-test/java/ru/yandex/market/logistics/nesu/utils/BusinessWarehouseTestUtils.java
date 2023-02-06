package ru.yandex.market.logistics.nesu.utils;

import java.math.BigDecimal;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.experimental.UtilityClass;

import ru.yandex.market.logistics.management.entity.request.businessWarehouse.BusinessWarehouseValidationRequest;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.CreateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.businessWarehouse.UpdateBusinessWarehouseDto;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.partner.PlatformClientStatusDto;
import ru.yandex.market.logistics.management.entity.response.businessWarehouse.BusinessWarehouseResponse;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSettingDto;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.StockSyncSwitchReason;
import ru.yandex.market.logistics.nesu.dto.WarehouseAddress;
import ru.yandex.market.logistics.nesu.dto.business.BusinessWarehouseRequest;
import ru.yandex.market.logistics.nesu.service.lms.PlatformClientId;

import static ru.yandex.market.logistics.nesu.model.LmsFactory.createScheduleDayDtoSetWithSize;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createShortAddressDtoBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.createSchedule;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseAddressMinimalBuilder;
import static ru.yandex.market.logistics.nesu.model.ModelFactory.warehouseContact;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.DROPSHIP_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.SUPPLIER_BUSINESS_ID;
import static ru.yandex.market.logistics.nesu.utils.PartnerTestUtils.SUPPLIER_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public final class BusinessWarehouseTestUtils {
    public static final String EXTERNAL_ID = "external-id";
    private static final String DROPSHIP_NAME = "дропшип";
    private static final String SUPPLIER_NAME = "Кроссдок";
    public static final long BUSINESS_ID = 42L;

    public static final String INVALID_SYMBOLS_IN_EXTERNAL_ID = "illegalSymbols!";
    public static final String WHITESPACES_AND_RUSSIAN_LETTERS = "externalId с пробелами";
    public static final String INVALID_NAME_LENGTH = "a".repeat(256 + 1);
    public static final String INVALID_EXTERNAL_ID_LENGTH = "a".repeat(64 + 1);

    @Nonnull
    public static BusinessWarehouseRequest dropshipBusinessWarehouseRequest() {
        BusinessWarehouseRequest request = new BusinessWarehouseRequest();
        request
            .setAddress(
                WarehouseAddress.builder()
                    .geoId(2)
                    .locality("Новосибирск")
                    .postCode("649220")
                    .latitude(new BigDecimal(1))
                    .longitude(new BigDecimal(2))
                    .street("Николаева")
                    .house("11")
                    .housing("1/2")
                    .building("2a")
                    .apartment("314")
                    .comment("как проехать")
                    .region("Новосибирская область")
                    .subRegion("Новосибирский округ")
                    .build()
            )
            .setName(DROPSHIP_NAME)
            .setSchedule(createSchedule(5))
            .setExternalId(EXTERNAL_ID)
            .setContact(warehouseContact());
        return request;
    }

    @Nonnull
    public static BusinessWarehouseRequest minimalValidBusinessWarehouseRequest() {
        return new BusinessWarehouseRequest().setSchedule(createSchedule(5))
            .setName("Name")
            .setAddress(warehouseAddressMinimalBuilder().build());
    }

    @Nonnull
    public static CreateBusinessWarehouseDto dropshipCreateWarehouseDtoToLms() {
        return dropshipCreateWarehouseDtoBuilder().build();
    }

    @Nonnull
    public static UpdateBusinessWarehouseDto dropshipUpdateWarehouseDtoToLms() {
        return UpdateBusinessWarehouseDto.newBuilder()
            .name(DROPSHIP_NAME)
            .externalId(EXTERNAL_ID)
            .phones(defaultPhones())
            .contact(defaultContact())
            .readableName(DROPSHIP_NAME)
            .schedule(createScheduleDayDtoSetWithSize(5))
            .address(createShortAddressDtoBuilder(2).build())
            .build();
    }

    @Nonnull
    private static CreateBusinessWarehouseDto.Builder commonLmsBuilder() {
        return CreateBusinessWarehouseDto.newBuilder()
            .businessId(BUSINESS_ID)
            .address(createShortAddressDtoBuilder(2).build())
            .schedule(createScheduleDayDtoSetWithSize(5))
            .externalId(EXTERNAL_ID)
            .contact(defaultContact())
            .phones(defaultPhones())
            .platformClients(Set.of(PlatformClientStatusDto.newBuilder()
                .status(PartnerStatus.ACTIVE)
                .platformClientId(PlatformClientId.BERU.getId())
                .build()));
    }

    @Nonnull
    public static PartnerSettingDto partnerSettingDto(PartnerType partnerType, Integer locationId) {
        return partnerSettingDtoBuilder(partnerType, locationId).build();
    }

    @Nonnull
    public static PartnerSettingDto.Builder partnerSettingDtoBuilder(PartnerType partnerType, Integer locationId) {
        return PartnerSettingDto.newBuilder()
            .stockSyncEnabled(false)
            .autoSwitchStockSyncEnabled(true)
            .stockSyncSwitchReason(StockSyncSwitchReason.NEW)
            .korobyteSyncEnabled(false)
            .autoItemRemovingEnabled(partnerType == PartnerType.DROPSHIP ? true : null)
            .updateCourierNeeded(partnerType == PartnerType.DROPSHIP ? true : null)
            .canSellMedicine(false)
            .canDeliverMedicine(false)
            .locationId(locationId);
    }

    @Nonnull
    public static BusinessWarehouseResponse dropshipResponse() {
        return commonResponse()
            .businessId(BUSINESS_ID)
            .logisticsPointId(29L)
            .partnerType(PartnerType.DROPSHIP)
            .partnerId(DROPSHIP_PARTNER_ID)
            .build();
    }

    @Nonnull
    public static PlatformClientPartnerFilter platformClientFilter(long partnerId) {
        return PlatformClientPartnerFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .platformClientIds(Set.of(PlatformClientId.BERU.getId()))
            .build();
    }

    @Nonnull
    public static BusinessWarehouseResponse.Builder commonResponse() {
        return BusinessWarehouseResponse.newBuilder()
            .partnerStatus(PartnerStatus.INACTIVE)
            .address(createShortAddressDtoBuilder(2).build());
    }

    @Nonnull
    public static BusinessWarehouseResponse supplierResponse() {
        return commonResponse()
            .logisticsPointId(31L)
            .partnerId(SUPPLIER_PARTNER_ID)
            .partnerType(PartnerType.SUPPLIER)
            .partnerId(SUPPLIER_PARTNER_ID)
            .build();
    }

    @Nonnull
    public static CreateBusinessWarehouseDto supplierCreateWarehouseDtoToLms() {
        return commonLmsBuilder()
            .businessId(SUPPLIER_BUSINESS_ID)
            .marketId(202L)
            .partnerType(PartnerType.SUPPLIER)
            .name(SUPPLIER_NAME)
            .readableName(SUPPLIER_NAME)
            .partnerSettingDto(partnerSettingDto(PartnerType.SUPPLIER, 2))
            .build();
    }

    @Nonnull
    public static BusinessWarehouseRequest supplierBusinessWarehouseRequest() {
        return dropshipBusinessWarehouseRequest().setName(SUPPLIER_NAME);
    }

    @Nonnull
    public static BusinessWarehouseValidationRequest validationRequest() {
        return validationRequest(BUSINESS_ID, null);
    }

    @Nonnull
    public static BusinessWarehouseValidationRequest validationRequest(long businessId) {
        return validationRequest(businessId, null);
    }

    @Nonnull
    public static BusinessWarehouseValidationRequest validationRequest(long businessId, @Nullable Long partnerId) {
        return validationRequest(businessId, EXTERNAL_ID, partnerId);
    }

    @Nonnull
    public static BusinessWarehouseValidationRequest validationRequest(
        long businessId,
        String externalId,
        @Nullable Long partnerId
    ) {
        return BusinessWarehouseValidationRequest.builder()
            .businessId(businessId)
            .partnerId(partnerId)
            .externalId(externalId)
            .build();
    }

    @Nonnull
    public static ValidationErrorData externalIdValidationErrorData() {
        return fieldErrorBuilder("externalId", ValidationErrorData.ErrorType.IS_UNIQUE)
            .forObject("businessWarehouseRequest");
    }

    @Nonnull
    public static CreateBusinessWarehouseDto.Builder dropshipCreateWarehouseDtoBuilder() {
        return commonLmsBuilder()
            .marketId(200L)
            .partnerType(PartnerType.DROPSHIP)
            .name(DROPSHIP_NAME)
            .readableName(DROPSHIP_NAME)
            .partnerSettingDto(partnerSettingDto(PartnerType.DROPSHIP, 2));
    }

    @Nonnull
    private static Contact defaultContact() {
        return new Contact("Иван", "Иванов", "Иванович");
    }

    @Nonnull
    private static Set<Phone> defaultPhones() {
        return Set.of(new Phone("+7 923 243 5555", "777", null, PhoneType.PRIMARY));
    }
}
