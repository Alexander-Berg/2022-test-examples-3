package ru.yandex.common.framework.http;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Date: Jun 7, 2009
 * Time: 12:40:37 AM
 *
 * @author Nikolay Malevanny nmalevanny@yandex-team.ru
 */
public class HttpServerTest extends AbstractHttpTestCase {
    protected String[] getLocalConfigLocations() {
        return new String[]{
                "classpath:http/test-http-servantlet-config.xml"
        };
    }

    public void testSendNameAndGetName() throws Throwable {
        final String result = loadPage("fake", new BasicNameValuePair("name", "Вася"));
        System.out.println("result = " + result);
        assertTrue(result.contains("string"));
    }

    // TODO this test fails because FakeServantlet produces non-JSONable data. Need another test servantlet
    public void _testSendNameAndGetNameJson() throws Throwable {
        final String result = loadPage("fake.json", new BasicNameValuePair("name", "Вася"));
        System.out.println("result = " + result);
        assertTrue(result.contains("string"));
    }

    // TODO this test fails because FakeServantlet produces non-JSONable data. Need another test servantlet
    public void _testSendNameAndGetNameJsonByParam() throws Throwable {
        final String result = loadPage("fake",
                new BasicNameValuePair("name", "Вася"),
                new BasicNameValuePair("out", "json")
        );
        System.out.println("result = " + result);
        assertTrue(result.contains("string"));
    }

    public void testSendNameIntoTwoServantlentsAndGetName() throws Throwable {
        final String result = loadPage("fake_fake", new BasicNameValuePair("name", "Вася"));
        System.out.println("result = " + result);
        assertTrue(result.contains("string"));
    }

    public void testRedirTo() throws Throwable {
        final String result = loadPage("fakeRedir", new BasicNameValuePair("name", "Вася"));
        System.out.println("result = " + result);
        assertTrue(result.contains("redir-to"));
        assertTrue(result.contains("test.xml"));
    }

    public void testSend2NameAndGetName() throws Throwable {
        final String result = loadPage("fake",
                new BasicNameValuePair("name", "Вася1"),
                new BasicNameValuePair("name", "Вася2")
        );

        System.out.println("result = " + result);
//        assertTrue(result.contains("Вася1"));  // todo fix
        assertTrue(result.contains("string"));
        assertTrue(result.contains("collection"));
    }

    public void testPing() throws Exception {
        final String result = loadPage("ping").trim();
        System.out.println("result = " + result);
        assertEquals("0;OK", result);
    }

    public void testCookie() throws Exception {
        final String result = loadPage("fake", "c1=bla-bla; c2=cla-cla");
        System.out.println("result = " + result);
        assertTrue(result.contains("bla-bla"));
    }

    public void testDirect() throws Exception {
        String result = loadPage("fakeDirect");
        System.out.println("result = " + result);
        assertEquals("fakeDirect", result);
    }

    public void testExceptionDirect() throws Exception {
        loadPage("throwExceptionDirect");
    }

    // TODO this test fails because DifferentDataServantlet produces non-JSONable data. Need another test servantlet
    public void _testJsonOutput() throws Exception {
        final String result = loadPage("differentData.json");
        final JSONObject json;
        try {
            json = new JSONObject(result);
        } catch (JSONException e) {
            throw new AssertionError("Incorrect JSON output");
        }
        System.out.println(json);
    }

    @Override
    protected boolean isLocal() {
        return false;
    }
}
