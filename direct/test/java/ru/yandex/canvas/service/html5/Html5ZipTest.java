package ru.yandex.canvas.service.html5;

import java.io.IOException;
import java.util.Base64;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
public class Html5ZipTest {

    public static final byte[] WIN32_ARCHIVE = Base64.getDecoder().decode("UEsDBBQAAAAAAFxmV00ctcttDQAAAA0AAAAcAAAAja6i66kg4q"
            + "Wq4eKuouupIKSuquOspa3iLnR4dPLl8fIg7eAg4ujt5OVQSwECFAAUAAAAAABcZldNHLXLbQ0AAAANAAAAHAAAAAAAAAAAACAAAAAAAAA"
            + "Aja6i66kg4qWq4eKuouupIKSuquOspa3iLnR4dFBLBQYAAAAAAQABAEoAAABHAAAAAAA=");

    public static final byte[] ZIP_ARCHIVE =
            Base64.getDecoder().decode("UEsDBAoAAAAAABRhV00AAAAAAAAAAAAAAAAJABwAdGVzdF9hcmMvVVQJAAMX5c5bgufOW3V4CwAB"
                    + "BIuIAHEEvjBiI1BLAwQKAAAAAADyYFdNkwbXMgEAAAABAAAAHgAcAHRlc3RfYXJjLyVFOCVFQyVGRl8lRTJfMTI1MS5qc1VUCQAD2OTOW"
                    + "7Hkzlt1eAsAAQSLiABxBL4wYiMKUEsDBBQAAAAIAHtgV0258XmcCAEAAJwBAAATABwAdGVzdF9hcmMvaW5kZXguaHRtbFVUCQAD+ePOW/"
                    + "njzlt1eAsAAQSLiABxBL4wYiNlUV1LwzAUfd+viPFlA0k2xBdNCk4EhemD6PO4Ta5L1jYtzZ1b/fWmrWwT85JDuOeej6gLWxvqGmSOqjK"
                    + "bqOFSDsFmE/Z7VDStb4j1c5oTHkhu4QvGV85iazR3RE28lRL20ETRQbB4EAFJWiCQpc8l2GgLsY08U3KknilUSMACVGk/WBH9N3Jm6kAY"
                    + "SPO9t+T09Xx+5dBvHPWQj2QlT1ZVXtvubCcwbzU3pTfFYg0tAmeuxU/NLzkjaDeYVq/zEkKRLBkIKVGydgTwr4AsdbWrkiWRuI8l9nDZP"
                    + "dvpH5GZ6FWYZmMJT+8vq5slhIDtfeN75kM//PG2et1V08Xs7tSGkkOClGn4ix9QSwMECgAAAAAAFGFXTeAJoQgcAAAAHAAAABoAHAB0ZX"
                    + "N0X2FyYy/QuNC80Y9f0LJfdXRmOC5qc1VUCQADF+XOWxflzlt1eAsAAQSLiABxBL4wYiNhbGVydCgiSGVsbG8gdXRmOCB3b3JsZCEiKTs"
                    + "KUEsBAh4DCgAAAAAAFGFXTQAAAAAAAAAAAAAAAAkAGAAAAAAAAAAQAO1BAAAAAHRlc3RfYXJjL1VUBQADF+XOW3V4CwABBIuIAHEEvjBi"
                    + "I1BLAQIeAwoAAAAAAPJgV02TBtcyAQAAAAEAAAAeABgAAAAAAAEAAACkgUMAAAB0ZXN0X2FyYy8lRTglRUMlRkZfJUUyXzEyNTEuanNVV"
                    + "AUAA9jkzlt1eAsAAQSLiABxBL4wYiNQSwECHgMUAAAACAB7YFdNufF5nAgBAACcAQAAEwAYAAAAAAABAAAApIGcAAAAdGVzdF9hcmMvaW"
                    + "5kZXguaHRtbFVUBQAD+ePOW3V4CwABBIuIAHEEvjBiI1BLAQIeAwoAAAAAABRhV03gCaEIHAAAABwAAAAaABgAAAAAAAEAAACkgfEBAAB"
                    + "0ZXN0X2FyYy/QuNC80Y9f0LJfdXRmOC5qc1VUBQADF+XOW3V4CwABBIuIAHEEvjBiI1BLBQYAAAAABAAEAGwBAABhAgAAAAA=");

