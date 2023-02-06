package ru.yandex.market.wms.servicebus.api.external.logistics.server.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Order;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class OrderConverterTest {

    private final XmlMapper xmlMapper;
    private final FulfilmentOrderToInforOrderConverter fulfilmentOrderToInforOrderConverter;

    public OrderConverterTest() {
        xmlMapper = new XmlMapper();
        fulfilmentOrderToInforOrderConverter = new FulfilmentOrderToInforOrderConverter();
    }

    @Test
    public void normalAddressConverter() throws JsonProcessingException {
        Order ffOrder = xmlMapper.readValue(
                getFileContent("api/logistics/server/converter/normalAddressConvert/order.xml"), Order.class);
        OrderDTO inforOrder = fulfilmentOrderToInforOrderConverter.convert(ffOrder);
        assertNotNull(inforOrder);
        assertEquals(inforOrder.getCaddress1(), "Щербаковская, 32/7");
    }

    @Test
    public void longAddressConverter() throws JsonProcessingException {
        Order ffOrder = xmlMapper.readValue(
                getFileContent("api/logistics/server/converter/longAddressConvert/order.xml"), Order.class);
        OrderDTO inforOrder = fulfilmentOrderToInforOrderConverter.convert(ffOrder);
        assertNotNull(inforOrder);
        assertEquals(inforOrder.getCaddress1(), "кто-то решил тут написать название своей ");
        assertEquals(inforOrder.getCaddress2(), "улицы и дома, 32/7");
        assertNull(inforOrder.getCaddress3());
    }

    @Test
    public void veryLongAddressConverter() throws JsonProcessingException {
        Order ffOrder = xmlMapper.readValue(
                getFileContent("api/logistics/server/converter/veryLongAddressConvert/order.xml"), Order.class);
        OrderDTO inforOrder = fulfilmentOrderToInforOrderConverter.convert(ffOrder);
        assertNotNull(inforOrder);
        assertEquals(inforOrder.getCaddress1(), "кто_то_решил_тут_написать_название_своей_улиц");
        assertEquals(inforOrder.getCaddress2(), ", 32/7");
    }

    public XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(NON_NULL);
        return mapper;
    }
}
