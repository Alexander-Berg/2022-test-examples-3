package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServiceMember;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;

public class GroupServiceMemberDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServiceMemberDao dao;
    @Autowired
    private GroupDao groupDao;

    @Test
    public void insert() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        dao.batchInsert(Cf.list(GroupServiceMemberDao.InsertData.builder()
                .uid(PsBillingUsersFactory.DEFAULT_UID)
                .groupServiceId(service.getId()).build()), Target.ENABLED);

        GroupServiceMember inserted = dao.findAll().first();
        Assert.assertEquals(PsBillingUsersFactory.DEFAULT_UID, inserted.getUid());
        Assert.assertEquals(service.getId(), inserted.getGroupServiceId());
    }

    @Test
    public void getEnabledMembersCount() {
        Group group = psBillingGroupsFactory.createGroup();
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        GroupService service = psBillingGroupsFactory.createGroupService(group, product);

        dao.batchInsert(Cf.list(createMember(service), createMember(service)), Target.ENABLED);
        dao.batchInsert(Cf.list(createMember(service)), Target.DISABLED);
        int membersCount = dao.getEnabledMembersCount(service.getId());
        Assert.assertEquals(2, membersCount);
    }

    private GroupServiceMemberDao.InsertData createMember(GroupService service) {
        return GroupServiceMemberDao.InsertData.builder()
                .uid(UUID.randomUUID().toString())
                .groupServiceId(service.getId()).build();
    }
}
