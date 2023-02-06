package ru.yandex.market.loyalty.core.model;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.loyalty.api.model.EnumWithPermanentCode;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.config.CoreConfigInternal.TRIGGER_OBJECT_MAPPER;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_ITEM_KEY;
import static ru.yandex.market.loyalty.core.utils.OrderRequestUtils.DEFAULT_WAREHOUSE_ID;

public class GenericParamTest {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat dateFormatWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Test
    public void integer() {
        GenericParam<Integer> param = GenericParam.of(42);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("42", asString.value());
        assertEquals(42, asString.cast(Integer.class).value().intValue());
    }

    @Test
    public void bigDecimal() {
        BigDecimal half = BigDecimal.valueOf(0.5);
        GenericParam<BigDecimal> param = GenericParam.of(half);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("0.5", asString.value());
        assertThat(asString.cast(BigDecimal.class).value(), comparesEqualTo(half));
    }

    @Test
    public void bool() {
        GenericParam<Boolean> param = GenericParam.of(true);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("true", asString.value());
        assertTrue(asString.cast(Boolean.class).value());
    }

    @Test
    public void date() throws ParseException {
        Date newYear = dateFormat.parse("2017-12-31");
        GenericParam<Date> param = GenericParam.of(newYear);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("2017-12-31 00:00:00", asString.value());
        assertEquals(newYear, asString.cast(Date.class).value());
    }

    @Test
    public void dateWithTime() throws ParseException {
        Date newYearMorning = dateFormatWithTime.parse("2018-01-01 12:30:00");
        GenericParam<Date> param = GenericParam.of(newYearMorning);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("2018-01-01 12:30:00", asString.value());
        assertEquals(newYearMorning, asString.cast(Date.class).value());
    }

    @Test
    public void dateWithTimeInMidnight() throws ParseException {
        Date newYearMorning = dateFormatWithTime.parse("2018-01-01 00:00:00");
        GenericParam<Date> param = GenericParam.of(newYearMorning);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals("2018-01-01 00:00:00", asString.value());
        assertEquals(newYearMorning, asString.cast(Date.class).value());
    }

    @Test
    public void notRenamedEnum() {
        SampleEnum sampleEnum = SampleEnum.NOT_RENAMED;
        GenericParam<SampleEnum> param = GenericParam.of(sampleEnum);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals(sampleEnum.getCode(), asString.value());
        assertEquals(sampleEnum, asString.cast(SampleEnum.class).value());
    }

    @Test
    public void renamedEnum() {
        SampleEnum sampleEnum = SampleEnum.RENAMED;
        GenericParam<SampleEnum> param = GenericParam.of(sampleEnum);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals(sampleEnum.getCode(), asString.value());
        assertEquals(sampleEnum, asString.cast(SampleEnum.class).value());
    }

    @Test
    public void orderItems() throws JsonProcessingException {
        OrderStatusUpdatedEvent.OrderItems orderItems =
                new OrderStatusUpdatedEvent.OrderItems(Collections.singletonList(
                Item.Builder.create()
                        .withKey(DEFAULT_ITEM_KEY)
                        .withPrice(BigDecimal.ONE)
                        .withQuantity(BigDecimal.ONE)
                        .withDownloadable(false)
                        .withHyperCategoryId(1)
                        .withSku("1")
                        .withSsku("1")
                        .withPromoKeys(Collections.emptySet())
                        .withSupplierId(1L)
                        .withWarehouseId(DEFAULT_WAREHOUSE_ID)
                        .atSupplierWarehouse(false)
                        .withPayByYaPlus(0)
                        .build()
        ));
        GenericParam<OrderStatusUpdatedEvent.OrderItems> param = GenericParam.of(orderItems);
        GenericParam<String> asString = param.cast(String.class);
        assertEquals(asString.value(), TRIGGER_OBJECT_MAPPER.writeValueAsString(orderItems));
        assertThat(asString.cast(OrderStatusUpdatedEvent.OrderItems.class).value(),
                hasProperty("orderItems", contains(orderItems.getOrderItems().stream()
                        .map(Matchers::samePropertyValuesAs)
                        .collect(Collectors.toList())
                ))
        );
    }

    @Test
    public void orderItemsRegress() {
        OrderStatusUpdatedEvent.OrderItems orderItems =new OrderStatusUpdatedEvent.OrderItems(Collections.singletonList(
                Item.Builder.create()
                        .withKey(ItemKey.ofFeedOffer(1L, "1"))
                        .withPrice(BigDecimal.ONE)
                        .withQuantity(BigDecimal.ONE)
                        .withDownloadable(false)
                        .withHyperCategoryId(1)
                        .withSku("1")
                        .withSsku("1")
                        .withPromoKeys(Collections.emptySet())
                        .withSupplierId(1L)
                        .withWarehouseId(DEFAULT_WAREHOUSE_ID)
                        .atSupplierWarehouse(false)
                        .withPayByYaPlus(0)
                        .build()
        ));
        GenericParam<String> asString = GenericParam.of("" +
                '{' +
                "   \"orderItems\": [{" +
                "       \"itemKey\": {" +
                "           \"feedId\": 1," +
                "           \"offerId\": \"1\"" +
                "       }," +
                "       \"price\": 1," +
                "       \"quantity\": 1," +
                "       \"hyperCategoryId\": 1," +
                "       \"sku\": \"1\"," +
                "       \"ssku\": \"1\"," +
                "       \"oldMinPrice\": null," +
                "       \"feedId\": 1," +
                "       \"offerId\": \"1\"," +
                "       \"warehouseId\": 123," +
                "       \"atSupplierWarehouse\": false," +
                "       \"supplierId\": 1," +
                "       \"payByYaPlus\": 0" +
                "   }]" +
                '}');
        assertThat(asString.cast(OrderStatusUpdatedEvent.OrderItems.class).value(),
                hasProperty("orderItems", contains(orderItems.getOrderItems().stream()
                        .map(Matchers::samePropertyValuesAs)
                        .collect(Collectors.toList())
                ))
        );
    }

    public enum SampleEnum implements EnumWithPermanentCode {
        NOT_RENAMED("NOT_RENAMED"),
        RENAMED("SOMETHING_OLD");

        private static final Map<String, SampleEnum> cache = Util.createCache(values());

        private final String code;

        SampleEnum(String code) {
            this.code = code;
        }

        @JsonValue
        @Override
        public String getCode() {
            return code;
        }

        @JsonCreator
        public static SampleEnum findByCode(String code) {
            return cache.get(code);
        }
    }

}
