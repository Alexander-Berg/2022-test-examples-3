package ru.yandex.utils;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class ApplicationUtilsTest extends TestCase {

    public void testApplyParams() {
        Map<String, String> params = new HashMap<>();
        params.put("1", "value");
        assertEquals("Lalala value", ApplicationUtils.applyParams("Lalala $(1)", params));
    }

}
