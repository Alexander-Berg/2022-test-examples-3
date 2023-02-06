package ru.yandex.market.delivery.transport_manager.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.delivery.transport_manager.converter.ffwf.RequestSubtypeIds;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocCreateData;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDockTimeSlotDto;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.enums.RequestType;

public interface XDocTestingConstants {
    Duration X_DOC_HANDLING_TIME = Duration.ofDays(2);
    Duration LEAD_TIME = Duration.ofDays(3);

    long REQUEST_ID = 123;
    Long X_DOC_PARTNER_ID = 147L;
    Long X_DOC_WAREHOUSE_ID = 1000001L;
    String X_DOC_WAREHOUSE_NAME = "DC_1";
    Long X_DOC_MARKET_ID = 147000L;
    Long SUPPLIER_ID = 1234L;
    long SUPPLIER_MARKET_ID = 12345678L;
    Long TARGET_PARTNER_ID = 172L;
    Long TARGET_WAREHOUSE_ID = 1000002L;
    String TARGET_WAREHOUSE_NAME = "FF_1";
    String EXTERNAL_REQUEST_ID = "axapta-3П-12345";
    LocalDateTime X_DOC_REQUESTED_DATE = LocalDateTime.of(
        2021, 5, 5, 12, 0
    );
    LocalDateTime X_DOC_OUTBOUND_DATE = X_DOC_REQUESTED_DATE.plus(X_DOC_HANDLING_TIME);
    String SERVICE_REQUEST_ID = "wms-00012345";
    int SLOT_SIZE_MINUTES = 30;
    TransportationSubtype SUBTYPE = TransportationSubtype.BREAK_BULK_XDOCK;
    LocalDateTime REQUESTED_DATE =  X_DOC_OUTBOUND_DATE.plus(LEAD_TIME);

    XDocCreateData X_DOC_CREATE_DATA_1P = new XDocCreateData(
        REQUEST_ID,
        null,
        null,
        null,
        X_DOC_REQUESTED_DATE,
        REQUESTED_DATE,
        SUPPLIER_MARKET_ID,
        X_DOC_PARTNER_ID,
        X_DOC_WAREHOUSE_ID,
        X_DOC_WAREHOUSE_NAME,
        TARGET_PARTNER_ID,
        TARGET_WAREHOUSE_ID,
        TARGET_WAREHOUSE_NAME,
        EXTERNAL_REQUEST_ID,
        X_DOC_OUTBOUND_DATE,
        SERVICE_REQUEST_ID,
        SUPPLIER_ID,
        "ЯНДЕКС МАРКЕТ",
        true,
        slot(),
        true,
        false
    );
    XDocCreateData X_DOC_CREATE_DATA_3P = new XDocCreateData(
        REQUEST_ID,
        null,
        null,
        null,
        X_DOC_REQUESTED_DATE,
        REQUESTED_DATE,
        SUPPLIER_MARKET_ID,
        X_DOC_PARTNER_ID,
        X_DOC_WAREHOUSE_ID,
        X_DOC_WAREHOUSE_NAME,
        TARGET_PARTNER_ID,
        TARGET_WAREHOUSE_ID,
        TARGET_WAREHOUSE_NAME,
        null,
        X_DOC_OUTBOUND_DATE,
        SERVICE_REQUEST_ID,
        SUPPLIER_ID,
        "Сторонний поставщик",
        false,
        slot(),
        false,
        true
    );
    XDocCreateData X_DOC_CREATE_DATA_BREAK_BULK_XDOCK = new XDocCreateData(
        REQUEST_ID,
        1000L,
        "0000001000",
        SUBTYPE,
        X_DOC_REQUESTED_DATE,
        REQUESTED_DATE,
        SUPPLIER_MARKET_ID,
        X_DOC_PARTNER_ID,
        X_DOC_WAREHOUSE_ID,
        X_DOC_WAREHOUSE_NAME,
        TARGET_PARTNER_ID,
        TARGET_WAREHOUSE_ID,
        TARGET_WAREHOUSE_NAME,
        EXTERNAL_REQUEST_ID,
        X_DOC_OUTBOUND_DATE,
        SERVICE_REQUEST_ID,
        SUPPLIER_ID,
        "ЯНДЕКС МАРКЕТ",
        true,
        null,
        true,
        false
    );

