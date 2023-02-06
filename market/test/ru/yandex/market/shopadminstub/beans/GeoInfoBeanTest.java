package ru.yandex.market.shopadminstub.beans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.common.util.region.RegionTreePlainTextBuilder;
import ru.yandex.market.shopadminstub.errors.GeoInfoException;

/**
 * @author oroboros
 * 29.08.14
 */
public class GeoInfoBeanTest {
    public static final String GEODAT_FILE = "/geoexport.dat";
    private static final int Unknown = 999999999;
    private static final int Chad = 21331;
    private static final int Moscow = 213;
    private static final int SAO = 20356;

    private static GeoInfoBean bean;

    @BeforeAll
    public static void createTmpGeobaseAndGeoInfoBean() throws Exception {
        RegionTreePlainTextBuilder regionTreeBuilder = new RegionTreePlainTextBuilder();
        regionTreeBuilder.setPlainTextURL(ShopOutletsServiceTest.class.getResource("/geoexport.dat"));
        regionTreeBuilder.setSkipHeader(true);
        regionTreeBuilder.setSkipUnRootRegions(true);

        RegionService regionService = new RegionService();
        regionService.setRegionTreeBuilder(regionTreeBuilder);
        regionService.afterPropertiesSet();

        bean = new GeoInfoBean();
        bean.setGeoInfoService(regionService);
    }

    @Test
    public void cityMustStayCity() throws GeoInfoException {
        Assertions.assertEquals(bean.toCity(Moscow),Moscow,"Moscow must stay moscow");
    }

    @Test
    public void regionMustBeUprisedToCity() throws GeoInfoException {
        Assertions.assertEquals(bean.toCity(SAO),Moscow,"SAO must be uprised to Moscow");
    }

    @Test
    public void countryMustNotBeUprisedToCity() {
        Assertions.assertThrows(GeoInfoException.class, () -> {
            bean.toCity(Chad);
        });
    }

    @Test
    public void unknownRegionId() {
        Assertions.assertThrows(GeoInfoException.class, () -> {
            bean.toCity(Unknown);
        });
    }
}
