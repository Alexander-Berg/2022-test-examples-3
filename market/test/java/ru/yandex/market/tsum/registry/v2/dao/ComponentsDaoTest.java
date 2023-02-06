package ru.yandex.market.tsum.registry.v2.dao;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.dao.model.Component;

import java.util.List;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 18/05/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, InstallationsDao.class, ComponentsDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComponentsDaoTest {

    @Autowired
    private ComponentsDao componentsDao;

    @Test
    public void testGeAllIds() throws Exception {
        Component component1 = new Component();
        component1.setName("component1");
        component1.setAbcSlug("component1");
        componentsDao.save(component1);
        Component component2 = new Component();
        component2.setName("component2");
        component2.setAbcSlug("component2");
        componentsDao.save(component2);

        List<String> allIds = componentsDao.getAllIds();

        Assert.assertEquals(allIds.size(), 2);
        Assert.assertTrue(allIds.contains(component1.getId()));
        Assert.assertTrue(allIds.contains(component2.getId()));
    }

    @Test
    public void testGetByName() throws Exception {
        String componentName = "SomeComponent";
        Component component = new Component();
        component.setName(componentName);
        componentsDao.save(component);

        Component componentFromMongo = componentsDao.getByName(componentName);
        Assert.assertEquals(component.getId(), componentFromMongo.getId());
    }

    @Test
    public void testGetComponentsByServiceId() throws Exception {
        String componentName1 = "SomeComponent1";
        String componentName2 = "SomeComponent2";
        String serviceId = "serviceId";
        Component component = new Component();
        component.setServiceId(serviceId);
        component.setName("component1");
        component.setAbcSlug("component1");
        componentsDao.save(component);
        Component component2 = new Component();
        component2.setServiceId(serviceId);
        component2.setName("component2");
        component2.setAbcSlug("component2");
        componentsDao.save(component2);

        List<String> components = componentsDao.getComponentsInService(serviceId);
        Assert.assertEquals(components.size(), 2);
        Assert.assertTrue(components.contains(component.getId()));
        Assert.assertTrue(components.contains(component2.getId()));
    }

    @Test
    public void testGetComponentsInGroup() throws Exception {
        String componentName1 = "SomeComponent1";
        String componentName2 = "SomeComponent2";
        String componentsGroupId = "serviceId";
        Component component = new Component();
        component.setName("component");
        component.setComponentsGroupId(componentsGroupId);
        component.setAbcSlug("component1");
        componentsDao.save(component);
        Component component2 = new Component();
        component2.setName("component2");
        component2.setComponentsGroupId(componentsGroupId);
        component2.setAbcSlug("component2");
        componentsDao.save(component2);

        List<String> components = componentsDao.getComponentsInGroup(componentsGroupId);
        Assert.assertEquals(components.size(), 2);
        Assert.assertTrue(components.contains(component.getId()));
        Assert.assertTrue(components.contains(component2.getId()));
    }
}
