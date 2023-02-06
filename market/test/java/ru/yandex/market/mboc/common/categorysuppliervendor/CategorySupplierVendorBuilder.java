package ru.yandex.market.mboc.common.categorysuppliervendor;

import java.time.LocalDateTime;

/**
 * @author dmserebr
 * @date 06.12.18
 */
public final class CategorySupplierVendorBuilder {
    private int id;
    private long categoryId;
    private int supplierId;
    private int vendorId;
    private CategorySupplierVendor.State state;
    private LocalDateTime modifDate;

    private CategorySupplierVendorBuilder() {
    }

    public static CategorySupplierVendorBuilder newBuilder() {
        return new CategorySupplierVendorBuilder();
    }

    public CategorySupplierVendorBuilder id(int id) {
        this.id = id;
        return this;
    }

    public CategorySupplierVendorBuilder categoryId(long categoryId) {
        this.categoryId = categoryId;
        return this;
    }

    public CategorySupplierVendorBuilder supplierId(int supplierId) {
        this.supplierId = supplierId;
        return this;
    }

    public CategorySupplierVendorBuilder vendorId(int vendorId) {
        this.vendorId = vendorId;
        return this;
    }

    public CategorySupplierVendorBuilder state(CategorySupplierVendor.State state) {
        this.state = state;
        return this;
    }

    public CategorySupplierVendorBuilder modificationDate(LocalDateTime modifDate) {
        this.modifDate = modifDate;
        return this;
    }

    public CategorySupplierVendor build() {
        CategorySupplierVendor categorySupplierVendor = new CategorySupplierVendor();
        categorySupplierVendor.setId(id);
        categorySupplierVendor.setCategoryId(categoryId);
        categorySupplierVendor.setSupplierId(supplierId);
        categorySupplierVendor.setVendorId(vendorId);
        categorySupplierVendor.setState(state);
        categorySupplierVendor.setModificationTs(modifDate);
        return categorySupplierVendor;
    }
}
