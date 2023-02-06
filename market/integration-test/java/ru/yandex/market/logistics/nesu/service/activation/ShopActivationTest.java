package ru.yandex.market.logistics.nesu.service.activation;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.message.model.MessageRecipients;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointCreateRequest;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;
import ru.yandex.market.logistics.nesu.client.model.warehouse.WarehouseContactDto;
import ru.yandex.market.logistics.nesu.dto.ScheduleDayDto;
import ru.yandex.market.logistics.nesu.dto.WarehouseAddress;
import ru.yandex.market.logistics.nesu.enums.TaxSystem;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationProducer;
import ru.yandex.market.logistics.nesu.jobs.producer.SendNotificationToShopProducer;
import ru.yandex.market.logistics.nesu.model.LmsFactory;
import ru.yandex.market.logistics.nesu.model.entity.Sender;
import ru.yandex.market.logistics.nesu.model.entity.Shop;
import ru.yandex.market.logistics.nesu.repository.ShopRepository;
import ru.yandex.market.logistics.nesu.request.warehouse.ShopWarehouseCreateRequest;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.model.LmsFactory.createLogisticsPointResponse;

@DisplayName("Активация магазина при соблюдении необходимых условий")
abstract class ShopActivationTest extends AbstractContextualTest {
    protected static final LogisticsPointResponse LOGISTICS_POINT_RESPONSE =
        createLogisticsPointResponse(1L, 100L, 5L, "name", PointType.WAREHOUSE);

    protected static final int MBI_NOTIFICATION_SHOP_ACTIVATE_ID = 1576834791;
    protected static final int MBI_SALES_NOTIFICATION_SHOP_ACTIVATE_ID = 1581398791;
    protected static final String YADO_SALES_EMAIL = "sales@delivery.yandex.ru";

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    protected ShopRepository shopRepository;

    @Autowired
    protected SendNotificationToShopProducer sendNotificationToShopProducer;

    @Autowired
    protected SendNotificationProducer sendNotificationProducer;

    @Autowired
    protected ShopStatusService shopStatusService;

    @BeforeEach
    void setUp() {
        when(lmsClient.createLogisticsPoint(any())).thenReturn(LOGISTICS_POINT_RESPONSE);
        doNothing().when(sendNotificationToShopProducer).produceTask(anyInt(), anyLong(), isNull(), isNull());
        doNothing().when(sendNotificationProducer).produceTask(anyInt(), any(MessageRecipients.class), anyString());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
        verifyNoMoreInteractions(mbiApiClient);
        verifyNoMoreInteractions(sendNotificationProducer);
        verifyNoMoreInteractions(sendNotificationToShopProducer);
    }

    @ParameterizedTest
    @EnumSource(value = ShopStatus.class, mode = EnumSource.Mode.EXCLUDE, names = {"NEED_SETTINGS"})
    @DisplayName("Магазин в неподходящем для активации статусе")
    void invalidStatus(ShopStatus oldStatus) {
        Shop shop = getShop().setStatus(oldStatus);
        shopStatusService.activateFromStatus(shop, ShopStatus.NEED_SETTINGS);
        softly.assertThat(shop.getStatus()).isEqualTo(oldStatus);
    }

    @Nonnull
    protected static Shop validDaasShop() {
        return validDbsShop()
            .setRole(ShopRole.DAAS)
            .setTaxSystem(TaxSystem.OSN)
            .setBalanceProductId("prod1")
            .setSenders(List.of(new Sender()));
    }

    @Nonnull
    protected static Shop validDbsShop() {
        return validDropship()
            .setRole(ShopRole.DROPSHIP_BY_SELLER)
            .setBalanceContractId(100L)
            .setBalancePersonId(144L);
    }

    @Nonnull
    protected static Shop validDropship() {
        return new Shop()
            .setId(1L)
            .setRole(ShopRole.DROPSHIP)
            .setMarketId(123L)
            .setBalanceClientId(1L)
            .setStatus(ShopStatus.NEED_SETTINGS)
            .setBusinessId(123L)
            .setName("name");
    }

    @Nonnull
    protected static Shop validSupplier() {
        return validDropship().setRole(ShopRole.SUPPLIER);
    }

    protected void assertShopStatus(ShopStatus expectedStatus) {
        softly.assertThat(shopRepository.getShop(1L).getStatus()).isEqualTo(expectedStatus);
    }

    protected abstract Shop getShop();

    @Nonnull
    protected LogisticsPointCreateRequest.Builder logisticsPointCreateRequestBuilder() {
        return LogisticsPointCreateRequest.newBuilder()
            .businessId(41L)
            .name("Имя склада")
            .contact(new Contact("Иван", "Иванов", null))
            .phones(Set.of(new Phone("+7 923 243 5555", null, null, PhoneType.PRIMARY)))
            .active(true)
            .type(PointType.WAREHOUSE)
            .address(
                Address.newBuilder()
                    .locationId(65)
                    .region("Новосибирская область")
                    .settlement("Новосибирск")
                    .street("Николаева")
                    .house("11")
                    .postCode("649220")
                    .build()
            )
            .schedule(LmsFactory.createScheduleDayDtoSetWithSize(5));
    }

    @Nonnull
    protected ShopWarehouseCreateRequest warehouseCreateRequest() {
        ShopWarehouseCreateRequest request = new ShopWarehouseCreateRequest();
        request.setName("Имя склада")
            .setContact(
                WarehouseContactDto.builder()
                    .lastName("Иванов")
                    .firstName("Иван")
                    .phoneNumber("+7 923 243 5555")
                    .build()
            )
            .setAddress(
                WarehouseAddress.builder()
                    .geoId(65)
                    .region("Новосибирская область")
                    .locality("Новосибирск")
                    .street("Николаева")
                    .house("11")
                    .postCode("649220")
                    .build()
            )
            .setSchedule(
                IntStream.range(1, 6)
                    .mapToObj(
                        day -> new ScheduleDayDto()
                            .setDay(day)
                            .setTimeFrom(LocalTime.of(10, 0))
                            .setTimeTo(LocalTime.of(18, 0))
                    )
                    .collect(Collectors.toSet())
            );

        return request;
    }

    @Nonnull
    protected static LogisticsPointFilter createLogisticsPointFilter() {
        return LogisticsPointFilter.newBuilder()
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }
}
