package ru.yandex.market.util;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 10.09.2018
 */
public class FormatUtilsTest {

    @Test
    public void testJsonTransformation() {
        TestJsonObj source = new TestJsonObj();
        source.setOk(true);
        source.setIvalue(3414);
        source.setData("Test data");

        final String jsonStr = FormatUtils.toJson(source);

        assertEquals("{\"ok\":true,\"ivalue\":3414,\"data\":\"Test data\"}", jsonStr);

        final TestJsonObj result = FormatUtils.fromJson(jsonStr, TestJsonObj.class);
        assertNotNull(result);
        assertEquals(source.isOk, result.isOk);
        assertEquals(source.getIvalue(), result.getIvalue());
        assertEquals(source.getData(), result.getData());

        final Map<String, String> mapResult = FormatUtils.fromJsonToStringMap(jsonStr);
        assertNotNull(mapResult);
        assertEquals(String.valueOf(source.isOk), mapResult.get("ok"));
        assertEquals(String.valueOf(source.getIvalue()), mapResult.get("ivalue"));
        assertEquals(source.getData(), mapResult.get("data"));
    }

    @JsonPropertyOrder ({"ok", "ivalue", "data"})
    public static class TestJsonObj {
        private boolean isOk;
        private int ivalue;
        private String data;

        public boolean isOk() {
            return isOk;
        }

        public void setOk(boolean ok) {
            isOk = ok;
        }

        public int getIvalue() {
            return ivalue;
        }

        public void setIvalue(int ivalue) {
            this.ivalue = ivalue;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
