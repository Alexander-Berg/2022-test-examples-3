package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.packer;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.ModelParameters;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PackagingRound;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PallettingId;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.dto.PreparedPayloadData;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.Pallet;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.PackagingBlock;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.Size;
import ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives.WeightClass;

class RegularPalletPackerTest {

    public static final ModelParameters MODEL_PARAMETERS = new ModelParameters(
        ModelParameters.SplitDirection.SHORT_SIDE_FIRST,
        true,
        0,
        1,
        false,
        1.0
    );
    final RegularPalletPacker regularPalletPacker = new RegularPalletPacker();
    final PackagingBlock block1 = new PackagingBlock(
        List.of(
            new PallettingId(1L, 0, CountType.FIT),
            new PallettingId(2L, 0, CountType.FIT)
        ),
        new Size(100, 80),
        130,
        50,
        50,
        WeightClass.MEDIUM,
        null,
        null,
        1
    );
    final PackagingBlock block2 = new PackagingBlock(
        List.of(
            new PallettingId(1L, 0, CountType.FIT),
            new PallettingId(3L, 0, CountType.FIT)
        ),
        new Size(100, 80),
        130,
        50,
        50,
        WeightClass.MEDIUM,
        null,
        null,
        1
    );
    final PackagingBlock block3 = new PackagingBlock(
        List.of(
            new PallettingId(4L, 0, CountType.FIT)
        ),
        new Size(100, 80),
        130,
        50,
        50,
        WeightClass.MEDIUM,
        null,
        null,
        1
    );

    @DisplayName("Нельзя упаковать блоки с одинаковыми коробками внутри")
    @Test
    void testNotPackingSameIdTwice() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> {
                regularPalletPacker.pack(
                    new PreparedPayloadData(
                        1,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        List.of(new PackagingRound(
                            List.of(block1, block2),
                            false,
                            false,
                            false
                        ))
                    ),
                    MODEL_PARAMETERS
                );
            }
        );
    }

    @DisplayName("Нельзя упаковать блоки с одинаковыми коробками внутри в разных весовых группах")
    @Test
    void testNotPackingSameIdTwiceDifferentRounds() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> {
                regularPalletPacker.pack(
                    new PreparedPayloadData(
                        1,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        List.of(
                            new PackagingRound(
                                List.of(block1),
                                false,
                                false,
                                false
                            ),
                            new PackagingRound(
                                List.of(block2),
                                false,
                                false,
                                false
                            )
                        )
                    ),
                    MODEL_PARAMETERS
                );
            }
        );
    }

    @DisplayName("Блок не будет конфликтовать сам с собой при второй попытке укладки после добавления новой паллеты")
    @Test
    void testDeduplicate() {
        List<Pallet> pallets = regularPalletPacker.pack(
            new PreparedPayloadData(
                1,
                Collections.emptyList(),
                Collections.emptyList(),
                List.of(new PackagingRound(
                    List.of(block1, block3),
                    false,
                    false,
                    false
                ))
            ),
            MODEL_PARAMETERS
        );
        Assertions.assertEquals(2, pallets.size());
    }
}
