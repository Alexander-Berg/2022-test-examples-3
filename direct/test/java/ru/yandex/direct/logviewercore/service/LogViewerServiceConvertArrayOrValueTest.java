package ru.yandex.direct.logviewercore.service;

import java.sql.Timestamp;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logviewercore.service.LogViewerService.convertArrayOrValue;

public class LogViewerServiceConvertArrayOrValueTest {
    @Test
    public void convertArrayOrValue_String() {
        String string = "abc";
        Object actualResult = convertArrayOrValue(string.getClass(), string);
        assertThat(actualResult).isEqualTo(string);
    }

    @Test
    public void convertArrayOrValue_Timestamp() {
        Timestamp timestamp = Timestamp.valueOf("2019-06-24 10:00:00");
        Object actualResult = convertArrayOrValue(timestamp.getClass(), timestamp.toString());
        assertThat(actualResult).isEqualTo(timestamp.toString());
    }

    @Test
    public void convertArrayOrValue_Integer() {
        int integer = 234;
        Object actualResult = convertArrayOrValue(int.class, Integer.toString(integer));
        assertThat(actualResult).isEqualTo(integer);
    }

    @Test
    public void convertArrayOrValue_Long() {
        long longValue = 234432L;
        Object actualResult = convertArrayOrValue(long.class, Long.toString(longValue));
        assertThat(actualResult).isEqualTo(longValue);
    }

    @Test
    public void convertArrayOrValue_Float() {
        float floatValue = 3.14F;
        Object actualResult = convertArrayOrValue(float.class, Float.toString(floatValue));
        assertThat(actualResult).isEqualTo(floatValue);
    }

    @Test
    public void convertArrayOrValue_Double() {
        double doubleValue = 2.71828;
        Object actualResult = convertArrayOrValue(double.class, Double.toString(doubleValue));
        assertThat(actualResult).isEqualTo(doubleValue);
    }

    @Test
    public void convertArrayOrValue_ArrayOfInteger() {
        int[] arrayOfInteger = {234};
        Object actualResult = convertArrayOrValue(arrayOfInteger.getClass(), Integer.toString(arrayOfInteger[0]));
        assertThat(actualResult).isEqualTo(arrayOfInteger[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void convertArrayOrValue_ArrayOfArrayOfInteger() {
        int[][] arrayOfArrayOfInteger = {{234}};
        Object actualResult = convertArrayOrValue(arrayOfArrayOfInteger.getClass(),
                Integer.toString(arrayOfArrayOfInteger[0][0]));
    }

    @Test
    public void convertArrayOrValue_Boolean() {
        boolean booleanValue = true;
        Object actualResult = convertArrayOrValue(boolean.class, Boolean.toString(booleanValue));
        assertThat(actualResult).isEqualTo(booleanValue);
    }
}
