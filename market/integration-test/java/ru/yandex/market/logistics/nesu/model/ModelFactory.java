package ru.yandex.market.logistics.nesu.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.client.model.warehouse.WarehouseContactDto;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.WarehouseAddress;
import ru.yandex.market.logistics.nesu.dto.enums.NumericValueOperationType;
import ru.yandex.market.logistics.nesu.dto.modifier.NumericValueModificationRuleDto;
import ru.yandex.market.logistics.nesu.dto.modifier.NumericValueRangeDto;
import ru.yandex.market.logistics.nesu.enums.FeedStatus;
import ru.yandex.market.logistics.nesu.enums.FileExtension;
import ru.yandex.market.logistics.nesu.enums.FileType;
import ru.yandex.market.logistics.nesu.enums.SenderStatus;
import ru.yandex.market.logistics.nesu.model.entity.Contact;
import ru.yandex.market.logistics.nesu.model.entity.Feed;
import ru.yandex.market.logistics.nesu.model.entity.MdsFile;
import ru.yandex.market.logistics.nesu.model.entity.Offer;
import ru.yandex.market.logistics.nesu.model.entity.Phone;
import ru.yandex.market.logistics.nesu.model.entity.Sender;
import ru.yandex.market.logistics.nesu.model.entity.Shop;

/**
 * Методы создания билдеров моделей.
 * Билдеры предзаполняют все обязательные поля.
 */
public final class ModelFactory {

    public static final String OFFER_NAME = "test-offer-name";
    public static final String EXTERNAL_ID = "test-offer-external-id";
    public static final String SHOP_NAME = "test-shop-name";
    public static final String FILE_URL = "http://localhost/test-file-url.xml";
    public static final String FILE_NAME = "test-filename";
    public static final long OFFER_ID = 1L;
    public static final BigDecimal PRICE = BigDecimal.valueOf(1500);
    public static final long SHOP_ID = 1L;
    public static final long BALANCE_CLIENT_ID = 1L;
    public static final long MDS_FILE_ID = 1L;
    public static final long FEED_ID = 1L;
    public static final long CONTACT_ID = 1L;
    public static final String LAST_NAME = "test-last-name";
    public static final String FIRST_NAME = "test-first-name";
    public static final String MIDDLE_NAME = "test-middle-name";
    public static final String EMAIL = "test-email@test-sender-name.com";
    public static final String SECOND_EMAIL = "second-email@second-sender-name.com";
    public static final String PHONE_NUMBER = "9999999999";
    public static final String SENDER_NAME = "test-sender-name";
    public static final String SITE_URL = "www.test-sender-name.com";

    private ModelFactory() {
        throw new UnsupportedOperationException();
    }

    public static Offer offer() {
        return new Offer()
            .setPrice(PRICE)
            .setName(OFFER_NAME)
            .setExternalId(EXTERNAL_ID);
    }

    public static Shop shop() {
        return new Shop()
            .setId(SHOP_ID)
            .setStatus(ShopStatus.NEED_SETTINGS)
            .setRole(ShopRole.DAAS)
            .setBalanceClientId(BALANCE_CLIENT_ID)
            .setName(SHOP_NAME)
            .setHasCommitedOrders(false)
            .setFirstOrderEmailSent(false);
    }

    public static MdsFile mdsFile() {
        return new MdsFile()
            .setExtension(FileExtension.XML)
            .setFileType(FileType.FEED)
            .setUrl(FILE_URL)
            .setFileName(FILE_NAME)
            .setCanBeDeleted(false);
    }

    public static Feed feed() {
        return new Feed()
            .setSender(sender())
            .setMdsFile(mdsFile())
            .setStatus(FeedStatus.ACTIVE);
    }

    public static Phone phone() {
        return new Phone()
            .setPhoneNumber(PHONE_NUMBER);
    }

    public static Contact contact() {
        return new Contact()
            .setLastName(LAST_NAME)
            .setFirstName(FIRST_NAME)
            .setMiddleName(MIDDLE_NAME)
            .addEmail(EMAIL)
            .addEmail(SECOND_EMAIL)
            .setPhone(phone());
    }

    public static Sender sender() {
        return new Sender()
            .setName(SENDER_NAME)
            .setShop(shop())
            .setContact(contact())
            .setSiteUrl(SITE_URL)
            .setStatus(SenderStatus.ACTIVE);
    }

    public static NumericValueRangeDto createNumericValueRangeDto() {
        return new NumericValueRangeDto()
            .setMin(BigDecimal.ZERO)
            .setMax(BigDecimal.TEN);
    }

    public static NumericValueModificationRuleDto createRuleDto() {
        return new NumericValueModificationRuleDto()
            .setValue(BigDecimal.valueOf(5))
            .setType(NumericValueOperationType.FIX_VALUE)
            .setResultRange(createNumericValueRangeDto());
    }

    @Nonnull
    public static WarehouseAddress.WarehouseAddressBuilder warehouseAddressBuilder() {
        return warehouseAddressMinimalBuilder()
            .housing("")
            .building("")
            .apartment("")
            .comment("как проехать");
    }

    @Nonnull
    public static WarehouseAddress warehouseAddress() {
        return warehouseAddressBuilder().build();
    }

    @Nonnull
    public static WarehouseAddress.WarehouseAddressBuilder warehouseAddressMinimalBuilder() {
        return WarehouseAddress.builder()
            .geoId(65)
            .region("Новосибирская область")
            .locality("Новосибирск")
            .street("Николаева")
            .house("11")
            .postCode("649220");
    }

    @Nonnull
    public static WarehouseContactDto warehouseContact() {
        return warehouseContactMinimalBuilder()
            .middleName("Иванович")
            .internalNumber("777")
            .build();
    }

    @Nonnull
    public static WarehouseContactDto.WarehouseContactDtoBuilder warehouseContactMinimalBuilder() {
        return WarehouseContactDto.builder()
            .firstName("Иван")
            .lastName("Иванов")
            .phoneNumber("+7 923 243 5555");
    }

    @Nonnull
    public static ScheduleDayDto scheduleDay() {
        return new ScheduleDayDto()
            .setDay(1)
            .setTimeFrom(LocalTime.of(10, 0))
            .setTimeTo(LocalTime.of(18, 0));
    }

    @Nonnull
    public static Set<ScheduleDayDto> createSchedule(int days) {
        return IntStream.range(1, days + 1)
            .mapToObj(day -> scheduleDay().setDay(day))
            .collect(Collectors.toSet());
    }

    @Nonnull
    public static WarehouseAddress.WarehouseAddressBuilder warehouseAddressExtendedBuilder() {
        return warehouseAddressMinimalBuilder()
            .geoId(2)
            .latitude(new BigDecimal(1))
            .longitude(new BigDecimal(2))
            .housing("1/2")
            .building("2a")
            .apartment("314")
            .comment("как проехать")
            .subRegion("Новосибирский округ");
    }
}
