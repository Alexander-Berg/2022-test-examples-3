package ru.yandex.market.ff.repository.replica;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.bo.SupplierSkuItemCount;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

public class RequestItemReplicaRepositoryTest extends IntegrationTest {

    @Autowired
    private RequestItemReplicaRepository repository;

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/find-utilization-transfer-items-count.xml")
    void findUtilizationItemsCountAndGetSingleSkuKeyWithMultipleSkus() {
        List<SupplierSkuItemCount> requests = repository.findUtilizationTransferItemCount(
                Set.of(1L), Set.of("sku2")
        );

        Assertions.assertThat(requests).hasSize(1);

        this.ValidateSupplierSkuItemCount(requests.get(0), 1L, "sku2", 2);
    }


    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/find-utilization-transfer-items-count.xml")
    void findUtilizationItemsCountAndGetSingleSupplierKeyWithMultipleSuppliers() {
        List<SupplierSkuItemCount> requests = repository.findUtilizationTransferItemCount(
                Set.of(3L), Set.of("sku3")
        );

        Assertions.assertThat(requests).hasSize(1);

        this.ValidateSupplierSkuItemCount(requests.get(0), 3L, "sku3", 5);
    }


    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/find-utilization-transfer-items-count.xml")
    void findUtilizationItemCountAndNoResultWithWrongStatuses() {
        List<SupplierSkuItemCount> requests = repository.findUtilizationTransferItemCount(
                Set.of(4L, 5L, 6L), Set.of("sku4", "sku5", "sku6")
        );

        Assertions.assertThat(requests).hasSize(0);
    }


    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/find-utilization-transfer-items-count.xml")
    void findUtilizationItemCountAndCorrectSumWithMultipleRequests() {
        List<SupplierSkuItemCount> requests = repository.findUtilizationTransferItemCount(
                Set.of(7L), Set.of("sku7")
        );

        Assertions.assertThat(requests).hasSize(1);

        ValidateSupplierSkuItemCount(requests.get(0), 7L, "sku7", 27L);
    }


    private void ValidateSupplierSkuItemCount(SupplierSkuItemCount supplierSkuItemCount,
                                              long supplierId,
                                              String sku,
                                              long itemsCount) {

        Assertions.assertThat(supplierSkuItemCount.getSupplierId()).isEqualTo(supplierId);
        Assertions.assertThat(supplierSkuItemCount.getSku()).isEqualTo(sku);
        Assertions.assertThat(supplierSkuItemCount.getCount()).isEqualTo(itemsCount);
    }
}
