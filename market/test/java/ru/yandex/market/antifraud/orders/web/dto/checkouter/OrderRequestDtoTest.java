package ru.yandex.market.antifraud.orders.web.dto.checkouter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.market.antifraud.orders.test.providers.OrderBuyerDeviceIdRequestProvider;
import ru.yandex.market.antifraud.orders.test.providers.OrderDeliveryBuyerAddressProvider;
import ru.yandex.market.antifraud.orders.web.AntifraudJsonUtil;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Novikov <a href="mailto:hronos@yandex-team.ru"></a>
 * @date 23.09.2019
 */
public class OrderRequestDtoTest {
    @Test
    public void serializationTest() throws IOException {
        ObjectMapper mapper = AntifraudJsonUtil.OBJECT_MAPPER;

        OrderItemRequestDto orderItemRequest1 = new OrderItemRequestDto(111L, 111L, "offer1", "bundle1",
                "shop_sku1", 111L, 111L, 111, new BigDecimal("123.456"), 10, 12L);
        OrderItemRequestDto orderItemRequest2 = new OrderItemRequestDto(112L, 112L, "offer2", "bundle2",
            "shop_sku2", 112L, 112L, 112, new BigDecimal("456.789"), 11, 13L);

        OrderDeliveryAddressRequestDto orderDeliveryAddressRequestDto =
            OrderDeliveryBuyerAddressProvider.getOrderDeliveryBuyerAddress();
        OrderDeliveryRequestDto orderDeliveryRequestDto = new OrderDeliveryRequestDto(999L, "outletCode",
            OrderDeliveryType.DELIVERY, orderDeliveryAddressRequestDto);

        OrderBuyerDeviceIdRequestDto deviceId = OrderBuyerDeviceIdRequestProvider.getBuyerDefaultDeviceId();
        OrderBuyerUserDeviceRequestDto userDevice = new OrderBuyerUserDeviceRequestDto(deviceId, true);
        OrderBuyerRequestDto orderBuyerRequestDto = OrderBuyerRequestDto.builder()
            .uid(111L)
            .email("email@email.com")
            .normalizedPhone("79123456789")
            .personalPhoneId("6a2110132421451ab1205474d18db585")
            .personalEmailId("1260ee2748324f89b5992f3b478a150e")
            .uuid("qwertyuiop_123456789")
            .yandexuid("6238887791581327997")
            .userDevice(userDevice)
            .ip("127.0.0.1")
            .assessor(true)
            .yandexEmployee(true)
            .businessBalanceId(7780L)
            .build();

        OrderPaymentFullInfoDto orderPaymentFullInfoDto = new OrderPaymentFullInfoDto(OrderPaymentType.PREPAID);

        OrderRequestDto orderRequestDto = OrderRequestDto.builder()
            .id(123456L)
            .fulfilment(true)
            .fake(true)
            .checkout(true)
            .items(Arrays.asList(orderItemRequest1, orderItemRequest2))
            .delivery(orderDeliveryRequestDto)
                .buyer(orderBuyerRequestDto)
                .note("Note")
                .paymentFullInfo(orderPaymentFullInfoDto)
                .build();

        //language=JSON
        String json = "{\"id\":123456,\"fulfilment\":true,\"fake\":true,\"items\":[{\"id\":111,\"feedId\":111," +
                "\"offerId\":\"offer1\",\"bundleId\":\"bundle1\",\"shopSku\":\"shop_sku1\",\"msku\":111," +
                "\"modelId\":111,\"categoryId\":111,\"price\":123.456,\"count\":10,\"supplierId\":12},{\"id\":112," +
            "\"feedId\":112,\"offerId\":\"offer2\",\"bundleId\":\"bundle2\",\"shopSku\":\"shop_sku2\"," +
            "\"msku\":112,\"modelId\":112,\"categoryId\":112,\"price\":456.789,\"count\":11,\"supplierId\":13}]," +
            "\"delivery\":{\"outletId\":999,\"outletCode\":\"outletCode\",\"type\":\"DELIVERY\"," +
            "\"buyerAddress\":{\"country" +
            "\":\"Россия\",\"postcode\":\"123000\",\"city\":\"Москва\",\"street\":\"Льва Толстого\"," +
            "\"house\":\"16\",\"entrance\":\"1\",\"entryPhone\":\"123\",\"floor\":\"4\",\"gps\":\"30" +
            ".70079620061024,61.71145050985435\",\"notes\":\"Заметка\"," +
            "\"phone\":\"+7 999 123456\",\"language\":\"RUS\",\"preciseRegionId\":213," +
            "\"recipientPerson\":{\"firstName\":\"Иван\",\"lastName\":\"Иванов\"}}},\"buyer\":{\"uid\":111," +
            "\"email\":\"email@email.com\",\"normalizedPhone\":\"79123456789\"," +
            "\"personalPhoneId\":\"6a2110132421451ab1205474d18db585\"," +
            "\"personalEmailId\":\"1260ee2748324f89b5992f3b478a150e\"," +
            "\"uuid\":\"qwertyuiop_123456789\"," +
            "\"yandexuid\":\"6238887791581327997\",\"businessBalanceId\":7780," +
            "\"userDevice\":{\"deviceId\":{\"androidDeviceId\":\"androidDeviceId\"," +
            "\"googleServiceId\":\"googleServiceId\",\"androidHardwareSerial\":\"androidHardwareSerial\"," +
            "\"androidBuildModel\":\"androidBuildModel\"," +
            "\"androidBuildManufacturer\":\"androidBuildManufacturer\",\"iosDeviceId\":\"iosDeviceId\"}," +
            "\"emulator\":true},\"ip\":\"127.0.0.1\",\"assessor\":true,\"yandexEmployee\":true}" +
            ",\"note\":\"Note\",\"checkout\":true,\"paymentFullInfo\":{\"orderPaymentType\":\"PREPAID\"}}";
        OrderRequestDto deserialized = mapper.readValue(json, OrderRequestDto.class);
        assertThat(deserialized).isEqualTo(orderRequestDto);
    }
}
