package ru.yandex.market.delivery.transport_manager.facade.xdoc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.ff.client.dto.ShopRequestDetailsDTO;
import ru.yandex.market.ff.client.enums.RequestType;

class XDocFetcherFacadeTest {
    public static final int BREAK_BULK_XDOCK_INBOUND_TYPE = 1121;
    public static final int BREAK_BULK_XDOCK_TRANSPORT_INBOUND_TYPE = 1122;

    @Test
    void getTransportationSubtype() {
        XDocFetcherFacade facade = new XDocFetcherFacade(null, null, null, null, null);

        ShopRequestDetailsDTO shopRequestDetailsDTO = new ShopRequestDetailsDTO();
        shopRequestDetailsDTO.setType(BREAK_BULK_XDOCK_INBOUND_TYPE);

        Assertions.assertEquals(
            TransportationSubtype.BREAK_BULK_XDOCK,
            facade.getTransportationSubtype(shopRequestDetailsDTO)
        );
    }

    @Test
    void getTransportationSubtypeNull() {
        XDocFetcherFacade facade = new XDocFetcherFacade(null, null, null, null, null);

        ShopRequestDetailsDTO shopRequestDetailsDTO = new ShopRequestDetailsDTO();
        shopRequestDetailsDTO.setType(RequestType.X_DOC_PARTNER_SUPPLY_TO_FF.getId());

        Assertions.assertNull(facade.getTransportationSubtype(shopRequestDetailsDTO));
    }

    @Test
    void getTransportationSubtypeIllegal() {
        XDocFetcherFacade facade = new XDocFetcherFacade(null, null, null, null, null);

        ShopRequestDetailsDTO shopRequestDetailsDTO = new ShopRequestDetailsDTO();
        shopRequestDetailsDTO.setType(BREAK_BULK_XDOCK_TRANSPORT_INBOUND_TYPE);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> facade.getTransportationSubtype(shopRequestDetailsDTO)
        );
    }
}
