package ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.model.primitives;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ru.yandex.market.delivery.transport_manager.facade.register.pallet.layer_based.PallettingIdConverter.toPalletingIds;

class RectTest {

    private final PackagingBlock block = new PackagingBlock(
        toPalletingIds(1L),
        new Size(4, 3),
        1,
        1.0,
        1.0,
        WeightClass.MEDIUM,
        null,
        null,
        1
    );

    @DisplayName("Один прямоугольник сбоку другого")
    @Test
    void intersectionNoneX() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(3, 0, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Один прямоугольник снизу другого")
    @Test
    void intersectionNoneY() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(0, 4, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Прямоугольники касаются боковой стороной")
    @Test
    void intersectionNoneRightSide() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(2, 0, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Прямоугольники касаются нижней/верхней стороной")
    @Test
    void intersectionNoneBottomSide() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(0, 3, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Прямоугольники касаются углом 1")
    @Test
    void intersectionNoneCorner1() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(2, 3, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Прямоугольники касаются углом 2")
    @Test
    void intersectionNoneCorner2() {
        final Rect r1 = new Rect(2, 0, 2, 3);
        final Rect r2 = new Rect(0, 3, 2, 3);

        Assertions.assertEquals(Optional.empty(), r1.intersection(r2));
        Assertions.assertEquals(Optional.empty(), r2.intersection(r1));
    }

    @DisplayName("Полное перекрытие")
    @Test
    void intersectionFull() {
        final Rect r1 = new Rect(0, 0, 2, 3);

        Assertions.assertEquals(Optional.of(r1), r1.intersection(r1));
    }

    @DisplayName("Пересечение 1")
    @Test
    void intersection1() {
        final Rect r1 = new Rect(0, 0, 2, 3);
        final Rect r2 = new Rect(1, 2, 2, 3);

        Assertions.assertEquals(Optional.of(new Rect(1, 2, 1, 1)), r1.intersection(r2));
        Assertions.assertEquals(Optional.of(new Rect(1, 2, 1, 1)), r2.intersection(r1));
    }

    @DisplayName("Пересечение 2")
    @Test
    void intersection2() {
        final Rect r1 = new Rect(1, 0, 2, 3);
        final Rect r2 = new Rect(0, 2, 2, 3);

        Assertions.assertEquals(Optional.of(new Rect(1, 2, 1, 1)), r1.intersection(r2));
        Assertions.assertEquals(Optional.of(new Rect(1, 2, 1, 1)), r2.intersection(r1));
    }

    @DisplayName("Площадь")
    @Test
    void area() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertEquals(12, r.area());
    }

    @DisplayName("Поместится ли один прямоугольник в другой: нет")
    @Test
    void canContainNoFlip() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertFalse(r.canContain(new Size(4, 3)));
    }

    @DisplayName("Поместится ли один прямоугольник в другой: нет. 1")
    @Test
    void canContainNo1() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertFalse(r.canContain(new Size(4, 4)));
    }

    @DisplayName("Поместится ли один прямоугольник в другой: нет. 2")
    @Test
    void canContainNo2() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertFalse(r.canContain(new Size(3, 5)));
    }

    @DisplayName("Поместится ли один прямоугольник в другой такой же")
    @Test
    void canContainSame() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertTrue(r.canContain(new Size(3, 4)));
    }

    @DisplayName("Поместится ли один прямоугольник в другой больший")
    @Test
    void canContain() {
        final Rect r = new Rect(1, 2, 3, 4);
        Assertions.assertTrue(r.canContain(new Size(2, 3)));
    }


    @DisplayName("Помещаем коробку в вертикально вытянутый (l < w) прямоугольник и считаем оставшееся свободное " +
        "место, разрезая вдоль короткой стороны")
    @Test
    void freeSpaceVerticalAndShortSideFirst() {
        final Rect r = new Rect(3, 4, 5, 10);

        Assertions.assertEquals(
            List.of(
                new Rect(7, 4, 1, 3),
                new Rect(3, 7, 5, 7)
            ),
            r.freeSpace(block, true)
        );
    }


    @DisplayName("Помещаем коробку в вертикально вытянутый (l < w) прямоугольник и считаем оставшееся свободное " +
        "место, разрезая вдоль длинной стороны")
    @Test
    void freeSpaceVerticalAndLongSideFirst() {
        final Rect r = new Rect(3, 4, 5, 10);

        Assertions.assertEquals(
            List.of(
                new Rect(3, 7, 4, 7),
                new Rect(7, 4, 1, 10)
            ),
            r.freeSpace(block, false)
        );
    }


    @DisplayName("Помещаем коробку в горизонтально вытянутый (l > w) прямоугольник и считаем оставшееся свободное " +
        "место, разрезая вдоль короткой стороны")
    @Test
    void freeSpaceHorisontalAndShortSideFirst() {
        final Rect r = new Rect(3, 4, 10, 5);

        Assertions.assertEquals(
            List.of(
                new Rect(3, 7, 4, 2),
                new Rect(7, 4, 6, 5)
            ),
            r.freeSpace(block, true)
        );
    }


    @DisplayName("Помещаем коробку в горизонтально вытянутый (l > w) прямоугольник и считаем оставшееся свободное " +
        "место, разрезая вдоль длинной стороны")
    @Test
    void freeSpaceHorisontalAndLongSideFirst() {
        final Rect r = new Rect(3, 4, 10, 5);

        Assertions.assertEquals(
            List.of(
                new Rect(7, 4, 6, 3),
                new Rect(3, 7, 10, 2)
            ),
            r.freeSpace(block, false)
        );
    }

    @DisplayName("Место идеально вмещает коробку")
    @Test
    void bestFit() {
        final Rect r = new Rect(3, 4, 10, 5);
        Assertions.assertTrue(r.bestFit(new Size(10, 5), 0));
    }


    @DisplayName("Место плотно вмещает коробку с небольшой погрешностью")
    @Test
    void bestFitDelta() {
        final Rect r = new Rect(3, 4, 10, 5);
        Assertions.assertTrue(r.bestFit(new Size(9, 4), 1));
    }


    @DisplayName("Место больше коробки и положение не идеально")
    @Test
    void bestFitTooSmallBox() {
        final Rect r = new Rect(3, 4, 10, 5);
        Assertions.assertFalse(r.bestFit(new Size(8, 5), 1));
    }


    @DisplayName("Место меньше коробки")
    @Test
    void bestFitTooBigBox() {
        final Rect r = new Rect(3, 4, 10, 5);
        Assertions.assertFalse(r.bestFit(new Size(11, 5), 1));
    }

}
