package ru.yandex.market.delivery.transport_manager.facade.register.pallet;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnitRelation;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PalletingItemWithCount;

class PalletingServiceTest {
    @DisplayName("Получаем все не упакованные вещи - коробки не на паллете и item-ы не в коробках и не на паллетах")
    @Test
    void getUnpackedItemsAndBoxes() {
        PalletingService palletingService = new PalletingService(null);

        RegisterUnit unit3 = unit(3L, UnitType.BOX);
        RegisterUnit unit6 = item(6L, 6);

        List<RegisterUnit> units = List.of(
            unit(1L, UnitType.PALLET),
            unit(2L, UnitType.BOX), // стоит на паллете 1
            unit3,
            item(4L, 4), // лежит в коробке 3
            item(5L, 5), // стоит на паллете 1
            unit6
        );
        List<RegisterUnitRelation> relations = List.of(
            relation(2L, 1L),
            relation(4L, 3L),
            relation(5L, 1L)
        );

        List<PalletingItemWithCount> unpackedItemsAndBoxes = palletingService.getUnpackedItemsAndBoxes(
            units,
            relations,
            new BoxContentsCountingRegisterUnitToPalletingUnitConverter(units, relations)
        );

        org.assertj.core.api.Assertions.assertThat(unpackedItemsAndBoxes).containsExactlyInAnyOrder(
            PalletingItemWithCount.ofBox(unit3, 4),
            PalletingItemWithCount.ofItem(unit6, 0, CountType.FIT),
            PalletingItemWithCount.ofItem(unit6, 1, CountType.FIT),
            PalletingItemWithCount.ofItem(unit6, 2, CountType.FIT),
            PalletingItemWithCount.ofItem(unit6, 3, CountType.FIT),
            PalletingItemWithCount.ofItem(unit6, 4, CountType.FIT),
            PalletingItemWithCount.ofItem(unit6, 5, CountType.FIT)
        );
    }

    @NotNull
    private RegisterUnitRelation relation(long id, long parentId) {
        return new RegisterUnitRelation().setId(id).setParentId(parentId);
    }

    @NotNull
    private RegisterUnit unit(long id, UnitType type) {
        int quantity = 1;
        List<UnitCount> counts = List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(quantity));
        return new RegisterUnit(id, null, type, null, null, null, null, counts, null, null, null, null);
    }

    @NotNull
    private RegisterUnit item(long id, int quantity) {
        List<UnitCount> counts = List.of(new UnitCount().setCountType(CountType.FIT).setQuantity(quantity));
        return new RegisterUnit(id, null, UnitType.ITEM, null, null, null, null, counts, null, null, null, null);
    }
}
