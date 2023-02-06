package ru.yandex.market.tsum.registry.v2.dao;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 18/05/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, ComponentsGroupsDao.class, ComponentsDao.class,
    InstallationsDao.class, DutiesDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ServicesDaoTest {

    @Autowired
    private ServicesDao servicesDao;

    @Test
    public void testGetAllIds() throws Exception {

        Service service1 = new Service("service1");
        service1.setAbcSlug("service1");
        servicesDao.save(service1);
        Service service2 = new Service("service2");
        service2.setAbcSlug("service2");
        servicesDao.save(service2);

        List<String> allIds = servicesDao.getAllIds();

        Assert.assertEquals(allIds.size(), 2);
        Assert.assertTrue(allIds.contains(service1.getId()));
        Assert.assertTrue(allIds.contains(service2.getId()));
    }

    @Test
    public void testGetAllNames() throws Exception {
        Service service1 = new Service("service1");
        service1.setAbcSlug("service1");
        servicesDao.save(service1);
        Service service2 = new Service("service2");
        service2.setAbcSlug("service2");
        servicesDao.save(service2);

        List<String> allIds = servicesDao.getAllNames();

        Assert.assertEquals(allIds.size(), 2);
        Assert.assertTrue(allIds.contains(service1.getName()));
        Assert.assertTrue(allIds.contains(service2.getName()));
    }

    @Test
    public void testGetByName() throws Exception {
        Service service1 = new Service("service1");
        servicesDao.save(service1);

        Service serviceFromMongo = servicesDao.getByName("service1");
        Assert.assertEquals(service1.getId(), serviceFromMongo.getId());
    }

    @Test
    public void testRemove() throws Exception {
        Service service1 = new Service("service1");
        servicesDao.save(service1);

        Assert.assertNotNull(service1.getId());
        Assert.assertNotNull(servicesDao.getByName("service1"));

        servicesDao.remove(service1.getId());
        Assert.assertNull(servicesDao.get(service1.getId()));
    }
}
