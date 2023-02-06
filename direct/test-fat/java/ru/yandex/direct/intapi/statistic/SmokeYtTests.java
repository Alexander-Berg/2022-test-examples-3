package ru.yandex.direct.intapi.statistic;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.intapi.fatconfiguration.FatIntApiTest;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.yt.ytclient.proxy.YtClient;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@FatIntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SmokeYtTests {

    @Autowired
    private YtProvider ytProvider;

    /**
     * Yt client via http proxy
     */
    private YtOperator ytOperator;
    /**
     * Yt client via rpc proxy
     */
    private YtClient ytClient;

    @Before
    public void before() {
        ytOperator = ytProvider.getOperator(YtCluster.YT_LOCAL);
        ytClient = ytProvider.getDynamicOperator(YtCluster.YT_LOCAL).getYtClient();
    }

    @Test
    public void testYtOperator() {
        // Проверяем работоспособность клиента Yt via http proxy
        List<YTreeStringNode> list = ytOperator.getYt().cypress().list(YPath.simple("//tmp"));
        assertThat(list, not(empty()));
    }

    @Test
    public void testYtClient() {
        // Проверяем работоспособность клиента Yt via rpc proxy
        YTreeNode listNode = ytClient.listNode("//tmp").join();
        assertThat(listNode.asList(), not(empty()));
    }
}
