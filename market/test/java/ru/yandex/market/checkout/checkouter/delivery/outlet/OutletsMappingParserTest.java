package ru.yandex.market.checkout.checkouter.delivery.outlet;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import com.google.common.io.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.common.xml.outlets.OutletType;

/**
 * @author mmetlov
 */
public class OutletsMappingParserTest {

    private final long SHOP_ID = 242102;

    @Test
    public void testParsing() throws XMLStreamException, IOException {

        try (InputStream inputStream = Resources.getResource("outlets/shopsOutlet.xml").openStream()) {
            OutletsMappingParser parser = new OutletsMappingParser(inputStream);
            parser.parse();
            OutletsMapping mapping = parser.getMapping();
            Assertions.assertEquals(mapping.size(), 2, "There must be 2 outlets");

            Assertions.assertEquals(new MarketOutletId(SHOP_ID, 466336L),
                    mapping.getMarketByShop(new ShopOutletId(SHOP_ID, "69")), "Shop outlet id should point to market " +
                            "outlet id");
            Assertions.assertEquals(new ShopOutletId(SHOP_ID, "69"), mapping.getShopByMarket(new MarketOutletId(SHOP_ID,
                    466336)), "Market outlet id should point to shop outlet id");
            ShopOutletMeta outletMeta = mapping.getOutletMetaByMarketId(new MarketOutletId(SHOP_ID, 466336));
            Assertions.assertEquals(OutletType.DEPOT, outletMeta.getType(), "Shop outlet should have type");
            Assertions.assertEquals(99L, outletMeta.getDeliveryServiceId(), "Shop outlet should have " +
                    "deliveryServiceId");
        }
    }
}
