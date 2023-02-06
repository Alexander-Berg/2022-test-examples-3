package ru.yandex.market.mboc.common.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlSplitterTest {
    @Test
    public void testEmptyString() {
        assertThat(UrlSplitter.splitUrls("")).isEmpty();
    }

    @Test
    public void testSimpleWrongUrlIsJustUrl() {
        assertThat(UrlSplitter.splitUrls("something")).containsExactly("something");
    }

    @Test
    public void testMultipleWrongUrlsAreJustUrl() {
        assertThat(UrlSplitter.splitUrls("something,another")).containsExactly("something,another");
    }

    @Test
    public void testCommaSeparatedValuesAreOk() {
        assertThat(UrlSplitter.splitUrls("http://ya.ru/pic1.jpg,https://ya.ru/pic2.jpg"))
            .containsExactly("http://ya.ru/pic1.jpg", "https://ya.ru/pic2.jpg");
    }

    @Test
    public void testCommaSeparatedUrlIsOne() {
        assertThat(UrlSplitter.splitUrls("http://ya.ru/pic1.jpg,rgb(0,1,2)"))
            .containsExactly("http://ya.ru/pic1.jpg,rgb(0,1,2)");
    }

    @Test
    public void testRealLife() {
        assertThat(UrlSplitter.splitUrls("https://whirlpool-cdn.thron.com/delivery/public/thumbnail/whirlpool/pi" +
            "-602147a2-2cf6-4da5-97c9-890b318acafd/jsind9/std/1000x1000/dif-16t1-a-eu-%D0%9F%D0%BE%D1%81%D1%83%D0%B4" +
            "%D0%BE%D0%BC%D0%BE%D0%B5%D1%87%D0%BD%D1%8B%D0%B5-%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D1%8B-1" +
            ".jpg?fill=zoom&fillcolor=rgba:255,255,255&scalemode=product," +
            "https://whirlpool-cdn.thron.com/delivery/public/thumbnail/whirlpool/pi-602147a2-2cf6-4da5-97c9" +
            "-890b318acafd/jsind9/std/1000x1000" +
            "/dif-16t1-a-eu-%D0%9F%D0%BE%D1%81%D1%83%D0%B4%D0%BE%D0%BC%D0%BE%D0%B5%D1%87%D0%BD%D1%8B%D0%B5-%D0%BC%D0" +
            "%B0%D1%88%D0%B8%D0%BD%D1%8B-2.jpg?fill=zoom&fillcolor=rgba:255,255,255&scalemode=product"))
            .containsExactly("https://whirlpool-cdn.thron.com/delivery/public/thumbnail/whirlpool/pi-602147a2-2cf6" +
                    "-4da5-97c9-890b318acafd/jsind9/std/1000x1000/dif-16t1-a-eu-%D0%9F%D0%BE%D1%81%D1%83%D0%B4%D0%BE" +
                    "%D0%BC%D0%BE%D0%B5%D1%87%D0%BD%D1%8B%D0%B5-%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D1%8B-1" +
                    ".jpg?fill=zoom&fillcolor=rgba:255,255,255&scalemode=product",

                "https://whirlpool-cdn.thron.com/delivery/public/thumbnail/whirlpool/pi-602147a2-2cf6-4da5-97c9" +
                    "-890b318acafd/jsind9/std/1000x1000/dif-16t1-a-eu-%D0%9F%D0%BE%D1%81%D1%83%D0%B4%D0%BE%D0%BC%D0" +
                    "%BE%D0%B5%D1%87%D0%BD%D1%8B%D0%B5-%D0%BC%D0%B0%D1%88%D0%B8%D0%BD%D1%8B-2" +
                    ".jpg?fill=zoom&fillcolor=rgba:255,255,255&scalemode=product");
    }
}
