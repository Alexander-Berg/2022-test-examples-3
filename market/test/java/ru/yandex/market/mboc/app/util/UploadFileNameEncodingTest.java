package ru.yandex.market.mboc.app.util;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.mboc.common.utils.UploadUtil;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
public class UploadFileNameEncodingTest {

    private final String raw;
    private final String expected;

    public UploadFileNameEncodingTest(String raw, String expected) {
        this.raw = raw;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "{index}: {0} filename should encode to {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
            {"j.xml", "j.xml"},
            {"Б.xml", "%D0%91.xml"},
            {" .xml", "%20.xml"}, // пробел не должен искепиться в +
            {"'.xml", "%27.xml"}, // кавычка обязательно должна искепиться для Chrome
            {"a+b.xml", "a%2Bb.xml"},
            {"/a.xml", "%2Fa.xml"},
            {"\\a.xml", "%5Ca.xml"},
            {"&.xml", "%26.xml"},
            {"?.xml", "%3F.xml"},
            {"=.xml", "%3D.xml"},
            {"\".xml", "%22.xml"},
            {"».xml", "%C2%BB.xml"},
            {").xml", "%29.xml"},
        });
    }

    @Test
    public void test() {
        String actual = UploadUtil.fileNameEncoding(raw);
        Assertions.assertThat(actual).isEqualTo(expected);
    }
}
