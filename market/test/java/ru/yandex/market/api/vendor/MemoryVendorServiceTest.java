package ru.yandex.market.api.vendor;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.category.CategoryService;
import ru.yandex.market.api.common.ErrorLogger;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactory;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.VendorField;
import ru.yandex.market.api.domain.v2.NavigationCategoryV2;
import ru.yandex.market.api.domain.v2.VendorV2;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.cataloger.CatalogerService;
import ru.yandex.market.api.internal.report.ReportClient;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.PagedResult;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.vendor.load.VendorInfoSupplier;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.ContextHolderTestHelper.initContextWithApiVersion;

/**
 * Created by apershukov on 23.08.16.
 */
public class MemoryVendorServiceTest extends BaseTest {

    private Vendor horizon, blackHorns, blackCircus, paudiModels;
    private Category sportAndLeisure, treadmills, steppers, bikes;
    private NavigationCategoryV2 bikesV2, steppersV2;

    private MemoryVendorService service;

    @Inject
    private MarketUrls marketUrls;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        initContextWithApiVersion(Version.V2_0_0);

        ReportClient reportClient = mock(ReportClient.class);
        CategoryService categoryService = mock(CategoryService.class);
        CatalogerService catalogerService = mock(CatalogerService.class);
        VendorInfoSupplier vendorInfoSupplier = mock(VendorInfoSupplier.class);

        // Mock служащий для получения различной дополнительной информации для категорий.
        // Для всех категорий возвращает одну и ту же информацию.
        ru.yandex.market.api.category.Category categoryMock = mock(ru.yandex.market.api.category.Category.class);
        when(categoryMock.getLink()).thenReturn("https://market.yandex.ru/catalog/56006/");

        treadmills = new Category();
        treadmills.setId(91525);
        treadmills.setName("Treadmills");
        when(categoryService.getCategory(eq(treadmills.getId()), any())).thenReturn(categoryMock);

        steppers = new Category();
        steppers.setId(396898);
        steppers.setName("Steppers");
        when(categoryService.getCategory(eq(steppers.getId()), any())).thenReturn(categoryMock);

        steppersV2 = new NavigationCategoryV2();
        steppersV2.setId(396898);
        steppersV2.setName("Steppers");
        when(categoryService.getCategory(eq(steppersV2.getId()), any())).thenReturn(categoryMock);

        bikes = new Category();
        bikes.setId(91512);
        bikes.setName("Bikes");
        when(categoryService.getCategory(eq(bikes.getId()), any())).thenReturn(categoryMock);

        bikesV2 = new NavigationCategoryV2();
        bikesV2.setId(91512);
        bikesV2.setName("Bikes");
        when(categoryService.getCategory(eq(bikesV2.getId()), any())).thenReturn(categoryMock);

        sportAndLeisure = new Category();
        sportAndLeisure.setId(91512);
        sportAndLeisure.setName("Sport and leisure");
        sportAndLeisure.addInnerCategory(treadmills);
        sportAndLeisure.addInnerCategory(steppers);
        sportAndLeisure.addInnerCategory(bikes);
        when(categoryService.getCategory(eq(sportAndLeisure.getId()), any())).thenReturn(categoryMock);

        Long2ObjectMap<Vendor> vendors = new Long2ObjectOpenHashMap<>();

        horizon = new Vendor();
        horizon.setId(1042232);
        horizon.setName("Horizon");
        horizon.setCategories(singletonList(sportAndLeisure));
        horizon.setSite("www.world.horizonfitness.com");
        horizon.setPicture("https://mdata.yandex.net/i?path=b1125192216_img_id9076591173493315833.png");
        vendors.put(horizon.getId(), horizon);
        when(catalogerService.getVendorWithCategories(eq(horizon.getId()), anyInt(), anyCollectionOf(Field.class)))
            .thenReturn(Futures.newSucceededFuture(horizon));

        paudiModels = new Vendor(10716517, "Paudi Models", null, null, false);
        vendors.put(paudiModels.getId(), paudiModels);

        blackCircus = new Vendor(13557157, "Black Circus", null, null, false);
        vendors.put(blackCircus.getId(), blackCircus);

        blackHorns = new Vendor(8335013, "Black Horns", null, null, false);
        vendors.put(blackHorns.getId(), blackHorns);

        when(reportClient.getTopCategories(eq(horizon.getId()), anyInt()))
            .thenReturn(Pipelines.startWithValue(asList(bikesV2, steppersV2)));

        when(reportClient.getTopCategories(not(eq(horizon.getId())), anyInt()))
            .thenAnswer(invocation -> Pipelines.startWithValue(Collections.<NavigationCategoryV2>emptyList()));

