package ru.yandex.market.api.geo;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.Region;
import ru.yandex.market.api.domain.v2.BaseRegionV2;
import ru.yandex.market.api.domain.v2.RegionField;
import ru.yandex.market.api.domain.v2.RegionV2;
import ru.yandex.market.api.domain.v2.wishlist.RegionUtils;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.PagedResult;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.ContextHolderTestHelper.initContextWithApiVersion;
import static ru.yandex.market.api.geo.GeoRegionService.DEFAULT_FIELDS;

/**
 * Created by apershukov on 19.08.16.
 */
@WithMocks
public class GeoRegionServiceTest extends BaseTest {

    private GeoRegionService service;

    @Mock
    private CachedGeoDataSupplier geoDataSupplier;

    @Inject
    private RegionUtils regionUtils;

    private GeoRegion sng, russia, ukraine, kazakhstan, belarus, uralFO, centralFO, siberiaFO, farEastFO, southFO;

    @Before
    public void setUp() {
        initContextWithApiVersion(Version.V2_0_0);

        Int2ObjectMap<GeoRegion> regionIndex = new Int2ObjectOpenHashMap<>();

        sng = new GeoRegion();
        sng.setId(166);
        regionIndex.put(sng.getId(), sng);

        russia = new GeoRegion();
        russia.setId(225);
        russia.setName("Russia");
        russia.setParentId(sng.getId());
        russia.setChildrenCount(17);
        russia.setCountry(225);
        regionIndex.put(russia.getId(), russia);

        ukraine = new GeoRegion();
        ukraine.setId(187);
        ukraine.setParentId(sng.getId());
        regionIndex.put(ukraine.getId(), ukraine);

        kazakhstan = new GeoRegion();
        kazakhstan.setId(159);
        kazakhstan.setParentId(sng.getId());
        regionIndex.put(kazakhstan.getId(), kazakhstan);

        belarus = new GeoRegion();
        belarus.setId(149);
        belarus.setParentId(sng.getId());
        regionIndex.put(belarus.getId(), belarus);

        uralFO = new GeoRegion();
        uralFO.setId(52);
        uralFO.setParentId(russia.getId());
        regionIndex.put(uralFO.getId(), uralFO);

        centralFO = new GeoRegion();
        centralFO.setId(3);
        centralFO.setParentId(russia.getId());
        regionIndex.put(centralFO.getId(), centralFO);

        siberiaFO = new GeoRegion();
        siberiaFO.setId(59);
        siberiaFO.setParentId(russia.getId());
        regionIndex.put(siberiaFO.getId(), siberiaFO);

        farEastFO = new GeoRegion();
        farEastFO.setId(73);
        farEastFO.setParentId(russia.getId());
        farEastFO.setCountry(russia.getId());
        regionIndex.put(farEastFO.getId(), farEastFO);

        southFO = new GeoRegion();
        southFO.setId(26);
        southFO.setParentId(russia.getId());
        regionIndex.put(southFO.getId(), southFO);

        Int2ObjectMap<IntList> children = new Int2ObjectOpenHashMap<>();
        children.put(russia.getId(), new IntArrayList(asList(centralFO.getId(), uralFO.getId(),
            siberiaFO.getId(), farEastFO.getId(), southFO.getId())));

        service = new GeoRegionService(geoDataSupplier, regionUtils);
        when(geoDataSupplier.getGeoData()).thenReturn(new GeoData(regionIndex, children, null));
        service.init();
    }

    @After
    public void tearDown() {
        ContextHolderTestHelper.destroyContext();
    }

    @Test
    public void testGetRootsV2() {
        PageInfo pageInfo = PageInfo.ALL_ITEMS;
        PagedResult<RegionV2> geoRegions = service.getRoots(DEFAULT_FIELDS, pageInfo);

        Set<Integer> expected = Sets.newHashSet(toIds(asList(russia, ukraine, kazakhstan, belarus)));
        Set<Integer> actual = Sets.newHashSet(toIds(geoRegions.getElements()));

        assertEquals(expected, actual);
        assertEquals(1, geoRegions.getPageInfo().getNumber());
        assertEquals(4, geoRegions.getPageInfo().getCount());
        assertEquals(4, (int) geoRegions.getPageInfo().getTotal());

        assertElementsClass(geoRegions, RegionV2.class);
    }

