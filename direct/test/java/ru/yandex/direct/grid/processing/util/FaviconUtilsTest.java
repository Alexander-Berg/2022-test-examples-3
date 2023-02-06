package ru.yandex.direct.grid.processing.util;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

public class FaviconUtilsTest {

    @Test
    public void testGetFaviconLink() {
        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(FaviconUtils.getFaviconLink("yandex.ru")).isEqualTo("https://favicon.yandex.net/favicon/v2/https://yandex.ru/?size=32&stub=1");
        soft.assertThat(FaviconUtils.getFaviconLink("YaNDeX.Ru/")).isEqualTo("https://favicon.yandex.net/favicon/v2/https://yandex.ru/?size=32&stub=1");
        soft.assertThat(FaviconUtils.getFaviconLink("ременьвподарок.рф")).isEqualTo("https://favicon.yandex.net/favicon/v2/https://xn--80adgca7ajjkchgj6n.xn--p1ai/?size=32&stub=1");
        soft.assertThat(FaviconUtils.getFaviconLink("Губка-Клининг.рф/")).isEqualTo("https://favicon.yandex.net/favicon/v2/https://xn----7sbchd3aamckxb2e.xn--p1ai/?size=32&stub=1");
        soft.assertAll();
    }
}
