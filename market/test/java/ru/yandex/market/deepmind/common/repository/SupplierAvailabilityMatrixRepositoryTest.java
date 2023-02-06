package ru.yandex.market.deepmind.common.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SupplierAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierAvailabilityFilter;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.mbo.jooq.repo.OffsetFilter;
import ru.yandex.market.mbo.jooq.repo.Sorting;

public class SupplierAvailabilityMatrixRepositoryTest extends DeepmindBaseDbTestClass {
    @Autowired
    private SupplierAvailabilityMatrixRepository supplierAvailabilityMatrixRepository;
    @Autowired
    private SupplierRepository deepmindSupplierRepository;

    private Supplier supplier;

    @Before
    public void setUp() throws Exception {
        supplier = deepmindSupplierRepository.save(new Supplier()
            .setId(100)
            .setName("supplier name"));
    }

    @Test
    public void testDeletedRowsDontReturnByDefault() {
        SupplierAvailabilityMatrix entry = supplierAvailabilityMatrixRepository.save(new SupplierAvailabilityMatrix()
            .setSupplierId(supplier.getId())
            .setWarehouseId(1L)
            .setAvailable(false)
            .setCreatedLogin("user"));

        Assertions.assertThat(supplierAvailabilityMatrixRepository.findAll())
            .hasSize(1);

        supplierAvailabilityMatrixRepository.delete(entry.getId());

        Optional<SupplierAvailabilityMatrix> byId = supplierAvailabilityMatrixRepository.findById(entry.getId());
        Assertions.assertThat(byId).isPresent();
        Assertions.assertThat(byId.get().getDeleted()).isTrue();

        List<SupplierAvailabilityMatrix> rows = supplierAvailabilityMatrixRepository.find(
            new SupplierAvailabilityFilter().setSupplierIds(List.of(supplier.getId())),
            Sorting.notSorting(), OffsetFilter.all());

        List<SupplierAvailabilityMatrix> notEmptyRows = rows.stream()
            .filter(s -> s.getAvailable() != null)
            .collect(Collectors.toList());
        Assertions.assertThat(notEmptyRows).isEmpty();
    }

    @Test
    public void testDeletedAndAgainInserted() {
        SupplierAvailabilityMatrix entry = supplierAvailabilityMatrixRepository.save(new SupplierAvailabilityMatrix()
            .setSupplierId(supplier.getId())
            .setWarehouseId(1L)
            .setAvailable(false)
            .setCreatedLogin("user")
        );
        supplierAvailabilityMatrixRepository.delete(entry.getId());

        // again insert
        SupplierAvailabilityMatrix again = supplierAvailabilityMatrixRepository.save(
            supplierAvailabilityMatrixRepository.findById(entry.getId()).get()
                .setAvailable(true)
                .setDeleted(false)
                .setDeletedAt(null)
        );

        Assertions.assertThat(again.getId()).isEqualTo(entry.getId());
    }

    @Test
    public void testDeletedAndAgainInsertedTheSame() {
        SupplierAvailabilityMatrix entry = supplierAvailabilityMatrixRepository.save(new SupplierAvailabilityMatrix()
            .setSupplierId(supplier.getId())
            .setWarehouseId(1L)
            .setAvailable(true)
            .setCreatedLogin("user")
        );
        supplierAvailabilityMatrixRepository.delete(entry.getId());

        // again insert the same availability
        supplierAvailabilityMatrixRepository.saveAvailabilities(List.of(
            new SupplierAvailabilityMatrix()
                .setSupplierId(supplier.getId())
                .setWarehouseId(1L)
                .setAvailable(true)
                .setCreatedLogin("user")
            )
        );
        var again = supplierAvailabilityMatrixRepository.findById(entry.getId()).get();

        Assertions.assertThat(again.getDeleted()).isFalse();
        Assertions.assertThat(again.getAvailable()).isEqualTo(entry.getAvailable()).isTrue();
    }
}
