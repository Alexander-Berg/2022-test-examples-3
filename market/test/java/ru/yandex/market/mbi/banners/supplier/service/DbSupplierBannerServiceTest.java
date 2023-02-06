package ru.yandex.market.mbi.banners.supplier.service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.model.PartnerPlacementType;
import ru.yandex.market.mbi.banners.FunctionalTest;
import ru.yandex.market.mbi.banners.supplier.SupplierBannerDao;
import ru.yandex.market.mbi.banners.supplier.model.BannerHash;
import ru.yandex.market.mbi.banners.supplier.model.BannerType;
import ru.yandex.market.mbi.banners.supplier.model.BlueBannerCache;
import ru.yandex.market.mbi.banners.supplier.model.SupplierAlivenessState;
import ru.yandex.market.mbi.banners.supplier.model.SupplierBanner;
import ru.yandex.market.mbi.banners.supplier.model.filter.SupplierBannerFilter;
import ru.yandex.market.mbi.banners.supplier.model.filter.condition.OnBoardingState;
import ru.yandex.market.mbi.banners.supplier.model.filter.condition.OnBoardingStateCondition;
import ru.yandex.market.mbi.banners.yt.YtSupplierIdsDao;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbi.banners.supplier.service.SupplierBannerService.DEFAULT_BANNER_PAGE_IDS;

class DbSupplierBannerServiceTest extends FunctionalTest {
    @Autowired
    private SupplierBannerService supplierBannerService;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private YtSupplierIdsDao ytSupplierIdsDao;

    private static SupplierBanner getBannerA() {
        return new SupplierBanner.Builder()
                .setId("aaa")
                .setPermanent(false)
                .setImg("img_a")
                .setText("text_a")
                .setButton("button_a")
                .setUrl("url_a")
                .setBackgroundColorHex("aaaaaa")
                .setSeverity(0)
                .build();
    }

    private static SupplierBanner getBannerB() {
        return new SupplierBanner.Builder()
                .setId("bbb")
                .setPermanent(true)
                .setImg("img_b")
                .setText("text_b")
                .setButton("button_b")
                .setUrl("url_b")
                .setBackgroundColorHex("bbbbbb")
                .setSeverity(1)
                .build();
    }

    private static SupplierBanner getBannerC() {
        return new SupplierBanner.Builder()
                .setId("ccc")
                .setPermanent(false)
                .setImg("img_c")
                .setText("text_c")
                .setButton("button_c")
                .setUrl("url_c")
                .setBackgroundColorHex("cccccc")
                .setSeverity(2)
                .build();
    }

    private static SupplierBanner getBannerG() {
        return new SupplierBanner.Builder()
                .setId("ggg")
                .setPermanent(true)
                .setImg("img_g")
                .setText("text_g")
                .setButton(null)
                .setUrl(null)
                .setBackgroundColorHex(null)
                .setSeverity(1)
                .build();
    }

