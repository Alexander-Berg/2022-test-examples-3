package ru.yandex.market.promoboss.service;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.promoboss.dao.constraints.CategoryConstraintsDao;
import ru.yandex.market.promoboss.dao.constraints.MskuConstraintsDao;
import ru.yandex.market.promoboss.dao.constraints.RegionConstraintsDao;
import ru.yandex.market.promoboss.dao.constraints.SupplierConstraintsDao;
import ru.yandex.market.promoboss.dao.constraints.VendorConstraintsDao;
import ru.yandex.market.promoboss.dao.constraints.WarehouseConstraintsDao;
import ru.yandex.market.promoboss.model.Constraints;
import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.market.promoboss.model.postgres.CategoryConstraintDto;
import ru.yandex.market.promoboss.model.postgres.MskuConstraintDto;
import ru.yandex.market.promoboss.model.postgres.RegionConstraintDto;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;
import ru.yandex.market.promoboss.model.postgres.VendorConstraintDto;
import ru.yandex.market.promoboss.model.postgres.WarehouseConstraintDto;
import ru.yandex.market.promoboss.utils.PromoFieldUtilsTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest
@ContextConfiguration(classes = {ConstraintsService.class})
public class ConstraintsServiceTest {
    private final Long promoId = 123L;
    private Constraints constraints;

    @Autowired
    private ConstraintsService constraintsService;

    @MockBean
    private SupplierConstraintsDao supplierConstraintsDao;
    @MockBean
    private WarehouseConstraintsDao warehouseConstraintsDao;
    @MockBean
    private VendorConstraintsDao vendorConstraintsDao;
    @MockBean
    private MskuConstraintsDao mskuConstraintsDao;
    @MockBean
    private CategoryConstraintsDao categoryConstraintsDao;
    @MockBean
    private RegionConstraintsDao regionConstraintsDao;

    @BeforeEach
    void prepare() {
        constraints = Constraints.builder()
                .suppliers(List.of(
                        SupplierConstraintDto.builder().supplierId(1L).exclude(false).build()
                ))
                .regions(List.of(
                        RegionConstraintDto.builder().regionId("1").exclude(false).build()
                ))
                .categories(List.of(
                        CategoryConstraintDto.builder().categoryId("1").exclude(false).build()
                ))
                .mskus(List.of(
                        MskuConstraintDto.builder().mskuId(1L).exclude(false).build()
                ))
                .vendors(List.of(
                        VendorConstraintDto.builder().vendorId("1").exclude(false).build()
                ))
                .warehouses(List.of(
                        WarehouseConstraintDto.builder().warehouseId(1L).exclude(false).build()
                ))
                .build();
    }

    @Test
    void insertAll() {
        // act
        constraintsService.saveForPromoId(promoId, PromoFieldUtilsTest.createAll(), constraints);

        // verify
        verify(supplierConstraintsDao).saveAll(any());
        verify(warehouseConstraintsDao).saveAll(any());
        verify(vendorConstraintsDao).saveAll(any());
        verify(mskuConstraintsDao).saveAll(any());
        verify(categoryConstraintsDao).saveAll(any());
        verify(regionConstraintsDao).saveAll(any());
    }

    @Test
    void insertAllNotEmpty() {
        // setup
        constraints
                .setSuppliers(List.of());

        // act
        constraintsService.saveForPromoId(promoId, PromoFieldUtilsTest.createAll(), constraints);

        // verify
        verifyNoInteractions(supplierConstraintsDao);
        verify(warehouseConstraintsDao).saveAll(any());
        verify(vendorConstraintsDao).saveAll(any());
        verify(mskuConstraintsDao).saveAll(any());
        verify(categoryConstraintsDao).saveAll(any());
        verify(regionConstraintsDao).saveAll(any());
    }

    @Test
    void insertOnlyModified() {
        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        modifiedFields.remove(PromoField.SUPPLIERS_CONSTRAINTS);

        // act
        constraintsService.saveForPromoId(promoId, modifiedFields, constraints);

        // verify
        verifyNoInteractions(supplierConstraintsDao);
        verify(warehouseConstraintsDao).saveAll(any());
        verify(vendorConstraintsDao).saveAll(any());
        verify(mskuConstraintsDao).saveAll(any());
        verify(categoryConstraintsDao).saveAll(any());
        verify(regionConstraintsDao).saveAll(any());
    }

    @Test
    void updateAll() {
        // act
        constraintsService.updateForPromoId(promoId, PromoFieldUtilsTest.createAll(), constraints);

        // verify
        verify(supplierConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(warehouseConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(vendorConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(mskuConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(categoryConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(regionConstraintsDao).updateByPromoId(any(), eq(promoId));
    }

    @Test
    void updateOnlyModified() {
        // setup
        Set<PromoField> modifiedFields = PromoFieldUtilsTest.createAll();
        modifiedFields.remove(PromoField.SUPPLIERS_CONSTRAINTS);

        // act
        constraintsService.updateForPromoId(promoId, modifiedFields, constraints);

        // verify
        verifyNoInteractions(supplierConstraintsDao);
        verify(warehouseConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(vendorConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(mskuConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(categoryConstraintsDao).updateByPromoId(any(), eq(promoId));
        verify(regionConstraintsDao).updateByPromoId(any(), eq(promoId));
    }
}
