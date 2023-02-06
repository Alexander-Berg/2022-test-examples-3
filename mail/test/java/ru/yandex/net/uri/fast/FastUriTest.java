package ru.yandex.net.uri.fast;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class FastUriTest extends TestBase {
    public FastUriTest() {
        super(false, 0L);
    }

    @Test
    public void testDescribe() throws Exception {
        Assert.assertEquals(
            "(scheme=http,ssp=//dpotapov@yandex.ru:8080"
            + "/path/here?param1=value1&p%20ram=%D1%91,"
            + "user=dpotapov,host=yandex.ru,port=8080,"
            + "authority=dpotapov@yandex.ru:8080,path=/path/here,"
            + "query=param1=value1&p%20ram=%D1%91,fragment=frag%23ment)",
            new FastUriParser(
                "http://dpotapov@yandex.ru:8080/path/here?param1=value1"
                + "&p ram=ё#frag#ment")
                .parse()
                .describe());
        Assert.assertEquals(
            "(ssp=,path=,fragment=frag)",
            new FastUriParser("#frag").parse().describe());
    }

    private static String normalize(final String uri) throws Exception {
        return new FastUriParser(uri).parse().normalize().describe();
    }

    private static String resolve(final String base, final String child)
        throws Exception
    {
        FastUri baseUri = new FastUriParser(base).parse();
        FastUri childUri = new FastUriParser(child).parse();
        return baseUri.resolve(childUri).describe();
    }

    @Test
    public void testNormalize() throws Exception {
        // /../ shouldn't be normalized, uri won't be changed
        // ssp won't be nullified
        Assert.assertEquals(
            "(scheme=scheme,ssp=//host/../path,host=host,authority=host,"
            + "path=/../path)",
            normalize("scheme://host/../path"));

        Assert.assertEquals(
            "(ssp=a/c/d,path=a/c/d)",
            normalize("a/./b/../c/d"));
        Assert.assertEquals(
            "(scheme=http,ssp=//host/some/pa/th/there?query,host=host,"
            + "authority=host,path=/some/pa/th/there,query=query,"
            + "fragment=frag)",
            normalize("http://host/some/./pa///th/here/../there?query#frag"));
        Assert.assertEquals("(ssp=b,path=b)", normalize("a/../b"));
        Assert.assertEquals("(ssp=/a,path=/a)", normalize("/./a"));
        Assert.assertEquals("(ssp=/../a,path=/../a)", normalize("/../a"));
        // DEVIATION: java.net.URI will normalize this to ./b:c
        Assert.assertEquals("(ssp=b:c,path=b:c)", normalize("a/../b:c"));
    }

    @Test
    public void testResolve() throws Exception {
        String base = "http://basehost/some/path/he;re?query";
        Assert.assertEquals(
            "(scheme=scheme,ssp=host)",
            resolve(base, "scheme:host"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath,host=basehost,"
            + "authority=basehost,path=/some/path/childpath)",
            resolve(base, "childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath,host=basehost,"
            + "authority=basehost,path=/some/path/childpath)",
            resolve(base, "./childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath/,host=basehost,"
            + "authority=basehost,path=/some/path/childpath/)",
            resolve(base, "childpath/"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/childpath,host=basehost,"
            + "authority=basehost,path=/childpath)",
            resolve(base, "/childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//childhost,host=childhost,authority=childhost,"
            + "path=)",
            resolve(base, "//childhost"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/?childquery,host=basehost,"
            + "authority=basehost,path=/some/path/,query=childquery)",
            resolve(base, "?childquery"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath?childquery,"
            + "host=basehost,authority=basehost,path=/some/path/childpath,"
            + "query=childquery)",
            resolve(base, "childpath?childquery"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/he;re?query,host=basehost,"
            + "authority=basehost,path=/some/path/he;re,query=query,"
            + "fragment=fragment)",
            resolve(base, "#fragment"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath,host=basehost,"
            + "authority=basehost,path=/some/path/childpath,"
            + "fragment=fragment)",
            resolve(base, "childpath#fragment"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath?childquery,"
            + "host=basehost,authority=basehost,"
            + "path=/some/path/childpath,query=childquery,fragment=fragment)",
            resolve(base, "childpath?childquery#fragment"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/,host=basehost,"
            + "authority=basehost,path=/some/path/)",
            resolve(base, "."));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/,host=basehost,"
            + "authority=basehost,path=/some/path/)",
            resolve(base, "./"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/,host=basehost,"
            + "authority=basehost,path=/some/)",
            resolve(base, ".."));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/,host=basehost,"
            + "authority=basehost,path=/some/)",
            resolve(base, "../"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/,host=basehost,authority=basehost,"
            + "path=/)",
            resolve(base, "../.."));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/,host=basehost,authority=basehost,"
            + "path=/)",
            resolve(base, "../../"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/childpath,host=basehost,"
            + "authority=basehost,path=/some/childpath)",
            resolve(base, "../childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/childpath,host=basehost,"
            + "authority=basehost,path=/childpath)",
            resolve(base, "../../childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/../childpath,host=basehost,"
            + "authority=basehost,path=/../childpath)",
            resolve(base, "../../../childpath"));
        // DEVIATION: normalize child path
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/childpath,host=basehost,"
            + "authority=basehost,path=/childpath)",
            resolve(base, "/./childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/../childpath,host=basehost,"
            + "authority=basehost,path=/../childpath)",
            resolve(base, "/../childpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath/,host=basehost,"
            + "authority=basehost,path=/some/path/childpath/)",
            resolve(base, "./childpath/."));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/childpath/subpath,"
            + "host=basehost,authority=basehost,"
            + "path=/some/path/childpath/subpath)",
            resolve(base, "childpath/./subpath"));
        Assert.assertEquals(
            "(scheme=http,ssp=//basehost/some/path/subpath,host=basehost,"
            + "authority=basehost,path=/some/path/subpath)",
            resolve(base, "childpath/../subpath"));

        Assert.assertEquals(
            "(scheme=https,ssp=//ya.ru/path,host=ya.ru,authority=ya.ru,"
            + "path=/path)",
            resolve("https://ya.ru", "/path"));
    }

    private void testHashCodeAndEquals(final String str) throws Exception {
        String normalized = new FastUriParser(str).parse().toString();
        int expected = normalized.hashCode();
        FastUri uri = new FastUriParser(str).parse();
        FastUri normalizedUri = new FastUriParser(normalized).parse();
        Assert.assertEquals(expected, uri.hashCode());
        Assert.assertEquals(uri, normalizedUri);

        // will initialize uri.ssp, so hash code calculation will be easier
        Assert.assertEquals(normalized, uri.toString());
        Assert.assertEquals(uri, normalizedUri);
        Assert.assertEquals(normalized, normalized.toString());
        Assert.assertEquals(uri, normalizedUri);

        Assert.assertEquals(expected, uri.hashCode());
        Assert.assertEquals(
            expected,
            new FastUriParser(normalized).parse().hashCode());

        // Replace single character with / and check that new uri not equals
        // with our uri
        for (int i = 0; i < normalized.length(); ++i) {
            if (normalized.charAt(i) != '/') {
                String newString =
                    normalized.substring(0, i) + '/'
                    + normalized.substring(i + 1);
                try {
                    FastUri newUri = new FastUriParser(newString).parse();
                    Assert.assertNotEquals(
                        newUri,
                        uri);
                    Assert.assertNotEquals(
                        newUri,
                        new FastUriParser(normalized).parse());
                    Assert.assertNotEquals(newUri.toString(), normalized);
                    Assert.assertNotEquals(
                        newUri,
                        uri);
                } catch (Exception e) {
                }
            }
        }

        for (int i = 0; i < normalized.length(); ++i) {
            if (normalized.charAt(i) != '8') {
                String newString =
                    normalized.substring(0, i) + '8'
                    + normalized.substring(i + 1);
                try {
                    FastUri newUri = new FastUriParser(newString).parse();
                    Assert.assertNotEquals(
                        newUri,
                        uri);
                    Assert.assertNotEquals(
                        newUri,
                        new FastUriParser(normalized).parse());
                    Assert.assertNotEquals(newUri.toString(), normalized);
                    Assert.assertEquals(newString, newUri.toString());
                    Assert.assertNotEquals(
                        newUri,
                        uri);
                } catch (Exception e) {
                }
            }
        }
    }

    @Test
    public void testHashCode() throws Exception {
        testHashCodeAndEquals(
            "http://dpotapov@yandex.ru:8080/path/here?param1=value1"
            + "&p ram=ё#frag#ment");
        testHashCodeAndEquals("/../a");
        testHashCodeAndEquals("https://ya.ru");
        testHashCodeAndEquals("https://ya.ru:27273/?");
        testHashCodeAndEquals("http://ya.ru:444/?");
        testHashCodeAndEquals("http://ya.ru:88/?");
        testHashCodeAndEquals("http://ya.ru:4/?");
        testHashCodeAndEquals("mailto:dpotapov@yandex.ru");
    }

    @Test
    public void testToString() throws Exception {
        FastUri uri = new FastUriParser("http://тест.ru").parse();
        uri.schemeSpecificPart(null);
        Assert.assertEquals("http://xn--e1aybc.ru", uri.toString());
        Assert.assertEquals("http://тест.ru", uri.toString(true));
        Assert.assertEquals("//xn--e1aybc.ru", uri.schemeSpecificPart());
        Assert.assertEquals("//тест.ru", uri.schemeSpecificPart(true));
    }

    @Test
    public void testMailtoToString() throws Exception {
        String mailto = "mailto:dpotapov@ya.ru";
        FastUri uri = new FastUriParser(mailto).parse();
        Assert.assertEquals(mailto, uri.toString());
        Assert.assertEquals(mailto, uri.toString(true));
    }

    @Test
    public void testNumericLabel() throws Exception {
        Assert.assertEquals(
            "(scheme=https,"
            + "ssp=//30488.redirect.appmetrica.yandex.com/,"
            + "host=30488.redirect.appmetrica.yandex.com,"
            + "authority=30488.redirect.appmetrica.yandex.com,"
            + "path=/)",
            new FastUriParser("https://30488.redirect.appmetrica.yandex.com/")
                .parse()
                .describe());
    }
}

