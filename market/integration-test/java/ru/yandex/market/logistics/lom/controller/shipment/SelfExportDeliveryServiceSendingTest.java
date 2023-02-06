package ru.yandex.market.logistics.lom.controller.shipment;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.Car;
import ru.yandex.market.logistic.gateway.common.model.delivery.Courier.CourierBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.Person.PersonBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport;
import ru.yandex.market.logistic.gateway.common.model.delivery.SelfExport.SelfExportBuilder;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.entity.enums.CourierType;
import ru.yandex.market.logistics.lom.entity.enums.ResourceType;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.exception.DbQueueJobExecutionException;
import ru.yandex.market.logistics.lom.jobs.model.ShipmentApplicationIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.DeliveryServiceShipmentProcessingService;
import ru.yandex.market.logistics.lom.service.AbstractExternalServiceTest;
import ru.yandex.market.logistics.lom.utils.LmsFactory;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createShipmentApplicationIdPayload;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createResourceId;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwDeliveryEntitiesUtils.createWarehouseSelfExport;

@DisplayName("Тесты отправки заявки на самопривоз в службу доставки")
class SelfExportDeliveryServiceSendingTest extends AbstractExternalServiceTest {
    @Autowired
    private DeliveryServiceShipmentProcessingService deliveryServiceShipmentProcessingService;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private DeliveryClient deliveryClient;

    private static final long POINT_FROM_ID = 1;
    private static final long POINT_TO_ID = 2;
    private static final long PARTNER_FROM_ID = 2;
    private static final long PARTNER_TO_ID = 3;

    @BeforeEach
    void setup() {
        mockPoints(POINT_FROM_ID, POINT_TO_ID, PARTNER_FROM_ID, PARTNER_TO_ID);
    }

