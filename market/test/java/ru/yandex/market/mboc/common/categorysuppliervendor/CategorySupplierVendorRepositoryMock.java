package ru.yandex.market.mboc.common.categorysuppliervendor;

import java.util.List;

import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;

/**
 * @author dmserebr
 * @date 06.12.18
 */
public class CategorySupplierVendorRepositoryMock
    extends IntGenericMapperRepositoryMock<CategorySupplierVendor> implements CategorySupplierVendorRepository {

    public CategorySupplierVendorRepositoryMock() {
        super(CategorySupplierVendor::setId, CategorySupplierVendor::getId);
    }

    @Override
    public CategorySupplierVendor update(CategorySupplierVendor categorySupplierVendor) {
        categorySupplierVendor.setModificationTs(DateTimeUtils.dateTimeNow());
        return super.update(categorySupplierVendor);
    }

    @Override
    public List<CategorySupplierVendor> getCategorySupplierVendors(CategorySupplierVendorFilter filter) {
        return findWhere(csv ->
            (filter.getId() == null || filter.getId().equals(csv.getId())) &&
                (filter.getCategoryId() == null || filter.getCategoryId().equals(csv.getCategoryId())) &&
                (filter.getSupplierId() == null || filter.getSupplierId().equals(csv.getSupplierId())) &&
                (filter.getVendorId() == null || filter.getVendorId().equals(csv.getVendorId())) &&
                (filter.getState() == null || filter.getState().equals(csv.getState())));
    }

    @Override
    public List<CategorySupplierVendor> findSuspendedCategoryVendors() {
        return findWhere(csv ->
            CategorySupplierVendor.State.SUSPENDED.equals(csv.getState()));
    }
}
