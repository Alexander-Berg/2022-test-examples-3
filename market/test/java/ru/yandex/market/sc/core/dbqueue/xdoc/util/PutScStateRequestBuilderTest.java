package ru.yandex.market.sc.core.dbqueue.xdoc.util;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PutScStateRequestBuilderTest {
    private PushXdocReadyInboundsTestHelper helper;

    @BeforeEach
    void setUp() {
        helper = new PushXdocReadyInboundsTestHelper();
    }

    @Test
    @DisplayName("Две поставки из трёх")
    void twoReadyPallets() {
        var a = helper.fixedInbound(1, "100");
        var b = helper.fixedInbound(2, "100");
        var c = helper.arrivedInbound(3);
        var x = helper.pallet(1, a);
        var box = helper.box(4, b);
        var y = helper.pallet(2, b, box);
        var z = helper.pallet(3, c);
        var request = new PutScStateRequestBuilder(Map.of()).build(
                new XdocSortableGraph(helper.getAll()), Stream.of(x, y, box).map(helper::plain).toList());
        assertThat(request.getPallets().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Разные точки назначения в одной паллете")
    void differentNextLogisticPointId() {
        var a = helper.fixedInbound(1, "100");
        var b = helper.fixedInbound(2, "200");
        var x = helper.box(1, a);
        var y = helper.box(2, b);
        var z = helper.pallet(3, null, x, y);
        assertThrows(TplIllegalStateException.class,
                () -> new PutScStateRequestBuilder(Map.of())
                        .build(new XdocSortableGraph(helper.getAll()),
                                Stream.of(x, y, z).map(helper::plain).toList()));
    }
}
