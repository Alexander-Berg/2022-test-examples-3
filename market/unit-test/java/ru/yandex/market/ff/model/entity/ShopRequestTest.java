package ru.yandex.market.ff.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.CalendaringMode;
import ru.yandex.market.ff.client.enums.RequestDocumentType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.model.enums.ApiType;

class ShopRequestTest extends SoftAssertionSupport {

    @Test
    void testCopyWithoutItemsAndHistory() {

        var shopRequest = createShopRequestWithNonNullFieldValues();

        var fieldNamesWithNullValue = ReflectionTestUtils.findFieldNamesWithNullOrDefaultValue(
                shopRequest,
                ShopRequest.class
        );

        if (!fieldNamesWithNullValue.isEmpty()) {
            assertions.fail(
                    "All fields of copying object must be set. Field names with null or default values: " +
                            fieldNamesWithNullValue
            );
        }

        var actualShopRequest = shopRequest.copyWithoutItemsAndHistory();

        var expectedShopRequest = createShopRequestWithNonNullFieldValues();
        // ignore id, items and status history
        expectedShopRequest.setId(null);
        expectedShopRequest.setHistoryEntries(List.of());
        expectedShopRequest.setItems(List.of());
        expectedShopRequest.setVersion(0);
        expectedShopRequest.setAttempt(0);
        expectedShopRequest.setRegistryAttempt(0);
        expectedShopRequest.setConsolidatedShipping(shopRequest.getConsolidatedShipping());

        ReflectionTestUtils.AssertingFieldValuesConsumer fieldValuesConsumer =
                (fieldName, actualFieldValue, expectedFieldValue) -> {
                    if (actualFieldValue instanceof Collection) {
                        Collection actual = (Collection) actualFieldValue;
                        Collection expected = (Collection) expectedFieldValue;
                        assertions.assertThat(actual.size()).as(fieldName).isEqualTo(expected.size());
                        if (fieldName.equals("couriers")) {
                            assertions.assertThat(actual.toArray()).as(fieldName).containsExactly(expected.toArray());
                            return;
                        }
                        if (fieldName.equals("relevantOutbounds")) {
                            // ignored due to not implemented equals and hashCode in ShopRequest
                            return;
                        }
                    }
                    if (actualFieldValue instanceof Supplier) {
                        Supplier actual = (Supplier) actualFieldValue;
                        Supplier expected = (Supplier) expectedFieldValue;
                        assertions.assertThat(actual.getId()).isEqualTo(expected.getId());
                        assertions.assertThat(actual.getName()).isEqualTo(expected.getName());
                        assertions.assertThat(actual.getSupplierType()).isEqualTo(expected.getSupplierType());
                    } else {
                        assertions.assertThat(actualFieldValue).as(fieldName).isEqualTo(expectedFieldValue);
                    }
                };

        ReflectionTestUtils.compareFieldValues(
                actualShopRequest,
                expectedShopRequest,
                ShopRequest.class,
                fieldValuesConsumer
        );
    }

    private ShopRequest createShopRequestWithNonNullFieldValues() {
        var shopRequest = new ShopRequest();

        var requestedDate = LocalDateTime.of(2020, 10, 10, 0, 0);
        var createdDate = LocalDateTime.of(2020, 5, 10, 0, 0);
        var supplier = new Supplier();
        supplier.setId(465857);
        supplier.setName("Amazon");
        supplier.setSupplierType(SupplierType.FIRST_PARTY);

        shopRequest.setId(47L);
        shopRequest.setAuctionId("auction");
        shopRequest.setVersion(101L);
        shopRequest.setServiceId(147L);
        shopRequest.setServiceRequestId("45234");
        shopRequest.setxDocServiceId(222L);
        shopRequest.setxDocRequestedDate(requestedDate);
        shopRequest.setxDocServiceRequestId("xdoc-1231");
        shopRequest.setSupplier(supplier);
        shopRequest.setType(RequestType.SHADOW_SUPPLY);
        shopRequest.setApiType(ApiType.DELIVERY);
        shopRequest.setCreatedAt(createdDate);
        shopRequest.setUpdatedAt(createdDate);
        shopRequest.setStatus(RequestStatus.VALIDATED);
        shopRequest.setComment("comment");
        shopRequest.setRequestedDate(requestedDate);
        shopRequest.setItemsTotalCount(10L);
        shopRequest.setItemsTotalFactCount(11L);
        shopRequest.setItemsTotalSurplusCount(12L);
        shopRequest.setItemsTotalDefectCount(13L);
        shopRequest.setItemsTotalShortageCount(14L);
        shopRequest.setStockType(StockType.DEFECT);
        shopRequest.setStockTypeTo(StockType.EXPIRED);
        shopRequest.setDetailsLoaded(true);
        shopRequest.setExternalOperationType(ExternalOperationType.MOVE);
        shopRequest.setInboundId(453L);
        shopRequest.setActualPalletAmount(81L);
        shopRequest.setActualBoxAmount(283L);
        shopRequest.setConsignor("consignor");
        shopRequest.setConsignorId(77L);
        shopRequest.setConsignorRequestId("909");
        shopRequest.setIgnoreItemsWithError(true);
        shopRequest.setWithdrawAllWithLimit(true);
        shopRequest.setExternalRequestId("Зп-1111");
        shopRequest.setNeedConfirmation(true);
        shopRequest.setCalendaringMode(CalendaringMode.REQUIRED);
        shopRequest.setRequestCreator("Jeff Bezos");
        shopRequest.setItems(List.of(new RequestItem(), new RequestItem()));
        shopRequest.setHistoryEntries(List.of(new RequestStatusHistory()));
        shopRequest.setParentRequestId(512343L);
        shopRequest.setDocumentType(RequestDocumentType.ELECTRONIC);
        shopRequest.setUploadingFinishDate(LocalDateTime.parse("2020-06-20T23:24"));
        shopRequest.setLogisticsPoint(new LogisticsPoint(12341L));
        shopRequest.setSupplierRating(33);
        shopRequest.setRatingIsFake(true);
        shopRequest.setOnlyInternal(true);
        shopRequest.setCouriers(Collections.unmodifiableCollection(List.of(new RequestCourier())));
        shopRequest.setHasAnomaly(true);
        shopRequest.setItemsTotalSurplusCountWithDefect(0L);
        shopRequest.setItemsTotalShortageCountWithDefect(0L);
        shopRequest.setItemsTotalUndefinedCount(123L);
        shopRequest.setApproximateVolume(BigDecimal.ZERO);
        shopRequest.setShippers(Set.of(new ShipperRequestParty()));
        shopRequest.setRelevantOutbounds(Set.of(new ShopRequest()));
        shopRequest.setVetis(true);
        shopRequest.setReceiver(new RequestPartyImpl());
        shopRequest.setNextReceiver(new RequestPartyImpl());
        shopRequest.setConfirmed(true);
        shopRequest.setSupplyRequestId(46L);
        shopRequest.setDocumentTicketUrl("test");
        shopRequest.setDocumentTicketStatus("test");
        shopRequest.setConsolidatedShipping(new ConsolidatedShipping());
        shopRequest.setAttempt(1);
        shopRequest.setRegistryAttempt(1);
        shopRequest.setAxaptaMovementRequestId("Зпер123");
        shopRequest.setRemainingShelfLifeStartDate(LocalDateTime.parse("2020-06-20T23:24"));
        shopRequest.setTransportationId("TMT1");
        shopRequest.setRegistry(true);
        return shopRequest;
    }
}
