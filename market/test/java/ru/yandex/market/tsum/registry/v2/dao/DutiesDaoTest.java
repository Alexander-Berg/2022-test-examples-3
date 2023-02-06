package ru.yandex.market.tsum.registry.v2.dao;

import java.util.List;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.dao.model.Duty;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 18/05/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, DutiesDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DutiesDaoTest {

    @Autowired
    private DutiesDao dutiesDao;

    @Test
    public void testSaveGetDelete() throws Exception {
        String serviceId = new ObjectId().toHexString();
        Duty newDuty = new Duty();
        newDuty.setServiceId(serviceId);

        dutiesDao.save(newDuty);
        Assert.assertNotNull(newDuty.getId());

        Duty dutyFromMongo = dutiesDao.get(newDuty.getId());
        Assert.assertEquals(newDuty.getId(), dutyFromMongo.getId());

        List<String> duties = dutiesDao.getDutiesByServiceId(serviceId);
        Assert.assertEquals(newDuty.getId(), duties.get(0));

        dutiesDao.delete(newDuty.getId());


        Duty duty2 = dutiesDao.get(newDuty.getId());
        Assert.assertNull(duty2);
    }
}
