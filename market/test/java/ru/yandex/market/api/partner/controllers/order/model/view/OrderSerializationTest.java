package ru.yandex.market.api.partner.controllers.order.model.view;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.api.partner.controllers.order.model.Delivery;
import ru.yandex.market.api.partner.controllers.order.model.DeliveryDates;
import ru.yandex.market.api.partner.controllers.order.model.OrderDTO;
import ru.yandex.market.api.partner.controllers.order.model.OrderItemResponseDTO;
import ru.yandex.market.api.partner.controllers.order.model.view.json.promo.ItemPromoJsonSerializer;
import ru.yandex.market.api.partner.controllers.order.model.view.xml.promo.ItemPromoXmlSerializer;
import ru.yandex.market.api.partner.controllers.serialization.BaseOldSerializationTest;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;

/**
 * @author zoom
 */
class OrderSerializationTest extends BaseOldSerializationTest {

    /**
     * @see ItemPromoJsonSerializer
     * @see ItemPromoXmlSerializer
     */
    private static void assertSupportedPromoTypes() {
        final PromoType[] promoTypes = PromoType.values();
        assertThat(
                "Внимание! В checkouter-client добавился новый тип promo." +
                        "Нужно обновить тест shouldShowOrderItemPromos." +
                        "За сериализацию в json/xml в апи отаечают ItemPromoJsonSerializer и " +
                        " ItemPromoXmlSerializer",
                promoTypes,
                arrayContainingInAnyOrder(
                        PromoType.MARKET_COUPON, PromoType.MARKET_DEAL, PromoType.MARKET_BLUE,
                        PromoType.MARKET_PRIME, PromoType.YANDEX_PLUS, PromoType.YANDEX_EMPLOYEE, PromoType.MARKET_COIN,
                          PromoType.LIMITED_FREE_DELIVERY_PROMO, PromoType.FREE_DELIVERY_THRESHOLD,
                        PromoType.MULTICART_DISCOUNT, PromoType.PRICE_DROP_AS_YOU_SHOP, PromoType.FREE_DELIVERY_FOR_LDI,
                        PromoType.UNKNOWN, PromoType.GENERIC_BUNDLE, PromoType.BERU_PLUS, PromoType.FREE_DELIVERY_FOR_LSC,
                        PromoType.SECRET_SALE, PromoType.FREE_PICKUP, PromoType.CHEAPEST_AS_GIFT, PromoType.BLUE_FLASH,
                        PromoType.BLUE_SET, PromoType.DIRECT_DISCOUNT, PromoType.CASHBACK, PromoType.MARKET_PROMOCODE,
                        PromoType.SUPPLIER_MULTICART_DISCOUNT, PromoType.SPREAD_DISCOUNT_COUNT,
                        PromoType.SPREAD_DISCOUNT_RECEIPT
                )
        );
    }

