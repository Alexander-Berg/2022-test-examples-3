package ru.yandex.market.tsum.clients.gencfg;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.request.netty.HttpClientConfig;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 28/07/2017
 */
@Ignore
@SuppressWarnings("magicNumber")
public class GenCfgClientTest {

    private GenCfgClient client;
    private static final String GROUP_NAME_PREFIX = "SAS_TSUM_LOCAL_TEST_GROUP";

    @Before
    public void setUp() throws Exception {
        HttpClientConfig config = new HttpClientConfig();
        config.setReadTimeout(10, TimeUnit.MINUTES);
        config.setConnectTimeout(10, TimeUnit.MINUTES);
        client = new GenCfgClient("https://api.gencfg.yandex-team.ru/");
    }

    @Test
    public void groupInfo() throws Exception {
        System.out.println(client.getGroupInfo("SAS_MENACE_BALANCER").get());
        System.out.println(client.getGroupInfo(client.getLastReleaseTag(), "ALL_DYNAMIC").get());
    }

    @Test
    public void getLastReleaseTag() throws Exception {
        String releaseTag = client.getLastReleaseTag();
        Assert.assertNotNull(releaseTag);
        System.out.println(releaseTag);
    }

    @Test
    public void getAllGroups() throws Exception {
        String lastReleaseTag = client.getLastReleaseTag();
        Set<String> groups = client.getAllGroups(lastReleaseTag);
        Assert.assertNotNull(groups);
        groups.forEach(System.out::println);
    }
}
