package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteWaybill;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.SupplierId;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.common.SystemPropertyKey;
import ru.yandex.market.logistic.api.model.fulfillment.Consignment;
import ru.yandex.market.logistic.api.model.fulfillment.Inbound;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.utils.DateTimeInterval;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static org.junit.jupiter.params.ParameterizedTest.INDEX_PLACEHOLDER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaybillConverterTest extends BaseIntegrationTest {


    private static final long THIRD_PARTY_SUPPLIER_ID = 3333L;
    private static final long FIRST_PARTY_SUPPLIER_ID = 11111L;

    private static Consignment createItem(String sku, long vendorId) {
        var item = new Item.ItemBuilder(sku, 10, BigDecimal.TEN)
                .setUnitId(new UnitId(sku, vendorId, sku))
                .build();

        return new Consignment(null, item, null);
    }

    @MethodSource("data")
    @ParameterizedTest(name = " [" + INDEX_PLACEHOLDER + "] {0}")
    void convert(String name, boolean shouldSendSupplierId, Inbound inbound, MarschrouteWaybill waybill) {
        var systemPropertyService = mock(SystemPropertyService.class);
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.SHOULD_SEND_SUPPLIER_ID_ON_CREATE_INBOUND))
                .thenReturn(shouldSendSupplierId);

        MarschrouteWaybill actual =
                new WaybillConverter(systemPropertyService, FIRST_PARTY_SUPPLIER_ID).convert(inbound);

        softly.assertThat(actual.getComment())
                .as("check converted waybill comment")
                .isEqualTo(waybill.getComment());
        softly.assertThat(actual.getDate())
                .as("check converted waybill date")
                .isEqualTo(waybill.getDate());
        softly.assertThat(actual.getSupplierId())
                .as("check supplierId")
                .isEqualTo(waybill.getSupplierId());
    }

    @Nonnull
    static Stream<Arguments> data() {
        // В интервале специально указана таймзона +5
        final DateTimeInterval interval = DateTimeInterval.fromFormattedValue("2017-09-19T12:57:59+05:00/2017-09" +
                "-19T12:57:59");
        // В waybill время должно попасть в московской таймзоне
        final LocalDateTime wayBillTime = LocalDateTime.of(2017, 9, 19, 10, 57, 59);

        return Stream.of(
                Arguments.of(
                        "With comment",
                        false,
                        new Inbound(
                                null,
                                null,
                                List.of(createItem("sku1", THIRD_PARTY_SUPPLIER_ID)),
                                null,
                                null,
                                interval,
                                "comment"
                        ),
                        new MarschrouteWaybill().setComment("comment").setDate(wayBillTime)),
                Arguments.of(
                        "Without comment",
                        false,
                        new Inbound(
                                null,
                                null,
                                null,
                                null,
                                null,
                                interval,
                                null
                        ),
                        new MarschrouteWaybill().setDate(wayBillTime)),
                Arguments.of(
                        "One 1p and one 3p items should lead to waybill.supplierId=3p",
                        true,
                        new Inbound(
                                null,
                                null,
                                List.of(
                                        createItem("sku1", THIRD_PARTY_SUPPLIER_ID),
                                        createItem("sku2", FIRST_PARTY_SUPPLIER_ID)
                                ),
                                null,
                                null,
                                interval,
                                null
                        ),
                        new MarschrouteWaybill().setDate(wayBillTime).setSupplierId(SupplierId.THIRD_PARTY)),
                Arguments.of(
                        "Two 3p items should lead to waybill.supplierId=3p",
                        true,
                        new Inbound(
                                null,
                                null,
                                List.of(
                                        createItem("sku1", THIRD_PARTY_SUPPLIER_ID),
                                        createItem("sku2", THIRD_PARTY_SUPPLIER_ID)
                                ),
                                null,
                                null,
                                interval,
                                null
                        ),
                        new MarschrouteWaybill().setDate(wayBillTime).setSupplierId(SupplierId.THIRD_PARTY)),
                Arguments.of(
                        "Two 1p items should lead to waybill.supplierId=3p",
                        true,
                        new Inbound(
                                null,
                                null,
                                List.of(
                                        createItem("sku1", FIRST_PARTY_SUPPLIER_ID),
                                        createItem("sku2", FIRST_PARTY_SUPPLIER_ID)
                                ),
                                null,
                                null,
                                interval,
                                null
                        ),
                        new MarschrouteWaybill().setDate(wayBillTime).setSupplierId(SupplierId.FIRST_PARTY)),
                Arguments.of(
                        "On empty consignments with shouldSendSupplierId=true",
                        true,
                        new Inbound(
                                null,
                                null,
                                List.of(),
                                null,
                                null,
                                interval,
                                null
                        ),
                        new MarschrouteWaybill().setDate(wayBillTime))

        );
    }
}
