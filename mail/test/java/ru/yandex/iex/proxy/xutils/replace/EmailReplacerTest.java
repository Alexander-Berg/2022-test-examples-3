package ru.yandex.iex.proxy.xutils.replace;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class EmailReplacerTest extends TestBase {
    @Test
    public void fewEmailsTest() throws Exception {
        URL msgUrl = this.getClass().getResource("emails.html");
        File msgFile = new File(msgUrl.toURI());
        String msg = fileToString(msgFile);
        String result = EmailReplacer.getInstance().replace(msg);
        URL msgUrlResult = this.getClass().getResource("emails_result.html");
        File msgFileResult = new File(msgUrlResult.toURI());
        String msgResult = fileToString(msgFileResult);
        Assert.assertEquals(msgResult, result);
    }

    private String fileToString(final File file) throws IOException {
        return new String(
            java.nio.file.Files.readAllBytes(file.toPath()),
            StandardCharsets.UTF_8);
    }
}
