package ru.yandex.market.api.util;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class BooleansTest {

    @Test
    public void shouldTransformFalseToNull() throws Exception {
        assertThat(Booleans.falseAsNull(false), is(nullValue()));
    }

    @Test
    public void shouldTransformNullToNull() throws Exception {
        assertThat(Booleans.falseAsNull(null), is(nullValue()));
    }

    @Test
    public void shouldTransformTrueToTrue() throws Exception {
        assertThat(Booleans.falseAsNull(true), is(true));
    }
}
