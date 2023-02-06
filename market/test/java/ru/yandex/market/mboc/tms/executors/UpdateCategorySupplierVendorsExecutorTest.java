package ru.yandex.market.mboc.tms.executors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboSizeMeasures;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendor;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorBuilder;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorFilter;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorRepository;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorRepositoryMock;

/**
 * @author dmserebr
 * @date 06.12.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UpdateCategorySupplierVendorsExecutorTest {

    private static final ComplexMonitoring DUMMY_MONITORING = new ComplexMonitoring();

    private UpdateCategorySupplierVendorsExecutor executor;
    private CategorySupplierVendorRepository repository;
    private CategorySizeMeasureService sizeMeasureService;

    @Before
    public void before() {
        repository = new CategorySupplierVendorRepositoryMock();
        sizeMeasureService = Mockito.mock(CategorySizeMeasureService.class);
        executor = new UpdateCategorySupplierVendorsExecutor(repository, sizeMeasureService);
    }

    @Test
    public void testSuspendedVendorsUpdate() throws Exception {
        // CSV in category 10, vendor with scale - should become ALIVE after job
        CategorySupplierVendor csv1 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(10)
                .supplierId(20)
                .vendorId(30)
                .state(CategorySupplierVendor.State.SUSPENDED)
                .build());

        // Another CSV in category 10, vendor with scale - should become ALIVE after job
        CategorySupplierVendor csv2 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(10)
                .supplierId(21)
                .vendorId(30)
                .state(CategorySupplierVendor.State.SUSPENDED)
                .build());

        // CSV in category 10, another vendor with scale - should become ALIVE after job
        CategorySupplierVendor csv3 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(10)
                .supplierId(22)
                .vendorId(31)
                .state(CategorySupplierVendor.State.SUSPENDED)
                .build());

        // CSV in category 11, vendor without scale - should not be affected by job
        CategorySupplierVendor csv4 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(11)
                .supplierId(22)
                .vendorId(33)
                .state(CategorySupplierVendor.State.SUSPENDED)
                .build());

        // CSV in category 11, vendor with scale - should become ALIVE after job
        CategorySupplierVendor csv5 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(11)
                .supplierId(22)
                .vendorId(34)
                .state(CategorySupplierVendor.State.SUSPENDED)
                .build());

        // CSV in category 10, vendor with scale but state is already ALIVE - should not be affected by job
        CategorySupplierVendor csv6 = repository.insert(
            CategorySupplierVendorBuilder.newBuilder()
                .categoryId(10)
                .supplierId(23)
                .vendorId(30)
                .state(CategorySupplierVendor.State.ALIVE)
                .build());

        Mockito.when(sizeMeasureService.getSizeMeasuresVendorsInfo(Mockito.any()))
            .thenReturn(MboSizeMeasures.GetSizeMeasuresInfoVendorResponse.newBuilder()
                .addVendorResponse(MboSizeMeasures.GetSizeMeasuresInfoVendorResponse.VendorResponse.newBuilder()
                    .setCategoryId(10)
                    .addSizeMeasureInfos(MboSizeMeasures.SizeMeasureInfo.newBuilder()
                        .addScales(MboSizeMeasures.ScaleInfo.newBuilder().setVendorId(30))
                        .addScales(MboSizeMeasures.ScaleInfo.newBuilder().setVendorId(31))
                    ))
                .addVendorResponse(MboSizeMeasures.GetSizeMeasuresInfoVendorResponse.VendorResponse.newBuilder()
                    .setCategoryId(11)
                    .addSizeMeasureInfos(MboSizeMeasures.SizeMeasureInfo.newBuilder()
                        .addScales(MboSizeMeasures.ScaleInfo.newBuilder().setVendorId(34))
                    ))
                .build());

        executor.execute();

        Assert.assertEquals(CategorySupplierVendor.State.ALIVE, getCsvFromRepository(csv1).getState());
        Assert.assertEquals(CategorySupplierVendor.State.ALIVE, getCsvFromRepository(csv2).getState());
        Assert.assertEquals(CategorySupplierVendor.State.ALIVE, getCsvFromRepository(csv3).getState());
        Assert.assertEquals(CategorySupplierVendor.State.SUSPENDED, getCsvFromRepository(csv4).getState());
        Assert.assertEquals(CategorySupplierVendor.State.ALIVE, getCsvFromRepository(csv5).getState());
        Assert.assertEquals(CategorySupplierVendor.State.ALIVE, getCsvFromRepository(csv6).getState());

        Assert.assertNotNull(getCsvFromRepository(csv1).getModificationTs());
        Assert.assertNotNull(getCsvFromRepository(csv2).getModificationTs());
        Assert.assertNotNull(getCsvFromRepository(csv3).getModificationTs());
        Assert.assertNull(getCsvFromRepository(csv4).getModificationTs());
        Assert.assertNotNull(getCsvFromRepository(csv5).getModificationTs());
        Assert.assertNull(getCsvFromRepository(csv6).getModificationTs());
    }

    private CategorySupplierVendor getCsvFromRepository(CategorySupplierVendor categorySupplierVendor) {
        CategorySupplierVendorFilter filter = new CategorySupplierVendorFilter();
        filter.setCategoryId(categorySupplierVendor.getCategoryId());
        filter.setSupplierId(categorySupplierVendor.getSupplierId());
        filter.setVendorId(categorySupplierVendor.getVendorId());
        return repository.getCategorySupplierVendors(filter).get(0);
    }
}
