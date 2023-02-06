package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.metrics.checker;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PallettingId;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.Pallet;

class AllPackagedTest {
    public static final PallettingId ID_1 = new PallettingId(1L, 0, CountType.FIT);
    public static final PallettingId ID_2 = new PallettingId(2L, 0, CountType.FIT);
    public static final PallettingId ID_3 = new PallettingId(3L, 0, CountType.FIT);
    public static final PallettingId ID_4 = new PallettingId(4L, 0, CountType.FIT);
    public static final PallettingId ID_5 = new PallettingId(5L, 0, CountType.FIT);
    private static final PallettingId ID_10 = new PallettingId(10L, 0, CountType.FIT);
    private final AllPackaged checker = new AllPackaged(List.of(ID_1, ID_2, ID_3, ID_4, ID_5));

    @DisplayName("Проверка, что все id лежат на каких-то паллетах")
    @Test
    void check() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2, ID_3));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        checker.check(List.of(regular), List.of(oversize1, oversize2), List.of(oversize3));
    }

    @DisplayName("Один товар на 2 паллетах")
    @Test
    void checkTwoPallets() {
        Pallet regular1 = Mockito.mock(Pallet.class);
        Mockito.when(regular1.getItemIds()).thenReturn(List.of(ID_1, ID_2));
        Pallet regular2 = Mockito.mock(Pallet.class);
        Mockito.when(regular2.getItemIds()).thenReturn(List.of(ID_2, ID_3));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(
                List.of(regular1, regular2),
                List.of(oversize1, oversize2),
                List.of(oversize3)
            )
        );
    }

    @DisplayName("Один товар не упакован")
    @Test
    void goodMissing() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(List.of(regular), List.of(oversize1, oversize2), List.of(oversize3))
        );
    }

    @DisplayName("Один КГТ, который ставится по неск штук, не упакован")
    @Test
    void oversizeBunchGoodMissing() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(List.of(regular), List.of(oversize1, oversize2), Collections.emptyList())
        );
    }

    @DisplayName("Один КГТ не упакован")
    @Test
    void oversizeGoodMissing() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(List.of(regular), List.of(oversize1), List.of(oversize2))
        );
    }

    @DisplayName("Лишний товар")
    @Test
    void unknownThingOnPallet() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2, ID_10));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(List.of(regular), List.of(oversize1, oversize2), List.of(oversize3))
        );
    }

    @DisplayName("Лишний КГТ")
    @Test
    void unknownOversizeThingOnPallet() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        Pallet oversize4 = Mockito.mock(Pallet.class);
        Mockito.when(oversize4.getItemIds()).thenReturn(List.of(ID_10));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(
                List.of(regular),
                List.of(oversize1, oversize2, oversize4),
                List.of(oversize3)
            )
        );
    }

    @DisplayName("Лишний КГТ, который ставится по неск штук")
    @Test
    void unknownOversizeBunchThingOnPallet() {
        Pallet regular = Mockito.mock(Pallet.class);
        Mockito.when(regular.getItemIds()).thenReturn(List.of(ID_1, ID_2));

        // Один КГТ на 2 паллетах
        Pallet oversize1 = Mockito.mock(Pallet.class);
        Mockito.when(oversize1.getItemIds()).thenReturn(List.of(ID_4));
        Pallet oversize2 = Mockito.mock(Pallet.class);
        Mockito.when(oversize2.getItemIds()).thenReturn(List.of(ID_4));

        Pallet oversize3 = Mockito.mock(Pallet.class);
        Mockito.when(oversize3.getItemIds()).thenReturn(List.of(ID_5));

        Pallet oversize4 = Mockito.mock(Pallet.class);
        Mockito.when(oversize4.getItemIds()).thenReturn(List.of(ID_10));

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> checker.check(
                List.of(regular),
                List.of(oversize1, oversize2),
                List.of(oversize3, oversize4)
            )
        );
    }
}
