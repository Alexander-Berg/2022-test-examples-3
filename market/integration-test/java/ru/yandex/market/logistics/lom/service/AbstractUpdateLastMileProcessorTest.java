package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.lom.AbstractContextualYdbTest;
import ru.yandex.market.logistics.lom.entity.WaybillSegment;
import ru.yandex.market.logistics.lom.entity.combinator.embedded.CombinedRoute;
import ru.yandex.market.logistics.lom.entity.enums.ApiType;
import ru.yandex.market.logistics.lom.entity.enums.CampaignType;
import ru.yandex.market.logistics.lom.entity.enums.SegmentType;
import ru.yandex.market.logistics.lom.filter.WaybillSegmentSearchFilter;
import ru.yandex.market.logistics.lom.jobs.consumer.CommitOrderConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.ConvertRouteToWaybillConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.OrderExternalValidationConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.ProcessCreateOrderAsyncSuccessResultConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.WaybillCreateOrderConsumer;
import ru.yandex.market.logistics.lom.jobs.consumer.order.create.DeliveryServiceCreateOrderExternalConsumer;
import ru.yandex.market.logistics.lom.jobs.model.ChangeOrderRequestPayload;
import ru.yandex.market.logistics.lom.jobs.model.CreateOrderSuccessPayload;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdWaybillSegmentPayload;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResult;
import ru.yandex.market.logistics.lom.jobs.model.ProcessingResultStatus;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.model.async.CreateOrderSuccessDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerSubtype;
import ru.yandex.market.logistics.lom.repository.ydb.BusinessProcessStateStatusHistoryYdbRepository;
import ru.yandex.market.logistics.lom.repository.ydb.description.BusinessProcessStateStatusHistoryTableDescription;
import ru.yandex.market.logistics.lom.repository.ydb.description.OrderCombinedRouteHistoryTableDescription;
import ru.yandex.market.logistics.lom.service.marketid.MarketIdService;
import ru.yandex.market.logistics.lom.service.waybill.TransferCodesService;
import ru.yandex.market.logistics.lom.service.waybill.WaybillSegmentService;
import ru.yandex.market.logistics.lom.utils.MarketIdFactory;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.market.logistics.lom.utils.ydb.converter.OrderCombinedRouteHistoryYdbConverter;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public abstract class AbstractUpdateLastMileProcessorTest extends AbstractContextualYdbTest {

    protected static final UUID NEW_SAVED_UUID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    protected static final ChangeOrderRequestPayload CHANGE_ORDER_REQUEST_PAYLOAD =
        PayloadFactory.createChangeOrderRequestPayload(101, null);

    protected static final Long ITEMS_SUPPLIER_PARTNER_ID = 1L;
    protected static final Long SENDER_PARTNER_ID = 1020304L;
    protected static final Long SENDER_MARKET_ID = 10203040506L;
    protected static final Long FF_PARTNER_ID = 172L;
    protected static final Long FF_MARKET_ID = 350L;
    protected static final Long SC_MARKET_ID = 3035012L;
    protected static final Long PICKUP_PARTNER_ID = 1005720L;
    protected static final Long PICKUP_MARKET_ID = 2025012L;
    protected static final Long PICKUP_LOGISTICS_POINT_ID = 10001660932L;
    protected static final PartnerResponse PICKUP_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(PICKUP_PARTNER_ID)
        .marketId(PICKUP_MARKET_ID)
        .name("Маркет ПВЗ")
        .readableName("Маркет ПВЗ")
        .billingClientId(58873179L)
        .domain("https://pokupki.market.yandex.ru")
        .partnerType(PartnerType.DELIVERY)
        .subtype(PartnerSubtypeResponse.newBuilder()
            .id(3)
            .name(PartnerSubtype.MARKET_OWN_PICKUP_POINT.name())
            .build()
        )
        .params(List.of(
            new PartnerExternalParam("CAN_UPDATE_INSTANCES", null, "true"),
            new PartnerExternalParam("IS_DROPOFF", null, "false"),
            new PartnerExternalParam("DROPSHIP_EXPRESS", null, "false"),
            new PartnerExternalParam("RECIPIENT_UID_ENABLED", null, "false"),
            new PartnerExternalParam("UPDATE_COURIER_NEEDED", null, "false"),
            new PartnerExternalParam("UPDATE_INSTANCES_ENABLED", null, "false"),
            new PartnerExternalParam("ASSESSED_VALUE_TOTAL_CHECK", null, "false"),
            new PartnerExternalParam("ITEM_REMOVING_ENABLED", null, "false"),
            new PartnerExternalParam("INBOUND_VERIFICATION_CODE_REQUIRED", null, "false"),
            new PartnerExternalParam("OUTBOUND_VERIFICATION_CODE_REQUIRED", null, "true"),
            new PartnerExternalParam("ELECTRONIC_ACCEPTANCE_CERTIFICATE_REQUIRED", null, "false")
        ))
        .build();
    protected static final LogisticsPointResponse PICKUP_LOGISTICS_POINT_RESPONSE = LogisticsPointResponse.newBuilder()
        .id(PICKUP_LOGISTICS_POINT_ID)
        .type(PointType.PICKUP_POINT)
        .partnerId(PICKUP_PARTNER_ID)
        .schedule(Set.of(new ScheduleDayResponse(101L, 1, LocalTime.MIDNIGHT, LocalTime.NOON, true)))
        .address(Address.newBuilder()
            .region("Москва")
            .street("Попутная улица")
            .country("Россия")
            .postCode("119619")
            .latitude(BigDecimal.valueOf(55.654446))
            .longitude(BigDecimal.valueOf(37.38676))
            .settlement("Москва")
            .locationId(213)
            .house("6")
            .build())
        .instruction("ПВЗ расположен в магазине продуктов Fix Price")
        .externalId("3257")
        .phones(Set.of(Phone.newBuilder().number("8 (800) 234-27-12").build()))
        .build();
    protected static final LegalInfo PICKUP_LEGAL_INFO = LegalInfo.newBuilder()
        .setInn("7736207543")
        .setLegalName("ЯНДЕКС")
        .setRegistrationNumber("1027700229193")
        .setLegalAddress("119021, г. Москва, ул. Льва Толстого, д. 16")
        .setType("OOO")
        .build();
    protected static final MarketAccount PICKUP_MARKET_ACCOUNT = MarketAccount.newBuilder()
        .setMarketId(PICKUP_MARKET_ID)
        .setLegalInfo(PICKUP_LEGAL_INFO)
        .build();
    protected static final PartnerResponse FF_PARTNER_RESPONSE = PartnerResponse.newBuilder()
        .id(FF_PARTNER_ID)
        .marketId(FF_MARKET_ID)
        .partnerType(PartnerType.FULFILLMENT)
        .build();
    protected static final MarketAccount SENDER_MARKET_ACCOUNT = MarketIdFactory.marketAccount(
        SENDER_MARKET_ID,
        MarketIdFactory.legalInfoBuilder().build()
    );
    protected static final MarketAccount FF_MARKET_ACCOUNT = MarketIdFactory.marketAccount(
        FF_MARKET_ID,
        MarketIdFactory.legalInfoBuilder().build()
    );
    protected static final MarketAccount SC_MARKET_ACCOUNT = MarketIdFactory.marketAccount(
        SC_MARKET_ID,
        MarketIdFactory.legalInfoBuilder().build()
    );
    protected static final PartnerInfoDTO SENDER_PARTNER_INFO_DTO = new PartnerInfoDTO(
        SENDER_PARTNER_ID,
        null,
        ru.yandex.market.core.campaign.model.CampaignType.SUPPLIER,
        null,
        null,
        "88005553535",
        null,
        null,
        true,
        null
    );
    protected static final Address FF_ADDRESS = Address.newBuilder()
        .region("Москва")
        .street("Запутанная улица")
        .country("Россия")
        .postCode("119621")
        .latitude(BigDecimal.valueOf(57.654446))
        .longitude(BigDecimal.valueOf(39.38676))
        .settlement("Москва")
        .locationId(215)
        .house("6")
        .build();
    protected static final Address SC_ADDRESS = Address.newBuilder()
        .region("Москва")
        .street("Непопутная улица")
        .country("Россия")
        .postCode("119620")
        .latitude(BigDecimal.valueOf(56.654446))
        .longitude(BigDecimal.valueOf(38.38676))
        .settlement("Москва")
        .locationId(214)
        .house("6")
        .build();

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected MbiApiClient mbiApiClient;

    @Autowired
    protected MarketIdService marketIdService;

    @Autowired
    protected WaybillSegmentService waybillSegmentService;

    @Autowired
    private TransferCodesService transferCodesService;

    @Autowired
    protected ConvertRouteToWaybillConsumer convertRouteToWaybillConsumer;

    @Autowired
    protected CommitOrderConsumer commitOrderConsumer;

    @Autowired
    protected TransactionTemplate transactionTemplate;

    @Autowired
    protected OrderExternalValidationConsumer orderExternalValidationConsumer;

    @Autowired
    protected WaybillCreateOrderConsumer waybillCreateOrderConsumer;

    @Autowired
    protected DeliveryServiceCreateOrderExternalConsumer deliveryServiceCreateOrderExternalConsumer;

    @Autowired
    protected ProcessCreateOrderAsyncSuccessResultConsumer processCreateOrderAsyncSuccessResultConsumer;

    @Autowired
    protected BusinessProcessStateStatusHistoryYdbRepository businessProcessStateStatusHistoryYdbRepository;

    @Autowired
    protected OrderCombinedRouteHistoryTableDescription routeHistoryTable;

    @Autowired
    protected BusinessProcessStateStatusHistoryTableDescription businessProcessStateStatusHistoryTableDescription;

    @Autowired
    protected OrderCombinedRouteHistoryYdbConverter converter;

    @BeforeEach
    void setUpUpdateLastMileProcessorTest() {
        doNothing().when(businessProcessStateStatusHistoryYdbRepository).save(any());
        when(transferCodesService.generateCode()).thenReturn("54321");
        clock.setFixed(Instant.parse("2021-03-09T12:30:00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDownUpdateLastMileProcesorTest() {
        verifyNoMoreInteractions(lmsClient, marketIdService, mbiApiClient);
    }

    protected void processWaybillCreateOrder(
        Long waybillSegmentId,
        Long sequenceId,
        String requestId,
        String externalId
    ) {
        waybillCreateOrderConsumer.execute(TaskFactory.createTask(queueTaskChecker.getProducedTaskPayload(
            QueueType.PROCESS_WAYBILL_CREATE_ORDER,
            OrderIdWaybillSegmentPayload.class
        )));
        OrderIdWaybillSegmentPayload createOrderExternalPayload = new OrderIdWaybillSegmentPayload(
            requestId,
            1,
            waybillSegmentId
        );
        createOrderExternalPayload.setSequenceId(sequenceId);
        deliveryServiceCreateOrderExternalConsumer.execute(TaskFactory.createTask(
            QueueType.CREATE_ORDER_EXTERNAL,
            createOrderExternalPayload
        ));
        CreateOrderSuccessPayload createOrderAsyncSuccessPayload = new CreateOrderSuccessPayload(
            requestId + "/1",
            ApiType.DELIVERY,
            SENDER_PARTNER_ID,
            waybillSegmentId,
            1L,
            new CreateOrderSuccessDto(
                externalId,
                2L,
                "LOinttest-1",
                sequenceId
            )
        );
        createOrderAsyncSuccessPayload.setSequenceId(sequenceId);
        processCreateOrderAsyncSuccessResultConsumer.execute(TaskFactory.createTask(
            QueueType.PROCESS_CREATE_ORDER_ASYNC_SUCCESS_RESULT,
            createOrderAsyncSuccessPayload
        ));
    }

    protected void mockUpdateLastMileToPickup() {
        when(lmsClient.getPartner(PICKUP_PARTNER_ID)).thenReturn(Optional.of(PICKUP_PARTNER_RESPONSE));
        when(marketIdService.findLegalInfoByMarketIdOrThrow(PICKUP_MARKET_ID)).thenReturn(PICKUP_LEGAL_INFO);
        when(lmsClient.getLogisticsPoint(PICKUP_LOGISTICS_POINT_ID))
            .thenReturn(Optional.of(PICKUP_LOGISTICS_POINT_RESPONSE));
        insertAllIntoTable(
            routeHistoryTable,
            List.of(createCombinedRoute(
                "orders/update_last_mile_to_pickup/before/combined_route_pickup.json",
                NEW_SAVED_UUID
            )),
            converter::mapToItem
        );
    }

    protected void verifyUpdateLastMileToPickup(ProcessingResult processingResult) {
        queueTaskChecker.assertQueueTaskCreated(
            QueueType.CHANGE_ORDER_REQUEST,
            PayloadFactory.createChangeOrderRequestPayload(101, "1", 1)
        );
        softly.assertThat(processingResult.getStatus()).isEqualTo(ProcessingResultStatus.SUCCESS);
        verify(lmsClient).getPartner(PICKUP_PARTNER_ID);
        verify(marketIdService).findLegalInfoByMarketIdOrThrow(PICKUP_MARKET_ID);
        verify(lmsClient).getLogisticsPoint(PICKUP_LOGISTICS_POINT_ID);
    }

    @Nonnull
    protected WaybillSegment findSegmentByOrderIdAndSegmentType(Long orderId, SegmentType segmentType) {
        return waybillSegmentService.search(
            WaybillSegmentSearchFilter.builder()
                .orderId(orderId)
                .segmentType(segmentType)
                .build(),
            Pageable.unpaged()
        )
            .getContent()
            .get(0);
    }

    protected void assertWaybillSegmentEquallyEnriched(WaybillSegment segmentA, WaybillSegment segmentB) {
        softly.assertThat(segmentA.getPartnerType()).isEqualTo(segmentB.getPartnerType());
        softly.assertThat(segmentA.getPartnerSubtype()).isEqualTo(segmentB.getPartnerSubtype());
        softly.assertThat(segmentA.getPartnerId()).isEqualTo(segmentB.getPartnerId());
        softly.assertThat(segmentA.getWaybillShipment()).usingRecursiveComparison()
            .isEqualTo(segmentB.getWaybillShipment());
        softly.assertThat(segmentA.getPartnerSettings()).isEqualTo(segmentB.getPartnerSettings());
        softly.assertThat(segmentA.getPartnerInfo()).usingRecursiveComparison().isEqualTo(segmentB.getPartnerInfo());
    }

    protected void mockCommonExternalForNewOrderCreation() {
        when(mbiApiClient.getPartnerInfo(SENDER_PARTNER_ID)).thenReturn(SENDER_PARTNER_INFO_DTO);
        when(marketIdService.findAccountByPartnerIdAndPartnerType(SENDER_PARTNER_ID, CampaignType.SUPPLIER))
            .thenReturn(Optional.of(SENDER_MARKET_ACCOUNT));
        when(marketIdService.findAccountById(FF_MARKET_ID)).thenReturn(Optional.of(FF_MARKET_ACCOUNT));
        when(marketIdService.findAccountById(SC_MARKET_ID)).thenReturn(Optional.of(SC_MARKET_ACCOUNT));
    }

    protected void verifyCommonExternalAfterNewOrderCreation() {
        verify(marketIdService, times(2)).findAccountById(FF_MARKET_ID);
        verify(marketIdService, times(2)).findAccountById(SC_MARKET_ID);

        verify(marketIdService).findAccountByPartnerIdAndPartnerType(SENDER_PARTNER_ID, CampaignType.SUPPLIER);
        verify(marketIdService).findAccountByPartnerIdAndPartnerType(ITEMS_SUPPLIER_PARTNER_ID, CampaignType.SUPPLIER);

        verify(mbiApiClient).getPartnerInfo(SENDER_PARTNER_ID);
        verify(mbiApiClient).getPartnerInfo(ITEMS_SUPPLIER_PARTNER_ID);
    }

    @Nonnull
    @SneakyThrows
    protected CombinedRoute createCombinedRoute(String path, UUID routeUuid) {
        return new CombinedRoute()
            .setOrderId(101L)
            .setSourceRoute(objectMapper.readTree(
                extractFileContent(path)
            ))
            .setRouteUuid(routeUuid);
    }
}
