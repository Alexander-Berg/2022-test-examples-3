package ru.yandex.market.mboc.common.vendor.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalVendorsCacheRepositoryImplTest extends BaseDbTestClass {
    @Autowired
    private GlobalVendorsCacheRepository repository;

    @Test
    public void testDeleteOlderThan() {
        var olderThan = LocalDateTime.now().minusHours(1);

        var oldVendor = new CachedGlobalVendor(41214L, "old vendor");
        oldVendor.setRequireGtinBarcodes(true);
        oldVendor.setCachedAt(olderThan.minus(1, ChronoUnit.MICROS));

        var onEdgeVendor = new CachedGlobalVendor(252352352L, "on edge vendor");
        onEdgeVendor.setRequireGtinBarcodes(false);
        onEdgeVendor.setCachedAt(olderThan);

        var newVendor = new CachedGlobalVendor(1249194192L, "new vendor");
        newVendor.setRequireGtinBarcodes(false);
        newVendor.setCachedAt(olderThan.plus(1, ChronoUnit.MICROS));

        repository.insertBatch(oldVendor, onEdgeVendor, newVendor);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(oldVendor, onEdgeVendor, newVendor);
        repository.deleteOlderThan(olderThan);
        assertThat(repository.findAll()).containsExactlyInAnyOrder(newVendor);
        repository.deleteOlderThan(newVendor.getCachedAt());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testFindAllFromIdLimited() {
        var v1 = new CachedGlobalVendor(1L, "1");
        v1.setRequireGtinBarcodes(true);

        var v2 = new CachedGlobalVendor(2L, "2");
        v2.setRequireGtinBarcodes(false);

        var v3 = new CachedGlobalVendor(3L, "3");
        v3.setRequireGtinBarcodes(false);

        repository.insertBatch(v1, v2, v3);

        assertThat(repository.findAllFromIdOrdered(0, 2)).containsExactly(v1, v2);
        assertThat(repository.findAllFromIdOrdered(1, 1)).containsExactly(v1);
        assertThat(repository.findAllFromIdOrdered(2, 2)).containsExactly(v2, v3);
    }
}
