package ru.yandex.market.mbi.affiliate.promo.regions;

import java.util.List;

import org.dbunit.database.DatabaseConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import retrofit2.Response;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.dao.VarsDao;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class RegionServiceTest {

    @Autowired
    private VarsDao varsDao;
    private RegionService regionService;

    @Before
    public void setup() throws Exception {
        GeobaseApi api = mock(GeobaseApi.class, Mockito.RETURNS_DEEP_STUBS);
        when(api.getByType(RegionService.TYPE_REPUBLIC).execute())
                .thenReturn(Response.success(List.of(
                        new Region(10, "Регион10"),
                        new Region(11, "Регион11"),
                        new Region(20, "Регион20"),
                        new Region(100, "Регион100")
                )));
        when(api.getChildren(RegionService.RUSSIA_ID).execute())
                .thenReturn(Response.success(List.of(1, 2)));
        when(api.getChildren(1).execute())
                .thenReturn(Response.success(List.of(10, 11)));
        when(api.getChildren(2).execute())
                .thenReturn(Response.success(List.of(20)));
        regionService = new RegionService(api, varsDao);
        regionService.setUp();
    }

    @After
    public void tearDown() {
        if (regionService != null) {
            regionService.tearDown();
        }
    }

    @Test
    public void testGetAvailable() {
        var result = regionService.getAvailableRegions();
        assertThat(result).containsExactlyInAnyOrder(
                new Region(10, "Регион10"),
                new Region(11, "Регион11"),
                new Region(20, "Регион20"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "regions_standard_before.csv")
    public void testGetStandard() {
        var result = regionService.getStandardRegions();
        assertThat(result).containsExactlyInAnyOrder(
                new Region(10, "Регион10"),
                new Region(11, "Регион11"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "regions_standard_empty.csv")
    public void testGetStandardNull() {
        assertThat(regionService.getStandardRegions()).isNull();
    }

    @Test
    @DbUnitDataSet(
            dataSource = "promoDataSource",
            before = "regions_standard_before.csv",
            after = "regions_standard_after.csv")
    public void testSetStandardRegions() {
        regionService.setStandardRegions(List.of(10, 20));
    }

    @Test
    @DbUnitDataSet(
            dataSource = "promoDataSource",
            before = "regions_standard_before.csv",
            after = "regions_standard_empty.csv")
    public void testSetStandardRegionsNull() {
        regionService.setStandardRegions(null);
    }
}