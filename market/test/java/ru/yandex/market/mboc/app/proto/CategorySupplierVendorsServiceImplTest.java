package ru.yandex.market.mboc.app.proto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendor;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorBuilder;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorFilter;
import ru.yandex.market.mboc.common.categorysuppliervendor.CategorySupplierVendorRepository;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingService;
import ru.yandex.market.mboc.common.vendor.GlobalVendorsCachingServiceImpl;
import ru.yandex.market.mboc.common.vendor.models.CachedGlobalVendor;
import ru.yandex.market.mboc.http.CategorySupplierVendors;

/**
 * @author dmserebr
 * @date 17/12/2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategorySupplierVendorsServiceImplTest {

    private CategorySupplierVendorRepository repository;
    private TrackerServiceMock trackerService;
    private SupplierRepository supplierRepository;
    private GlobalVendorsCachingService globalVendorsCachingService;

    private CategorySupplierVendorsServiceImpl categorySupplierVendorsService;

    @Before
    public void before() {
        repository = Mockito.mock(CategorySupplierVendorRepository.class);
        trackerService = new TrackerServiceMock();
        CategoryCachingService categoryCachingService = Mockito.mock(CategoryCachingService.class);
        Mockito.when(categoryCachingService.getCategoryName(Mockito.any())).thenReturn("Test category");
        supplierRepository = new SupplierRepositoryMock();
        globalVendorsCachingService = Mockito.mock(GlobalVendorsCachingServiceImpl.class);
        categorySupplierVendorsService = new CategorySupplierVendorsServiceImpl(
            repository);
    }

    @Test
    public void testAddCategorySupplierVendorSuccess() {
        CategorySupplierVendors.AddCategorySupplierVendorRequest request =
            CategorySupplierVendors.AddCategorySupplierVendorRequest.newBuilder()
                .setCategorySupplierVendor(
                    CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                        .setCategoryId(1)
                        .setSupplierId(10L)
                        .setVendorId(100)
                        .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                        .build())
                .build();

        Mockito.doReturn(new ArrayList())
            .when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));
        Mockito.doReturn(CategorySupplierVendorBuilder.newBuilder()
            .categoryId(1).supplierId(10).vendorId(100).state(CategorySupplierVendor.State.SUSPENDED).build())
            .when(repository).insert(Mockito.any(CategorySupplierVendor.class));

        Optional<CachedGlobalVendor> globalVendor = Optional.of(new CachedGlobalVendor(200L, "Abibas"));
        Mockito.doReturn(globalVendor).when(globalVendorsCachingService).getVendor(Mockito.anyLong());

        supplierRepository.insert(new Supplier(10, "abibas.ru", "", ""));

        CategorySupplierVendors.AddCategorySupplierVendorResponse response =
            categorySupplierVendorsService.addCategorySupplierVendor(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.SUCCESS, response.getStatus());
        Assert.assertEquals("Successfully added entry", response.getMessage());
    }

    @Test
    public void testAddCategorySupplierVendorNotAddedRequestWithMissingSupplierId() {
        CategorySupplierVendors.AddCategorySupplierVendorRequest request =
            CategorySupplierVendors.AddCategorySupplierVendorRequest.newBuilder()
                .setCategorySupplierVendor(
                    CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                        .setCategoryId(1)
                        .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                        .build())
                .build();

        CategorySupplierVendors.AddCategorySupplierVendorResponse response =
            categorySupplierVendorsService.addCategorySupplierVendor(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.FAILURE, response.getStatus());
        Assert.assertEquals("Cannot add category-supplier-vendor - supplier id is missing", response.getMessage());

        Assert.assertEquals(0, trackerService.getAllTickets().size());
    }

    @Test
    public void testAddCategorySupplierVendorNotAddedAlreadyPresent() {
        CategorySupplierVendors.AddCategorySupplierVendorRequest request =
            CategorySupplierVendors.AddCategorySupplierVendorRequest.newBuilder()
                .setCategorySupplierVendor(
                    CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                        .setCategoryId(1)
                        .setSupplierId(10L)
                        .setVendorId(100)
                        .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                        .build())
                .build();

        Mockito.doReturn(Collections.singletonList(CategorySupplierVendorBuilder.newBuilder()
            .id(1).categoryId(1).supplierId(10).vendorId(100).state(CategorySupplierVendor.State.SUSPENDED).build()))
            .when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));

        CategorySupplierVendors.AddCategorySupplierVendorResponse response =
            categorySupplierVendorsService.addCategorySupplierVendor(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.FAILURE, response.getStatus());
        Assert.assertEquals("Category-supplier-vendor [category id = 1, supplier id = 10, vendor id = 100]" +
            " has been already added", response.getMessage());

        Assert.assertEquals(0, trackerService.getAllTickets().size());
    }

    @Test
    public void testAddCategorySupplierVendorNotAddedInternalError() {
        CategorySupplierVendors.AddCategorySupplierVendorRequest request =
            CategorySupplierVendors.AddCategorySupplierVendorRequest.newBuilder()
                .setCategorySupplierVendor(
                    CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                        .setCategoryId(1)
                        .setSupplierId(10L)
                        .setVendorId(100)
                        .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                        .build())
                .build();

        Mockito.doThrow(new RuntimeException("message"))
            .when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));

        CategorySupplierVendors.AddCategorySupplierVendorResponse response =
            categorySupplierVendorsService.addCategorySupplierVendor(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.FAILURE, response.getStatus());
        Assert.assertEquals("message", response.getMessage());

        Assert.assertEquals(0, trackerService.getAllTickets().size());
    }

    @Test
    public void testGetCategorySupplierVendorsEmptyResult() {
        CategorySupplierVendors.GetCategorySupplierVendorsRequest request =
            CategorySupplierVendors.GetCategorySupplierVendorsRequest.newBuilder()
                .setCategoryId(1)
                .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                .build();

        Mockito.doReturn(new ArrayList())
            .when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));

        CategorySupplierVendors.GetCategorySupplierVendorsResponse response =
            categorySupplierVendorsService.getCategorySupplierVendors(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.SUCCESS, response.getStatus());
        Assert.assertEquals("Found 0 entries", response.getMessage());
    }

    @Test
    public void testGetCategorySupplierVendorsNotEmptyResult() {
        CategorySupplierVendors.GetCategorySupplierVendorsRequest request =
            CategorySupplierVendors.GetCategorySupplierVendorsRequest.newBuilder().build();

        Mockito.doReturn(Arrays.asList(
            CategorySupplierVendorBuilder.newBuilder()
                .id(1).categoryId(1).supplierId(10).vendorId(100).state(CategorySupplierVendor.State.SUSPENDED).build(),
            CategorySupplierVendorBuilder.newBuilder()
                .id(2).categoryId(1).supplierId(11).vendorId(101).state(CategorySupplierVendor.State.ALIVE).build()
        )).when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));

        CategorySupplierVendors.GetCategorySupplierVendorsResponse response =
            categorySupplierVendorsService.getCategorySupplierVendors(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.SUCCESS, response.getStatus());
        Assert.assertEquals("Found 2 entries", response.getMessage());
        Assert.assertEquals(CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                .setId(1).setCategoryId(1).setSupplierId(10L).setVendorId(100)
                .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED).build(),
            response.getCategorySupplierVendors(0)
        );
        Assert.assertEquals(CategorySupplierVendors.CategorySupplierVendor.newBuilder()
                .setId(2).setCategoryId(1).setSupplierId(11L).setVendorId(101)
                .setState(CategorySupplierVendors.CategorySupplierVendor.State.ALIVE).build(),
            response.getCategorySupplierVendors(1)
        );
    }

    @Test
    public void testGetCategorySupplierVendorsInternalError() {
        CategorySupplierVendors.GetCategorySupplierVendorsRequest request =
            CategorySupplierVendors.GetCategorySupplierVendorsRequest.newBuilder()
                .setCategoryId(1)
                .setState(CategorySupplierVendors.CategorySupplierVendor.State.SUSPENDED)
                .build();

        Mockito.doThrow(new RuntimeException("message"))
            .when(repository).getCategorySupplierVendors(Mockito.any(CategorySupplierVendorFilter.class));

        CategorySupplierVendors.GetCategorySupplierVendorsResponse response =
            categorySupplierVendorsService.getCategorySupplierVendors(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.FAILURE, response.getStatus());
        Assert.assertEquals("message", response.getMessage());
    }

    @Test
    public void testRemoveCategorySupplierVendorsSuccess() {
        CategorySupplierVendors.RemoveCategorySupplierVendorsRequest request =
            CategorySupplierVendors.RemoveCategorySupplierVendorsRequest.newBuilder()
                .addIds(1)
                .build();

        CategorySupplierVendors.RemoveCategorySupplierVendorsResponse response =
            categorySupplierVendorsService.removeCategorySupplierVendors(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.SUCCESS, response.getStatus());
        Assert.assertEquals("Successfully removed", response.getMessage());
    }

    @Test
    public void testRemoveCategorySupplierVendorsInternalError() {
        CategorySupplierVendors.RemoveCategorySupplierVendorsRequest request =
            CategorySupplierVendors.RemoveCategorySupplierVendorsRequest.newBuilder()
                .addIds(1)
                .build();

        Mockito.doThrow(new RuntimeException("message"))
            .when(repository).delete(Mockito.any(CategorySupplierVendor.class));

        CategorySupplierVendors.RemoveCategorySupplierVendorsResponse response =
            categorySupplierVendorsService.removeCategorySupplierVendors(request);

        Assert.assertEquals(CategorySupplierVendors.ResponseStatus.FAILURE, response.getStatus());
        Assert.assertEquals("message", response.getMessage());
    }
}
