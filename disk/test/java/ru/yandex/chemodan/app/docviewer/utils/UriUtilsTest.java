package ru.yandex.chemodan.app.docviewer.utils;

import java.net.URI;
import java.net.URL;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

public class UriUtilsTest {

    @Test
    public void testToUrl() throws Exception {
        URI uri = new URI("file:/root/file.bin");
        URL url = UriUtils.toUrl(uri);
        Assert.A.equals("file:/root/file.bin", url.toString());
    }

    @Test
    public void testConcatenate() throws Exception {
        URI uri = new URI("/root/file.bin");
        URI complex = new URI("http://host.org");

        Assert.A.equals("http://host.org/root/file.bin",
                new URI(complex.toString() + uri.toString()).toString());
    }

    @Deprecated
    @Test
    public void toUriSafe() {
        UriUtils.toUriSafe("http:// #`<>|'{}[]^@&*()+=_-\":;!$~");
    }

    @Test
    public void getHost() {
        URI uri = UriUtils.toUri("http://dep_geometry.pnzgu.ru/files/dep_geometry.pnzgu.ru/mnogomernaya_geometriya.pdf");
        Assert.some("dep_geometry.pnzgu.ru", UriUtils.getHostO(uri));
    }
}
