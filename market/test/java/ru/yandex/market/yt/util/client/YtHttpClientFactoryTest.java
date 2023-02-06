package ru.yandex.market.yt.util.client;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;

/**
 * @author amaslak
 */
public class YtHttpClientFactoryTest {

    public static final String TEST_PROXY = "test-proxy";

    public static final String TEST_TOKEN = "test-token";

    @Test
    public void testConstructor() throws Exception {
        YtHttpClientFactory f = new YtHttpClientFactory(TEST_PROXY, TEST_TOKEN);

        Assert.assertNull(f.getJarsDir());
        Assert.assertNull(f.getTmpDir());
    }

    @Test
    public void testConstructorWithDir() throws Exception {
        YtHttpClientFactory f = new YtHttpClientFactory(TEST_PROXY, TEST_TOKEN, "//tmp/YtHttpClientFactoryTest");

        Assert.assertEquals(YPath.simple("//tmp/YtHttpClientFactoryTest/java-yt-wrapper/jars"), f.getJarsDir());
        Assert.assertEquals(YPath.simple("//tmp/YtHttpClientFactoryTest/java-yt-wrapper/tmp"), f.getTmpDir());
    }

}
