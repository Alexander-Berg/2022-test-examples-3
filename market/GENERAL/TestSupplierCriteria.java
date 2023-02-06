package ru.yandex.market.mboc.common.offers.repository.search;

import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.market.mbo.lightmapper.criteria.Criteria;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.NamedFilter;
import ru.yandex.market.mboc.common.offers.model.Offer;

/**
 * @author kravchenko-aa
 * @date 06/02/2019
 */
public class TestSupplierCriteria implements Criteria<Offer> {
    private final SupplierRepository supplierRepository;

    public TestSupplierCriteria(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void getWhere(String alias, NamedFilter where) {
        where.and(alias + "supplier_id in (" +
            "select id from mbo_category.supplier where test_supplier)");
    }

    @Override
    public boolean matches(Offer item) {
        Set<Integer> testSuppliers = supplierRepository.findAll()
            .stream()
            .filter(Supplier::isTestSupplier)
            .map(Supplier::getId)
            .collect(Collectors.toSet());
        return testSuppliers.contains(item.getBusinessId());
    }
}
