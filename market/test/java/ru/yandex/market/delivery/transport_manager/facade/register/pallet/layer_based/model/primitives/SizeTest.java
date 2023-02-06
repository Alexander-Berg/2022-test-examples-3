package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SizeTest {
    @DisplayName("Создание с отрицательной длинной")
    @Test
    void createNegativeLength() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Size(-1, 1)
        );
    }

    @DisplayName("Создание с отрицательной шириной")
    @Test
    void createNegativeWidth() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Size(-1, 1)
        );
    }

    @DisplayName("Нормализация: меняем длинну и ширину местами, если длинна меньше ширины")
    @Test
    void norm() {
        Assertions.assertEquals(
            new Size(10, 5),
            new Size(5, 10).norm()
        );
    }

    @DisplayName("Нормализация не требуется")
    @Test
    void normNotNeeded() {
        Assertions.assertEquals(
            new Size(10, 5),
            new Size(10, 5).norm()
        );
    }

    @DisplayName("Один прямоугольник входит в другой")
    @Test
    void contains() {
        Assertions.assertTrue(new Size(3, 2).canContain(new Size(2, 1)));
    }

    @DisplayName("Прямоугольник входит сам в себя")
    @Test
    void containsSelf() {
        final Size size = new Size(3, 2);
        Assertions.assertTrue(size.canContain(size));
    }

    @DisplayName("Один прямоугольник не входит в другой по ширине")
    @Test
    void notContainsByWidth() {
        Assertions.assertFalse(new Size(3, 1).canContain(new Size(2, 2)));
    }

    @DisplayName("Один прямоугольник не входит в другой по длине")
    @Test
    void notContainsByLength() {
        Assertions.assertFalse(new Size(3, 2).canContain(new Size(4, 1)));
    }
}
