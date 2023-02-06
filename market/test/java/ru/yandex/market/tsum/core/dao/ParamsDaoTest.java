package ru.yandex.market.tsum.core.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 11/11/2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class})
public class ParamsDaoTest {
    @Autowired
    private MongoTemplate mongoTemplate;

    private ParamsDao paramsDao;

    @Before
    public void setUp() throws Exception {
        paramsDao = new ParamsDao(mongoTemplate, "params");
    }

    @Test
    public void test() throws Exception {
        paramsDao.setValue("ns1", "k1", "value1");
        Assert.assertEquals("value1", paramsDao.getString("ns1", "k1"));

        paramsDao.setValue("ns1", "k2", 42L);
        Assert.assertEquals(42L, paramsDao.getLong("ns1", "k2"));

        Assert.assertEquals(21L, paramsDao.getLong("x", "noval", 21L));

    }
}