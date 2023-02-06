package ru.yandex.canvas.service;

import org.junit.Test;

import ru.yandex.canvas.service.video.Ratio;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RatioTest {

    @Test
    public void fractionTest() {
        assertThat(new Ratio(3, 1).toString(), is("3:1"));
        assertThat(new Ratio(30, 10).toString(), is("3:1"));
        assertThat(new Ratio(171, 32).toString(), is("171:32"));
        assertThat(new Ratio(1920, 1080).toString(), is("16:9"));
        assertThat(new Ratio(61440, 34560).toString(), is("16:9"));
        assertThat(new Ratio(0, 0).toString(), is("0:0"));
        assertThat(new Ratio(1, 1).toString(), is("1:1"));
        assertThat(new Ratio(100, 100).toString(), is("1:1"));
        assertThat(new Ratio(0, 100).toString(), is("0:1"));
        assertThat(new Ratio(736, 576).toString(), is("23:18"));
        assertThat(new Ratio(1248, 880).toString(), is("78:55"));
        assertThat(new Ratio(1504, 400).toString(), is("94:25"));
        assertThat(new Ratio(1536, 1152).toString(), is("4:3"));
        assertThat(new Ratio(1920, 576).toString(), is("10:3"));
    }

    @Test
    public void parseTest() {
        assertThat(new Ratio("3:1").toString(), is("3:1"));
        assertThat(new Ratio("1:1").toString(), is("1:1"));
        assertThat(new Ratio("1:3").toString(), is("1:3"));
        assertThat(new Ratio("1:1").getHeight(), is(1));
        assertThat(new Ratio("1:1").getWidth(), is(1));
    }

    @Test
    public void compareTest() {
        assertThat(new Ratio("3:1").compareTo(new Ratio("3:1")), is(0));
        assertThat(new Ratio("1:1").compareTo(new Ratio("1:1")), is(0));
        assertThat(new Ratio("3:1").compareTo(new Ratio("1:1")), is(1));
        assertThat(new Ratio("1:1").compareTo(new Ratio("3:1")), is(-1));
        assertThat(new Ratio("3:1").compareTo(new Ratio("1:3")), is(1));
        assertThat(new Ratio("1:3").compareTo(new Ratio("3:1")), is(-1));
    }

    @Test
    public void ratioPercentTest() {
        assertThat(new Ratio("3:1").ratioPercent(), is(300));
        assertThat(new Ratio("1:1").ratioPercent(), is(100));
        assertThat(new Ratio("1:3").ratioPercent(), is(33));
        assertThat(new Ratio("16:9").ratioPercent(), is(177));
    }
}
