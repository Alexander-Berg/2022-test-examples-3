package ru.yandex.market.ff.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.CisHandleMode;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.enums.IdentifierType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RequestItemTest extends SoftAssertionSupport {

    @Test
    void testCopyWithoutLinksExceptingModelData() {
        var requestItem = createRequestItemWithNonNullFieldValues();

        var fieldNamesWithNullValue = ReflectionTestUtils.findFieldNamesWithNullOrDefaultValue(
                requestItem,
                RequestItem.class
        );

        if (!fieldNamesWithNullValue.isEmpty()) {
            assertions.fail(
                    "All fields of copying object must be set. Field names with null or default values: " +
                            fieldNamesWithNullValue
            );
        }

        var actualRequestItem = requestItem.copyWithoutLinksExceptingModelData();

        var expectedRequestItem = createRequestItemWithNonNullFieldValues();
        // ignore id and any links excepting model data
        expectedRequestItem.setId(null);
        expectedRequestItem.setExternalItemErrorList(List.of());
        expectedRequestItem.setRequestItemErrorList(List.of());
        expectedRequestItem.setLogisticUnit(null);
        expectedRequestItem.setCreatedAt(null);
        expectedRequestItem.setUpdatedAt(null);

        ReflectionTestUtils.compareFieldValues(
                actualRequestItem,
                expectedRequestItem,
                RequestItem.class,
                (fieldName, actualFieldValue, expectedFieldValue) -> {
                    if (actualFieldValue instanceof Collection) {
                        Collection actual = (Collection) actualFieldValue;
                        Collection expected = (Collection) expectedFieldValue;
                        assertions.assertThat(actual.size()).as(fieldName).isEqualTo(expected.size());
                    } else {
                        assertions.assertThat(actualFieldValue).as(fieldName).isEqualTo(expectedFieldValue);
                    }
                }
        );

        // do extra checks for list fields with market data
        assertions.assertThat(actualRequestItem.getMarketVendorCodeStrings()).containsExactly("mvendorCode");
        assertions.assertThat(actualRequestItem.getMarketBarcodeStrings()).containsExactly("mbarcode");
        assertEquals(2, actualRequestItem.getRequestItemCargoTypes().size());

        assertions.assertThat(actualRequestItem.getRequestItemErrorList()).isEmpty();
        assertions.assertThat(actualRequestItem.getExternalItemErrorList()).isEmpty();
    }

    private RequestItem createRequestItemWithNonNullFieldValues() {
        var createdDate = LocalDateTime.of(2020, 5, 10, 0, 0);
        RequestItem item = new RequestItem();
        item.setId(55L);
        item.setRequestId(1L);
        item.setSupplierId(468857L);
        item.setArticle("article");
        item.setBarcodes(List.of("barcode1", "barcode2"));
        item.setCount(10);
        item.setComment("comment");
        item.setName("name");
        item.setMarketName("marketName");
        item.setSku(5L);
        item.setSupplyPrice(BigDecimal.TEN);
        item.setFactCount(10);
        item.setDefectCount(1);
        item.setSurplusCount(2);
        item.setShortageCount(3);
        item.setBoxCount(1);
        item.setHasExpirationDate(true);
        item.setVendorCode("vendorCode");
        item.setPackageNumInSpike(5);
        item.setWidth(BigDecimal.ONE);
        item.setHeight(BigDecimal.TEN);
        item.setLength(BigDecimal.ONE);
        item.setRealSupplierId("realSupplierId");
        item.setRealSupplierName("realSupplierName");
        item.setInboundRemainingLifetimeDays(10);
        item.setOutboundRemainingLifetimeDays(11);
        item.setInboundRemainingLifetimePercentage(12);
        item.setOutboundRemainingLifetimePercentage(13);
        item.setCategoryId(7L);
        item.setUntaxedPrice(BigDecimal.valueOf(20.51));
        item.setVatRate(VatRate.VAT_10);
        item.setNeedManualVerification(true);
        item.setLogisticUnit(new LogisticUnit());
        item.setSourceFulfillmentId(58823L);

        item.setRequestItemErrorList(List.of(new RequestItemError()));
        item.setExternalItemErrorList(List.of(new ExternalRequestItemError()));

        var barcode = new RequestItemMarketBarcode(55L, "mbarcode");
        var vendorCode = new RequestItemMarketVendorCode(55L, "mvendorCode");

        item.setMarketVendorCodes(List.of(vendorCode));
        item.setMarketBarcodes(List.of(barcode));

        item.setNeedMeasurement(false);
        item.setSurplusAllowed(true);

        item.setImeiCount(1);
        item.setImeiMask("\\d+");
        item.setSerialNumberCount(1);
        item.setSerialNumberMask("\\w+");
        item.setRequestItemCargoTypes(RequestItemCargoType.asSet(item, 10, 20));

        item.setSurplusCountWithDefect(0);
        item.setShortageCountWithDefect(0);
        item.setRequestItemIdentifiers(
            Set.of(
                new Identifier(55L, new RegistryUnitId(Set.of(new UnitPartialId(RegistryUnitIdType.CIS,
                    "CIS"))),
                IdentifierType.RECEIVED_UNFIT, null))
        );

        item.setCisHandleMode(CisHandleMode.ACCEPT_ONLY_DECLARED);
        item.setCreatedAt(createdDate);
        item.setUpdatedAt(createdDate);
        item.setConsignmentId("consignmentId");
        item.setConsignments(List.of(new Consignment()));
        return item;
    }
}
