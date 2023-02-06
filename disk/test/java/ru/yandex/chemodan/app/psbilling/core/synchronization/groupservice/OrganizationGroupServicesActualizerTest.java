package ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.directory.client.DirectoryClient;

public class OrganizationGroupServicesActualizerTest extends AbstractPsBillingCoreTest {
    @Autowired
    DirectoryClient directoryClientMock;
    @Autowired
    GroupServicesActualizationService groupServicesActualizationService;
    @Autowired
    GroupServiceMemberDao groupServiceMemberDao;
    @Autowired
    PsBillingGroupsFactory psBillingGroupsFactory;
    @Autowired
    PsBillingProductsFactory psBillingProductsFactory;

    @Test
    public void createServiceForSubgroups() {
        Group parent = initSubgroupTest();
        mockOrganizatiomMembers(Cf.set(uid.getUid()));
        groupServicesActualizationService.actualize(parent.getId());
        Assert.assertEquals(2, groupServiceMemberDao.findAll().length());

    }

    private Group initSubgroupTest() {
        Group parent = psBillingGroupsFactory.createGroup();
        Group child = psBillingGroupsFactory.createGroup(builder -> builder
                .parentGroupId(Option.of(parent.getId()))
                .type(GroupType.ORGANIZATION_USER)
                .externalId(uid.toString()));
        GroupProduct parentProduct = psBillingProductsFactory.createGroupProduct();
        GroupProduct childProduct = psBillingProductsFactory.createGroupProduct();
        psBillingGroupsFactory.createGroupService(parent, parentProduct);
        psBillingGroupsFactory.createGroupService(child, childProduct);
        return parent;
    }

    @Test
    public void disableServiceForSubgroups() {
        Group parent = initSubgroupTest();
        mockOrganizatiomMembers(Cf.set(uid.getUid()));
        groupServicesActualizationService.actualize(parent.getId());
        Assert.assertEquals(2, groupServiceMemberDao.findAll().length());
        mockOrganizatiomMembers(Cf.set());
        groupServicesActualizationService.actualize(parent.getId());
        Assert.assertEquals(0, groupServiceMemberDao
                .findAll()
                .filter(member -> member.getTarget() == Target.ENABLED).length());
    }

    private void mockOrganizatiomMembers(SetF<Long> set) {
        Mockito.when(directoryClientMock
                .usersInOrganization(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(set);
    }
}
