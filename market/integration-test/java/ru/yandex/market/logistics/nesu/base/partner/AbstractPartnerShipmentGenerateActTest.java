package ru.yandex.market.logistics.nesu.base.partner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.delivery.transport_manager.model.dto.PartialIdDto;
import ru.yandex.market.delivery.transport_manager.model.dto.RegisterUnitDto;
import ru.yandex.market.delivery.transport_manager.model.enums.IdType;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;
import ru.yandex.market.logistics.lom.model.filter.OrderSearchFilter;
import ru.yandex.market.logistics.lom.model.search.Pageable;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.nesu.model.CheckouterFactory;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;

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
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

public abstract class AbstractPartnerShipmentGenerateActTest extends AbstractPartnerShipmentTest {

    protected static final Long INBOUND_ID = 300L;
    protected static final Long INBOUND_FACT_REGISTER_ID = 1100L;

    private final RequestClientInfo requestClientInfo = RequestClientInfo.builder(ClientRole.SHOP)
        .withClientIds(Set.of(SHOP_ID))
        .build();
    private final OrderSearchRequest orderSearchRequest = CheckouterFactory.checkouterSearchRequest(
        CHECKOUTER_ORDER_ID
    );

    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    public void setup() {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(TMFactory.transportation(
                TMFactory.defaultOutbound()
                    .externalId("external-id")
                    .registers(List.of(TMFactory.outboundRegister().build()))
                    .build(),
                TMFactory.defaultMovement().build(),
                TMFactory.defaultInbound()
                    .id(INBOUND_ID)
                    .externalId("external-id")
                    .partner(TMFactory.transportationPartner(null, "Получатель"))
                    .registers(List.of(TMFactory.inboundRegister().build()))
                    .build()
            ));

        TMFactory.mockOutboundUnits(transportManagerClient, List.of(
            RegisterUnitDto.builder()
                .partialIds(List.of(
                    PartialIdDto.builder().idType(IdType.ORDER_ID).value(ORDER_ID).build()
                ))
                .build()
        ));

        Order checkouterOrder = CheckouterFactory.checkouterOrder(CHECKOUTER_ORDER_ID, OrderSubstatus.SHIPPED);
        when(checkouterAPI.getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest)))
            .thenReturn(new PagedOrders(List.of(checkouterOrder), null));
    }

    @AfterEach
    public void verifyMocks() {
        verifyNoMoreInteractions(wwClient, lomClient, checkouterAPI, outboundApi);
    }

    @Test
    @DisplayName("Успешно сгенерирован АПП в WW")
    void generateReceptionTransferAct() throws Exception {
        mockTransportationUnit();
        mockSearchOrders(createLomOrders(BigDecimal.valueOf(100)));

        try (var ignored = mockGenerateAct(createRtaOrdersData())) {
            generateShipmentAct(SHOP_ID)
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(status().isOk())
                .andExpect(content().bytes(BYTES));
        }

        verifyCheckouterGetOrders();
        verifyLomGetOrders();
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Успешно сгенерирован АПП. Список shopId")
    void generateReceptionTransferActWithShopIds() throws Exception {
        mockTransportationUnit();
        mockSearchOrders(createLomOrders(BigDecimal.valueOf(100)));

        try (var ignored = mockGenerateAct(createRtaOrdersData())) {
            mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
                    .param("userId", "-1")
                    .param("shopId", String.valueOf(SHOP_ID), String.valueOf(SECOND_SHOP_ID))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(BYTES));
        }

        verifyCheckouterGetOrders();
        verifyLomGetOrders();
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Ошибка генерации АПП. Пустой shopId")
    void generateReceptionTransferActWithOutShopIds() throws Exception {
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
    @DisplayName("Вэйбилла нет в заказе для отгружающего партнера")
    void waybillNotFoundForOutboundPartner() throws Exception {
        mockTransportationUnit();
        mockSearchOrders(List.of(
            new OrderDto()
                .setExternalId(ORDER_ID)
                .setWaybill(List.of(WaybillSegmentDto.builder().partnerId(123L).build()))
        ));

        generateShipmentAct(SHOP_ID)
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(errorMessage(String.format("Waybill not found for outbound partner %s", TMFactory.PARTNER_ID)));

        verifyCheckouterGetOrders();
        verifyLomGetOrders();
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Коробки из чекаутера")
    void checkouterPlaces() throws Exception {
        mockTransportationUnit();
        mockSearchOrders(createLomOrders(BigDecimal.valueOf(100)));

        try (var ignored = mockGenerateAct(createRtaOrdersData())) {
            generateShipmentAct(SHOP_ID)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(BYTES));
        }

        verifyCheckouterGetOrders();
        verifyLomGetOrders();
        verifyGetOutbound();
    }

    @Test
    @DisplayName("Неверный идентификатор магазина")
    void wrongShopId() throws Exception {
        generateShipmentAct(SECOND_SHOP_ID)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find dropship partner for shops [20]"));
    }

    @Test
    @DisplayName("Перемещение не найдено")
    void transportationNotFound() throws Exception {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID))
            .thenReturn(Optional.empty());

        generateShipmentAct(SHOP_ID)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(resourceNotFoundMatcher(ResourceType.PARTNER_SHIPMENT, List.of(TMFactory.SHIPMENT_ID)));
    }

    @Nonnull
    private List<OrderDto> createLomOrders(BigDecimal itemsSum) {
        return createLomOrders(TMFactory.PARTNER_ID, itemsSum);
    }

    @Nonnull
    protected List<OrderDto> createLomOrders(long partnerId, BigDecimal itemsSum) {
        return List.of(
            new OrderDto()
                .setExternalId(ORDER_ID)
                .setBarcode("barcode-123")
                .setCost(createLomOrderCost().itemsSum(itemsSum).build())
                .setItems(List.of(
                    createLomItemBuilder().build()
                ))
                .setUnits(List.of(
                    createRootUnit(),
                    createPlaceUnitBuilder().build()
                ))
                .setWaybill(List.of(
                    WaybillSegmentDto.builder()
                        .externalId("external-id-1")
                        .partnerId(partnerId)
                        .build()
                ))
        );
    }

    @Nonnull
    protected abstract AutoCloseable mockGenerateAct(RtaOrdersData data);

    @Nonnull
    private RtaOrdersData createRtaOrdersData() {
        return createRtaOrdersData(TMFactory.PARTNER_NAME, BigDecimal.valueOf(100));
    }

    @Nonnull
    private RtaOrdersData createRtaOrdersData(@Nullable BigDecimal itemsSum) {
        return createRtaOrdersData(TMFactory.PARTNER_NAME, itemsSum);
    }

    @Nonnull
    protected RtaOrdersData createRtaOrdersData(String partnerName, @Nullable BigDecimal itemsSum) {
        return RtaOrdersData.builder()
            .actId(String.valueOf(TMFactory.SHIPMENT_ID))
            .shipmentId("external-id")
            .shipmentDate(LocalDate.of(2021, 3, 4))
            .senderLegalName("legal " + partnerName)
            .partnerLegalName("legal Получатель")
            .senderId(String.valueOf(SHOP_ID))
            .orders(createDocOrders(itemsSum))
            .build();
    }

    @Nonnull
    private List<DocOrder> createDocOrders(@Nullable BigDecimal itemsSum) {
        return List.of(
            DocOrder.builder()
                .yandexId("barcode-123")
                .partnerId("external-id-1")
                .assessedCost(BigDecimal.valueOf(125))
                .itemsSum(itemsSum)
                .weight(BigDecimal.valueOf(100))
                .placesCount(1)
                .build()
        );
    }

    @Nonnull
    protected abstract String url(long shipmentId);

    @Nonnull
    protected ResultActions generateShipmentAct(long shopId) throws Exception {
        return mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
            .param("userId", "-1")
            .param("shopId", String.valueOf(shopId))
        );
    }

    protected void verifyCheckouterGetOrders() {
        verify(checkouterAPI).getOrders(safeRefEq(requestClientInfo), safeRefEq(orderSearchRequest));
    }

    protected void verifyLomGetOrders() {
        verify(lomClient).searchOrders(
            safeRefEq(OrderSearchFilter.builder()
                .platformClientId(1L)
                .externalIds(Set.of(ORDER_ID))
                .build()),
            safeRefEq(Pageable.unpaged())
        );
    }

    protected void verifyGetOutbound() {
    }
}
