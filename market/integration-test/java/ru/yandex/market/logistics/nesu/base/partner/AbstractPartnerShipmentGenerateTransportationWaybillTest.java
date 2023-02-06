package ru.yandex.market.logistics.nesu.base.partner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationPartnerExtendedInfoDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.StorageUnitDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Car;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Cargo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Courier;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.LegalInfo;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.Payer;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.ReceptionTransferAct;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.TransportationUnit;
import ru.yandex.market.logistics.werewolf.model.entity.waybill.WaybillInformation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomItemBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createLomOrderCost;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createPlaceUnitBuilder;
import static ru.yandex.market.logistics.nesu.base.order.OrderDtoFactory.createRootUnit;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.resourceNotFoundMatcher;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentGenerateTransportationWaybillTest extends AbstractPartnerShipmentTest {

    private static final long ANOTHER_ORDER_ID = 200;
    private static final String ANOTHER_EXTERNAL_ID = String.valueOf(ANOTHER_ORDER_ID);

    @BeforeEach
    public void setup() {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement()
                    .partner(
                        TransportationPartnerExtendedInfoDto.builder()
                            .id(TMFactory.THIRD_PARTNER_ID)
                            .legalName("legal " + TMFactory.THIRD_PARTNER_NAME)
                            .legalAddress(TMFactory.PARTNER_ADDRESS)
                            .legalType(TMFactory.PARTNER_TYPE)
                            .build()
                    )
                    .build(),
                TMFactory.defaultInbound().build()
            ));

        TMFactory.mockOutboundUnits(transportManagerClient, List.of(
            RegisterUnitDto.builder()
                .partialIds(List.of(
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value(ORDER_ID).build()
                ))
                .build()
        ));

        mockWarehouses();
    }

    @AfterEach
    public void verifyMocks() {
        verifyNoMoreInteractions(wwClient, outboundApi);
    }

    @Test
    @DisplayName("Успешно сгенерирована накладная в WW новый формат")
    void generateTransportationWaybillSuccessNewFormat() throws Exception {
        WaybillInformation waybillInformationData = createWaybillInformationData("external-id");
        when(wwClient.generateWaybill(any())).thenReturn(BYTES);
        mockTransportationUnit();
        mockSearchOrders(createLomOrders(List.of(
            createPlaceUnitBuilder().build(),
            createRootUnit()
        )));

        generateTransportationWaybill(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX_MIME_TYPE))
            .andExpect(content().bytes(BYTES));

        verify(wwClient).generateWaybill(waybillInformationData);
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Успешно сгенерирована накладная. Список shopId")
    void generateTransportationWaybillSuccessWithShopIds() throws Exception {
        WaybillInformation waybillInformationData = createWaybillInformationData("external-id");
        when(wwClient.generateWaybill(any())).thenReturn(BYTES);
        mockTransportationUnit();
        mockSearchOrders(createLomOrders(List.of(
            createPlaceUnitBuilder().build(),
            createRootUnit()
        )));

        mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
                .param("userId", "-1")
                .param("shopIds", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
            )
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX_MIME_TYPE))
            .andExpect(content().bytes(BYTES));

        verify(wwClient).generateWaybill(waybillInformationData);
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Ошибка генерации накладной. Пустой shopId")
    void generateTransportationWaybillWithOutShopIds() throws Exception {
        ValidationErrorData.ValidationErrorDataBuilder validationError =
            ValidationErrorData.objectErrorBuilder(
                "Must specify either shopId or shopIds",
                "ValidShopIds"
            );
        mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
                .param("userId", "-1")
            )
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(validationErrorMatcher(validationError.forObject("shopIdsHolder")));
    }

    @Test
    @DisplayName("Отгрузка не найдена")
    void outboundNotFound() throws Exception {
        mockEmptyOutbounds();

        WaybillInformation waybillInformationData = createWaybillInformationData("500");
        when(wwClient.generateWaybill(any())).thenReturn(BYTES);

        generateTransportationWaybill(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX_MIME_TYPE))
            .andExpect(content().bytes(BYTES));

        verify(wwClient).generateWaybill(waybillInformationData);
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Отгрузка не подтверждена")
    void outboundNotConfirmed() throws Exception {
        WaybillInformation emptyOrdersInfo = createWaybillInformationData("500");
        mockOutbounds();
        when(wwClient.generateWaybill(any())).thenReturn(BYTES);

        generateTransportationWaybill(SHOP_ID)
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX_MIME_TYPE))
            .andExpect(content().bytes(BYTES));

        verify(wwClient).generateWaybill(emptyOrdersInfo);
        verifyGetOutbounds();
    }

    @Test
    @DisplayName("Отгрузка не заборная")
    void outboundNotWithdraw() throws Exception {
        mockTransportationUnit();
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build(),
                TMFactory.defaultInbound().build()
            ));
        generateTransportationWaybill(SHOP_ID)
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(validationErrorMatcher(ValidationErrorData.objectError(
                "shipment",
                "must be withdraw",
                "ValidShipmentType"
            )));
    }

    @Test
    @DisplayName("Неверный идентификатор магазина")
    void wrongShopId() throws Exception {
        generateTransportationWaybill(SECOND_SHOP_ID)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20]"));
    }

    @Test
    @DisplayName("Перемещение не найдено")
    void transportationNotFound() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(Optional.empty());

        generateTransportationWaybill(SHOP_ID)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(resourceNotFoundMatcher(ResourceType.PARTNER_SHIPMENT, List.of(TMFactory.SHIPMENT_ID)));
    }

    @Nonnull
    private List<OrderDto> createLomOrders(List<StorageUnitDto> units) {
        return List.of(
            new OrderDto()
                .setExternalId(ORDER_ID)
                .setBarcode("barcode-123")
                .setCost(
                    createLomOrderCost()
                        .assessedValue(BigDecimal.valueOf(1000))
                        .build()
                )
                .setItems(List.of(
                    createLomItemBuilder().build()
                ))
                .setUnits(units)
                .setWaybill(List.of(
                    WaybillSegmentDto.builder()
                        .externalId("external-id-1")
                        .partnerId(TMFactory.PARTNER_ID)
                        .build()
                )),
            new OrderDto()
                .setExternalId(ANOTHER_EXTERNAL_ID)
                .setBarcode("barcode-456")
                .setCost(
                    createLomOrderCost()
                        .assessedValue(BigDecimal.valueOf(1000))
                        .build()
                )
                .setItems(List.of(
                    createLomItemBuilder().build()
                ))
                .setUnits(units)
                .setWaybill(List.of(
                    WaybillSegmentDto.builder()
                        .externalId("external-id-2")
                        .partnerId(TMFactory.PARTNER_ID)
                        .build()
                ))
        );
    }

    @Nonnull
    private WaybillInformation createWaybillInformationData(String externalId) {
        return WaybillInformation.builder()
            .id(externalId)
            .date(LocalDate.of(2021, 3, 4))
            .receptionTransferAct(ReceptionTransferAct.builder()
                .id(externalId)
                .date(LocalDate.of(2021, 3, 4))
                .build()
            )
            .payer(Payer.builder()
                .organizationName("ООО «ЯНДЕКС»")
                .address("119021, Россия, г. Москва, ул. Льва Толстого, д. 16")
                .account("40702810438000034726")
                .bankName("ПАО «Сбербанк»")
                .bic("044525225")
                .correspondentAccount("30101810400000000225")
                .build()
            )
            .sender(LegalInfo.builder()
                .legalName("legal Какой-то партнёр")
                .legalAddress("Какой-то адрес")
                .build()
            )
            .receiver(LegalInfo.builder()
                .legalName("legal Второй партнёр")
                .legalAddress("Какой-то адрес")
                .build()
            )
            .inbound(TransportationUnit.builder()
                .address("Другой адрес")
                .legalName("legal Второй партнёр")
                .build()
            )
            .outbound(TransportationUnit.builder()
                .address("Один адрес")
                .legalName("legal Какой-то партнёр")
                .plannedIntervalStart(LocalDateTime.of(2021, 3, 4, 10, 21))
                .build()
            )
            .transporter(LegalInfo.builder().build())
            .car(Car.builder().build())
            .cargo(Cargo.builder().name("Товарно-материальные ценности").build())
            .courier(Courier.builder().build())
            .build();
    }

    @Nonnull
    protected abstract String url(long shipmentId);

    @Nonnull
    private ResultActions generateTransportationWaybill(long shopId) throws Exception {
        return mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
            .param("userId", "-1")
            .param("shopId", String.valueOf(shopId))
        );
    }

    protected abstract void verifyGetOutbounds();

    protected abstract void mockOutbounds();

    protected abstract void mockEmptyOutbounds();
}
