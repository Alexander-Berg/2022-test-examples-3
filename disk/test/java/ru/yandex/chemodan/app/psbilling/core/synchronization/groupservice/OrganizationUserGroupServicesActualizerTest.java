package ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice;

import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.directory.client.DirectoryClient;

public class OrganizationUserGroupServicesActualizerTest extends AbstractPsBillingCoreTest {
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
    public void scheduleTheOnlyParentSynchronizationOnChildSync() {
        Group parent = psBillingGroupsFactory.createGroup();
        Group child1 = buildChild(parent);
        Group child2 = buildChild(parent);
        groupServicesActualizationService.scheduleForceGroupActualization(child1.getId());
        groupServicesActualizationService.scheduleForceGroupActualization(child2.getId());

        Assert.assertEquals(1, bazingaTaskManagerStub.tasksWithParams.size());
        Assert.assertEquals(
                new GroupActualizationTask.Parameters(parent.getId()),
                bazingaTaskManagerStub.tasksWithParams.map(x -> x._1).filter(GroupActualizationTask.class::isInstance).first().getParameters()
        );
    }

    @NotNull
    private Group buildChild(Group parent) {
        return psBillingGroupsFactory.createGroup(builder -> builder
                .parentGroupId(Option.of(parent.getId()))
                .type(GroupType.ORGANIZATION_USER)
                .externalId(new Random().nextLong()+""));
    }
}