    public static final String INDEX_HTML = "<!doctype html>\n"
            + "<html><head>\n"
            + "        <script type=\"text/javascript\" src=\"https://awaps.yandex.net/data/lib/adsdk.js\"></script>\n"
            + "        <meta name=\"ad.size\" content=\"width=300,height=300\">\n"
            + "    </head>\n"
            + "    <body>\n"
            + "        <a id=\"click1_area\" href=\"#\" target=\"_blank\"><canvas></canvas></a>\n"
            + "        <script>document.getElementById(\"click1_area\").href = yandexHTML5BannerApi.getClickURLNum(1);</script>\n"
            + "</body></html>\n";

    public static final byte[] ZIP_ARCHIVE_WITH_UTF8 =
            Base64.getDecoder().decode("UEsDBAoAAAAAAMKLd00AAAAAAAAAAAAAAAAJABwAdGVzdF9hcmMvVVQJAAPsDvhbbw/4W3V4CwABBIuIAHEEv"
                    + "jBiI1BLAwQKAAIAAADCi3dNP3cAbRcAAAAXAAAAEwAcAHRlc3RfYXJjL2luZGV4Lmh0bWxVVAkAA+wO+FtKD/hbdXgLAAEEi4"
                    + "gAcQS+MGIjbGliLtCQ0L3QuNC80LDRhtC40Y8xMgpQSwECHgMKAAAAAADCi3dNAAAAAAAAAAAAAAAACQAYAAAAAAAAABAA7UE"
                    + "AAAAAdGVzdF9hcmMvVVQFAAPsDvhbdXgLAAEEi4gAcQS+MGIjUEsBAh4DCgACAAAAwot3TT93AG0XAAAAFwAAABMAGAAAAAAA"
                    + "AQAAAKSBQwAAAHRlc3RfYXJjL2luZGV4Lmh0bWxVVAUAA+wO+Ft1eAsAAQSLiABxBL4wYiNQSwUGAAAAAAIAAgCoAAAApwAAAAAA");

    public static final String UTF8_CONTENT = "alert(\"Hello utf8 world!\");\n";

    public static final byte[] CP1251_CONTENT = Base64.getDecoder().decode("8uXx8iDt4CDi6O3k5Q==");

    @Test
    public void decompressTest() throws IOException {
        Html5Zip zip = new Html5Zip(ZIP_ARCHIVE);

        assertThat("Got valid filelist", zip.files(), Matchers.containsInAnyOrder(
                "test_arc/%E8%EC%FF_%E2_1251.js",
                "test_arc/index.html",
                "test_arc/имя_в_utf8.js"));

        assertEquals("Index.html contains expected data", new String(zip.getFileContent("test_arc/index.html")), INDEX_HTML);
        assertEquals("имя_в_utf8.js contains expected data", new String(zip.getFileContent("test_arc/имя_в_utf8.js")), UTF8_CONTENT);
    }

    @Test
    public void decompressWin32Test() throws IOException {
        Html5Zip zip = new Html5Zip(WIN32_ARCHIVE);

        assertThat("Got valid filelist", zip.files(), Matchers.containsInAnyOrder(
                "Новый текстовый документ.txt"));

        assertArrayEquals("Index.html contains expected data", zip.getFileContent("Новый текстовый документ.txt"), CP1251_CONTENT);
    }

    @Test
    public void decompressUtf8Test() throws IOException {
        Html5Zip zip = new Html5Zip(ZIP_ARCHIVE_WITH_UTF8);

        assertThat("Got valid filelist", zip.files(), Matchers.containsInAnyOrder(
                "test_arc/index.html"));

        assertEquals("Index.html contains expected data", zip.getFileAsUtf8String("test_arc/index.html"), "lib.Анимация12\n");
    }

    @Test
    public void compressTest() throws IOException {

        byte[] packed = Html5Zip.builder()
                .addFile("index.html", "Hello world!".getBytes())
                .build().toArchive();

        Html5Zip unzipped = new Html5Zip(packed);

        assertThat("Got valid filelist", unzipped.files(), Matchers.containsInAnyOrder("index.html"));
        assertEquals("Index.html contains expected data", new String(unzipped.getFileContent("index.html")), "Hello world!");
    }


}
