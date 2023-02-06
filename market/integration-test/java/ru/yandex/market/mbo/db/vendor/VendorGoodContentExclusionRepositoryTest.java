package ru.yandex.market.mbo.db.vendor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.core.conf.databases.SiteCatalogPgDBConfig;
import ru.yandex.market.mbo.db.pg.BasePgTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

/**
 * @author apluhin
 * @created 11/25/21
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class VendorGoodContentExclusionRepositoryTest extends BasePgTestClass {

    @Inject
    private DataSource siteCatalogPgDb;
    @Inject
    private TransactionHelper transactionHelper;
    private VendorGoodContentExclusionRepository vendorGoodContentExclusionRepository;

    @Before
    public void setUp() throws Exception {
        SiteCatalogPgDBConfig mock = Mockito.mock(SiteCatalogPgDBConfig.class);
        Mockito.when(mock.siteCatalogPgJdbcTemplate()).thenReturn(new JdbcTemplate(siteCatalogPgDb));
        Mockito.when(mock.siteCatalogTransactionHelper()).thenReturn(transactionHelper);
        vendorGoodContentExclusionRepository = new VendorGoodContentExclusionRepository(mock);
    }

    @Test
    public void testSaveVendorsForCategory() {
        List<Long> categoryInsert =
            Collections.singletonList(1L);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1, categoryInsert);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1)).containsExactly(1L);

        List<Long> categoryUpdate = new ArrayList<>();
        categoryUpdate.add(2L);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1, categoryUpdate);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1)).containsExactly(2L);

        categoryUpdate.addAll(categoryInsert);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1L, categoryUpdate);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1))
            .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    public void testRemoveGlobalVendor() {
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1L, Arrays.asList(1L, 2L));
        vendorGoodContentExclusionRepository.setNewGlobalVendors(2L, Arrays.asList(1L));
        vendorGoodContentExclusionRepository.removeGlobalVendorFromAllCategories(1L);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1)).containsExactlyInAnyOrder(2L);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(2)).isEmpty();
    }

    @Test
    public void testRemoveCategories() {
        List<Long> vendors = Arrays.asList(1L, 2L);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1L, vendors);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(2L, vendors);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(3L, vendors);
        vendorGoodContentExclusionRepository.cleanCategories(Arrays.asList(1L, 2L));
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1)).isEmpty();
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(2)).isEmpty();
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(3)).isNotEmpty();
    }

    @Test
    public void testCleanCategoryAcrossUpdate() {
        List<Long> vendors = Arrays.asList(1L, 2L);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(1L, vendors);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(2L, vendors);
        vendorGoodContentExclusionRepository.setNewGlobalVendors(3L, vendors);
        vendorGoodContentExclusionRepository.cleanCategories(vendors);
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(1)).isEmpty();
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(2)).isEmpty();
        Assertions.assertThat(vendorGoodContentExclusionRepository.getVendorIdByHid(3)).isNotEmpty();
    }

}
