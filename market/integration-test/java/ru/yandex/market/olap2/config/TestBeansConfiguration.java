package ru.yandex.market.olap2.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockReset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.olap2.dao.ClickhouseDao;
import ru.yandex.market.olap2.dao.LoggingJdbcTemplate;
import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.graphite.Graphite;
import ru.yandex.market.olap2.leader.LeaderElector;
import ru.yandex.market.olap2.leader.Shutdowner;
import ru.yandex.market.olap2.model.EnvironmentDetector;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.services.JugglerEventsSender;
import ru.yandex.market.olap2.yt.YtClusterLiveliness;
import ru.yandex.market.olap2.yt.YtTableService;

import static com.google.common.collect.Sets.newHashSet;

@Configuration
public class TestBeansConfiguration {


    private final static YtCluster HAHN = new YtCluster("hahn");
    private final static Set<YtCluster> LIVE_CLUSTERS = newHashSet(HAHN);

    @MockBean(reset = MockReset.BEFORE)
    public ClickhouseDao clickhouseDao;

    @MockBean(reset = MockReset.BEFORE)
    public Map<String, List<LoggingJdbcTemplate>> clickhouseJdbcTemplates;

    @MockBean(reset = MockReset.BEFORE)
    public YtTableService tableService;

    @MockBean(reset = MockReset.BEFORE)
    public Graphite graphite;

    @MockBean(reset = MockReset.BEFORE)
    public Shutdowner shutdowner;

    @MockBean(reset = MockReset.BEFORE)
    public YtClusterLiveliness ytClusterLiveliness;

    @MockBean(reset = MockReset.BEFORE)
    public CloseableHttpClient httpClient;

    @MockBean(reset = MockReset.BEFORE)
    public JugglerEventsSender jugglerEventsSender;


    @Bean
    public LeaderElector testLeaderElector(Shutdowner shutdowner, Graphite graphite, MetadataDao metadataDao) {
        return new TestLeaderElector(shutdowner, graphite, metadataDao);
    }

    /**
     * Основной LeaderElector первым делом пытается начать работать zookeeper-ом,
     * что усложняет его использование в тестах.
     */
    private static class TestLeaderElector extends LeaderElector {

        public TestLeaderElector(Shutdowner shutdowner, Graphite graphite, MetadataDao metadata) {
            super(null, new EnvironmentDetector("integration-test"), shutdowner, graphite, metadata);
            I_AM_LEADER = true;
        }

        @Override
        public void initZk() {
            // we do not need it here
        }
    }
}
