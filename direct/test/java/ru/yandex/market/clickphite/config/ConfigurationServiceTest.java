package ru.yandex.market.clickphite.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.clickphite.QueryBuilder;
import ru.yandex.market.clickphite.config.metric.GraphiteMetricConfig;
import ru.yandex.market.clickphite.config.metric.MetricPeriod;
import ru.yandex.market.clickphite.config.metric.MetricType;
import ru.yandex.market.clickphite.config.validation.context.ConfigValidationException;
import ru.yandex.market.clickphite.metric.MetricStorage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:clickphite-testing.xml")
@Ignore
public class ConfigurationServiceTest {
    private static final Logger log = LogManager.getLogger();

    private static final String SRC_DIR = "/Users/kukabara/Documents/svn/git/market-health/config-cs-clickphite/src-test";
    private static final String TARGET_DIR = "/Users/kukabara/Documents/svn/git/market-health/config-cs-clickphite/src2";
    @Autowired
    private ConfigurationService reader;

    private void check(List<GraphiteMetricConfig> newConfigs, List<GraphiteMetricConfig> oldConfigs) throws IOException, ConfigValidationException {
        sortConfig(oldConfigs);
        sortConfig(newConfigs);


        Assert.assertEquals(oldConfigs.size(), newConfigs.size());

        for (GraphiteMetricConfig oldConfig : oldConfigs) {
            boolean found = false;
            for (GraphiteMetricConfig newConfig : newConfigs) {
                if (newConfig.equals(oldConfig)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                System.out.println("Can't find " + oldConfig);
            }
        }
        Assert.assertTrue(oldConfigs.containsAll(newConfigs));
        Assert.assertTrue(newConfigs.containsAll(oldConfigs));
    }

    private void sortConfig(List<GraphiteMetricConfig> configs) {
        if (configs == null) {
            return;
        }
        for (GraphiteMetricConfig config : configs) {
            if (config.getSplits() != null) {
                config.getSplits().sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
            }
            if (config.getDashboards() != null) {
                config.getDashboards().sort((o1, o2) -> o1.getMetric().compareTo(o2.getMetric()));
            }
        }
    }

    private File[] getCurrentConfigFiles(String configDir) {
        File dir = new File(configDir);
        return dir.listFiles((dir1, name) -> {
            return name.toLowerCase().endsWith(".json");
        });
    }

    @Test
    public void testReadFile() throws Exception {
        String fileName = "abo_main.json";
        File targetFile = new File(SRC_DIR + "/" + fileName);
        ConfigFile configFile = new ConfigFile(targetFile);
        reader.parseFile(configFile);
        System.out.println(configFile.getGraphiteMetricConfigs());
    }

    @Test
    public void testArgMax() throws Exception {
        ConfigFile file = new ConfigFile(new File("/Users/imelnikov/dev/ArgMax.json"));
        reader.parseFile(file);
        List<GraphiteMetricConfig> configs = file.getGraphiteMetricConfigs();
        System.out.println(QueryBuilder.buildMetricQueryTemplate(configs.get(0), QueryBuilder.UNREAL_PERIOD));
    }

    static class MetricParam {
        private String metricField;
        private String filter;
        private MetricPeriod period;
        private MetricType type;
        private String tableName;
        private MetricStorage storage;
        private String title;

        MetricParam(String metricField, String filter, MetricPeriod period, MetricType type, String tableName,
                    MetricStorage storage, String title) {
            this.metricField = metricField;
            this.filter = filter;
            this.period = period;
            this.type = type;
            this.tableName = tableName;
            this.storage = storage;
            this.title = title;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MetricParam)) {
                return false;
            }

            MetricParam that = (MetricParam) o;

            if (metricField != null ? !metricField.equals(that.metricField) : that.metricField != null) {
                return false;
            }
            if (filter != null ? !filter.equals(that.filter) : that.filter != null) {
                return false;
            }
            if (period != that.period) {
                return false;
            }
            if (type != that.type) {
                return false;
            }
            if (tableName != null ? !tableName.equals(that.tableName) : that.tableName != null) {
                return false;
            }
            if (storage != that.storage) {
                return false;
            }
            return !(title != null ? !title.equals(that.title) : that.title != null);

        }

        @Override
        public int hashCode() {
            int result = metricField != null ? metricField.hashCode() : 0;
            result = 31 * result + (filter != null ? filter.hashCode() : 0);
            result = 31 * result + (period != null ? period.hashCode() : 0);
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
            result = 31 * result + (storage != null ? storage.hashCode() : 0);
            result = 31 * result + (title != null ? title.hashCode() : 0);
            return result;
        }

    }

    class CollectionAdapter implements JsonSerializer<Collection<?>> {
        @Override
        public JsonElement serialize(Collection<?> src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null || src.isEmpty()) // exclusion is made here
            {
                return null;
            }

            JsonArray array = new JsonArray();

            for (Object child : src) {
                JsonElement element = context.serialize(child);
                array.add(element);
            }

            return array;
        }
    }
}