package ru.yandex.market.mbo.category;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorService;
import ru.yandex.market.mbo.catalogue.CategoryFavoriteVendorServiceImpl;
import ru.yandex.market.mbo.db.JdbcFactory;

/**
 * @author moskovkin@yandex-team.ru
 * @since 22.11.18
 */
public class CategoryFavoriteVendorServiceTest {
    private static final long HID_1 = 1L;
    private static final long HID_2 = 2L;
    private static final long VENDOR_ID_10 = 10L;
    private static final long VENDOR_ID_20 = 20L;

    private NamedParameterJdbcOperations jdbcOperations;
    private CategoryFavoriteVendorService service;

    @Before
    public void setUp() {
        jdbcOperations = JdbcFactory.createH2NamedJdbcOperations(
            JdbcFactory.Mode.POSTGRES,
            "classpath:category/category_favorite_vendor_stubs.sql"
        );
        service = new CategoryFavoriteVendorServiceImpl(jdbcOperations);
        addCategory(HID_1);
        addCategory(HID_2);
        addVendor(VENDOR_ID_10);
        addVendor(VENDOR_ID_20);
    }

    private void addCategory(Long hid) {
        jdbcOperations.update(
            "INSERT INTO market_content.mc_category(hyper_id) VALUES(:hid) ",
            ImmutableMap.of("hid", hid)
        );
    }

    private void addVendor(Long vendorId) {
        jdbcOperations.update(
            "INSERT INTO site_catalog.sc_vendor(id) VALUES(:vendorId) ",
            ImmutableMap.of("vendorId", vendorId)
        );
    }

    @Test
    public void testAdd() {
        service.addFavoriteVendor(HID_1, VENDOR_ID_10);
    }

    @Test
    public void testFind() {
        service.addFavoriteVendor(HID_1, VENDOR_ID_10);
        service.addFavoriteVendor(HID_2, VENDOR_ID_20);

        List<Long> favoriteVendors = service.getFavoriteVendors(HID_1);
        Assertions.assertThat(favoriteVendors).containsOnly(VENDOR_ID_10);
    }

    @Test
    public void testRemove() {
        service.addFavoriteVendor(HID_1, VENDOR_ID_10);
        service.addFavoriteVendor(HID_1, VENDOR_ID_20);
        service.addFavoriteVendor(HID_2, VENDOR_ID_10);

        List<Long> favoriteVendors = service.getFavoriteVendors(HID_1);
        Assertions.assertThat(favoriteVendors).containsOnly(VENDOR_ID_10, VENDOR_ID_20);

        favoriteVendors = service.getFavoriteVendors(HID_2);
        Assertions.assertThat(favoriteVendors).containsOnly(VENDOR_ID_10);

        service.removeFavoriteVendor(HID_1, VENDOR_ID_10);

        favoriteVendors = service.getFavoriteVendors(HID_1);
        Assertions.assertThat(favoriteVendors).containsOnly(VENDOR_ID_20);

        favoriteVendors = service.getFavoriteVendors(HID_2);
        Assertions.assertThat(favoriteVendors).containsOnly(VENDOR_ID_10);
    }
}
