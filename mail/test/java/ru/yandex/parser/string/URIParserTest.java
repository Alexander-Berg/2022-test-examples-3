package ru.yandex.parser.string;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class URIParserTest extends TestBase {
    private static void testEquals(final String uri) throws Exception {
        URI expected = new URI(uri).parseServerAuthority();
        URI actual = URIParser.INSTANCE.apply(uri);
        assertEquals(expected, actual);
    }

    private static void assertEquals(final URI expected, final URI actual) {
        Assert.assertEquals(expected.toASCIIString(), actual.toASCIIString());
        Assert.assertEquals(expected.getScheme(), actual.getScheme());
        Assert.assertEquals(
            expected.getRawUserInfo(),
            actual.getRawUserInfo());
        Assert.assertEquals(expected.getHost(), actual.getHost());
        Assert.assertEquals(expected.getPort(), actual.getPort());
        Assert.assertEquals(expected.getRawPath(), actual.getRawPath());
        Assert.assertEquals(expected.getRawQuery(), actual.getRawQuery());
        Assert.assertEquals(
            expected.getRawFragment(),
            actual.getRawFragment());
        Assert.assertEquals(
            expected.getRawSchemeSpecificPart(),
            actual.getRawSchemeSpecificPart());
        Assert.assertEquals(
            expected.getRawAuthority(),
            actual.getRawAuthority());
    }

    @Test
    public void test() throws Exception {
        testEquals("https://127.0.0.1");
        testEquals("https://dpotapov@127.0.0.1/query%20here?a=b+c#frag");
        testEquals("https://[ffff::127.0.0.1]");
        testEquals("https://[a:b:c:d:e:f:127.0.0.1]");
        testEquals("http://[fe80:3438:7667:c77::ce27%18]");
        testEquals("https://[::1]:883/#frag");
        testEquals("yandex.ru");
        testEquals("urn:isbn:096139210x");
        testEquals("news:comp.lang.java");
        testEquals("mailto:java-net@java.sun.com");
        testEquals("sample/a/index.html#28");
        testEquals("../../demo/b/index.html");
        testEquals("file:///~/calendar");
        assertEquals(
            new URI("https://xn--kxae4bafwg.xn--pxaix.gr/")
                .parseServerAuthority(),
            URIParser.INSTANCE.apply("https://ουτοπία.δπθ.gr/"));
    }
}

