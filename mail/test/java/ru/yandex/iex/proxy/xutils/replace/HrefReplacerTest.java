package ru.yandex.iex.proxy.xutils.replace;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class HrefReplacerTest extends TestBase {
    @Test
    public void noHrefsTest() throws Exception {
        URL msgUrl = this.getClass().getResource("no_hrefs.html");
        File msgFile = new File(msgUrl.toURI());
        String msg = fileToString(msgFile);
        Assert.assertEquals(msg, HrefReplacer.getInstance().replace(msg));
    }

    @Test
    public void hrefsTest() throws Exception {
        URL msgUrl = this.getClass().getResource("hrefs.html");
        File msgFile = new File(msgUrl.toURI());
        String msg = fileToString(msgFile);
        String result = HrefReplacer.getInstance().replace(msg);
        String testValue = replaceRandomValue(result);
        URL msgUrlResult = this.getClass().getResource("hrefs_result.html");
        File msgFileResult = new File(msgUrlResult.toURI());
        String msgResult = fileToString(msgFileResult);
        Assert.assertEquals(msgResult, testValue);
    }

    @Test
    public void hrefsWithKeywordsTest() throws Exception {
        URL msgUrl = this.getClass().getResource("hrefs_keywords.html");
        File msgFile = new File(msgUrl.toURI());
        String msg = fileToString(msgFile);
        String result = HrefReplacer.getInstance().replace(msg);
        String testValue = replaceRandomValue(result);
        URL msgUrlResult = this.getClass()
            .getResource("hrefs_keywords_result.html");
        File msgFileResult = new File(msgUrlResult.toURI());
        String msgResult = fileToString(msgFileResult);
        Assert.assertEquals(msgResult, testValue);
    }

    private String fileToString(final File file) throws IOException {
        return new String(
            java.nio.file.Files.readAllBytes(
                file.toPath()),
                StandardCharsets.UTF_8);
    }

    private String replaceRandomValue(final String msg) {
        return msg.replaceAll(
            "/random\\?id=\\d{1,4}\"",
            "/random?id=<random value>\"");
    }
}
