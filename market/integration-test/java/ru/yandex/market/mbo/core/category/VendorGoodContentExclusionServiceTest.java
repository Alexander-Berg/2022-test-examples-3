package ru.yandex.market.mbo.core.category;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.core.conf.databases.SiteCatalogPgDBConfig;
import ru.yandex.market.mbo.db.pg.BasePgTestClass;
import ru.yandex.market.mbo.db.vendor.GlobalVendorService;
import ru.yandex.market.mbo.db.vendor.VendorGoodContentExclusionRepository;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendorWithName;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

/**
 * @author apluhin
 * @created 11/25/21
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class VendorGoodContentExclusionServiceTest extends BasePgTestClass {

    private static List<GlobalVendorWithName> linkedGlobalVendors = Arrays.asList(
        vendor(1L),
        vendor(2L),
        vendor(3L)
    );
    @Inject
    private DataSource siteCatalogPgDb;
    @Inject
    private TransactionHelper transactionHelper;
    private GlobalVendorService globalVendorService;
    private VendorGoodContentExclusionRepository vendorGoodContentExclusionRepository;
    private VendorGoodContentExclusionService vendorGoodContentExclusionService;

    private static GlobalVendorWithName vendor(Long id) {
        return new GlobalVendorWithName(id, id.toString());
    }

    private static GlobalVendor mockGlobalVendor(Long id) {
        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setId(id);
        globalVendor.addName(Language.RUSSIAN.getId(), id.toString());
        return globalVendor;
    }

    @Before
    public void setUp() throws Exception {
        SiteCatalogPgDBConfig mock = Mockito.mock(SiteCatalogPgDBConfig.class);
        Mockito.when(mock.siteCatalogPgJdbcTemplate()).thenReturn(new JdbcTemplate(siteCatalogPgDb));
        Mockito.when(mock.siteCatalogTransactionHelper()).thenReturn(transactionHelper);
        vendorGoodContentExclusionRepository = new VendorGoodContentExclusionRepository(mock);
        globalVendorService = Mockito.mock(GlobalVendorService.class);
        vendorGoodContentExclusionService = new VendorGoodContentExclusionService(vendorGoodContentExclusionRepository,
            globalVendorService);
    }

    @Test
    public void testGetSuggestVendorExclusion() {
        Mockito.when(globalVendorService.loadGlobalVendorsByHid(Mockito.anyLong())).thenReturn(linkedGlobalVendors);
        vendorGoodContentExclusionService.updateExclusionVendor(1L, Arrays.asList(vendor(1L)));
        List<GlobalVendorWithName> candidateForExclusion =
            vendorGoodContentExclusionService.getCandidateForExclusion(1L);
        Assertions.assertThat(candidateForExclusion).containsExactlyInAnyOrder(
            vendor(1L), vendor(2L), vendor(3L)
        );

        vendorGoodContentExclusionService.updateExclusionVendor(1L, Arrays.asList(vendor(1L), vendor(2L)));
        candidateForExclusion = vendorGoodContentExclusionService.getCandidateForExclusion(1L);
        Assertions.assertThat(candidateForExclusion).containsExactlyInAnyOrder(vendor(1L), vendor(2L), vendor(3L));
    }

    @Test
    public void testExcludedVendors() {
        Mockito.when(globalVendorService.loadGlobalVendorsByHid(Mockito.anyLong())).thenReturn(linkedGlobalVendors);
        vendorGoodContentExclusionService.updateExclusionVendor(1L,
            Arrays.asList(vendor(1L), vendor(2L), vendor(3L)));

        Mockito.when(globalVendorService.getCachedGlobalVendorsByIds(Mockito.any())).thenReturn(
            Arrays.asList(
                mockGlobalVendor(1L),
                mockGlobalVendor(2L)
            ));

        List<GlobalVendorWithName> excludedVendors = vendorGoodContentExclusionService.getExcludedVendors(1L);
        Assertions.assertThat(excludedVendors).containsExactlyInAnyOrder(vendor(1L), vendor(2L));
    }

    @Test
    public void testSaveExclude() {
        vendorGoodContentExclusionService.updateExclusionVendor(1L, Arrays.asList(vendor(1L), vendor(2L)));
        List<Long> vendorIdByHid = vendorGoodContentExclusionRepository.getVendorIdByHid(1L);
        Assertions.assertThat(vendorIdByHid).containsExactlyInAnyOrder(1L, 2L);
    }


}
