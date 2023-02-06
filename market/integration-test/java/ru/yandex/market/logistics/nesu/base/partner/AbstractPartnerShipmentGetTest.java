package ru.yandex.market.logistics.nesu.base.partner;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterOrdersCountRequestDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.StatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationStatusHistoryInfoRequestDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.delivery.transport_manager.model.enums.RegisterType;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.model.L4ShopsFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics4shops.client.model.MdsFilePath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.nesu.model.TMFactory.DROPOFF_LOGISTIC_POINT;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentGetTest extends AbstractPartnerShipmentTest {
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 15).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        mockWarehouses();
        mockPartnerRelation();
        mockHandlingTime(0);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(outboundApi);
    }

    @Test
    @DisplayName("Нет настроек партнёра магазина")
    void noShopPartner() throws Exception {
        getShipment(SECOND_SHOP_ID)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20]"));
    }

    @Test
    @DisplayName("Нет настроек партнёра магазина при запросе со списком магазинов.")
    void noShopPartnerWithShopList() throws Exception {
        mockDefaultTransportation();
        mockMvc.perform(
                get(url(TMFactory.SHIPMENT_ID))
                    .param("userId", "-1")
                    .param("shopIds", "20", "30", "40")
            )
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20, 30, 40]"));
    }

    @Test
    @DisplayName("Получение данных отгрузки со списком магазинов.")
    void withShopList() throws Exception {
        mockDefaultTransportation();
        mockMvc.perform(
                get(url(TMFactory.SHIPMENT_ID))
                    .param("userId", "-1")
                    .param("shopIds", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
            )
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Отсутствует идентификатор магазина.")
    void withoutShopId() throws Exception {
        ValidationErrorData.ValidationErrorDataBuilder validationError =
            ValidationErrorData.objectErrorBuilder(
                "Must specify either shopId or shopIds",
                "ValidShopIds"
            );
        mockDefaultTransportation();
        mockMvc.perform(
                get(url(TMFactory.SHIPMENT_ID))
                    .param("userId", "-1")
            )
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(validationError.forObject("shopIdsHolder")));
    }

    @Test
    @DisplayName("Отгрузка не найдена")
    void transportationNotFound() throws Exception {
        getShipment()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER_SHIPMENT] with ids [500]"));
    }

    @Test
    @DisplayName("Чужая отгрузка")
    void anotherTransportationPartner() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TransportationUnitDto.builder()
                    .partner(
                        TransportationPartnerExtendedInfoDto.builder()
                            .id(-TMFactory.PARTNER_ID)
                            .build()
                    )
                    .build(),
                TMFactory.defaultMovement().build()
            ));

        getShipment()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER_SHIPMENT] with ids [500]"));
    }

    @Test
    @DisplayName("Самопривоз")
    void importShipment() throws Exception {
        mockDefaultTransportation();

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Самопривоз в dropoff")
    void importToDropoff() throws Exception {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(TMFactory.WAREHOUSE_FROM, TMFactory.DROPOFF_LOGISTIC_POINT))
                .build()
        )).thenReturn(List.of(
            warehouse(TMFactory.WAREHOUSE_FROM, TMFactory.PARTNER_ID, "Какой-то склад", "Один адрес"),
            LogisticsPointResponse.newBuilder()
                .id(TMFactory.DROPOFF_LOGISTIC_POINT)
                .name("Дропофф в доме напротив")
                .type(PointType.PICKUP_POINT)
                .address(
                    Address.newBuilder()
                        .locationId(213)
                        .addressString("Адрес дропоффа")
                        .shortAddressString("Адрес дропоффа (короткий)")
                        .build()
                )
                .build()
        ));

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().build(),
                TMFactory.defaultMovement().build(),
                TMFactory.defaultInbound()
                    .logisticPointId(DROPOFF_LOGISTIC_POINT)
                    .build()
            ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import_to_dropoff.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Самопривоз второго партнёра")
    void secondPartnerImportShipment() throws Exception {
        mockPartnerRelation(TMFactory.SECOND_PARTNER_ID);
        doReturn(Duration.ofHours(1))
            .when(lmsClient)
            .getWarehouseHandlingDuration(TMFactory.SECOND_PARTNER_ID);

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .partner(TMFactory.transportationPartner(
                        TMFactory.SECOND_PARTNER_ID,
                        TMFactory.SECOND_PARTNER_NAME
                    ))
                    .build(),
                TMFactory.defaultMovement()
                    .partner(TMFactory.transportationPartner(
                        TMFactory.SECOND_PARTNER_ID,
                        TMFactory.SECOND_PARTNER_NAME
                    ))
                    .build()
            ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import_second_partner.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Забор")
    void withdrawShipment() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().build(),
                TMFactory.movement(200L, "Другой партнёр").build()
            ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/withdraw.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Забор с подтвержденной отгрузкой")
    void withdrawConfirmedShipment() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().build(),
                TMFactory.movement(200L, "Другой партнёр").build()
            ));

        mockOutbounds(List.of(1L));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/withdraw_confirmed.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Заказы из реестра: один запрос в TM")
    void registerOrders() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(
                        TMFactory.outboundRegister(-1).build(),
                        TMFactory.outboundRegister().build(),
                        TMFactory.outboundRegister(-2).build()
                    )).build(),
                TMFactory.defaultMovement().build()
            ));

        TMFactory.mockOutboundUnits(transportManagerClient, List.of(
            RegisterUnitDto.builder()
                .partialIds(List.of(
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value("200").build(),
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value("100").build(),
                    PartialIdDto.builder().idType(IdType.CIS).build()
                ))
                .build(),
            RegisterUnitDto.builder()
                .partialIds(List.of(
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value("100").build()
                ))
                .build()
        ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/orders.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Заказы из реестра: несколько запросов в TM")
    void registerOrdersSeveralRequests() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));

        TMFactory.mockOutboundUnits(
            transportManagerClient,
            List.of(
                RegisterUnitDto.builder()
                    .partialIds(List.of(PartialIdDto.builder().idType(IdType.ORDER_ID).value("200").build()))
                    .build()
            ),
            0,
            2
        );

        TMFactory.mockOutboundUnits(
            transportManagerClient,
            List.of(
                RegisterUnitDto.builder()
                    .partialIds(List.of(PartialIdDto.builder().idType(IdType.ORDER_ID).value("100").build()))
                    .build()
            ),
            1,
            2
        );

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/orders.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Счётчики заказов")
    void orderCount() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build(),
                TMFactory.defaultInbound()
                    .registers(List.of(TMFactory.inboundRegister().build()))
                    .build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());

        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID, TMFactory.INBOUND_REGISTER_ID))
        )).thenReturn(List.of(
            new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L),
            new RegisterOrdersCountDto(TMFactory.INBOUND_REGISTER_ID, 3L)
        ));

        mockOutbounds(List.of(1L, 2L, 3L, 4L));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/orders_count.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Статус из TM")
    void transportationStatus() throws Exception {
        mockDefaultTransportation();
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());

        when(transportManagerClient.getTransportationsStatusHistory(
            new TransportationStatusHistoryInfoRequestDto()
                .setTransportationIds(List.of(TMFactory.SHIPMENT_ID))
                .setGetUnitsHistory(true)
        )).thenReturn(List.of(
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID)
                .setStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setChangedAt(Instant.parse("2021-05-01T01:00:00.00Z"))
                        .setNewStatus("OUTBOUND_CREATED"),
                    new StatusHistoryInfoDto()
                        .setChangedAt(Instant.parse("2021-05-01T00:00:00.00Z"))
                        .setNewStatus("SCHEDULED")
                ))
                .setOutboundStatusHistoryList(List.of())
                .setMovementStatusHistoryList(List.of())
                .setInboundStatusHistoryList(List.of())
        ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/status_tm.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Получение фактического время прибытия мерча на дропофф")
    void inboundPartnerArrivalTime() throws Exception {
        mockTransportationWithInboundRegister();

        when(transportManagerClient.getTransportationsStatusHistory(
            new TransportationStatusHistoryInfoRequestDto()
                .setTransportationIds(List.of(TMFactory.SHIPMENT_ID))
                .setGetUnitsHistory(true)
        )).thenReturn(List.of(
            new TransportationStatusHistoryInfoDto()
                .setTransportationId(TMFactory.SHIPMENT_ID)
                .setStatusHistoryList(List.of())
                .setOutboundStatusHistoryList(List.of())
                .setMovementStatusHistoryList(List.of())
                .setInboundStatusHistoryList(List.of(
                    new StatusHistoryInfoDto()
                        .setChangedAt(Instant.parse("2021-05-01T02:00:00.00Z"))
                        .setNewStatus("ARRIVED")
                ))
        ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/inbound_arrival_time.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Получение фактического время прибытия мерча на дропофф, inbound реестр не в статусе FACT")
    void outboundPartnerArrivalTime() throws Exception {
        mockDefaultTransportationWithOutboundRegisters(
            List.of(TMFactory.outboundRegister().type(RegisterType.FACT).build())
        );

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Статус из l4s")
    void outboundStatus() throws Exception {
        mockDefaultTransportation();
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());

        mockOutbounds(List.of(3L));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/status_l4s.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Идентификатор из l4s")
    void outboundExternalId() throws Exception {
        mockDefaultTransportation();
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());

        mockOutbounds(List.of(3L), "shipment-number");

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/external_id.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Отправка ещё не создана в l4s")
    void notCreatedOutbound() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().yandexId(null).build(),
                TMFactory.defaultMovement().build()
            ));

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/import.json"));

        verify(outboundApi, never()).searchOutbounds(any());
    }

    @Test
    @DisplayName("Доступно подтверждение")
    void confirmAvailable() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID))
        ))
            .thenReturn(List.of(new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L)));

        mockNonConfirmedOutbound();

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/confirm_available.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Для отгрузки без заказов подтверждение недоступно")
    void noOrdersOutboundConfirmUnavailable() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID))
        ))
            .thenReturn(List.of(new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 0L)));

        mockNonConfirmedOutbound();

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/confirm_no_orders.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Доступно скачивание акта о расхождениях - акт готов, есть расхождения")
    void discrepancyActDownloadAvailable() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID))
        ))
            .thenReturn(List.of(new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L)));

        mockOutboundsWithDiscrepancyActIsReady(L4ShopsFactory.mdsFilePath());

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/discrepancy_act_download_available.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Доступно скачивание акта о расхождениях - акт готов, есть расхождения, фича выключена")
    void discrepancyActDownloadFeatureNotAvailableAvailable() throws Exception {
        when(featureProperties.isEnableDownloadDiscrepancyActAction()).thenReturn(false);
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID))
        ))
            .thenReturn(List.of(new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L)));

        mockOutboundsWithDiscrepancyActIsReady(L4ShopsFactory.mdsFilePath());

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/confirm_available.json"));
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Недоступно скачивание акта о расхождениях - акт готов, нет расхождений")
    void discrepancyActDownloadNotAvailableNoDiscrepancy() throws Exception {
        clock.setFixed(TMFactory.SHIPMENT_DATE.atTime(12, 35).toInstant(ZoneOffset.UTC), ZoneId.systemDefault());

        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build()
            ));
        TMFactory.mockOutboundUnits(transportManagerClient, List.of());
        when(transportManagerClient.getOrdersCount(
            new RegisterOrdersCountRequestDto(List.of(TMFactory.OUTBOUND_REGISTER_ID))
        ))
            .thenReturn(List.of(new RegisterOrdersCountDto(TMFactory.OUTBOUND_REGISTER_ID, 10L)));

        mockOutboundsWithDiscrepancyActIsReady(null);

        getShipment()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/partner/shipment/get/confirm_available.json"));
        verifyGetOutbounds();
    }

    private void mockDefaultTransportation() {
        mockDefaultTransportationWithOutboundRegisters(null);
    }

    private void mockDefaultTransportationWithOutboundRegisters(@Nullable List<RegisterDto> outboundRegisters) {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().registers(outboundRegisters).build(),
                TMFactory.defaultMovement().build()
            ));
    }

    private void mockTransportationWithInboundRegister() {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound().build(),
                TMFactory.defaultMovement()
                    .arrivedAt(
                        ZonedDateTime.ofInstant(Instant.parse("2021-05-01T01:00:00.00Z"), ZoneId.systemDefault())
                    )
                    .build(),
                TMFactory.defaultInbound().registers(List.of(TMFactory.inboundRegister().build())).build()
            ));
    }

    @Nonnull
    private ResultActions getShipment() throws Exception {
        return getShipment(SHOP_ID);
    }

    @Nonnull
    private ResultActions getShipment(long shopId) throws Exception {
        return mockMvc.perform(
            get(url(TMFactory.SHIPMENT_ID))
                .param("userId", "-1")
                .param("shopId", String.valueOf(shopId))
        );
    }

    @Nonnull
    protected abstract String url(long shipmentId);

    protected abstract void mockOutbounds(List<Long> orderIds);

    protected abstract void mockOutbounds(List<Long> orderIds, String externalId);

    protected abstract void mockNonConfirmedOutbound();

    protected abstract void verifyGetOutbounds();

    protected abstract void mockOutboundsWithDiscrepancyActIsReady(MdsFilePath mdsFilePath);
}
