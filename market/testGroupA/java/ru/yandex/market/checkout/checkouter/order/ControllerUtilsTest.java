package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;

public class ControllerUtilsTest {

    @Test
    public void parseIpsFromXFFHeaderShouldReturnEmptySet() {
        assertThat(ControllerUtils.parseIpsFromXFFHeader(null), emptyIterable());
        assertThat(ControllerUtils.parseIpsFromXFFHeader(""), emptyIterable());
        assertThat(ControllerUtils.parseIpsFromXFFHeader(" "), emptyIterable());
        assertThat(ControllerUtils.parseIpsFromXFFHeader(" ,"), emptyIterable());
        assertThat(ControllerUtils.parseIpsFromXFFHeader(" , "), emptyIterable());
        assertThat(ControllerUtils.parseIpsFromXFFHeader(" , , "), emptyIterable());
    }

    @Test
    public void parseIpsFromXFFHeaderShouldReturnNotEmptySet() {
        assertThat(ControllerUtils.parseIpsFromXFFHeader("1"), containsInAnyOrder("1"));
        assertThat(ControllerUtils.parseIpsFromXFFHeader("1,2"), containsInAnyOrder("1", "2"));
        assertThat(ControllerUtils.parseIpsFromXFFHeader("1, 2"), containsInAnyOrder("1", "2"));
        assertThat(ControllerUtils.parseIpsFromXFFHeader("1, 2,1"), containsInAnyOrder("1", "2"));
        assertThat(ControllerUtils.parseIpsFromXFFHeader("1.1.1.1, 2.1.2.1"),
                containsInAnyOrder("1.1.1.1", "2.1.2.1"));
    }
}
