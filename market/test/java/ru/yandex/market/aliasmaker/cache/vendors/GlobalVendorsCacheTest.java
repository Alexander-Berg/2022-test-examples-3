package ru.yandex.market.aliasmaker.cache.vendors;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.mbo.http.GlobalVendorsService;
import ru.yandex.market.mbo.http.MboVendors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.aliasmaker.TestFileUtils.load;

/**
 * @author galaev@yandex-team.ru
 * @since 13/12/2018.
 */
public class GlobalVendorsCacheTest {

    private GlobalVendorsCache globalVendorsCache;

    @Before
    public void before() {
        GlobalVendorsService globalVendorsService = Mockito.mock(GlobalVendorsService.class);
        Mockito.when(globalVendorsService.searchVendors(Mockito.any(MboVendors.SearchVendorsRequest.class)))
                .thenAnswer(invocation -> {
                    MboVendors.SearchVendorsRequest searchVendorsRequest =
                            invocation.getArgument(0, MboVendors.SearchVendorsRequest.class);
                    if (searchVendorsRequest.getIdCount() > 0) {
                        return load("/additional_vendor.json", MboVendors.SearchVendorsResponse.newBuilder())
                                .build();
                    }
                    MboVendors.SearchVendorsResponse.Builder builder =
                            load("/vendors.json", MboVendors.SearchVendorsResponse.newBuilder());
                    int offset = searchVendorsRequest.getOffset();
                    if (offset >= builder.getVendorsCount()) {
                        builder.clearVendors();
                    } else {
                        List<MboVendors.GlobalVendor> vendors = builder.getVendorsList().subList(
                                offset,
                                Math.min(builder.getVendorsCount(), offset + searchVendorsRequest.getLimit())
                        );
                        builder.clearVendors();
                        builder.addAllVendors(vendors);
                    }
                    return builder.build();
                });

        globalVendorsCache = new GlobalVendorsCacheImpl(globalVendorsService);
    }

    @Test
    public void testGetGlobalVendor() {
        int vendorId = 153043;
        AliasMaker.Vendor vendor = globalVendorsCache.getGlobalVendor(vendorId);
        assertThat(vendor.getName()).isEqualTo("Apple");
        assertThat(vendor.getVendorId()).isEqualTo(vendorId);
        assertThat(vendor.getComment()).isEqualTo("Акустика");
        assertThat(vendor.getGlobalVendorAliasList()).hasSize(1);
    }

    @Test
    public void testGetGlobalVendors() {
        Collection<AliasMaker.Vendor> vendors = globalVendorsCache.getGlobalVendors();
        assertThat(vendors).hasSize(6);
    }

    @Test
    public void testGetOrLoadGlobalVendor() {
        int vendorId = 666;
        AliasMaker.Vendor vendor = globalVendorsCache.getGlobalVendor(vendorId);
        assertThat(vendor).isNull();
        vendor = globalVendorsCache.getOrLoadVendor(vendorId);
        assertThat(vendor).isNotNull();
        vendor = globalVendorsCache.getGlobalVendor(vendorId);
        assertThat(vendor).isNotNull(); //vendor is added to cache
    }
}
