package ru.yandex.market.crm.platform.reducers.utils;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReduceUtilsTest {

    @Mock
    Consumer<String> setter;
    @Mock
    LongConsumer longSetter;

    @Test
    public void testSetNewValue() {
        String v = UUID.randomUUID().toString();
        ReduceUtils.merge(new Container(), new Container(v), Container::getStringValue, setter, null);
        verify(setter, only()).accept(eq(v));
    }

    @Test
    public void testSetNewValueMode() {
        String vOld = UUID.randomUUID().toString();
        String vNew = UUID.randomUUID().toString();
        ReduceUtils.merge(new Container(vOld), new Container(vNew), Container::getStringValue, setter, true);
        verify(setter, only()).accept(eq(vNew));
    }

    @Test
    public void testRetainOldValue() {
        String v = UUID.randomUUID().toString();
        ReduceUtils.merge(new Container(v), new Container(), Container::getStringValue, setter, null);
        verify(setter, never()).accept(any());
    }

    @Test
    public void testRetainOldValueMode() {
        String vOld = UUID.randomUUID().toString();
        String vNew = UUID.randomUUID().toString();
        ReduceUtils.merge(new Container(vOld), new Container(vNew), Container::getStringValue, setter, false);
        verify(setter, never()).accept(any());
    }

    @Test
    public void testNoChangeIfValueEquals() {
        String v = UUID.randomUUID().toString();
        ReduceUtils.merge(new Container(v), new Container(v), Container::getStringValue, setter, null);
        verify(setter, never()).accept(any());
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIfValueDifferents() {
        ReduceUtils.merge(
                new Container(UUID.randomUUID().toString()),
                new Container(UUID.randomUUID().toString()),
                Container::getStringValue, setter, null
        );
    }

    @Test
    public void testSetNewLongValue() {
        long v = System.currentTimeMillis();
        ReduceUtils.mergeLong(new Container(0), new Container(v), Container::getLongValue, longSetter, null);
        verify(longSetter, only()).accept(eq(v));
    }

    @Test
    public void testSetNewLongValueMode() {
        long vOld = System.nanoTime();
        long vNew = vOld + 17;
        ReduceUtils.mergeLong(new Container(vOld), new Container(vNew), Container::getLongValue, longSetter, true);
        verify(longSetter, only()).accept(eq(vNew));
    }

    @Test
    public void testRetainOldLongValue() {
        long v = System.currentTimeMillis();
        ReduceUtils.mergeLong(new Container(v), new Container(0), Container::getLongValue, longSetter, null);
        verify(longSetter, never()).accept(anyLong());
    }

    @Test
    public void testRetainOldLongValueMode() {
        long vOld = System.nanoTime();
        long vNew = vOld + 17;
        ReduceUtils.mergeLong(new Container(vOld), new Container(vNew), Container::getLongValue, longSetter, false);
        verify(longSetter, never()).accept(anyLong());
    }

    @Test
    public void testNoChangeIfLongValueEquals() {
        long v = System.currentTimeMillis();
        ReduceUtils.mergeLong(new Container(v), new Container(v), Container::getLongValue, longSetter, null);
        verify(longSetter, never()).accept(anyLong());
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionIfLongValueDifferents() {
        long v = System.currentTimeMillis();
        ReduceUtils.simpleMergeLong(new Container(v), new Container(v + 1), Container::getLongValue, longSetter);
    }

    public static class Container {

        private String stringValue;

        private long longValue;

        public Container() {
        }

        public Container(String value) {
            this.stringValue = value;
        }

        public Container(long value) {
            this.longValue = value;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(String stringValue) {
            this.stringValue = stringValue;
            //return this;
        }

        public long getLongValue() {
            return longValue;
        }

        public Container setLongValue(long longValue) {
            this.longValue = longValue;
            return this;
        }
    }
}
