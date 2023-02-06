package ru.yandex.parser.uri;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.util.BadRequestException;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class UriParserTest extends TestBase {
    @Test
    public void test() throws BadRequestException {
        UriParser parser = new UriParser(
            "/hello//%d0%BC%D0%b8р//?how=are/you%2F");
        Assert.assertEquals(parser.pathDecoder(),
            parser.pathParser().decoder());
        Assert.assertEquals(parser.queryDecoder(),
            parser.queryParser().decoder());
        Assert.assertNotEquals(parser.pathDecoder(),
            parser.fragmentDecoder());
        Assert.assertEquals(
            "/hello//%d0%BC%D0%b8р//",
            parser.path().toString());
        Assert.assertEquals(parser.path().toString(), parser.rawPath());
        Assert.assertEquals("how=are/you%2F", parser.query().toString());
        Assert.assertEquals(parser.query().toString(), parser.rawQuery());
        Assert.assertEquals("/hello//мир//?how=are/you/", parser.toString());
        Iterator<PctEncodedString> path = parser.pathParser().iterator();
        Assert.assertTrue(path.hasNext());
        Assert.assertEquals("hello", path.next().toString());
        Assert.assertEquals("мир", path.next().decode());
        try {
            path.remove();
            Assert.fail();
        } catch (UnsupportedOperationException e) {
            Assert.assertFalse(path.hasNext());
        }
        Iterator<QueryParameter> query = parser.queryParser().iterator();
        QueryParameter param = query.next();
        Assert.assertEquals("how", param.name());
        Assert.assertEquals("are/you/", param.value().decode());
        Assert.assertEquals(parser.queryDecoder(), param.value().decoder());
        Assert.assertFalse(query.hasNext());
        YandexAssert.assertEmpty(parser.fragment().decode());

        parser = new UriParser("пр%d0%b8%d0%b2ет/w%2Frld#%D1%84ragment");
        Assert.assertEquals("привет/w/rld#фragment", parser.toString());
        path = parser.pathParser().iterator();
        Assert.assertEquals("привет", path.next().decode());
        Assert.assertEquals("w/rld", path.next().decode());
        Assert.assertFalse(path.hasNext());
        Assert.assertFalse(parser.queryParser().iterator().hasNext());
        Assert.assertEquals("фragment", parser.fragment().decode());

        parser = new UriParser(new StringBuilder("/test/queriless%2F"));
        Assert.assertEquals("/test/queriless/", parser.toString());
        path = parser.pathParser().iterator();
        Assert.assertEquals("test", path.next().decode());
        Assert.assertEquals("queriless/", path.next().decode());
        Assert.assertFalse(path.hasNext());
        Assert.assertFalse(parser.queryParser().iterator().hasNext());
        YandexAssert.assertEmpty(parser.fragment().decode());

        parser = new UriParser("/?&&par%61m&name=value&#frag");
        Assert.assertEquals("/", parser.path().toString());
        Assert.assertEquals(parser.path().toString(), parser.rawPath());
        Assert.assertEquals(
            "&&par%61m&name=value&",
            parser.query().toString());
        Assert.assertEquals(parser.query().toString(), parser.rawQuery());
        Assert.assertEquals("frag", parser.fragment().toString());
        Assert.assertEquals(
            parser.fragment().toString(),
            parser.rawFragment());
        Assert.assertEquals("/?&&param&name=value&#frag", parser.toString());
        Assert.assertFalse(parser.pathParser().iterator().hasNext());
        query = parser.queryParser().iterator();
        param = query.next();
        Assert.assertEquals("par%61m", param.name());
        Assert.assertEquals("true", param.value().decode());
        param = query.next();
        Assert.assertEquals("name", param.name());
        Assert.assertEquals("value", param.value().toString());
        Assert.assertFalse(query.hasNext());

        path = new PathParser("").iterator();
        Assert.assertFalse(path.hasNext());

        parser = new UriParser("/single/");
        path = parser.pathParser().iterator();
        Assert.assertEquals("single", path.next().toString());
        Assert.assertFalse(path.hasNext());
    }

    @Test
    public void testEmpty() throws BadRequestException {
        YandexAssert.assertEmpty(
            new CgiParams(new UriParser("").queryParser()).entrySet());
    }
}