    @AfterEach
    void afterEach() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Успешное создание заявки на самопривоз (на машине)")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_car.xml")
    @SneakyThrows
    void sendRequestOnCar() {
        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "123");
        deliveryServiceShipmentProcessingService.processPayload(payload);

        verify(deliveryClient).createSelfExport(
            eq(createSelfExport(CourierType.CAR).build()),
            eq(new Partner(PARTNER_TO_ID)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешное создание заявки на самопривоз (пеший)")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_onfoot.xml")
    @SneakyThrows
    void sendRequestOnFoot() {
        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "123");
        deliveryServiceShipmentProcessingService.processPayload(payload);

        verify(deliveryClient).createSelfExport(
            eq(createSelfExport(CourierType.COURIER).build()),
            eq(new Partner(PARTNER_TO_ID)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешное создание заявки на самопривоз в случае, когда не указан контакт у склада партнера")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_car.xml")
    @SneakyThrows
    void sendRequestWithoutPartnerWarehouseContact() {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder().ids(Set.of(1L, 2L)).build()
        ))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(POINT_FROM_ID, null, "point", PointType.WAREHOUSE)
                    .build(),
                LmsFactory.createLogisticsPointResponse(POINT_TO_ID, PARTNER_TO_ID, "point", PointType.WAREHOUSE)
                    .contact(null)
                    .build()
            ));

        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "123");
        deliveryServiceShipmentProcessingService.processPayload(payload);

        verify(deliveryClient).createSelfExport(
            eq(createSelfExport(CourierType.CAR)
                .setWarehouse(createWarehouseSelfExport().setContact(null).build())
                .build()),
            eq(new Partner(PARTNER_TO_ID)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Успешное создание заявки на самопривоз для DAAS магазина (без partner_id)")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_car.xml")
    @SneakyThrows
    void sendingImportForDaasShopShipment() {
        mockPoints(POINT_FROM_ID, POINT_TO_ID, null, PARTNER_TO_ID);
        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "123");
        deliveryServiceShipmentProcessingService.processPayload(payload);

        verify(deliveryClient).createSelfExport(
            eq(createSelfExport(CourierType.CAR).build()),
            eq(new Partner(PARTNER_TO_ID)),
            eq(EXPECTED_CLIENT_REQUEST_META)
        );
    }

    @Test
    @DisplayName("Заявка не найдена")
    void sendRequestRequestNotFound() {
        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L);
        assertThrows(
            ResourceNotFoundException.class,
            () -> deliveryServiceShipmentProcessingService.processPayload(payload),
            "Failed to find [SHIPMENT_APPLICATION] with id [1]"
        );
    }

    @Test
    @DisplayName("Не найден склад")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_onfoot.xml")
    void sendRequestWarehouseNotFound() {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder().ids(Set.of(1L, 2L)).build()
        ))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(POINT_FROM_ID, null, "point", PointType.WAREHOUSE)
                    .build()
            ));

        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "1");

        DbQueueJobExecutionException exception = assertThrows(
            DbQueueJobExecutionException.class,
            () -> deliveryServiceShipmentProcessingService.processPayload(payload)
        );

        assertThat(exception).hasCauseInstanceOf(ResourceNotFoundException.class);
        assertThat(exception.getCause())
            .hasMessage("Failed to find [%s] with id [%s]", ResourceType.LOGISTICS_POINT, POINT_TO_ID);
    }

    @Test
    @DisplayName("DSClient выбросил исключение")
    @DatabaseSetup("/controller/shipment/before/create_shipment_import_onfoot.xml")
    @SneakyThrows
    void sendRequestClientException() {
        doThrow(new GatewayApiException("Exception")).when(deliveryClient).createSelfExport(any(), any(), any());

        ShipmentApplicationIdPayload payload = createShipmentApplicationIdPayload(1L, "1");

        DbQueueJobExecutionException exception = assertThrows(
            DbQueueJobExecutionException.class,
            () -> deliveryServiceShipmentProcessingService.processPayload(payload)
        );

        assertThat(exception).hasCauseInstanceOf(GatewayApiException.class);
        assertThat(exception.getCause()).hasMessage("Exception");

        verify(deliveryClient).createSelfExport(
            eq(createSelfExport(CourierType.COURIER).build()),
            eq(new Partner(PARTNER_TO_ID)),
            eq(new ClientRequestMeta("1"))
        );
    }

    @Nonnull
    private SelfExportBuilder createSelfExport(CourierType courierType) {
        return new SelfExport.SelfExportBuilder()
            .setSelfExportId(createResourceId("1", null).build())
            .setWarehouse(createWarehouseSelfExport().build())
            .setTime(createDateTimeInterval())
            .setVolume(0.1F)
            .setWeight(0.5F)
            .setCourier(createCourier(courierType).build());
    }

    @Nonnull
    private DateTimeInterval createDateTimeInterval() {
        return new DateTimeInterval(
            OffsetDateTime.of(2019, 5, 25, 12, 0, 0, 0, ZoneOffset.of("+03:00")),
            OffsetDateTime.of(2019, 5, 25, 14, 0, 0, 0, ZoneOffset.of("+03:00"))
        );
    }

    @Nonnull
    private CourierBuilder createCourier(CourierType courierType) {
        return new CourierBuilder(
            Collections.singletonList(createPerson("test-first-name-1", "test-last-name-1", null).build())
        )
            .setCar(courierType == CourierType.CAR ? createCar("A001BC23", "Renault").build() : null);
    }

    @Nonnull
    private Car.CarBuilder createCar(String number, String description) {
        return new Car.CarBuilder(number)
            .setDescription(description);
    }

    @Nonnull
    private PersonBuilder createPerson(String name, String surname, String patronymic) {
        return new PersonBuilder(name, surname)
            .setPatronymic(patronymic);
    }

    private void mockPoints(Long pointFromId, Long pointToId, Long partnerFromId, Long partnerToId) {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder().ids(Set.of(pointFromId, pointToId)).build()
        ))
            .thenReturn(List.of(
                LmsFactory.createLogisticsPointResponse(pointFromId, partnerFromId, "point", PointType.WAREHOUSE)
                    .build(),
                LmsFactory.createLogisticsPointResponse(pointToId, partnerToId, "point", PointType.WAREHOUSE)
                    .build()
            ));
    }
}
