package ru.yandex.market.delivery.transport_manager.service.register.splitter;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialId;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.PartialIds;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.IdType;

public class RegisterUnitMergerTest {

    @Test
    public void merge() {
        var update = singleVendorUpdate(1);
        var existing = singleVendorUpdate(2);
        RegisterUnit registerUnit = new RegisterUnit().setCounts(existing);
        registerUnit.addCount(update);
        var expected = singleVendorMerged();
        Assertions.assertThat(registerUnit.getCounts()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void mergeEmptyExisting() {
        var update = singleVendorUpdate(1);
        List<UnitCount> existing = List.of();
        RegisterUnit registerUnit = new RegisterUnit().setCounts(existing);
        registerUnit.addCount(update);
        var expected = singleVendorUpdate(1);
        Assertions.assertThat(registerUnit.getCounts()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void mergeEmptyUpdate() {
        List<UnitCount> update = List.of();
        var existing = singleVendorUpdate(1);
        RegisterUnit registerUnit = new RegisterUnit().setCounts(existing);
        registerUnit.addCount(update);
        var expected = singleVendorUpdate(1);
        Assertions.assertThat(registerUnit.getCounts()).containsExactlyInAnyOrderElementsOf(expected);
    }

    // ожидаемый результат: один вендор, одна поставка, 2 sku, у одного sku есть фиты и дефекты
    private List<UnitCount> singleVendorMerged() {
        return List.of(
            new UnitCount().setQuantity(150).setCountType(CountType.FIT).setUnitIds(
                samePartialIds("SKU150")),
            new UnitCount().setQuantity(30).setCountType(CountType.FIT).setUnitIds(
                samePartialIds("SKU100")),
            new UnitCount().setQuantity(3).setCountType(CountType.DEFECT).setUnitIds(
                samePartialIds("SKU100"))

        );
    }

    @NotNull
    private List<PartialIds> samePartialIds(String sku) {
        return List.of(
            new PartialIds().setPartialIds(
                List.of(
                    new PartialId().setIdType(IdType.ARTICLE).setValue(sku),
                    new PartialId().setIdType(IdType.CONSIGNMENT_ID).setValue("Postavka #1"),
                    new PartialId().setIdType(IdType.VENDOR_ID).setValue("Vendor #1")
                ))
        );
    }

    @NotNull
    private List<UnitCount> singleVendorUpdate(int multiplier) {
        return List.of(
            new UnitCount().setQuantity(multiplier * 50).setCountType(CountType.FIT).setUnitIds(
                samePartialIds("SKU150")),
            new UnitCount().setQuantity(multiplier * 10).setCountType(CountType.FIT).setUnitIds(
                samePartialIds("SKU100")),
            new UnitCount().setQuantity(multiplier).setCountType(CountType.DEFECT).setUnitIds(
                samePartialIds("SKU100"))
        );
    }
}