    @Test
    public void testGetRootsV1() {
        initContextWithApiVersion(Version.V1_0_0);

        PageInfo pageInfo = PageInfo.ALL_ITEMS;
        PagedResult<GeoRegion> geoRegions = service.getRoots(DEFAULT_FIELDS, pageInfo);

        assertElementsClass(geoRegions, GeoRegion.class);

        assertEquals(asList(russia, ukraine, kazakhstan, belarus), geoRegions.getElements());
        assertEquals(1, geoRegions.getPageInfo().getNumber());
        assertEquals(4, geoRegions.getPageInfo().getCount());
        assertEquals(4, (int) geoRegions.getPageInfo().getTotal());
    }

    @Test
    public void testEmptyParentsByDefault() {
        PagedResult<RegionV2> geoRegions = service.getRoots(DEFAULT_FIELDS, PageInfo.ALL_ITEMS);

        assertTrue(geoRegions.getElements().stream()
            .allMatch(region -> region.getParent() == null));
    }

    @Test
    public void testGetChildrenV2() {
        PagedResult<RegionV2> regionList = service.getChildren(russia.getId(), DEFAULT_FIELDS,
            PageInfo.ALL_ITEMS);

        assertElementsClass(regionList, RegionV2.class);

        Set<Integer> expected = Sets.newHashSet(toIds(asList(uralFO, centralFO, siberiaFO, farEastFO, southFO)));
        Set<Integer> actual = Sets.newHashSet(toIds(regionList.getElements()));

        assertEquals(expected, actual);
    }

    @Test
    public void testGetChildrenV2Paging() {
        PagedResult<RegionV2> regionList = service.getChildren(russia.getId(), DEFAULT_FIELDS,
            PageInfo.fromTotalElements(2, 4, 5));

        assertEquals(2, regionList.getPageInfo().getPage());
        assertEquals(4, regionList.getPageInfo().getCount());
        assertEquals(2, (int) regionList.getPageInfo().getTotalPages());
    }

    @Test
    public void testGetRegionWithParent() {
        RegionV2 region = service.getRegion(uralFO.getId(), singleton(RegionField.PARENT));

        RegionV2 parent = region.getParent();
        assertEquals(russia.getId(), parent.getId());
        assertEquals(russia.getName(), parent.getName());
        assertEquals(russia.getType(), parent.getType());
        assertNull(parent.getParent());
    }

    @Test
    public void testGetChildrenV1() {
        initContextWithApiVersion(Version.V1_0_0);

        PagedResult<GeoRegion> regionList = service.getChildren(russia.getId(), DEFAULT_FIELDS,
            PageInfo.ALL_ITEMS);

        assertElementsClass(regionList, GeoRegion.class);

        Set<Integer> expected = Sets.newHashSet(toIds(asList(uralFO, centralFO, siberiaFO, farEastFO, southFO)));
        Set<Integer> actual = Sets.newHashSet(toIds(regionList.getElements()));

        assertEquals(expected, actual);
    }

    @Test
    public void testGetChildrenV1Paging() {
        initContextWithApiVersion(Version.V1_0_0);

        PagedResult<RegionV2> regionList = service.getChildren(russia.getId(), DEFAULT_FIELDS,
            PageInfo.fromTotalElements(2, 4, 5));

        assertEquals(2, regionList.getPageInfo().getPage());
        assertEquals(4, regionList.getPageInfo().getCount());
        assertEquals(2, (int) regionList.getPageInfo().getTotalPages());
    }

