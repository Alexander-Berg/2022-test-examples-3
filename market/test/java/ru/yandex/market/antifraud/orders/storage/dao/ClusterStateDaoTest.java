package ru.yandex.market.antifraud.orders.storage.dao;

import java.time.Instant;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.configuration.ClusterNodeEvent;
import ru.yandex.market.antifraud.orders.storage.entity.configuration.NodeEventType;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClusterStateDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private ClusterStateDao clusterStateDao;

    @Before
    public void init() {
        clusterStateDao = new ClusterStateDao(jdbcTemplate);
    }

    @Test
    public void getLatestEvents(){
        ClusterNodeEvent event1 = clusterStateDao.save(ClusterNodeEvent.builder()
                .ip("ip")
                .node("node")
                .eventType(NodeEventType.TAKE_LEADERSHIP)
                .addedAt(Instant.now())
                .build());
        ClusterNodeEvent event2 = clusterStateDao.save(ClusterNodeEvent.builder()
                .ip("ip")
                .node("node")
                .eventType(NodeEventType.RELEASE_LEADERSHIP)
                .addedAt(Instant.now())
                .build());
        List<ClusterNodeEvent> events = clusterStateDao.getLatestEvents(10);
        assertThat(events).contains(event1, event2);
    }

}
