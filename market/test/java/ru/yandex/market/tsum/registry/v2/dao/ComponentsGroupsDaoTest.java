package ru.yandex.market.tsum.registry.v2.dao;

import java.util.List;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.core.TestMongo;
import ru.yandex.market.tsum.registry.v2.RegistryRolesProvider;
import ru.yandex.market.tsum.registry.v2.dao.model.ComponentsGroup;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 18/05/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestMongo.class, ServicesDao.class, ComponentsGroupsDao.class, ComponentsDao.class,
    DutiesDao.class, InstallationsDao.class, ResourceDao.class, GeneralDao.class, RegistryRolesProvider.class,
    ComponentUpdateRequestDao.class, ComponentSpecsDao.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ComponentsGroupsDaoTest {

    @Autowired
    private ComponentsGroupsDao componentsGroupsDao;

    @Test
    public void testRemoveRecurcively() throws Exception {
        String serviceId = "serviceId";
        ComponentsGroup componentsGroup1 = new ComponentsGroup(serviceId);
        componentsGroupsDao.save(componentsGroup1);
        ComponentsGroup componentsGroup2 = new ComponentsGroup(serviceId);
        componentsGroup2.setParentComponentsGroupId(componentsGroup1.getId());
        componentsGroupsDao.save(componentsGroup2);

        componentsGroupsDao.removeRecursively(componentsGroup1.getId());

        Assert.assertNull(componentsGroupsDao.get(componentsGroup1.getId()));
        Assert.assertNull(componentsGroupsDao.get(componentsGroup2.getId()));
    }

    @Test
    public void testGetChildGroups() throws Exception {
        String serviceId = "serviceId";
        ComponentsGroup componentsGroup1 = new ComponentsGroup(serviceId);
        componentsGroupsDao.save(componentsGroup1);
        ComponentsGroup componentsGroup2 = new ComponentsGroup(serviceId);
        componentsGroup2.setParentComponentsGroupId(componentsGroup1.getId());
        componentsGroupsDao.save(componentsGroup2);
        ComponentsGroup componentsGroup3 = new ComponentsGroup(serviceId);
        componentsGroup3.setParentComponentsGroupId(componentsGroup1.getId());
        componentsGroupsDao.save(componentsGroup3);

        List<String> groupsIds = componentsGroupsDao.getChildComponentsGroupsIds(componentsGroup1.getId());

        Assert.assertEquals(groupsIds.size(), 2);
        Assert.assertTrue(groupsIds.contains(componentsGroup2.getId()));
        Assert.assertTrue(groupsIds.contains(componentsGroup3.getId()));
    }

}
