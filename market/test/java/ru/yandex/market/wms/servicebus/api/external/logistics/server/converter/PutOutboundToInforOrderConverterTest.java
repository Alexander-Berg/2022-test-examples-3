package ru.yandex.market.wms.servicebus.api.external.logistics.server.converter;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.request.PutOutboundRequest;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.servicebus.IntegrationTest;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PutOutboundToInforOrderConverterTest extends IntegrationTest {

    @Autowired
    private PutOutboundToInforOrderConverter putOutboundToInforOrderConverter;

    private XmlMapper xmlMapper = xmlMapper();

    private XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(NON_NULL);
        return mapper;
    }

    @Test
    public void outboundConverter() throws JsonProcessingException {

        PutOutboundRequest ffOutbound = xmlMapper.readValue(
                getFileContent("converter/putOutbound.xml"), PutOutboundRequest.class);
        OrderDTO order = putOutboundToInforOrderConverter.convert(ffOutbound);
        assertNotNull(order);
        assertEquals(order.getExternorderkey(), "outbound-775325");
        assertEquals(order.getStorerkey(), "");
        assertNotNull(order.getOrderdetails());
        assertEquals(order.getOrderdetails(), List.of());
        assertEquals(order.getTrailernumber(), "о000оо777");
    }
}
