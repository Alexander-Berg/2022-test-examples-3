package ru.yandex.chemodan.app.docviewer.web.backend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.misc.test.Assert;

/**
 * @author ssytnik
 */
@RunWith(Parameterized.class)
public class SourceActionFilenameMimeParamTest {
    // filename examples
    private static final String EN_FN  = "a.txt";
    private static final String RU_FN  = "русский файл.txt";
    private static final String SYM_FN = "!@#$%^&*()_;-+x|.pdf";

    // user-agent examples
    private static final String CURL   = "curl/7.22.0 (x86_64-pc-linux-gnu) libcurl/7.22.0 OpenSSL/1.0.1 zlib/1.2.3.4 libidn/1.23 librtmp/2.3";
    private static final String IE9    = "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)";
    private static final String CHROME = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.13 (KHTML, like Gecko) Chrome/0.2.149.29 Safari/525.13";
    private static final String OPERA  = "Opera/9.25 (Windows NT 6.0; U; en)";
    private static final String IE7    = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0)";
    private static final String SAFARI = "Mozilla/5.0 (Macintosh; U; PPC Mac OS X; en) AppleWebKit/125.2 (KHTML, like Gecko) Safari/125.8";


    @Parameters
    public static ListF<Object[]> data() {
        return Cf.<Object[]>list()
                .plus1(entry(EN_FN , CURL  , "filename*=UTF-8''a.txt"))
                .plus1(entry(EN_FN , IE9   , "filename*=UTF-8''a.txt"))
                .plus1(entry(EN_FN , CHROME, "filename*=UTF-8''a.txt"))
                .plus1(entry(EN_FN , OPERA , "filename*=UTF-8''a.txt"))
                .plus1(entry(EN_FN , IE7   , "filename=a.txt"))
                .plus1(entry(EN_FN , SAFARI, "filename=a.txt"))

                .plus1(entry(RU_FN , CURL  , "filename*=UTF-8''%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9%20%D1%84%D0%B0%D0%B9%D0%BB.txt"))
                .plus1(entry(RU_FN , IE9   , "filename*=UTF-8''%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9%20%D1%84%D0%B0%D0%B9%D0%BB.txt"))
                .plus1(entry(RU_FN , CHROME, "filename*=UTF-8''%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9%20%D1%84%D0%B0%D0%B9%D0%BB.txt"))
                .plus1(entry(RU_FN , OPERA , "filename*=UTF-8''%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9%20%D1%84%D0%B0%D0%B9%D0%BB.txt"))
                .plus1(entry(RU_FN , IE7   , "filename=%D1%80%D1%83%D1%81%D1%81%D0%BA%D0%B8%D0%B9 %D1%84%D0%B0%D0%B9%D0%BB.txt"))
                .plus1(entry(RU_FN , SAFARI, "filename=русский файл.txt"))

                .plus1(entry(SYM_FN, CURL  , "filename*=UTF-8''%21%40%23%24%25%5E%26%2A%28%29_-%2Bx%7C.pdf"))
                .plus1(entry(SYM_FN, IE9   , "filename*=UTF-8''%21%40%23%24%25%5E%26%2A%28%29_-%2Bx%7C.pdf"))
                .plus1(entry(SYM_FN, CHROME, "filename*=UTF-8''%21%40%23%24%25%5E%26%2A%28%29_-%2Bx%7C.pdf"))
                .plus1(entry(SYM_FN, OPERA , "filename*=UTF-8''%21%40%23%24%25%5E%26%2A%28%29_-%2Bx%7C.pdf"))
                .plus1(entry(SYM_FN, IE7   , "filename=%21%40%23%24%%5E%26%2A%28%29_-%2Bx%7C.pdf"))
                .plus1(entry(SYM_FN, SAFARI, "filename=!@#$%^&*()_-+x|.pdf"))

                ;
    }

    private static Object[] entry(String filename, String userAgent, String expectedMimeParam) {
        return new Object[] {filename, userAgent, expectedMimeParam};
    }

    private String filename;
    private String userAgent;
    private String expectedMimeParam;

    public SourceActionFilenameMimeParamTest(String filename, String userAgent, String expectedMimeParam) {
        this.filename = filename;
        this.userAgent = userAgent;
        this.expectedMimeParam = expectedMimeParam;
    }

    @Test
    public void getFilenameMimeParam() {
        Assert.equals(expectedMimeParam, SourceAction.getFilenameMimeParam(filename, userAgent));
    }
}
