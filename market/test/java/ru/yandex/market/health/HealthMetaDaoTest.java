package ru.yandex.market.health;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.common.util.date.DateUtil;

import static org.junit.Assert.assertTrue;

/**
 * @author kukabara
 */
@Ignore
public class HealthMetaDaoTest {
    private HealthMetaDao healthMetaDao;

    @Before
    public void init() throws Exception {
        healthMetaDao = new HealthMetaDao(
            "mongodb://admin:qUaek4pa3Aiwee@localhost",
            "health",
            5000,
            60000,
            "",
            false,
            100
        );
    }

    @Test
    public void test() throws Exception {
        List<OutputInfo> outputInfos = healthMetaDao.get();
        System.out.println(outputInfos.size());

        healthMetaDao.delete(outputInfos);
        outputInfos = healthMetaDao.get();
        assertTrue(outputInfos.isEmpty());

        Date start = DateUtil.addDay(new Date(), -7);
        Date end = DateUtil.addDay(new Date(), -6);
        List<OutputInfo> infosToSave = Arrays.asList(
            new OutputInfo("market.nginx2", start.getTime(), end.getTime(), 100500)
        );
        healthMetaDao.save(infosToSave);
    }

}
