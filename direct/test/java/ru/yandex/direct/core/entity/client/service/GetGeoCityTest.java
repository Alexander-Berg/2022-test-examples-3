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

import ru.yandex.direct.common.enums.YandexDomain;
import ru.yandex.direct.core.entity.client.model.office.GeoCity;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(Parameterized.class)
public class GetGeoCityTest {

    private static final Long KIEV_REGION_ID = 143L;

    private GeoTree geoTree;

    @Autowired
    private ClientOfficeService clientOfficeService;

    @Autowired
    private ClientGeoService clientGeoService;

    @Parameterized.Parameters(name = "regionId={0}, expectedGeoCity={1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {Region.ODESA_DISTRICT_REGION_ID, GeoCity.ODESSA},
                {KIEV_REGION_ID, GeoCity.KIEV},
                {Region.UKRAINE_REGION_ID, GeoCity.UA},
                {Region.KAZAKHSTAN_REGION_ID, GeoCity.KZ},
                {Region.BY_REGION_ID, GeoCity.BY},

                {Region.MOSCOW_REGION_ID, GeoCity.MOSCOW},
                {Region.MOSCOW_AND_MOSCOW_PROVINCE_REGION_ID, GeoCity.MOSCOW},
                {Region.SAINT_PETERSBURG_AND_LENINGRAD_OBLAST_REGION_ID, GeoCity.SPB},
                {Region.SVERDLOVSK_OBLAST_REGION_ID, GeoCity.EBURG},
                {Region.NOVOSIBIRSK_OBLAST_REGION_ID, GeoCity.NOVOSIB},
                {Region.NIZHNY_NOVGOROD_OBLAST_REGION_ID, GeoCity.N_NOVGOROD},

                {Region.CENTRAL_FEDERAL_DISTRICT_REGION_ID, GeoCity.CENTER},
                {Region.VORONEZH_OBLAST_REGION_ID, GeoCity.SOUTH},
                {Region.VOLGA_FEDERAL_DISTRICT_REGION_ID, GeoCity.VOLGA},
                {Region.PERM_KRAI_REGION_ID, GeoCity.URAL},

                {Region.NORTHWESTERN_FEDERAL_DISTRICT_REGION_ID, GeoCity.NORTH},
                {Region.REPUBLIC_OF_TATARSTAN_REGION_ID, GeoCity.TATARSTAN},
                {Region.URAL_FEDERAL_DISTRICT_REGION_ID, GeoCity.URAL},
                {Region.SIBERIAN_FEDERAL_DISTRICT_REGION_ID, GeoCity.SIBERIA},
                {Region.SOUTH_FEDERAL_DISTRICT_REGION_ID, GeoCity.SOUTH},

                {Region.RUSSIA_REGION_ID, GeoCity.RU},
                {Region.GLOBAL_REGION_ID, null},
        });
    }

    @Parameterized.Parameter
    public Long regionId;
    @Parameterized.Parameter(1)
    public GeoCity expectedGeoCity;

    @Before
    public void initTestData() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        geoTree = clientGeoService.getClientTranslocalGeoTree(YandexDomain.RU);
    }


    @Test
    public void checkGetGeoCity() {
        GeoCity geoCity = clientOfficeService.getGeoCity(regionId, geoTree);

        assertThat(geoCity).isEqualTo(expectedGeoCity);
    }
}