        when(catalogerService.getVendors(anyObject(), anyInt(), anyCollectionOf(Field.class), any()))
            .thenReturn(Futures.newSucceededFuture(null));

        when(vendorInfoSupplier.get()).thenReturn(vendors);

        MemoryVendorCache cache = new MemoryVendorCache(mock(ErrorLogger.class), vendorInfoSupplier);
        cache.init();
        service = new MemoryVendorService(
            cache,
            catalogerService,
            reportClient,
            categoryService,
            marketUrls
        );
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        ContextHolderTestHelper.destroyContext();
    }

    @Test
    public void testGetV1VendorList() {
        initContextWithApiVersion(Version.V1_0_0);

        PagedResult<Vendor> vendors = Futures.waitAndGet(
            service.getVendorList(PageInfo.DEFAULT, EnumSet.noneOf(VendorField.class))
        );

        assertEquals(4, vendors.getElements().size());
        assertEquals(4, (int) vendors.getPageInfo().getTotal());
        assertEquals(10, vendors.getPageInfo().getCount());
        assertEquals(1, vendors.getPageInfo().getNumber());

        assertElementsClass(vendors.getElements(), Vendor.class);
    }

    @Test
    public void testGetV2VendorList() {
        PagedResult<VendorV2> vendors = Futures.waitAndGet(
            service.getVendorList(PageInfo.DEFAULT, EnumSet.noneOf(VendorField.class))
        );

        assertEquals(4, vendors.getElements().size());
        assertEquals(4, (int) vendors.getPageInfo().getTotal());
        assertEquals(10, vendors.getPageInfo().getCount());
        assertEquals(1, vendors.getPageInfo().getNumber());

        assertElementsClass(vendors.getElements(), VendorV2.class);
    }

    @Test
    public void testGetVendorV1() {
        initContextWithApiVersion(Version.V1_0_0);

        Vendor vendor = Futures.waitAndGet(
            service.getVendor(horizon.getId(), EnumSet.noneOf(VendorField.class))
        );

        assertEquals(horizon.getId(), vendor.getId());
        assertEquals(horizon.getName(), vendor.getName());
        assertEquals(horizon.getCategories(), vendor.getCategories());
        List<Category> expectedTopCategories = asList(
            new TopCategory(bikesV2, 0),
            new TopCategory(steppersV2, 0));
        assertEquals(expectedTopCategories, vendor.getTopCategories());

        assertNull(vendor.getLink());
    }

    @Test
    public void testGetVendorV1WithLink() {
        initContextWithApiVersion(Version.V1_0_0);

        Vendor vendor = Futures.waitAndGet(
            service.getVendor(horizon.getId(), EnumSet.of(VendorField.LINK))
        );

        assertEquals("http://market.yandex.ru/brands--horizon/1042232", vendor.getLink());
        vendor.getCategories().forEach(category -> assertNotNull(category.getLink()));
    }

    @Test
    public void testGetVendorV2() {
        VendorV2 vendor = Futures.waitAndGet(
            service.getVendor(horizon.getId(), EnumSet.noneOf(VendorField.class))
        );

        assertEquals(horizon.getId(), vendor.getId());
        assertEquals(horizon.getName(), vendor.getName());
        assertEquals(horizon.getSite(), vendor.getSite());
        assertEquals(horizon.getPicture(), vendor.getPicture());

        assertNull(vendor.getCategories());
        assertNull(vendor.getTopCategories());

        assertNull(vendor.getLink());
    }

    @Test
    public void testGetVendorV2WithLink() {
        VendorV2 vendor = Futures.waitAndGet(
            service.getVendor(horizon.getId(), EnumSet.of(VendorField.LINK))
        );
        assertEquals("http://market.yandex.ru/brands--horizon/1042232", vendor.getLink());
        assertNull(vendor.getCategories());
        assertNull(vendor.getTopCategories());
    }

    @Test(expected = NotFoundException.class)
    public void testVendorNotFound() {
        ContextHolder.get().setVersion(Version.V1_0_0);
        Futures.waitAndGet(service.getVendor(1, Collections.emptyList()));
    }

    @Test(expected = NotFoundException.class)
    public void testVendorV2NotFound() {
        ContextHolder.get().setVersion(Version.V2_0_0);
        Futures.waitAndGet(service.getVendor(1, Collections.emptyList()));
    }

    private void assertElementsClass(Collection<?> collection, Class<?> clazz) {
        assertTrue(collection.stream().allMatch(elem -> clazz.equals(elem.getClass())));
    }

}
