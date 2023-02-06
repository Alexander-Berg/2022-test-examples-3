package ru.yandex.market.delivery.transport_manager.facade.register.pallet;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.RegisterUnitRelation;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.UnitCount;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.UnitType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PalletingItemWithCount;

class BoxContentsCountingRegisterUnitToPalletingUnitConverterTest {
    @DisplayName("Кол-во для айтема")
    @Test
    void withItemCountForItem() {
        var converter = new BoxContentsCountingRegisterUnitToPalletingUnitConverter(
            List.of(),
            List.of()
        );
        RegisterUnit unit = unit(1L, UnitType.ITEM);
        Stream<PalletingItemWithCount> unitWithCount = converter.convert(unit);

        org.assertj.core.api.Assertions
            .assertThat(unitWithCount.collect(Collectors.toList()))
            .containsExactly(PalletingItemWithCount.ofItem(unit, 0, CountType.FIT));
    }

    @DisplayName("Кол-во для коробки")
    @Test
    void withItemCountForBox() {
        var converter = new BoxContentsCountingRegisterUnitToPalletingUnitConverter(
            List.of(
                item(2L, 100),
                unit(1L, UnitType.BOX)
            ),
            List.of(
                new RegisterUnitRelation().setId(2L).setParentId(1L)
            )
        );
        List<PalletingItemWithCount> unitWithCount = converter.convert(
            unit(1L, UnitType.BOX)
        )
            .collect(Collectors.toList());
        Assertions.assertEquals(1, unitWithCount.size());
        Assertions.assertEquals(100, unitWithCount.get(0).getCount());
    }

    @DisplayName("Кол-во для коробки: сломан маппинг")
    @Test
    void withItemCountForBoxBadMapping() {
        var converter = new BoxContentsCountingRegisterUnitToPalletingUnitConverter(
            List.of(
                item(2L, 100),
                unit(1L, UnitType.BOX)
            ),
            List.of()
        );
        List<PalletingItemWithCount> unitWithCount = converter.convert(
            unit(1L, UnitType.BOX)
        )
            .collect(Collectors.toList());
        Assertions.assertEquals(1, unitWithCount.size());
        Assertions.assertEquals(0, unitWithCount.get(0).getCount());
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