    static ShopRequestDetailsDTO createShopRequestDetailsBreakBulkXDockDTO() {
        ShopRequestDetailsDTO shopRequestDetailsBreakBulkXDockDTO = new ShopRequestDetailsDTO();
        shopRequestDetailsBreakBulkXDockDTO.setType(RequestSubtypeIds.id(
            TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
            TransportationSubtype.BREAK_BULK_XDOCK,
            TransportationUnitType.INBOUND
        ));
        shopRequestDetailsBreakBulkXDockDTO.setSupplyRequestId(1000L);
        shopRequestDetailsBreakBulkXDockDTO.setExternalRequestId(EXTERNAL_REQUEST_ID);
        shopRequestDetailsBreakBulkXDockDTO.setXDocServiceId(X_DOC_PARTNER_ID);
        shopRequestDetailsBreakBulkXDockDTO.setXDocRequestedDate(X_DOC_REQUESTED_DATE);
        shopRequestDetailsBreakBulkXDockDTO.setRequestedDate(REQUESTED_DATE);
        shopRequestDetailsBreakBulkXDockDTO.setServiceId(TARGET_PARTNER_ID);
        shopRequestDetailsBreakBulkXDockDTO.setShopId(SUPPLIER_ID);
        shopRequestDetailsBreakBulkXDockDTO.setShopOrganizationName("ЯНДЕКС МАРКЕТ");
        shopRequestDetailsBreakBulkXDockDTO.setServiceRequestId(SERVICE_REQUEST_ID);
        shopRequestDetailsBreakBulkXDockDTO.setSupplierType(SupplierType.FIRST_PARTY);
        shopRequestDetailsBreakBulkXDockDTO.setEdo(true);
        shopRequestDetailsBreakBulkXDockDTO.setVetis(false);
        return shopRequestDetailsBreakBulkXDockDTO;
    }

    static ShopRequestDetailsDTO createShopRequestDetails1pDTO() {
        ShopRequestDetailsDTO shopRequestDetails1pDTO = new ShopRequestDetailsDTO();
        shopRequestDetails1pDTO.setType(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF.getId());
        shopRequestDetails1pDTO.setExternalRequestId(EXTERNAL_REQUEST_ID);
        shopRequestDetails1pDTO.setXDocServiceId(X_DOC_PARTNER_ID);
        shopRequestDetails1pDTO.setXDocRequestedDate(X_DOC_REQUESTED_DATE);
        shopRequestDetails1pDTO.setRequestedDate(REQUESTED_DATE);
        shopRequestDetails1pDTO.setServiceId(TARGET_PARTNER_ID);
        shopRequestDetails1pDTO.setShopId(SUPPLIER_ID);
        shopRequestDetails1pDTO.setShopOrganizationName("ЯНДЕКС МАРКЕТ");
        shopRequestDetails1pDTO.setServiceRequestId(SERVICE_REQUEST_ID);
        shopRequestDetails1pDTO.setSupplierType(SupplierType.FIRST_PARTY);
        shopRequestDetails1pDTO.setEdo(true);
        shopRequestDetails1pDTO.setVetis(false);
        return shopRequestDetails1pDTO;
    }

    static XDockTimeSlotDto slot() {
        return new XDockTimeSlotDto()
            .setFromDate(
                X_DOC_REQUESTED_DATE
                    .atZone(ZoneId.systemDefault())
            )
            .setToDate(
                X_DOC_REQUESTED_DATE
                    .atZone(ZoneId.systemDefault())
                    .plusMinutes(SLOT_SIZE_MINUTES)
            )
            .setCalendaringServiceId(1L)
            .setGateId(1L);
    }
}
