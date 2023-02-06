package ru.yandex.market.mbo.db.params;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.configs.db.parameter.ParameterMigrationLogConfig;
import ru.yandex.market.mbo.utils.BaseDbTest;

@RunWith(SpringJUnit4ClassRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
@ContextConfiguration(classes = {
    ParameterMigrationLogConfig.class
})
public class ParameterMigrationLogImplTest extends BaseDbTest {

    @Resource
    ParameterMigrationLog parameterMigrationLog;

    @Test
    public void testReadMethodsForEmptyLog() {
        Assert.assertEquals(Collections.emptyList(), parameterMigrationLog.loadAll());
        Assert.assertEquals(Collections.emptyList(), parameterMigrationLog.forCategory(123L));
    }

    @Test
    public void testAddToLogWithoutOptions() {
        parameterMigrationLog.addParameter(125L, 1L, 2L, Collections.emptyMap(),
            false);
        List<ParameterMigrationLog.Entry> res1 = parameterMigrationLog.loadAll();
        Assert.assertEquals(1, res1.size());
        long ts = res1.get(0).getTimestamp();
        Assert.assertTrue(ts > 0);
        Assert.assertEquals(Collections.singletonList(
            new ParameterMigrationLog.Entry(125L, 1L, 2L, Collections.emptyMap(), ts)
        ), res1);
        Assert.assertEquals(Collections.singletonList(
            new ParameterMigrationLog.Entry(125L, 1L, 2L, Collections.emptyMap(), ts)
        ), parameterMigrationLog.forCategory(125L));
        Assert.assertEquals(Collections.emptyList(), parameterMigrationLog.forCategory(124L));
    }

    @Test
    public void testAddToLogWithOptions() {
        Map<Long, Long> map = new HashMap<>();
        map.put(12L, 112L);
        map.put(13L, 113L);
        parameterMigrationLog.addParameter(123L, 1L, 2L, map, false);
        List<ParameterMigrationLog.Entry> res1 = parameterMigrationLog.loadAll();
        Assert.assertEquals(1, res1.size());
        long ts = res1.get(0).getTimestamp();
        Assert.assertEquals(
            Collections.singletonList(new ParameterMigrationLog.Entry(123L, 1L, 2L, map, ts)),
            res1
        );
    }


    @Test
    public void testAddToLogWithExistingOptions() {
        Map<Long, Long> map = new HashMap<>();
        map.put(12L, 112L);
        map.put(13L, 113L);
        parameterMigrationLog.addParameter(123L, 1L, 2L, map, false);
        Map<Long, Long> map2 = new HashMap<>();
        map2.put(14L, 114L);
        parameterMigrationLog.addParameter(123L, 1L, 2L, map2, false);
        List<ParameterMigrationLog.Entry> res1 = parameterMigrationLog.loadAll();
        Assert.assertEquals(1, res1.size());
        long ts = res1.get(0).getTimestamp();
        Map<Long, Long> map3 = new HashMap<>(map);
        map3.putAll(map2);
        Assert.assertEquals(
            Collections.singletonList(new ParameterMigrationLog.Entry(123L, 1L, 2L,
                map3, ts)),
            res1
        );
    }
}
