package ru.yandex.market.mbo.db.modelstorage.compatibility.converters;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.yt.CategoryVendorYtIndexQuery;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.gwt.models.compatibility.CompatibilityFilter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author apluhin
 * @created 11/26/20
 */
public class ModelCompatibilityFilterConverterTest {

    ModelCompatibilityFilterConverter converter;
    GlobalVendorService vendorService;

    @Before
    public void setUp() throws Exception {
        vendorService = Mockito.mock(GlobalVendorService.class);
        converter = new ModelCompatibilityFilterConverter(vendorService);
    }

    @Test
    public void testCorrectConvert() {
        GlobalVendor vendor = new GlobalVendor();
        vendor.setId(1);
        Mockito.when(vendorService.getCachedVendorIgnoreCase(Mockito.eq("testName"))).thenReturn(Arrays.asList(
            vendor
        ));

        CompatibilityFilter filter = new CompatibilityFilter();
        filter.setCategoryIds(Arrays.asList(1L, 2L));
        filter.setGlobalVendorName("testName");
        MboIndexesFilter convert = converter.convertToMboFilter(filter);
        Assertions.assertThat(convert.getCategoryIds()).containsExactly(1L, 2L);
        Assertions.assertThat(convert.getVendorIds()).containsExactly(1L);
        Assertions.assertThat(convert.getDeleted()).isFalse();
        //check that filter support by some index
        CategoryVendorYtIndexQuery.isSupportFilter(convert);
    }

    @Test
    public void testEmptyConvert() {
        Mockito.when(vendorService.getCachedVendorIgnoreCase(Mockito.eq("testName")))
            .thenReturn(Collections.emptyList());

        CompatibilityFilter filter = new CompatibilityFilter();
        filter.setGlobalVendorName("testName");
        MboIndexesFilter convert = converter.convertToMboFilter(filter);
        //empty filter doest support
        Assertions.assertThat(convert).isNull();
    }
}
