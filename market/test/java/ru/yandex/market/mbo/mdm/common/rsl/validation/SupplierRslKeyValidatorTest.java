package ru.yandex.market.mbo.mdm.common.rsl.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.rsl.RslExcelRowData;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;


public class SupplierRslKeyValidatorTest extends MdmBaseDbTestClass {

    private SupplierRslKeyValidator supplierRslKeyValidator;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    private BeruId beruId;

    @Before
    public void before() {
        CategoryCachingService categoryCachingService = new CategoryCachingServiceMock()
            .addCategory(1, "я разраб")
            .addCategory(2, "и у меня нет башки")
            .addCategory(3, "мне отбили ее фреймворками");
        beruId = new BeruIdMock();
        supplierRslKeyValidator = new SupplierRslKeyValidator(
            categoryCachingService,
            mdmSupplierRepository,
            beruId);
    }

    @Test
    public void testWhenCorrectShouldPass() {
        MdmSupplier supplier = new MdmSupplier()
            .setRealSupplierId("kek")
            .setType(MdmSupplierType.REAL_SUPPLIER);
        mdmSupplierRepository.insert(supplier);

        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(5),

            new RslExcelRowData().setCargoType750(false)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(beruId.getId())
                .setRowNumber(6),
            new RslExcelRowData().setCargoType750(false)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(beruId.getId())
                .setRowNumber(7),
            new RslExcelRowData().setCargoType750(false)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(beruId.getId())
                .setRowNumber(8),
            new RslExcelRowData().setCargoType750(false)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(beruId.getId())
                .setRowNumber(9),
            new RslExcelRowData().setCargoType750(false)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(beruId.getId())
                .setRowNumber(10)
        );
        List<RslValidationError> errors = supplierRslKeyValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWhenIncorrectShouldFail() {
        MdmSupplier supplier = new MdmSupplier()
            .setRealSupplierId("kek")
            .setId(2)
            .setType(MdmSupplierType.REAL_SUPPLIER);
        mdmSupplierRepository.insert(supplier);

        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("куку")
                .setCategoryId(1L)
                .setSupplierId(beruId.getId())
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(42L)
                .setSupplierId(beruId.getId())
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(21)
                .setRowNumber(4)
        );
        List<RslValidationError> errors = supplierRslKeyValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "На строке 3 нет идентификатора поставщика",
                "Данной категории не существует: 42",
                "Данный поставщик не является 1P: 21",
                "1P поставщика с таким real_id не существует: куку");
    }
}
