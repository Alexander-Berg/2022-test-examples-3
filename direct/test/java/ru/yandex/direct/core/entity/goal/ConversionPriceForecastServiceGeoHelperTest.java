package ru.yandex.direct.core.entity.goal;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.goal.service.ConversionPriceForecastServiceGeoHelper;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.regions.GeoTreeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.direct.regions.Region.AFRICA_REGION_ID;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.regions.Region.URAL_FEDERAL_DISTRICT_REGION_ID;
import static ru.yandex.direct.regions.Region.VOLGA_FEDERAL_DISTRICT_REGION_ID;

@CoreTest
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class ConversionPriceForecastServiceGeoHelperTest {

    ConversionPriceForecastServiceGeoHelper geoHelper;

    @Mock
    AdGroupService adGroupService;

    @Autowired
    GeoTreeFactory geoTreeFactory;

    @Mock
    ClientService clientService;

    private static final Long CLIENT_ID_L = 456L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(CLIENT_ID_L);

    private static final Long CAMP_ID = 10000L;

    private static final Long CLIENT_COUNTRY_ID = RUSSIA_REGION_ID;
    private static final List<Long> REGIONS = List.of(
            VOLGA_FEDERAL_DISTRICT_REGION_ID,
            -URAL_FEDERAL_DISTRICT_REGION_ID,
            CLIENT_COUNTRY_ID);

    private static final String URL = "https://market.yandex.ru";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        doReturn(CLIENT_COUNTRY_ID).when(clientService).getCountryRegionIdByClientIdStrict(CLIENT_ID);
        geoHelper = new ConversionPriceForecastServiceGeoHelper(adGroupService, clientService, geoTreeFactory);
    }

    @Test
    public void shouldReturnClientCountryIdAsGeo_whenHighLevelGeoInDefaultClientGeo() {
        List<Long> regionIdsWithGlobalId = new LinkedList<>(REGIONS);
        regionIdsWithGlobalId.add(0L);
        doReturn(Map.of(CAMP_ID, regionIdsWithGlobalId))
                .when(adGroupService).getDefaultGeoByCampaignId(eq(CLIENT_ID), eq(Set.of(CAMP_ID)));

        Map<Long, List<Long>> res = geoHelper.getCampRegionIds(CLIENT_ID, Map.of(CAMP_ID, URL));
        Map<Long, List<Long>> expectedResultWithClientCountryId = Map.of(CAMP_ID, List.of(CLIENT_COUNTRY_ID));
        assertThat(res).isEqualTo(expectedResultWithClientCountryId);
    }

    @Test
    public void shouldUpgradeRegionToCountry() {
        doReturn(Map.of(CAMP_ID, REGIONS))
                .when(adGroupService).getDefaultGeoByCampaignId(eq(CLIENT_ID), eq(Set.of(CAMP_ID)));

        Map<Long, List<Long>> res = geoHelper.getCampRegionIds(CLIENT_ID, Map.of(CAMP_ID, URL));
        Map<Long, List<Long>> expectedResultWithUpgradedToCountryRegionId = Map.of(CAMP_ID, List.of(CLIENT_COUNTRY_ID));
        assertThat(res).isEqualTo(expectedResultWithUpgradedToCountryRegionId);
    }

    @Test
    public void shouldReturnDefaultGeoWithoutMinusRegions_whenNoCommonAncestor() {
        List<Long> regionIdsWithOneMoreCountryId = new LinkedList<>(REGIONS);
        regionIdsWithOneMoreCountryId.add(AFRICA_REGION_ID);
        doReturn(Map.of(CAMP_ID, regionIdsWithOneMoreCountryId))
                .when(adGroupService).getDefaultGeoByCampaignId(eq(CLIENT_ID), eq(Set.of(CAMP_ID)));

        Map<Long, List<Long>> res = geoHelper.getCampRegionIds(CLIENT_ID, Map.of(CAMP_ID, URL));
        regionIdsWithOneMoreCountryId.remove(-URAL_FEDERAL_DISTRICT_REGION_ID);
        Map<Long, List<Long>> expectedResultWithDefaultGeoWithoutMinusRegions =
                Map.of(CAMP_ID, regionIdsWithOneMoreCountryId);
        assertThat(res).isEqualTo(expectedResultWithDefaultGeoWithoutMinusRegions);
    }

}
