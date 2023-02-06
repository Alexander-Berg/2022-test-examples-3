package ru.yandex.market.sc.core.dbqueue.xdoc.util;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.sortable.repository.dbo.PlainSortable;

import static org.assertj.core.api.Assertions.assertThat;

class ReadySortablesFinderTest {

    private PushXdocReadyInboundsTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PushXdocReadyInboundsTestHelper();
    }

    @Test
    @DisplayName("Одна поставка на двух паллетах")
    void oneInboundTwoPallet() {
        var a = helper.fixedInbound(1);
        helper.pallet(1, a);
        helper.pallet(2, null, helper.box(3, a), helper.box(4, a));
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.PACKED)
        ).find();
        assertThat(StreamEx.of(ready).map(PlainSortable::getId).toSet()).containsAll(Set.of(1L, 2L, 3L, 4L));
    }

    @Test
    @DisplayName("Две поставки, в одной коробка не упакована")
    void twoInbounds() {
        var a = helper.fixedInbound(1);
        var b = helper.fixedInbound(2);
        helper.pallet(1, a);
        helper.box(2, b);
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.PACKED)
        ).find();
        assertThat(StreamEx.of(ready).map(PlainSortable::getId).toSet()).containsAll(Set.of(1L));
    }

    @Test
    @DisplayName("Две поставки, палеты не закрыты")
    void twoInboundsOpened() {
        var a = helper.fixedInbound(1);
        var b = helper.fixedInbound(2);
        helper.basket(1, a);
        helper.box(2, b);
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.CREATED)
        ).find();
        assertThat(ready).isEmpty();
    }

    @Test
    @DisplayName("Две поставки, перемешанные между собой, одно из них не закрыта")
    void twoMixedInbound() {
        var a = helper.fixedInbound(1);
        var b = helper.arrivedInbound(2);
        helper.pallet(1, null, helper.box(2, a), helper.box(3, b));
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.PACKED)
        ).find();
        assertThat(StreamEx.of(ready).map(PlainSortable::getId).toSet()).isEmpty();
    }

    @Test
    @DisplayName("Из-за неконсолидированной коробки не готово ничего")
    void oneNonConsolidateBoxRuinsEverything() {
        var a = helper.fixedInbound(1);
        var b = helper.fixedInbound(2);
        var c = helper.fixedInbound(3);
        helper.pallet(1, a);
        helper.pallet(2, b);
        helper.pallet(3, null, helper.box(4, c), helper.box(5, b));
        helper.pallet(6, null, helper.box(7, b), helper.box(8, a));
        helper.box(9, c);
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.PACKED)
        ).find();
        assertThat(StreamEx.of(ready).map(PlainSortable::getId).toSet()).isEmpty();
    }

    @Test
    @DisplayName("Нет коробок вообще")
    void noSortablesAtAll() {
        Set<PlainSortable> ready = new ReadySortablesFinder(
                new XdocSortableGraph(helper.getAll()), helper.getDummyLots(LotStatus.PACKED)
        ).find();
        assertThat(StreamEx.of(ready).map(PlainSortable::getId).toSet()).isEmpty();
    }
}