    @Test
    public void testGetRootsV2SecondPage() {
        PageInfo pageInfo = new PageInfo(2, 2);
        PagedResult<RegionV2> geoRegions = service.getRoots(DEFAULT_FIELDS, pageInfo);

        List<Integer> expected = toIds(asList(kazakhstan, belarus));

        assertEquals(expected, toIds(geoRegions.getElements()));
        assertEquals(2, geoRegions.getPageInfo().getNumber());
        assertEquals(2, geoRegions.getPageInfo().getCount());
        assertEquals(4, (int) geoRegions.getPageInfo().getTotal());
    }

    @Test
    public void testGetRegionV2() {
        RegionV2 region = service.getRegion(russia.getId(), DEFAULT_FIELDS);

        assertEquals(russia.getId(), region.getId());
        assertEquals(russia.getName(), region.getName());
        assertEquals((int) russia.getChildrenCount(), region.getChildCount());
        assertEquals(russia.getType(), region.getType());
        assertNull(region.getParent());
    }

    /**
     * Проверяем правильность заполнения информации о стране для региона не являющегося страной
     */
    @Test
    public void testGetRegionV2Country() {
        RegionV2 region = service.getRegion(farEastFO.getId(), DEFAULT_FIELDS);

        BaseRegionV2 country = region.getCountryInfo();
        assertNotNull(country);
        assertEquals(russia.getId(), country.getId());
    }

    /**
     * Проверяем правильность заполнения информации о стране для региона являющегося страной
     */
    @Test
    public void testGetRegionV2CountryForCountry() {
        RegionV2 region = service.getRegion(russia.getId(), DEFAULT_FIELDS);

        BaseRegionV2 country = region.getCountryInfo();
        assertNotNull(country);
        assertEquals(russia.getId(), country.getId());
    }

    /**
     * Проверяем правильность заполнения информации о стране для региона не принадлежащего ни одной страной
     */
    @Test
    public void testGetRegionV2CountryWithoutCountr() {
        RegionV2 region = service.getRegion(sng.getId(), DEFAULT_FIELDS);

        BaseRegionV2 country = region.getCountryInfo();
        assertNull("У региона нет страны", country);
    }

    @Test
    public void testGetChildsWithParent() {
        PagedResult<RegionV2> regions = service.getChildren(russia.getId(), singleton(RegionField.PARENT),
            PageInfo.ALL_ITEMS);

        regions.getElements().forEach(region -> {
            RegionV2 parent = region.getParent();
            assertEquals(russia.getId(), parent.getId());
            assertEquals(russia.getName(), parent.getName());
            assertEquals(russia.getType(), parent.getType());
            assertNull(parent.getParent());
        });
    }

    @Test
    public void testGetChildrenV1AlwaysHaveParentId() {
        initContextWithApiVersion(Version.V1_0_0);

        PagedResult<GeoRegion> regions = service.getChildren(russia.getId(), EnumSet.noneOf(RegionField.class),
            PageInfo.ALL_ITEMS);

        regions.getElements().forEach(region -> assertEquals(russia.getId(), (int) region.getParentId()));
    }

    @Test
    public void testGetRegionV1() {
        initContextWithApiVersion(Version.V1_0_0);
        GeoRegion region = service.getRegion(russia.getId(), DEFAULT_FIELDS);
        assertEquals(russia, region);
    }

    @Test
    public void testGetChildrenPaging() {
        PagedResult<RegionV2> regions = service.getChildren(russia.getId(), DEFAULT_FIELDS, new PageInfo(2, 2));

        assertEquals(2, regions.getElements().size());
        assertEquals(2, regions.getPageInfo().getCount());
        assertEquals(2, regions.getPageInfo().getNumber());
        assertEquals(5, (int) regions.getPageInfo().getTotal());
    }

    private void assertElementsClass(PagedResult<?> result, Class<?> clazz) {
        assertTrue(result.getElements().stream().allMatch(elem -> clazz.equals(elem.getClass())));
    }


    private List<Integer> toIds(List<? extends Region> regions) {
        return regions.stream().map(Region::getId).collect(Collectors.toList());
    }
}
