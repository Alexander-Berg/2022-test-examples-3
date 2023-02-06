package ru.yandex.iex.proxy.xutils.replace;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.test.util.TestBase;

public class NameReplacerTest extends TestBase {
    @Test
    public void fewNamesTest() throws Exception {
        URL msgUrl = this.getClass().getResource("names.html");
        File msgFile = new File(msgUrl.toURI());
        String msg = fileToString(msgFile);
        String result = NameReplacer.getInstance().replace(msg);
        URL msgUrlResult = this.getClass().getResource("names_result.html");
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
