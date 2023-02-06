package ru.yandex.market.markup2.utils.mboc;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.markup2.tasks.supplier_sku_mapping.SupplierShortInfo;
import ru.yandex.market.mboc.http.SupplierOffer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * @author n-magp
 * @since 22.01.2021
 */
public class CachingSupplierShortInfoServiceTest {

    private MboCategoryServiceMock mboCategoryServiceMock;
    private CachingSupplierShortInfoService service;

    @Before
    public void setUp() {
        mboCategoryServiceMock = Mockito.spy(new MboCategoryServiceMock());
        service = new CachingSupplierShortInfoService(mboCategoryServiceMock);
    }

    @Test
    public void testGetAllSuppliersSimple() {
        final int supplierCount = 3;
        Set<Long> ids = LongStream.range(1, supplierCount).boxed().collect(Collectors.toSet());
        makeAndAddSuppliersToMboc(ids);
        loadAndCheck(ids, makeSupplierInfosList(ids));
        Mockito.verify(mboCategoryServiceMock, Mockito.times(1)).getShortSupplierInfos(Mockito.any());
        // second call with the same ids
        loadAndCheck(ids, makeSupplierInfosList(ids));
        Mockito.verify(mboCategoryServiceMock, Mockito.times(1)).getShortSupplierInfos(Mockito.any());
        // third call with cached subset
        Set<Long> cachedIdsSubset = LongStream.range(1, supplierCount).boxed()
                .filter(x -> x % 2 == 0).collect(Collectors.toSet());
        loadAndCheck(cachedIdsSubset, makeSupplierInfosList(cachedIdsSubset));
        Mockito.verify(mboCategoryServiceMock, Mockito.times(1)).getShortSupplierInfos(Mockito.any());
    }

    @Test
    public void testPartialLoad() {
        final int supplierCount = 3;
        Set<Long> ids = LongStream.range(1, supplierCount).boxed().collect(Collectors.toSet());
        makeAndAddSuppliersToMboc(ids);
        // add some unexisting ids
        final long unexistingId1 = supplierCount + 1;
        final long unexistingId2 = unexistingId1 + 1;
        Set<Long> idsWithUnexisting = new HashSet<>(ids);
        idsWithUnexisting.add(unexistingId1);
        idsWithUnexisting.add(unexistingId2);
        loadAndCheck(idsWithUnexisting, makeSupplierInfosList(ids));
        Mockito.verify(mboCategoryServiceMock, Mockito.times(1)).getShortSupplierInfos(Mockito.any());
        loadAndCheck(ids, makeSupplierInfosList(ids));
        // check no more was called
        Mockito.verify(mboCategoryServiceMock, Mockito.times(1)).getShortSupplierInfos(Mockito.any());
    }

    @Test
    public void testRemoteReturnsEmpty() {
        final int supplierCount = 3;
        Set<Long> ids = LongStream.range(1, supplierCount).boxed().collect(Collectors.toSet());
        List<SupplierShortInfo> supplierShortInfos = service.getSupplierShortInfos(ids);
        Assertions.assertThat(supplierShortInfos.size()).isEqualTo(0);
    }

    private void loadAndCheck(Set<Long> supplierIdsForLoad, List<SupplierShortInfo> expectedSupplierInfos) {
        List<SupplierShortInfo> supplierShortInfos = service.getSupplierShortInfos(supplierIdsForLoad);
        Map<Long, SupplierShortInfo> expectedInfos = expectedSupplierInfos.stream()
                .collect(Collectors.toMap(SupplierShortInfo::getId, Function.identity()));
        Assertions.assertThat(supplierShortInfos.size()).isEqualTo(expectedSupplierInfos.size());
        for (SupplierShortInfo supplierShortInfo : supplierShortInfos) {
            SupplierShortInfo expectedSupplier = expectedInfos.get(supplierShortInfo.getId());
            Assertions.assertThat(supplierShortInfo)
                    .as("While checking supplier with id = %d", supplierShortInfo.getId())
                    .isEqualToComparingFieldByField(expectedSupplier);
        }
    }

    private List<SupplierShortInfo> makeSupplierInfosList(Collection<Long> ids) {
        return ids.stream().map(id -> new SupplierShortInfo(id, "name" + id,
                MboCategoryServiceImpl.convert(SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER)))
                .collect(Collectors.toList());
    }

    private void makeAndAddSuppliersToMboc(Collection<Long> ids) {
        ids.forEach(id -> mboCategoryServiceMock.addSupplier(id.intValue(), "name" + id,
                SupplierOffer.SupplierType.TYPE_REAL_SUPPLIER));
    }
}