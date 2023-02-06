package ru.yandex.direct.utils;

import java.lang.management.ManagementFactory;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.SystemUtils.cloudHostnameShortener;
import static ru.yandex.direct.utils.SystemUtils.parseDatacenter;

public class SystemUtilsTest {
    @Test
    public void getPid_works() {
        long pid = SystemUtils.getPid();
        assertThat(pid).isGreaterThanOrEqualTo(1);
        assertThat(ManagementFactory.getRuntimeMXBean().getName()).startsWith(pid + "@");
    }


    @Test
    public void parseHostDatacenter_works() {
        var soft = new SoftAssertions();
        soft.assertThat(parseDatacenter("ldki75pk2ymjn7r6.man.yp-c.yandex.net"))
                .isEqualTo("man");
        soft.assertThat(parseDatacenter("direct-java-web-sas-yp-19.sas.yp-c.yandex.net"))
                .isEqualTo("sas");
        soft.assertThat(parseDatacenter("direct.yandex.net"))
                .isNull();
        soft.assertAll();
    }

    @Test
    public void shortCloudHostnameTest() {
        var softly = new SoftAssertions();

        softly.assertThat(cloudHostnameShortener("man1-0375-19170.vm.search.yandex.net")).isEqualTo("man1-0375-19170");
        softly.assertThat(cloudHostnameShortener("sas1-5092-21360.vm.search.yandex.net")).isEqualTo("sas1-5092-21360");

        softly.assertThat(cloudHostnameShortener("vla1-0256-vla-direct-java-test-23065.gencfg-c.yandex.net"))
                .isEqualTo("vla1-0256-vla-direct-java-test-23065");
        softly.assertThat(cloudHostnameShortener("man1-9648-man-ppc-direct-java-jobs-28252.gencfg-c.yandex.net"))
                .isEqualTo("man1-9648-man-ppc-direct-java-jobs-28252");

        softly.assertThat(cloudHostnameShortener("yaiopuuoife4fupn.myt.yp-c.yandex.net"))
                .isEqualTo("yaiopuuoife4fupn.myt");
        softly.assertThat(cloudHostnameShortener("wmjxm7pk4aoyfduj.vla.yp-c.yandex.net"))
                .isEqualTo("wmjxm7pk4aoyfduj.vla");

        softly.assertThat(cloudHostnameShortener("ppcdev7.ppc.yandex.ru")).isEqualTo("ppcdev7.ppc.yandex.ru");

        softly.assertAll();
    }
}
