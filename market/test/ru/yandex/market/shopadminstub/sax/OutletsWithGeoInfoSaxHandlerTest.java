package ru.yandex.market.shopadminstub.sax;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.io.Resources;
import org.apache.xerces.parsers.SAXParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.market.checkout.common.xml.outlets.Outlet;
import ru.yandex.market.shopadminstub.beans.GeoInfoBean;
import ru.yandex.market.shopadminstub.beans.GeoInfoBeanTest;
import ru.yandex.market.shopadminstub.beans.saxhandlers.BaseOutletsSaxHandler;
import ru.yandex.market.shopadminstub.beans.saxhandlers.OutletsWithGeoInfoSaxHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Created by oroboros on 07.11.14.
 */
public class OutletsWithGeoInfoSaxHandlerTest {
    private GeoInfoBean geoInfoBean;

    @BeforeEach
    public void initGeoInfo() throws Exception {
        RegionTreePlainTextBuilder regionTreeBuilder = new RegionTreePlainTextBuilder();
        regionTreeBuilder.setPlainTextURL(GeoInfoBeanTest.class.getResource(GeoInfoBeanTest.GEODAT_FILE));
        regionTreeBuilder.setSkipHeader(true);
        regionTreeBuilder.setSkipUnRootRegions(true);

        RegionService regionService = new RegionService();
        regionService.setRegionTreeBuilder(regionTreeBuilder);
        regionService.afterPropertiesSet();

        geoInfoBean = new GeoInfoBean();
        geoInfoBean.setGeoInfoService(regionService);
    }

    @Test
    public void mustParseProperly() throws IOException, SAXException {
        Map<Long, Outlet[]> outlets = parseOutlets(new ByteArrayInputStream(XML.getBytes())).getOutlets();

        assertEquals(1,outlets.size(),"There must be exactly one shop");
        assertEquals(true,outlets.containsKey(123456L),"Shop id must be 123456");
        assertEquals(1,outlets.get(123456L).length,"There must be exactly one outlet (RETAIL must not be added)");
        Assertions.assertEquals("87654",outlets.get(123456L)[0].shopPointCode,"ShopPointCode must be 87654");
        assertEquals(213,outlets.get(123456L)[0].cityRegionId,"Region id must be 213");
    }

    @Test
    public void mustParseOutletsWithDelivery() throws IOException, SAXException {
        BaseOutletsSaxHandler handler = parseOutlets(Resources.getResource("outlets/outletsWithDeliveryService.xml").openStream());
        Map<Long, Outlet[]> outlets = handler.getOutlets();

        assertEquals(1,outlets.size(),"There must be exactly one shop");
        assertEquals(3,outlets.get(242102L).length,"Shop must have 3 outlets");
        Map<String, Outlet> parsedOutlets = Arrays.stream(outlets.get(242102L)).collect(Collectors.toMap(o -> o.shopPointCode, Function.identity()));


        Outlet checkOutlet;

        checkOutlet = parsedOutlets.get("69");
        assertNotNull(checkOutlet);
        assertEquals(2, checkOutlet.cityRegionId);
        assertEquals(BigDecimal.valueOf(4000), checkOutlet.priceTo = BigDecimal.valueOf(4000));
        assertEquals(BigDecimal.valueOf(50), checkOutlet.cost);
        assertNull(checkOutlet.type);
        assertEquals(0, checkOutlet.shipperId);

        checkOutlet = parsedOutlets.get("987");
        assertNotNull(checkOutlet);
        assertEquals(5, checkOutlet.cityRegionId);
        assertEquals(BigDecimal.valueOf(5000), checkOutlet.priceTo);
        assertEquals(BigDecimal.valueOf(60), checkOutlet.cost);
        assertNull(checkOutlet.type);
        assertEquals(0, checkOutlet.shipperId);

        checkOutlet = parsedOutlets.get("2896188");
        assertNotNull(checkOutlet);
        assertEquals(972, checkOutlet.cityRegionId);
        assertEquals(BigDecimal.valueOf(5000), checkOutlet.priceTo);
        assertEquals(BigDecimal.valueOf(165), checkOutlet.cost);
        assertEquals("depot", checkOutlet.type);
        assertEquals(22, checkOutlet.shipperId);
    }

    private OutletsWithGeoInfoSaxHandler parseOutlets(InputStream is) throws IOException, SAXException {
        OutletsWithGeoInfoSaxHandler handler = new OutletsWithGeoInfoSaxHandler(geoInfoBean);
        OutletsWithGeoInfoSaxHandler.parseXmlStream(is, handler, new SAXParser());
        return handler;
    }

    private final String XML =
            "<OutletInfo>\n" +
                    "  <shops>\n" +
                    "    <Shop id=\"123456\">\n" +
                    "      <outlet>\n" +
                    "        <PointId>98765</PointId>\n" +
                    "        <ShopPointId>87654</ShopPointId>\n" +
                    "        <PointName>Мир Мебели</PointName>\n" +
                    "        <PointType>MIXED</PointType>\n" +
                    "        <RegionId>213</RegionId>\n" +
                    "      </outlet>\n" +
                    "      <outlet>\n" +
                    "        <PointId>246210</PointId>\n" +
                    "        <ShopPointId>246210</ShopPointId>\n" +
                    "        <PointName>ipaintshop.ru</PointName>\n" +
                    "        <PointType>RETAIL</PointType>\n" +
                    "        <RegionId>213</RegionId>\n" +
                    "      </outlet>\n" +
                    "    </Shop>\n" +
                    "  </shops>" +
                    "</OutletInfo>";
}
