package ru.yandex.market.ir.autogeneration.common.util;

import org.junit.Test;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class ParamUtilsTest {

    @Test
    public void wrapOptionValue() {
        assertSoftly(softly -> {
                softly.assertThat(wrapOptionValue("hello world")).isEqualTo("hello world");
                softly.assertThat(wrapOptionValue("hello, world")).isEqualTo("hello world");
                softly.assertThat(wrapOptionValue("hello ,world")).isEqualTo("hello world");
                softly.assertThat(wrapOptionValue("hello , world")).isEqualTo("hello  world");
                softly.assertThat(wrapOptionValue("hello, world,")).isEqualTo("hello world ");
                softly.assertThat(wrapOptionValue(",hello, world")).isEqualTo(" hello world");
                softly.assertThat(wrapOptionValue(",hello, world,")).isEqualTo(" hello world ");
                softly.assertThat(wrapOptionValue(",he,llo, world,")).isEqualTo(" he llo world ");
                softly.assertThat(wrapOptionValue(",,,he,,llo, world,,")).isEqualTo("   he  llo world  ");
            }
        );
    }

    private String wrapOptionValue(String value) {
        return ParamUtils.wrapOptionValue(value, ",");
    }
}