package ru.yandex.market.wms.servicebus.api.external.logistics.server.converter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.logistic.api.model.fulfillment.Outbound;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDTO;
import ru.yandex.market.wms.common.spring.domain.dto.OrderDetailDTO;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.WrapInforClient;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.IdentifierMappingDto;
import ru.yandex.market.wms.servicebus.api.external.wrap.client.dto.InforUnitId;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.reset;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class OutboundConvertTest extends IntegrationTest {

    @MockBean
    @Autowired
    private WrapInforClient wrapInforClient;

    @Autowired
    private FulfilmentOutboundToInforOrderConverter fulfilmentOutboundToInforOrderConverter;
    private XmlMapper xmlMapper = xmlMapper();

    @AfterEach
    public void resetMocks() {
        Mockito.reset(wrapInforClient);
    }

    @Test
    public void normalAddressConverter() throws JsonProcessingException {
        reset(wrapInforClient);
        Mockito.when(
                this.wrapInforClient.mapReferenceItems(getMapReferenceItemsRequest())
        ).thenReturn(getMapReferenceItemsResponse());

        Outbound ffOutbound = xmlMapper.readValue(
                getFileContent("api/logistics/server/createOutbound/outbound.xml"), Outbound.class);
        OrderDTO inforOrder = fulfilmentOutboundToInforOrderConverter.convert(ffOutbound);
        assertNotNull(inforOrder);
        assertEquals(inforOrder.getExternorderkey(), "outbound-8223278");
        assertEquals(inforOrder.getStorerkey(), "1026708");
        assertNotNull(inforOrder.getOrderdetails());
        for (OrderDetailDTO details : inforOrder.getOrderdetails()) {
            assertNull(details.getLottable08());
        }
    }

    private XmlMapper xmlMapper() {
        XmlMapper mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(NON_NULL);
        return mapper;
    }

    private List<UnitId> getMapReferenceItemsRequest() {
        return Stream.of(
                new UnitId("101209126683", 1026708L, "JB0206297"),
                new UnitId("101097686499", 1026708L, "JB1167926"),
                new UnitId("101099977624", 1026708L, "JB0333413")
        ).collect(Collectors.toList());
    }

    private List<IdentifierMappingDto> getMapReferenceItemsResponse() {
        return Stream.of(
                new IdentifierMappingDto(
                        new UnitId("101209126683", 1026708L, "JB0206297"),
                        new InforUnitId("ROV0000000000000000277", 1026708L)
                ),
                new IdentifierMappingDto(
                        new UnitId("101097686499", 1026708L, "JB1167926"),
                        new InforUnitId("ROV0000000000000000317", 1026708L)
                ),
                new IdentifierMappingDto(
                        new UnitId("101099977624", 1026708L, "JB0333413"),
                        new InforUnitId("ROV0000000000000000282", 1026708L)
                )
        ).collect(Collectors.toList());
    }
}
