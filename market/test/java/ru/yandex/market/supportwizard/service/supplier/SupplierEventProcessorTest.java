package ru.yandex.market.supportwizard.service.supplier;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.event.SupplierEventsOuterClass;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.suppplier.SupplierEventProcessor;

import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.APPROVAL_REQUEST;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.CONFIG_WAREHOUSE;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.CREATE_FEED;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.FIRST_SUPPLY_REQUEST;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.OFFER_IN_PLAINSHIFT;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.OFFER_MAPPING_APPROVED;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.OFFER_PUT_TO_CATALOG;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.OFFLINE_TEST_END;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.OFFLINE_TEST_START;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.ORDER_PROCESSED;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.REGISTRATION;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.SCAN_DOCUMENTS;
import static ru.yandex.market.partner.event.SupplierEventsOuterClass.SupplierEventType.STOCK_SUCCESSFULLY_LOADED;

public class SupplierEventProcessorTest extends BaseFunctionalTest {

    @Autowired
    private SupplierEventProcessor tested;

    @DbUnitDataSet(after = "registrationEvent.after.csv")
    @Test
    void testRegistrationEvent() {
        tested.onSupplierEvent(createEvent(REGISTRATION,
                SupplierEventsOuterClass.PartnerType.DROPSHIP_BY_SELLER,
                true, false));
    }

    @DbUnitDataSet(after = "scanDocumentEvent.after.csv")
    @Test
    void testDocumentScanEvent() {
        tested.onSupplierEvent(createEvent(SCAN_DOCUMENTS,
                SupplierEventsOuterClass.PartnerType.CROSSDOCK,
                false, false));
    }

    @DbUnitDataSet(before = "offerPutToCatalogEvent.before.csv", after = "offerPutToCatalogEvent.after.csv")
    @Test
    void testFirstOfferInCatalogEvent() {
        tested.onSupplierEvent(createEvent(OFFER_PUT_TO_CATALOG,
                SupplierEventsOuterClass.PartnerType.FULFILLMENT,
                false, false));
    }

    @DbUnitDataSet(after = "mappingApprovedEvent.notMetSandboxPreconditions.after.csv")
    @Test
    void testMappingApprovedEvent_preconditionsForSandboxNotMet() {
        tested.onSupplierEvent(createEvent(OFFER_MAPPING_APPROVED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(before = "mappingApprovedEvent.metSandboxPreconditions.before.csv",
            after = "mappingApprovedEvent.metSandboxPreconditions.after.csv")
    @Test
    void testMappingApprovedEvent_preconditionsForSandboxMet() {
        tested.onSupplierEvent(createEvent(OFFER_MAPPING_APPROVED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "createFeedEvent.after.csv")
    @Test
    void testFeedCreatedEvent() {
        tested.onSupplierEvent(createEvent(CREATE_FEED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, false));
        tested.onSupplierEvent(createEvent(CREATE_FEED,
                SupplierEventsOuterClass.PartnerType.FULFILLMENT,
                true, false));
    }

    @DbUnitDataSet(after = "requestApprovedEvent.after.csv")
    @Test
    void testRequestApprovedEvent() {
        tested.onSupplierEvent(createEvent(APPROVAL_REQUEST,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(before = "stocksLoadedEvent.metSandboxPreconditions.before.csv",
            after = "stocksLoadedEvent.metSandboxPreconditions.after.csv")
    @Test
    void testStocksLoadedEvent_preconditionsForSandboxMet() {
        tested.onSupplierEvent(createEvent(STOCK_SUCCESSFULLY_LOADED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "stocksLoadedEvent.notMetSandboxPreconditions.after.csv")
    @Test
    void testStocksLoadedEvent_preconditionsForSandboxNotMet() {
        tested.onSupplierEvent(createEvent(STOCK_SUCCESSFULLY_LOADED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "warehouseCreatedEvent.after.csv")
    @Test
    void testWarehouseConfiguredEvent_preconditionsForSandboxNotMet() {
        tested.onSupplierEvent(createEvent(CONFIG_WAREHOUSE,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(before = "warehouseCreatedEvent.metSandboxPreconditions.before.csv",
            after = "warehouseCreatedEvent.metSandboxPreconditions.after.csv")
    @Test
    void testWarehouseConfiguredEvent_preconditionsForSandboxMet() {
        tested.onSupplierEvent(createEvent(CONFIG_WAREHOUSE,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "offerInPlainshiftEvent.after.csv")
    @Test
    void testFirstOfferInPlainshiftEvent() {
        tested.onSupplierEvent(createEvent(OFFER_IN_PLAINSHIFT,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "offlineTestStartEvent.after.csv")
    @Test
    void testOfflineTestStartEvent() {
        tested.onSupplierEvent(createEvent(OFFLINE_TEST_START,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "orderProcessedEvent.after.csv")
    @Test
    void testOrderDeliveredEvent() {
        tested.onSupplierEvent(createEvent(ORDER_PROCESSED,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "offlineTestEndEvent.after.csv")
    @Test
    void testOfflineTestEndEvent() {
        tested.onSupplierEvent(createEvent(OFFLINE_TEST_END,
                SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, true));
    }

    @DbUnitDataSet(after = "firstSupplyRequestEvent.after.csv")
    @Test
    void testFirstSupplyRequestEvent() {
        tested.onSupplierEvent(createEvent(FIRST_SUPPLY_REQUEST, SupplierEventsOuterClass.PartnerType.FULFILLMENT,
                true, false));
        tested.onSupplierEvent(createEvent(FIRST_SUPPLY_REQUEST, SupplierEventsOuterClass.PartnerType.DROPSHIP,
                true, false));
    }

    private SupplierEventsOuterClass.SupplierEvent createEvent(
            SupplierEventsOuterClass.SupplierEventType type,
            SupplierEventsOuterClass.PartnerType partnerType,
            boolean isPartnerApi,
            boolean isClickAndCollect) {
        return SupplierEventsOuterClass.SupplierEvent.newBuilder()
                .setShopId(1L)
                .setShopName("Name")
                .setEventType(type)
                .setPartnerType(partnerType)
                .setPartnerApi(isPartnerApi)
                .setClickAndCollect(isClickAndCollect)
                .setEventTime(Timestamp.newBuilder()
                        .setSeconds(9999999999L)
                        .build())
                .setCreateAt(Timestamp.newBuilder()
                        .setSeconds(9999999999L)
                        .build())
                .setUpdateAt(Timestamp.newBuilder()
                        .setSeconds(9999999999L)
                        .build())
                .build();
    }
}
