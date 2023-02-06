package ru.yandex.market.shopadminstub.beans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.market.checkout.common.xml.outlets.Outlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;

/**
 * @author oroboros
 * 29.08.14
 */
public class ShopOutletsServiceTest {

    private static final Outlet[] OUTLETS = new Outlet[]{
            new Outlet(), new Outlet(), new Outlet(), new Outlet()
    };

    static {
        int i = 1;
        for (Outlet outlet : OUTLETS) {
            outlet.shopPointCode = "strCode" + (i++);
            outlet.cityRegionId = 213;
            outlet.cost = BigDecimal.valueOf(20L);
            outlet.priceTo = BigDecimal.valueOf(10L);
        }
    }

    ShopOutletsService bean;

    @BeforeEach
    public void createBean() throws Exception {
        RegionTreePlainTextBuilder regionTreeBuilder = new RegionTreePlainTextBuilder();
        regionTreeBuilder.setPlainTextURL(ShopOutletsServiceTest.class.getResource("/geoexport.dat"));
        regionTreeBuilder.setSkipHeader(true);
        regionTreeBuilder.setSkipUnRootRegions(true);

        RegionService regionService = new RegionService();
        regionService.setRegionTreeBuilder(regionTreeBuilder);
        regionService.afterPropertiesSet();

        GeoInfoBean geoInfoBean = new GeoInfoBean();
        geoInfoBean.setGeoInfoService(regionService);

        bean = new ShopOutletsService(geoInfoBean);
        bean.setOutletsXmlUrl(ShopOutletsServiceTest.class.getResource("/outlets/shopsOutlet.xml"));
        bean.afterPropertiesSet();
    }

    /**
     * Would not work with CachedZooKeeperBean because of caching
     *
     * @throws Exception
     */
    @Test
    public void setAndGetMustBeEqual() throws Exception {
        bean.set(1, OUTLETS);
        Outlet[] gotOutlets = bean.get(1);
        Assertions.assertArrayEquals(gotOutlets, OUTLETS,"Outlets got must be equal to those put");
    }

    @Test
    public void getUnsetMustBeNull() throws Exception {
        Outlet[] gotOutlets = bean.get(123);
        Assertions.assertNull(gotOutlets, "Outlets got from unknown shop must be null");
    }

    @Test
    public void setNull() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            bean.set(1, null);
        });
    }


    @Test
    public void doWriteRead(@TempDir Path tempDir) throws Exception {
        File tmpFile = tempDir.resolve(UUID.randomUUID().toString()).toFile();
        FileOutputStream fout = new FileOutputStream(tmpFile);
        ObjectOutputStream so = new ObjectOutputStream(fout);
        so.writeObject(OUTLETS[0]);
        so.close();

        FileInputStream fis = new FileInputStream(tmpFile);
        ObjectInputStream oin = new ObjectInputStream(fis);
        Outlet newOutlet = (Outlet) oin.readObject();
        oin.close();

        Assertions.assertEquals(OUTLETS[0], newOutlet);
    }
}