    private static SupplierBanner getBannerF() {
        return new SupplierBanner.Builder()
                .setId("fff")
                .setPermanent(true)
                .setImg("img_f")
                .setText("text_f")
                .setSeverity(1)
                .build();
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "SupplierBannerServiceTest.before.csv",
            dataSource = "mbiDbDataSource"
    )
    void shouldReturnBannersBySupplierIdPageIdsTest() {
        Set<SupplierBanner> result = supplierBannerService.fetchSupplierBannersBySupplierIdPageIds(111L, Set.of(
                "page1"));
        assertThat(result).containsExactlyInAnyOrder(
                getBannerA(),
                getBannerB()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "BannerHashServiceTest.before.csv",
            dataSource = "mbiDbDataSource"
    )
    void shouldReturnBannerHashesTest() {
        Set<BannerHash> result = supplierBannerService.fetchBannersHash(Set.of("111", "112"));
        assertThat(result).containsExactlyInAnyOrder(
                BannerHash.newBuilder().withBannerId("111").withHashId(813798121).build(),
                BannerHash.newBuilder().withBannerId("112").withHashId(813798122).build()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "BannerHashServiceTest.before.csv",
            after = "BannerHashServiceTest.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void updateBannerHashesTest() {
        Set<BannerHash> result = supplierBannerService.fetchBannersHash(Set.of("111", "112"));
        result.add(BannerHash.newBuilder().withBannerId("113").withHashId(813798123).build());
        supplierBannerService.updateBannersHash(result);
    }

    @Test
    @DbUnitDataSet(
            before = "BannerHashServiceTest.before.csv",
            after = "BannerHashServiceTest.replaced.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void replaceBannerHashesTest() {
        Set<BannerHash> result = Set.of(BannerHash.newBuilder().withBannerId("113").withHashId(813798123).build());
        supplierBannerService.replaceAllBannersHash(result);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "SupplierBannerServiceTest.before.csv",
            dataSource = "mbiDbDataSource"
    )
    void shouldReturnBannersByPageIdsTest() {
        Set<SupplierBanner> result = supplierBannerService.fetchSupplierBannersByPageIds(Set.of("page1", "page2"));
        assertThat(result).containsExactlyInAnyOrder(
                getBannerG()
        );
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "SupplierBannerServiceTest.before.csv",
            dataSource = "mbiDbDataSource"
    )
    void shouldReturnAllBannersTest() {
        BlueBannerCache blueBannerCache = supplierBannerService.fetchAllSupplierBanners();
        Map<Long, Map<String, Set<SupplierBanner>>> result = blueBannerCache.getFullMapping();

        assertThat(result.get(111L)).hasSize(2);
        assertThat(result.get(111L).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())).containsExactlyInAnyOrder(
                getBannerA(),
                getBannerB(),
                getBannerC()
        );
        assertThat(result.get(111L).keySet()).containsExactlyInAnyOrder(
                "page1",
                "page2"
        );
        assertThat(result.get(222L).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())).containsExactlyInAnyOrder(
                getBannerA(),
                getBannerF()
        );
        assertThat(result.get(222L).keySet()).containsExactlyInAnyOrder(
                "page1",
                "page2",
                "page3"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "updateSupplierBannersInfoTest.before.csv",
            after = "updateSupplierBannersInfoTest.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void updateSupplierBannersInfoTest() {
        supplierBannerService.clearBlueBannersInfoTable();
        supplierBannerService.updateSupplierBannersInfo(Sets.newHashSet(getBannerA(), getBannerB(), getBannerF()));
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetFulfillment.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetFulfillment() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(List.of("page1"))
                .setSuppliersTypes(List.of(PartnerPlacementType.FULFILLMENT))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);

        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetDropship.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetDropship() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.DROPSHIP))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetCrossdock.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetCrossdock() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.CROSSDOCK))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetDropshipBySeller.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetDbs() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.DROPSHIP_BY_SELLER))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetClickAndCollect.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetClickAndCollect() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.CLICK_AND_COLLECT))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetDropshipCrossdock.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetDropshipCrossdock() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.DROPSHIP, PartnerPlacementType.CROSSDOCK))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "allDeliveryModeTypeTargets.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetAllDeliveryModes() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(
                        PartnerPlacementType.FULFILLMENT,
                        PartnerPlacementType.DROPSHIP,
                        PartnerPlacementType.CROSSDOCK,
                        PartnerPlacementType.CLICK_AND_COLLECT
                ))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "allDeliveryModeTypeTargets.noActiveFilters.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetNoDeliveryModes() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetSuppliers.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetSuppliers() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierIds(List.of(100000000001L, 100000000003L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetBusinesses.after.csv",
            dataSource = "mbiDbDataSource")
    void targetAllBusinesess() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setBusinessIds(Collections.singletonList(100000000010L))
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setBannerType(BannerType.BUSINESS)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetBusinesses.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetBusinesessBySupplier() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setSupplierIds(Collections.singletonList(100000000001L))
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setBannerType(BannerType.BUSINESS)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetBusinesses.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetBusinesessByWarehouses() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setWarehousesIds(List.of(300000000001L))
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setBannerType(BannerType.BUSINESS)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetIsNewbie.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetIsNewbie() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .addCondition(new OnBoardingStateCondition(OnBoardingState.NEWBIE))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetWarehouses.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetWarehouses() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setWarehousesIds(List.of(300000000002L, 300000000004L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetSortingCenters.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetSortingCenters() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSortingCentersIds(List.of(300000000009L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetOffersCount.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetOffersCount() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setOffersCount(42)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetOffersCountClickAndCollect.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetOffersCountClickAndCollect() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.CLICK_AND_COLLECT))
                .setOffersCount(39)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetWarehouseDropship.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetWarehouseDropship() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.DROPSHIP))
                .setWarehousesIds(List.of(300000000002L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetSupplierCrossdock.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetSupplierCrossdock() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSuppliersTypes(List.of(PartnerPlacementType.CROSSDOCK))
                .setSupplierIds(List.of(100000000003L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetSupplierWarehouseOffersCountFulfillment.after.csv",
            dataSource = "mbiDbDataSource")
    void targetSupplierWarehouseOffersCountFulfillment() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierIds(List.of(100000000001L))
                .setWarehousesIds(List.of(300000000001L))
                .setOffersCount(40)
                .setSuppliersTypes(List.of(PartnerPlacementType.FULFILLMENT))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "targetRegion.before.csv",
            after = "targetRegion.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetRegion() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationRegionsIds(List.of(21212L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "targetChildRegion.before.csv",
            after = "targetChildRegion.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetChildRegion() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationRegionsIds(List.of(21213L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "targetGrandchildRegion.before.csv",
            after = "targetGrandchildRegion.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetGrandchildRegion() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationRegionsIds(List.of(21214L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "targetMultipleChildRegions.before.csv",
            after = "targetMultipleChildRegions.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetMultipleChildRegions() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationRegionsIds(List.of(21213L, 21214L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "testSingleFilterFailureHandling.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void testSingleFilterFailureHandling() {
        NamedParameterJdbcTemplate jdbcTemplate = Mockito.spy(namedParameterJdbcTemplate);
        Mockito.doAnswer(invocation -> {
            Object arg1 = invocation.getArgument(1);
            if (arg1 instanceof MapSqlParameterSource) {
                MapSqlParameterSource params = (MapSqlParameterSource) arg1;
                var supplierId = (Long) params.getValue("supplierid_array_0");
                if (Objects.equals(supplierId, 100000000002L)) {
                    throw new RuntimeException("Simulate DB query failure on second filter");
                }
            }
            return invocation.callRealMethod();
        }).when(jdbcTemplate)
                .query(ArgumentMatchers.anyString(),
                        ArgumentMatchers.any(SqlParameterSource.class),
                        ArgumentMatchers.<ResultSetExtractor<Long>>any());

        YtSupplierIdsDao ytDao = Mockito.spy(ytSupplierIdsDao);
        Mockito.doReturn(null)
                .when(ytDao)
                .getSuppliers(Mockito.any());

        SupplierBannerFilter filter1 = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierIds(List.of(100000000001L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        SupplierBannerFilter filter2 = new SupplierBannerFilter.Builder()
                .setId("banner2")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierIds(List.of(100000000002L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        SupplierBannerFilter filter3 = new SupplierBannerFilter.Builder()
                .setId("banner3")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierIds(List.of(100000000003L))
                .setBannerType(BannerType.SUPPLIER)
                .build();

        DbSupplierBannerService supplierBannerService =
                new DbSupplierBannerService(new SupplierBannerDao(jdbcTemplate, ytDao));
        BlueBannerImportState importState = supplierBannerService.calculateFullSupplierBannerMappings(Set.of(
                filter1,
                filter2,
                filter3
        ));

        assertThat(importState.getErrors()).hasSize(1);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetRegistrationDateSince.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetRegistrationDateSince() {
        ZonedDateTime regSince = ZonedDateTime.of(2000, 1, 2, 0, 0, 1, 0, ZoneId.systemDefault());
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationDateSince(Date.from(regSince.toInstant()))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetRegistrationDateUntil.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetRegistrationDateUntil() {
        ZonedDateTime regUntil = ZonedDateTime.of(2000, 1, 4, 23, 59, 59, 0, ZoneId.systemDefault());
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationDateUntil(Date.from(regUntil.toInstant()))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetRegistrationDateSinceUntil.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetRegistrationDateSinceUntil() {
        ZonedDateTime regSince = ZonedDateTime.of(2000, 1, 2, 0, 0, 1, 0, ZoneId.systemDefault());
        ZonedDateTime regUntil = ZonedDateTime.of(2000, 1, 4, 23, 59, 59, 0, ZoneId.systemDefault());
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setRegistrationDateSince(Date.from(regSince.toInstant()))
                .setRegistrationDateUntil(Date.from(regUntil.toInstant()))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetAlivenessAvailable.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetAlivenessAvailable() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierAlivenessState(SupplierAlivenessState.AVAILABLE)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetAlivenessUnavailable.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetAlivenessUnavailable() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierAlivenessState(SupplierAlivenessState.UNAVAILABLE)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetAlivenessAvailableSupplierId.after.csv",
            dataSource = "mbiDbDataSource")
    void targetAlivenessAvailableSupplierId() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setSupplierIds(List.of(100000000001L, 100000000005L, 100000000009L))
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierAlivenessState(SupplierAlivenessState.AVAILABLE)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetAlivenessUnavailableSupplierId.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetAlivenessUnavailableSupplierId() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setSupplierIds(List.of(100000000001L, 100000000005L))
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setSupplierAlivenessState(SupplierAlivenessState.UNAVAILABLE)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Disabled
    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetCategories.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetCategories() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setCategoriesIds(List.of(666000000001L, 666000000003L, 666000000005L))
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "targetDraftOffers.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetDraftOffers() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setUseSuppliersWithDraftOffers(true)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "allDeliveryModeTypeTargets.before.csv",
            after = "testAllFilters.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void testAllFilters() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setSupplierIds(List.of(100000000001L, 100000000002L, 100000000003L, 100000000004L, 100000000005L))
                .setPageIds(List.of("page1", "page2", "page3", "page4"))
                .setWarehousesIds(List.of(300000000001L, 300000000002L, 300000000004L))
                .setRegistrationRegionsIds(List.of(21212L, 21214L))
                .setRegistrationDateSince(Date.from(ZonedDateTime.now().minus(11, ChronoUnit.MONTHS).toInstant()))
                .setRegistrationDateUntil(new Date())
                .setSupplierAlivenessState(SupplierAlivenessState.AVAILABLE)
                .setOffersCount(42)
                .setSuppliersTypes(List.of(PartnerPlacementType.FULFILLMENT, PartnerPlacementType.CROSSDOCK,
                        PartnerPlacementType.DROPSHIP, PartnerPlacementType.CLICK_AND_COLLECT))
                .setCategoriesIds(List.of(666000000001L))
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }

    @Test
    @DbUnitDataSet(
            before = "DbSupplierBannerServiceFunctionalTest.targetDraftOffersFromWhite.before.csv",
            after = "DbSupplierBannerServiceFunctionalTest.targetDraftOffersFromWhite.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void targetDraftOffersFromWhite() {
        Set<SupplierBannerFilter> filters = new HashSet<>();
        SupplierBannerFilter filter = new SupplierBannerFilter.Builder()
                .setId("banner1")
                .setPageIds(new ArrayList<>(DEFAULT_BANNER_PAGE_IDS))
                .setUseSuppliersWithDraftOffers(true)
                .setBannerType(BannerType.SUPPLIER)
                .build();
        filters.add(filter);
        supplierBannerService.calculateFullSupplierBannerMappings(filters);
    }
}
