package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Size3DTest {
    @DisplayName("Поворот: самая длинная сторона - длинна, самая короткая - высота")
    @Test
    void norm() {
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(10, 9, 8).norm());
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(10, 8, 9).norm());
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(9, 10, 8).norm());
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(9, 8, 10).norm());
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(8, 10, 9).norm());
        Assertions.assertEquals(new Size3D(10, 9, 8), new Size3D(8, 9, 10).norm());

        Assertions.assertEquals(new Size3D(10, 8, 8), new Size3D(10, 8, 8).norm());
        Assertions.assertEquals(new Size3D(10, 8, 8), new Size3D(8, 10, 8).norm());
        Assertions.assertEquals(new Size3D(10, 8, 8), new Size3D(8, 8, 10).norm());

        Assertions.assertEquals(new Size3D(8, 8, 8), new Size3D(8, 8, 8).norm());
    }
}
