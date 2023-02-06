package ru.yandex.market.mboc.common.categorysuppliervendor.repository;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendor;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorBuilder;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorFilter;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorRepositoryImpl;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryImpl;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfoRepositoryImpl;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledge;
import ru.yandex.market.mboc.common.services.category_info.knowledges.CategoryKnowledgeRepository;
import ru.yandex.market.mboc.common.services.mbousers.MboUsersRepositoryImpl;
import ru.yandex.market.mboc.common.services.mbousers.models.MboUser;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author dmserebr
 * @date 17/12/2018
 * <p>
 * Tests for filtering in CategorySupplierVendorRepositoryImpl.
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategorySupplierVendorRepositoryImplTest extends BaseDbTestClass {

    @Autowired
    private CategoryInfoRepositoryImpl categoryInfoRepository;

    @Autowired
    private MboUsersRepositoryImpl mboUsersRepository;

    @Autowired
    private CategoryKnowledgeRepository categoryKnowledgeRepository;

    @Autowired
    private SupplierRepositoryImpl supplierRepository;

    @Autowired
    private CategorySupplierVendorRepositoryImpl categorySupplierVendorRepository;

    @Before
    public void before() {
        fillDependentTables();
        insertTestCsvs();
    }

    @Test
    public void testEmptyFilter() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(4, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 1, 10, 100, CategorySupplierVendor.State.SUSPENDED);
        assertCategorySupplierVendor(filtered.get(1), 1, 11, 100, CategorySupplierVendor.State.ALIVE);
        assertCategorySupplierVendor(filtered.get(2), 2, 11, 101, CategorySupplierVendor.State.SUSPENDED);
        assertCategorySupplierVendor(filtered.get(3), 2, 10, 101, CategorySupplierVendor.State.ALIVE);
    }

    @Test
    public void testFilterCategoryId() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setCategoryId(1L);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(2, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 1, 10, 100, CategorySupplierVendor.State.SUSPENDED);
        assertCategorySupplierVendor(filtered.get(1), 1, 11, 100, CategorySupplierVendor.State.ALIVE);
    }

    @Test
    public void testFilterSupplierId() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setSupplierId(11);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(2, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 1, 11, 100, CategorySupplierVendor.State.ALIVE);
        assertCategorySupplierVendor(filtered.get(1), 2, 11, 101, CategorySupplierVendor.State.SUSPENDED);
    }

    @Test
    public void testFilterVendorId() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setVendorId(101);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(2, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 2, 11, 101, CategorySupplierVendor.State.SUSPENDED);
        assertCategorySupplierVendor(filtered.get(1), 2, 10, 101, CategorySupplierVendor.State.ALIVE);
    }

    @Test
    public void testFilterSuspendedState() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setState(CategorySupplierVendor.State.SUSPENDED);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(2, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 1, 10, 100, CategorySupplierVendor.State.SUSPENDED);
        assertCategorySupplierVendor(filtered.get(1), 2, 11, 101, CategorySupplierVendor.State.SUSPENDED);
    }

    @Test
    public void testFilterAliveState() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setState(CategorySupplierVendor.State.ALIVE);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertEquals(2, filtered.size());
        assertCategorySupplierVendor(filtered.get(0), 1, 11, 100, CategorySupplierVendor.State.ALIVE);
        assertCategorySupplierVendor(filtered.get(1), 2, 10, 101, CategorySupplierVendor.State.ALIVE);
    }

    @Test
    public void testFilterNoCategory() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setCategoryId(12345L);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterNoSupplier() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setSupplierId(12345);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void testFilterNoVendor() {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setVendorId(12345);

        List<CategorySupplierVendor> filtered = categorySupplierVendorRepository.getCategorySupplierVendors(filter);

        Assert.assertTrue(filtered.isEmpty());
    }

    private void fillDependentTables() {
        MboUser testUser = new MboUser();
        testUser.setUid(1000L);
        testUser.setFullName("testUser");
        testUser.setYandexLogin("testUser");
        mboUsersRepository.insert(testUser);

        categoryKnowledgeRepository.insert(new CategoryKnowledge(1));
        categoryKnowledgeRepository.insert(new CategoryKnowledge(2));

        CategoryInfo categoryInfo1 = new CategoryInfo();
        categoryInfo1.setCategoryId(1);
        categoryInfo1.setContentManagerUid(1000L);
        categoryInfo1.setInputManagerUid(1000L);
        categoryInfoRepository.insert(categoryInfo1);

        CategoryInfo categoryInfo2 = new CategoryInfo();
        categoryInfo2.setCategoryId(2);
        categoryInfo2.setContentManagerUid(1000L);
        categoryInfo2.setInputManagerUid(1000L);
        categoryInfoRepository.insert(categoryInfo2);

        Supplier supplier10 = new Supplier();
        supplier10.setId(10);
        supplier10.setName("supplier10");
        supplierRepository.insert(supplier10);

        Supplier supplier11 = new Supplier();
        supplier11.setId(11);
        supplier11.setName("supplier11");
        supplierRepository.insert(supplier11);
    }

    private void insertTestCsvs() {
        categorySupplierVendorRepository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(1).supplierId(10).vendorId(100).state(CategorySupplierVendor.State.SUSPENDED).build());
        categorySupplierVendorRepository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(1).supplierId(11).vendorId(100).state(CategorySupplierVendor.State.ALIVE).build());
        categorySupplierVendorRepository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(2).supplierId(11).vendorId(101).state(CategorySupplierVendor.State.SUSPENDED).build());
        categorySupplierVendorRepository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(2).supplierId(10).vendorId(101).state(CategorySupplierVendor.State.ALIVE).build());
    }

    private void assertCategorySupplierVendor(CategorySupplierVendor csv,
                                              long categoryId, int supplierId, int vendorId,
                                              CategorySupplierVendor.State state) {

        Assert.assertEquals(categoryId, csv.getCategoryId());
        Assert.assertEquals(supplierId, csv.getSupplierId());
        Assert.assertEquals(vendorId, csv.getVendorId());
        Assert.assertEquals(state, csv.getState());
    }
}
