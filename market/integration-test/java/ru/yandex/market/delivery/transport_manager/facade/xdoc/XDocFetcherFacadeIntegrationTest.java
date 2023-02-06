package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.config.caledaring_service.CalendaringServiceClientConfig;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocCreateData;
import ru.yandex.market.delivery.transport_manager.service.external.marketd.MarketIdService;
import ru.yandex.market.delivery.transport_manager.util.XDocTestingConstants;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class XDocFetcherFacadeIntegrationTest extends AbstractContextualTest {

    @Autowired
    private FulfillmentWorkflowClientApi ffwfClient;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private CalendaringServiceClientApi csClient;

    @Autowired
    private MarketIdService marketIdService;

    @Autowired
    private XDocFetcherFacade xDocFetcherFacade;

    private MarketAccount marketAccount;
    private LogisticsPointFilter xDocLogisticPointFilter;
    private LogisticsPointFilter targetLogisticsPointFilter;

    private ShopRequestDetailsDTO shopRequestDetails1pDTO;
    private ShopRequestDetailsDTO shopRequestDetailsBreakBulkXDockDTO;
    private BookingResponseV2 bookingResponse;

    @BeforeEach
    void setUp() {
        marketAccount = MarketAccount.newBuilder()
            .setMarketId(XDocTestingConstants.SUPPLIER_MARKET_ID)
            .setLegalInfo(
                LegalInfo.newBuilder()
                    .setInn("inn")
                    .setRegistrationNumber("ogrn")
                    .setLegalName("Name")
                    .setType("OOO")
                    .setLegalAddress("Address")
                    .build()
            )
            .build();

        xDocLogisticPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(XDocTestingConstants.X_DOC_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        targetLogisticsPointFilter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(XDocTestingConstants.TARGET_PARTNER_ID))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();

        this.shopRequestDetails1pDTO = XDocTestingConstants.createShopRequestDetails1pDTO();
        this.shopRequestDetailsBreakBulkXDockDTO = XDocTestingConstants.createShopRequestDetailsBreakBulkXDockDTO();
        bookingResponse = new BookingResponseV2(
            1L,
            CalendaringServiceClientConfig.SOURCE,
            Objects.toString(XDocTestingConstants.REQUEST_ID),
            null,
            1L,
            XDocTestingConstants.X_DOC_REQUESTED_DATE.atZone(ZoneId.systemDefault()),
            XDocTestingConstants.X_DOC_REQUESTED_DATE.atZone(ZoneId.systemDefault()).plusMinutes(
                XDocTestingConstants.SLOT_SIZE_MINUTES),
            BookingStatus.ACTIVE,
            clock.instant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
            100L
        );
    }

    @DisplayName("Получение из других систем всех нужных данных для создания перемещений")
    @Test
    void fetchXDocTransportationsData() {
        testFetchXDocTransportationsData(
            shopRequestDetails1pDTO,
            null,
            XDocTestingConstants.X_DOC_CREATE_DATA_1P,
            bookingResponse
        );
    }

    @DisplayName("Получение из других систем всех нужных данных для создания перемещений Break Bulk XDock")
    @Test
    void fetchBreakBulkXDocTransportationsData() {
        ShopRequestDetailsDTO parentShopRequest = new ShopRequestDetailsDTO();
        parentShopRequest.setServiceRequestId(
            XDocTestingConstants.X_DOC_CREATE_DATA_BREAK_BULK_XDOCK.getParentServiceRequestId()
        );

        testFetchXDocTransportationsData(
            shopRequestDetailsBreakBulkXDockDTO,
            parentShopRequest,
            XDocTestingConstants.X_DOC_CREATE_DATA_BREAK_BULK_XDOCK,
            null
        );
    }

    private void testFetchXDocTransportationsData(
        ShopRequestDetailsDTO shopRequestDetailsDTO,
        ShopRequestDetailsDTO parentShopRequestDetailsDTO,
        XDocCreateData expectedXDocCreateData,
        BookingResponseV2 bookingResponse
    ) {
        initFetchXDocDataMocks(shopRequestDetailsDTO, parentShopRequestDetailsDTO);

        if (bookingResponse != null) {
            when(csClient.getSlotByExternalIdentifiersV2(
                eq(Set.of(Objects.toString(XDocTestingConstants.REQUEST_ID))),
                eq("FFWF"),
                eq(BookingStatus.ACTIVE)
            )).thenReturn(new BookingListResponseV2(List.of(
                bookingResponse
            )));
        }

        XDocCreateData xDocCreateData =
            xDocFetcherFacade.fetchXDocTransportationsData(XDocTestingConstants.REQUEST_ID);

        assertThatModelEquals(expectedXDocCreateData, xDocCreateData);

        verify(ffwfClient).getRequest(eq(XDocTestingConstants.REQUEST_ID));
        if (shopRequestDetailsDTO.getSupplyRequestId() != null) {
            verify(ffwfClient).getRequest(eq(shopRequestDetailsDTO.getSupplyRequestId()));
        }
        verify(lmsClient).getLogisticsPoints(eq(
            xDocLogisticPointFilter
        ));
        verify(lmsClient).getLogisticsPoints(eq(
            targetLogisticsPointFilter
        ));
        verify(marketIdService).findAccountByMbiSupplierId(XDocTestingConstants.SUPPLIER_ID);

        if (bookingResponse != null) {
            verify(csClient).getSlotByExternalIdentifiersV2(
                eq(Set.of(Objects.toString(XDocTestingConstants.REQUEST_ID))),
                eq("FFWF"),
                eq(BookingStatus.ACTIVE)
            );
        }

        verifyNoMoreInteractions(marketIdService, ffwfClient, lmsClient, csClient);
    }

    @DisplayName("Получение из других систем всех нужных данных для создания перемещений: нет слота")
    @Test
    void fetchXDocTransportationsDataNoSlot() {
        initFetchXDocDataMocks(shopRequestDetails1pDTO, null);

        XDocCreateData xDocCreateData =
            xDocFetcherFacade.fetchXDocTransportationsData(XDocTestingConstants.REQUEST_ID);

        softly.assertThat(xDocCreateData.getSlot()).isNull();
    }

    private void initFetchXDocDataMocks(ShopRequestDetailsDTO shopRequestDetailsDTO, ShopRequestDetailsDTO parent) {
        when(ffwfClient.getRequest(eq(XDocTestingConstants.REQUEST_ID))).thenReturn(shopRequestDetailsDTO);
        if (shopRequestDetailsDTO.getSupplyRequestId() != null) {
            when(ffwfClient.getRequest(shopRequestDetailsDTO.getSupplyRequestId())).thenReturn(parent);
        }
        when(lmsClient.getLogisticsPoints(eq(xDocLogisticPointFilter))).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(XDocTestingConstants.X_DOC_WAREHOUSE_ID)
                .name(XDocTestingConstants.X_DOC_WAREHOUSE_NAME)
                .handlingTime(XDocTestingConstants.X_DOC_HANDLING_TIME)
                .build()
        ));
        when(lmsClient.getLogisticsPoints(eq(targetLogisticsPointFilter))).thenReturn(List.of(
            LogisticsPointResponse.newBuilder()
                .id(XDocTestingConstants.TARGET_WAREHOUSE_ID)
                .name(XDocTestingConstants.TARGET_WAREHOUSE_NAME)
                .build()
        ));

        when(marketIdService.findAccountByMbiSupplierId(XDocTestingConstants.SUPPLIER_ID))
            .thenReturn(Optional.of(marketAccount));
    }

}