    @Test
    void shouldNotShowNullOrderFee() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setFeeUE(null);
        getChecker().testSerialization(
                orderDTO,
                "{}",
                "<order><items/></order>");
    }

    @Test
    void shouldShowNotNullOrderFee() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setFeeUE(BigDecimal.valueOf(1230, 2));
        getChecker().testSerialization(
                orderDTO,
                "{\"feeUE\":12.3}",
                "<order fee-ue=\"12.3\"><items/></order>");
    }

    @Test
    void shouldShowNotNullOrderItemFee() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setFeeUE(null);
        OrderItem item = new OrderItem();
        item.setFeeSum(BigDecimal.valueOf(30, 2));
        orderDTO.setItems(Collections.singletonList(new OrderItemResponseDTO(item)));
        getChecker().testSerialization(
                orderDTO,
                "{\"items\":[{\"feeUE\":0.3}]}",
                "<order><items><item fee-ue=\"0.3\"/></items></order>");
    }

    @Test
    void shouldNotShowWhenNullOrderItemFee() {
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setFeeUE(null);
        OrderItem item = new OrderItem();
        item.setFeeSum(null);
        orderDTO.setItems(Collections.singletonList(new OrderItemResponseDTO(item)));
        getChecker().testSerialization(
                orderDTO,
                "{\"items\":[{}]}",
                "<order><items><item/></items></order>");
    }

    @Test
    void shouldSerializeTaxInformation() {
        final OrderDTO orderDTO = new OrderDTO();
        orderDTO.setTaxSystem(TaxSystem.ENVD);

        final OrderItem item = new OrderItem();
        item.setVat(VatType.VAT_18);
        orderDTO.setItems(Collections.singletonList(new OrderItemResponseDTO(item)));

        final Delivery delivery = new Delivery();
        delivery.setVat(VatType.VAT_10);
        orderDTO.setDelivery(delivery);

        getChecker().testSerialization(orderDTO,
                "{\"items\":[{\"vat\":\"VAT_18\"}],\"delivery\":{\"vat\":\"VAT_10\"},\"taxSystem\":\"ENVD\"}",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<order tax-system=\"ENVD\"><items><item vat=\"VAT_18\"/></items><delivery vat=\"VAT_10\"/></order>");
    }

    @Test
    void shouldShowOrderItemPromos() {
        assertSupportedPromoTypes();

        OrderDTO orderDTO = new OrderDTO();
        OrderItem item = new OrderItem();
        item.setPromos(ImmutableSet.of(
                ItemPromo.createWithSubsidy(PromoDefinition.marketCouponPromo(), BigDecimal.valueOf(1.5)),
                ItemPromo.createWithSubsidy(PromoDefinition.marketDealPromo("promo"), BigDecimal.valueOf(2.5)),
                ItemPromo.createWithSubsidy(PromoDefinition.blueMarketPromo(), BigDecimal.valueOf(4.5)),
                ItemPromo.createWithSubsidy(PromoDefinition.primePromo("reason"), BigDecimal.valueOf(2.1)),
                ItemPromo.createWithSubsidy(PromoDefinition.yandexPlusPromo(), BigDecimal.valueOf(3.2)),
                ItemPromo.createWithSubsidy(PromoDefinition.yandexEmployeePromo("reason"), BigDecimal.valueOf(6.1)),
                ItemPromo.createWithSubsidy(PromoDefinition.marketPromocodePromo("PromoId", "LPromoID",123L)
                        , BigDecimal.valueOf(6.1)),
                ItemPromo.createWithSubsidy(PromoDefinition.priceDropAsYouShop(), BigDecimal.valueOf(6.9)),
                ItemPromo.createWithSubsidy(PromoDefinition.secretSale(), BigDecimal.valueOf(8.1)),
                new ItemPromo(PromoDefinition.limitedFreeDeliveryPromo(""),
                        BigDecimal.valueOf(7.5), BigDecimal.valueOf(5.4), BigDecimal.valueOf(5.4))));
        orderDTO.setItems(Collections.singletonList(new OrderItemResponseDTO(item)));
        getChecker().testSerializationWithNonStrictOrder(
                orderDTO,
                "{'items':[{'promos':" +
                        "[{'type':'MARKET_COUPON','subsidy':1.5}," +
                        "{'type':'MARKET_PROMOCODE','subsidy':6.1}," +
                        "{'type':'MARKET_DEAL','subsidy':2.5,'marketPromoId':'promo'}]" +
                        "}]}",
                "<order><items><item><promos>" +
                        "<promo type='MARKET_COUPON' subsidy='1.5'/>" +
                        "<promo type='MARKET_PROMOCODE' subsidy='6.1'/>" +
                        "<promo type='MARKET_DEAL' subsidy='2.5' market-promo-id='promo'/>" +
                        "</promos></item></items></order>");
    }

    @Test
    void shouldNotShowOrderItemEmptyPromos() {
        OrderDTO orderDTO = new OrderDTO();
        OrderItem item = new OrderItem();
        item.setPromos(new HashSet<>());
        orderDTO.setItems(Arrays.asList(
                new OrderItemResponseDTO(item),
                new OrderItemResponseDTO(new OrderItem()))
        );
        getChecker().testSerialization(
                orderDTO,
                "{\"items\":[{},{}]}",
                "<order><items><item/><item/></items></order>");
    }

    @Test
    void shouldSerializeDeliveryServiceIdAndShopSku() {
        final Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(303L);

        final OrderDTO orderDTO = new OrderDTO();
        orderDTO.setDelivery(delivery);

        final OrderItem item = new OrderItem();
        item.setShopSku("some-sku");
        orderDTO.setItems(Collections.singletonList(new OrderItemResponseDTO(item)));

        getChecker().testSerialization(orderDTO,
                "{\"items\":[{\"shopSku\":\"some-sku\"}],\"delivery\":{\"deliveryServiceId\":303}}",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<order><items><item shop-sku=\"some-sku\"/></items><delivery delivery-service-id=\"303\"/></order>");
    }

    @Test
    void shouldSerializeDeliveryDates() throws ParseException {
        final OrderDTO orderDTO = new OrderDTO();
        final Delivery delivery = new Delivery();

        DateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        delivery.setDeliveryDates(new DeliveryDates(
                DateUtil.asLocalDate(format.parse("06-06-2017")),
                DateUtil.asLocalDate(format.parse("09-06-2017")),
                LocalTime.parse("12:01:02"),
                LocalTime.parse("17:03:04"),
                DateUtil.asLocalDate(format.parse("08-06-2017"))));
        orderDTO.setDelivery(delivery);
        getChecker().testSerialization(orderDTO,
                "{\"delivery\":{\"dates\":{\"fromDate\":\"06-06-2017\"," +
                        "\"toDate\":\"09-06-2017\",\"fromTime\":\"12:01:02\",\"toTime\":\"17:03:04\", " +
                        "\"realDeliveryDate\":\"08-06-2017\"}}}",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<order><items/><delivery><dates from-date=\"06-06-2017\" " +
                        "to-date=\"09-06-2017\" from-time=\"12:01:02\" to-time=\"17:03:04\" " +
                        "real-delivery-date=\"08-06-2017\"/></delivery></order>");
    }

    @Test
    void shouldSerializeTotalWithSubsidy(){
        final OrderDTO orderDTO = new OrderDTO();
        orderDTO.setTotalWithSubsidy(BigDecimal.valueOf(1000));
        getChecker().testSerialization(
                orderDTO,
                "{\"totalWithSubsidy\":1000}",
                "<order total-with-subsidy=\"1000\"><items/></order>");
    }
}
