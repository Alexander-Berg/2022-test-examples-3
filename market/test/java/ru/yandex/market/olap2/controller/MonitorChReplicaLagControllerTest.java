package ru.yandex.market.olap2.controller;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.olap2.dao.ClickhouseDao;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MonitorChReplicaLagControllerTest {
    private MonitorChReplicaLagController controller;

    @Mock
    private ClickhouseDao dao;

    @Before
    public void init() {
        controller = new MonitorChReplicaLagController(dao);
    }

    @Test
    public void testLag() {
        Set<String> lags = new HashSet<>();
        lags.add("myhost:1005000seconds");
        when(dao.getOutdatedReplicas()).thenReturn(lags);
        Assert.assertThat(controller.replicasLag().getBody(), is("2;myhost:1005000seconds"));
    }

    @Test
    public void testNoLag() {
        Set<String> lags = new HashSet<>();
        when(dao.getOutdatedReplicas()).thenReturn(lags);
        Assert.assertThat(controller.replicasLag().getBody(), is("0;OK"));
    }

    @Test
    public void testNotOkReplica() {
        Set<String> lags = new HashSet<>();
        lags.add("myhost:table");
        when(dao.getProblemTables()).thenReturn(lags);
        controller.getReplicasStatus();
        Assert.assertThat(controller.replicasSanity().getBody(), is("2;myhost:table"));
    }

    @Test
    public void testOkReplica() {
        Set<String> lags = new HashSet<>();
        when(dao.getProblemTables()).thenReturn(lags);
        controller.getReplicasStatus();
        Assert.assertThat(controller.replicasSanity().getBody(), is("0;OK"));
    }
}
