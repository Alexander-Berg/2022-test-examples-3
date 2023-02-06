package ru.yandex.direct.ytwrapper.model;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.ytwrapper.client.YtClusterTypesafeConfigProvider;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.dynamic.YtDynamicTypesafeConfig;
import ru.yandex.direct.ytwrapper.dynamic.YtQueryComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Ignore
public class YtOperatorYqlTest {
    private YtOperator ytOperator;

    @Before
    public void setUp() {
        Config ytConfig = ConfigFactory.parseResources("ru/yandex/direct/ytwrapper/yt_test.conf").getConfig("yt");
        YtClusterTypesafeConfigProvider configProvider = new YtClusterTypesafeConfigProvider(ytConfig);
        YtDynamicTypesafeConfig dynConfig = new YtDynamicTypesafeConfig(ytConfig);
        YtProvider ytProvider = new YtProvider(configProvider, dynConfig, mock(YtQueryComposer.class));
        ytOperator = ytProvider.getOperator(YtCluster.HAHN);
    }

    @Test
    public void simpleInsertAndSelect() {
        Row etalon = new Row(ThreadLocalRandom.current().nextLong(), "asdf\"\'[]\\//");

        String tmpTable = "//home/direct/tmp/yql_test";
        ytOperator.yqlExecute("INSERT INTO [" + tmpTable + "] WITH TRUNCATE SELECT ? as id, ? as val",
                etalon.id, etalon.val
        );
        List<Row> ret = ytOperator.yqlQuery("SELECT * FROM [" + tmpTable + "]",
                rs -> new Row(rs.getLong(1), rs.getString("val")));

        assertThat(ret).isEqualTo(ImmutableList.of(etalon));
    }

    class Row {
        private final long id;
        private final String val;

        Row(long id, String val) {
            this.id = id;
            this.val = val;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Row row = (Row) o;
            return id == row.id &&
                    Objects.equals(val, row.val);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, val);
        }
    }
}
