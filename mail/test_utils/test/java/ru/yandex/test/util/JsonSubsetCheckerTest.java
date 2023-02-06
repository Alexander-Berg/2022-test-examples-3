package ru.yandex.test.util;

import org.junit.Assert;
import org.junit.Test;

public class JsonSubsetCheckerTest extends TestBase {
    public JsonSubsetCheckerTest() {
        super(false, 0L);
    }

    @Test
    public void testEquality() {
        String str = "{\"key\":\"value\",\"key2\":[{\"key\":\"value\"}]}";
        Assert.assertNull(new JsonSubsetChecker(str).check(str));
    }

    @Test
    public void testNonEquality() {
        String str = "{\"key\":\"value\",\"key2\":[{\"key\":\"value\"}]}";
        String str2 = "{\"key\":\"value2\",\"key2\":[{\"key\":\"value\"}]}";
        Assert.assertEquals(
            "string mismatch:\n"
            + " {\n"
            + "     \"key\": \"value\n"
            + "+2\n"
            + " \",\n"
            + "     \"key2\": [\n"
            + "         {\n"
            + "             \"key\": \"value\"\n"
            + "         }\n"
            + "     ]\n"
            + " }",
            new JsonSubsetChecker(str).check(str2));
    }

    @Test
    public void testNonEqualityFiltered() {
        String str = "{\"key2\":[{\"key\":\"value\"}]}";
        String str2 =
            "{\"key\":\"value2\",\"key2\":["
            + "{\"key\":\"value\",\"key2\":\"value2\"}]}";
        Assert.assertNull(new JsonSubsetChecker(str).check(str2));
    }

    @Test
    public void testNonEqualityNotFiltered() {
        String str = "{\"key2\":[{\"key\":\"value\"}]}";
        String str2 = "{\"key\":\"value2\",\"key2\":[{\"key\":\"value2\"}]}";
        Assert.assertEquals(
            "string mismatch:\n"
            + " {\n"
            + "     \"key2\": [\n"
            + "         {\n"
            + "             \"key\": \"value\n"
            + "+2\n"
            + " \"\n"
            + "         }\n"
            + "     ]\n"
            + " }",
            new JsonSubsetChecker(str).check(str2));
    }
}

