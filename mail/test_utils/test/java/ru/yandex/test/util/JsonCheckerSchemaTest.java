package ru.yandex.test.util;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

public class JsonCheckerSchemaTest extends TestBase {
    private static final String JSON1 =
        "{\"message_id\":\"id1\",\"uid\":5598601}";
    private static final String JSON2 =
        "{\"message_id\":\"id1\",\"uid\":5598601,\"description\":\"hello\"}";
    private static final String JSON3 =
        "{\"message_id\":\"id1\"}";

    public JsonCheckerSchemaTest() throws Exception {
        super(
            false,
            0L,
            TestBase.loadResourceAsString(
                JsonSubsetCheckerTest.class,
                "schema1.json"));
    }

    private void doTestSchema1() throws Exception {
        Assert.assertNull(
            loadResourceAsJsonChecker("json1.json").check(JSON1));
        Assert.assertNull(
            loadResourceAsJsonChecker("json2.json").check(JSON2));
        YandexAssert.assertContains(
            "string mismatch",
            loadResourceAsJsonChecker("json1.json").check(JSON2));
        YandexAssert.assertContains(
            "failed to validate",
            loadResourceAsJsonChecker("json1.json").check(JSON3));
    }

    @Test
    public void testSchema1() throws Exception {
        doTestSchema1();
    }

    @Test
    public void testSchema2() throws Exception {
        doTestSchema1();
        setJsonSchemaForTest(loadResourceAsString("schema2.json"));
        YandexAssert.assertContains(
            "failed to validate",
            loadResourceAsJsonChecker("json1.json").check(JSON1));
        YandexAssert.assertContains(
            "string mismatch",
            loadResourceAsJsonChecker("json1.json").check(JSON2));
        Assert.assertNull(
            loadResourceAsJsonChecker("json2.json").check(JSON2));
    }

    @Test
    public void testDefitionsSchema() throws Exception {
        doTestSchema1();
        setJsonSchemaForTest(loadResourceAsString("definitions-schema.json"));
        JsonChecker checker = loadResourceAsJsonChecker("coords.json");
        String json = loadResourceAsString("coords.json");
        Assert.assertNull(checker.check(json));
        Pattern pattern = Pattern.compile("points\".*", Pattern.DOTALL);
        YandexAssert.assertContains(
            "failed to validate",
            checker.check(pattern.matcher(json).replaceAll("points\":[]}")));
        YandexAssert.assertContains(
            "failed to parse",
            checker.check(pattern.matcher(json).replaceAll("points\":[]}}")));
        YandexAssert.assertContains(
            "failed to parse",
            checker.check(pattern.matcher(json).replaceAll("points\":[]")));
        YandexAssert.assertContains(
            "failed to validate",
            checker.check(json.replace("131", "181")));
        YandexAssert.assertContains(
            "failed to validate",
            checker.check(json.replace(",\"altitude\": 8", "")));
        YandexAssert.assertContains(
            "failed to validate",
            checker.check(
                json.replace(
                    "\"altitude\": 8",
                    "\"altitude\": \"hello, world\"")));
    }

    @Test
    public void testFamilypayUidsSchema() throws Exception {
        setJsonSchemaForTest(loadResourceAsString("familypay-uids.json"));
        String[] ok = new String[] {
            "{\"id\":\"id1\",\"uid\":1}",
            "{\"id\":\"id1\",\"uid\":1,\"initiator_uid\":2}",
            "{\"id\":\"id1\",\"uid\":1,\"sponsor_uid\":3}"
        };
        for (String json: ok) {
            Assert.assertNull(
                new JsonChecker(json, getJsonSchemaForTest()).check(json));
        }
        String[] notOk = new String[] {
            "{\"uid\":1}",
            "{\"id\":\"id1\"}",
            "{\"id\":\"id1\",\"uid\":1,\"initiator_uid\":2,\"sponsor_uid\":3}"
        };
        for (String json: notOk) {
            YandexAssert.assertContains(
                "failed to validate",
                new JsonChecker(json, getJsonSchemaForTest()).check(json));
        }
    }

    @Test
    public void testOhioSchema() throws Exception {
        setJsonSchemaForTest(
            loadResourceAsString(
                "mail/ohio/ohio_backend/orders-schema-v2.json"));
        String json =
            loadResourceAsString(
                "mail/ohio/ohio_backend/orders-example-1.json");
        Assert.assertNull(
            new JsonChecker(json, getJsonSchemaForTest()).check(json));
    }
}

