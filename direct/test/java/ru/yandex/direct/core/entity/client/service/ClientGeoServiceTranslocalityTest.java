package ru.yandex.direct.core.entity.client.service;

import java.util.Arrays;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.regions.Region.CRIMEA_REGION_ID;
import static ru.yandex.direct.regions.Region.GLOBAL_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.UKRAINE_REGION_ID;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class ClientGeoServiceTranslocalityTest {
    private static final long RYAZAN_REGION_ID = 11L;
    private static final long RYAZAN_PROVINCE_REGION_ID = 10776L;

    @Autowired
    private ClientGeoService clientGeoService;

    @Autowired
    private Steps steps;

    private TestContextManager testContextManager = new TestContextManager(ClientGeoServiceTranslocalityTest.class);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"Крым принадлежит России (российский клиент)", CRIMEA_REGION_ID, RUSSIA_REGION_ID, RUSSIA_REGION_ID},

                {"Крым принадлежит Украине (украинский клиент)", CRIMEA_REGION_ID, UKRAINE_REGION_ID,
                        UKRAINE_REGION_ID},
                {"Крым принадлежит России (крымский клиент)", CRIMEA_REGION_ID, RUSSIA_REGION_ID, CRIMEA_REGION_ID},
                {"Крым принадлежит России (др. клиенты, не из России)", CRIMEA_REGION_ID, RUSSIA_REGION_ID,
                        GLOBAL_REGION_ID},

                {"Рязань принадлежит Рязанской области (российский клиент)", RYAZAN_REGION_ID,
                        RYAZAN_PROVINCE_REGION_ID, RUSSIA_REGION_ID},
                {"Рязань принадлежит Рязанской области (др. клиент)", RYAZAN_REGION_ID, RYAZAN_PROVINCE_REGION_ID,
                        GLOBAL_REGION_ID}
        });
    }

    private long regionId;
    private long parentRegionId;
    private long clientCountryId;
    private ClientId clientId;

    public ClientGeoServiceTranslocalityTest(String testName, long regionId, long parentRegionId, long clientCountryId) {
        this.regionId = regionId;
        this.parentRegionId = parentRegionId;
        this.clientCountryId = clientCountryId;
    }

    @Before
    public void setUp() throws Exception {
        testContextManager.prepareTestInstance(this);

        clientId = steps.clientSteps().createClient(new ClientInfo()
                .withClient(defaultClient()
                        .withCountryRegionId(clientCountryId))).getClientId();
    }

    @Test
    public void testTranslocality() {
        GeoTree geoTree = clientGeoService.getClientTranslocalGeoTree(clientId);

        Region region = geoTree.getRegion(regionId);

        Region parent = region.getParent();
        assertThat(parent.getId(), is(parentRegionId));
    }
}
