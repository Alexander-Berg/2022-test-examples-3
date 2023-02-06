package ru.yandex.hadoop.woodsman.loaders.logbroker.parser;


import org.junit.Test;
import ru.yandex.hadoop.woodsman.loaders.logbroker.parser.service.RangeTree;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by oroboros on 17.12.15.
 */
public class RangeTreeTest {
    @Test
    public void noOverlap() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(2)
                .addStart(4)
                .addEnd(6)
                .addStart(8)
                .addEnd(10)
                .build();

        assertThat(rt.inRange(1), is(true));
        assertThat(rt.inRange(5), is(true));
        assertThat(rt.inRange(9), is(true));

        assertThat(rt.inRange(0), is(true));
        assertThat(rt.inRange(2), is(true));
        assertThat(rt.inRange(4), is(true));
        assertThat(rt.inRange(6), is(true));
        assertThat(rt.inRange(8), is(true));
        assertThat(rt.inRange(10), is(true));

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(3), is(false));
        assertThat(rt.inRange(7), is(false));
        assertThat(rt.inRange(11), is(false));
    }

    @Test
    public void overlap() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(6)
                .addStart(3)
                .addEnd(9)
                .build();

        assertThat(rt.inRange(0), is(true));
        assertThat(rt.inRange(3), is(true));
        assertThat(rt.inRange(5), is(true));
        assertThat(rt.inRange(6), is(true));
        assertThat(rt.inRange(9), is(true));

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(10), is(false));
    }

    @Test
    public void doubleDoubleOverlap() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(6)
                .addStart(3)
                .addEnd(9)
                .addStart(20)
                .addEnd(27)
                .addStart(25)
                .addEnd(30)
                .build();

        assertThat(rt.inRange(0), is(true));
        assertThat(rt.inRange(3), is(true));
        assertThat(rt.inRange(5), is(true));
        assertThat(rt.inRange(6), is(true));
        assertThat(rt.inRange(9), is(true));

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(10), is(false));

        assertThat(rt.inRange(20), is(true));
        assertThat(rt.inRange(25), is(true));
        assertThat(rt.inRange(26), is(true));
        assertThat(rt.inRange(27), is(true));
        assertThat(rt.inRange(30), is(true));

        assertThat(rt.inRange(15), is(false));
        assertThat(rt.inRange(40), is(false));
    }

    @Test
    public void coveredOverlap() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(6)
                .addStart(3)
                .addEnd(9)
                .addStart(20)
                .addEnd(27)
                .addStart(25)
                .addEnd(30)
                .addStart(-1)
                .addEnd(40)
                .build();

        for(int x = -1; x <= 40; x++) {
            assertThat(rt.inRange(x), is(true));
        }

        assertThat(rt.inRange(-2), is(false));
        assertThat(rt.inRange(45), is(false));
    }

    @Test
    public void multipleOverlap() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(10)
                .addEnd(14)
                .addStart(0)
                .addEnd(6)
                .addStart(5)
                .addEnd(7)
                .addStart(5)
                .addEnd(15)
                .build();

        for(int x = 0; x <= 15; x++) {
            assertThat("x: " + x, rt.inRange(x), is(true));
        }

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(16), is(false));
    }

    @Test
    public void sameStarts() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(4)
                .addStart(0)
                .addEnd(6)
                .addStart(0)
                .addEnd(8)
                .addStart(0)
                .addEnd(10)
                .build();

        for(int x = 0; x <= 10; x++) {
            assertThat("x: " + x, rt.inRange(x), is(true));
        }

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(11), is(false));
    }

    @Test
    public void sameStartsAndEnds() {
        RangeTree.Builder builder = RangeTree.builder();
        RangeTree rt = builder
                .addStart(0)
                .addEnd(4)
                .addStart(0)
                .addEnd(4)
                .addStart(0)
                .addEnd(4)
                .addStart(0)
                .addEnd(10)
                .build();

        for(int x = 0; x <= 10; x++) {
            assertThat("x: " + x, rt.inRange(x), is(true));
        }

        assertThat(rt.inRange(-1), is(false));
        assertThat(rt.inRange(11), is(false));
    }
}
