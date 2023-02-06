package ru.yandex.market.clickphite.metric;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.clickphite.TimeRange;
import ru.yandex.market.clickphite.config.ConfigFile;
import ru.yandex.market.clickphite.config.ConfigurationService;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.statface.StatfaceClient;

import java.io.File;
import java.util.Collections;

/**
 * @author kukabara
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:clickphite-testing.xml")
@Ignore
public class MetricServiceTest {

    @Autowired
    private MetricService metricService;
    @Autowired
    private ConfigurationService configurationService;
    @Autowired
    private StatfaceClient statfaceClient;
    @Value("${test.file.name}")
    private String fileName;

    @Before
    public void init() throws Exception {
        metricService.afterPropertiesSet();
    }

    @Test
    public void testStatface() throws Exception {
        String data = "fielddate\tvalue\tcategory_id\n" +
            "2015-12-30 03:00:00\t0.0\t\\tВсе товары    Детские товары    Игрушки и игровые комплексы    Сюжетно-ролевые игры    Магазин\n" +
            "2015-12-30 03:00:00\t212.0\t\\tВсе товары\\tОдежда, обувь и аксессуары\\tЖенская одежда\\tСвадебная мода\\tАксессуары\\t\n" +
            "2015-12-30 03:00:00\t757.0\t\\tВсе товары\\tОдежда, обувь и аксессуары\\tДетская одежда\\tОдежда для спорта\\t\n" +
            "2015-12-30 03:00:00\t121.0\t\\tВсе товары\\tОдежда, обувь и аксессуары\\tОбувь\\tМужская обувь\\t\n";
        String report = "Market/Add/IR/models-clusterizer/import/totalModelsChangedCount/daily";
        statfaceClient.sendData(report, data);
//        statfaceClient.sendAnyData("Market/IR/models-clusterizer/import/totalModelsChangedCount/daily", data);
    }

    @Test
    public void test() throws Exception {
        ConfigFile configFile = new ConfigFile(new File(fileName));
        configurationService.parseFile(configFile);
        GraphiteMetricConfig graphiteMetricConfig = configFile.getGraphiteMetricConfigs().get(0);

        MetricContext metricContext = configurationService.createMetricContext(graphiteMetricConfig);
        TimeRange timeRange = new TimeRange(
            DateUtil.convertToDate("2015-12-28 00:05:00"),// yyyy-MM-dd HH:mm:ss
            DateUtil.convertToDate("2015-12-28 14:20:00")
        );
        metricService.updateMetricGroup(MetricContextGroupImpl.create(Collections.singletonList(metricContext)), timeRange, QueryWeight.LIGHT);

    }

}