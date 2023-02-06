package ru.yandex.direct.core.entity.banner.model;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnitParamsRunner.class)
public class AgeTest {

    public static Object provideTestData() {
        return new Object[][]{
                {Age.AGE_0, "0"},
                {Age.AGE_6, "6"},
                {Age.AGE_12, "12"},
                {Age.AGE_16, "16"},
                {Age.AGE_18, "18"}
        };
    }

    @Test
    @Parameters(source = AgeTest.class)
    public void getValue(Age age, String sourceValue) {
        assertThat(age.getValue(), is(sourceValue));
    }

    @Test
    @Parameters(source = AgeTest.class)
    public void fromSource(Age age, String sourceValue) {
        assertThat(Age.fromSource(sourceValue), is(age));
    }

    @Test
    public void fromSource_ToEnum_Plus18IsAge18() {
        assertThat(Age.fromSource("plus18"), is(Age.AGE_18));
    }
}
