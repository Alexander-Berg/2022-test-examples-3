package ru.yandex.chemodan.app.psbilling.core.dao.features;

import java.util.UUID;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductTemplateFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.misc.test.Assert;

public class GroupServiceFeatureDaoTest extends AbstractPsBillingCoreTest {
    Group group;
    Group group2;
    Group group3;
    GroupProduct groupProduct;
    ProductFeatureEntity productFeature;
    ProductTemplateFeatureEntity productTemplateFeature;
    GroupService groupService;
    GroupService groupService2;
    GroupService groupService3;

    @Autowired
    private GroupServiceFeatureDao groupServiceFeatureDao;

    @Before
    public void Setup() {
        group = psBillingGroupsFactory.createGroup();
        group2 = psBillingGroupsFactory.createGroup();
        group3 = psBillingGroupsFactory.createGroup();

        groupProduct = psBillingProductsFactory.createGroupProduct();
        ProductTemplateEntity productTemplate = psBillingProductsFactory.createProductTemplate(
                groupProduct.getUserProduct().getCodeFamily());
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        productFeature = psBillingProductsFactory.createProductFeature(groupProduct.getUserProduct().getId(),
                feature);
        productTemplateFeature = psBillingProductsFactory.createProductTemplateFeature(productTemplate.getId(),
                feature);
        groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);
        groupService2 = psBillingGroupsFactory.createGroupService(group2, groupProduct);
        groupService3 = psBillingGroupsFactory.createGroupService(group3, groupProduct);
    }

    @Test
    public void insert() {
        UUID groupId = group.getId();

        GroupServiceFeature inserted = groupServiceFeatureDao.insert(GroupServiceFeatureDao.InsertData.builder()
                        .groupId(groupId)
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeature.getId())
                        .productTemplateFeatureId(productTemplateFeature.getId())
                        .build(),
                Target.ENABLED);

        Assert.notNull(inserted.getId());
        Assert.equals(groupId, inserted.getGroupId());
        Assert.equals(groupId, inserted.getOwnerId());
        Assert.equals(groupService.getId(), inserted.getGroupServiceId());
        Assert.equals(groupService.getId(), inserted.getParentServiceId());
        Assert.equals(productFeature.getId(), inserted.getProductFeatureId());
        Assert.equals(Option.of(productTemplateFeature.getId()), inserted.getProductTemplateFeatureId());
        Assert.equals(Option.empty(), inserted.getNextTry());
        Assert.equals(Option.empty(), inserted.getActualDisabledAt());
        Assert.equals(Option.empty(), inserted.getActualEnabledAt());
        Assert.equals(Instant.now(), inserted.getCreatedAt());
        Assert.equals(SynchronizationStatus.INIT, inserted.getStatus());
        Assert.equals(Instant.now(), inserted.getStatusUpdatedAt());
        Assert.equals(Target.ENABLED, inserted.getTarget());
        Assert.equals(Instant.now(), inserted.getTargetUpdatedAt());
    }

    @Test
    public void batchInsert() {
        ListF<GroupServiceFeatureDao.InsertData> data = Cf.list(
                GroupServiceFeatureDao.InsertData.builder()
                        .groupId(group.getId())
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeature.getId())
                        .build(),
                GroupServiceFeatureDao.InsertData.builder()
                        .groupId(group.getId())
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeature.getId())
                        .build());
        groupServiceFeatureDao.batchInsert(data, Target.ENABLED);
        ListF<GroupServiceFeature> all = groupServiceFeatureDao.findAll();
        Assert.equals(2, all.size());
    }

    @Test
    public void setTargetState() {
        GroupServiceFeature groupServiceFeature = createGroupServiceFeature(Target.DISABLED);
        DateUtils.shiftTime(Duration.standardDays(1));

        groupServiceFeatureDao.setTargetState(groupServiceFeature.getId(), Target.ENABLED);
        groupServiceFeature = groupServiceFeatureDao.findById(groupServiceFeature.getId());
        Assert.equals(Target.ENABLED, groupServiceFeature.getTarget());
        Assert.equals(Instant.now(), groupServiceFeature.getTargetUpdatedAt());
        Assert.equals(Instant.now(), groupServiceFeature.getStatusUpdatedAt());
        Assert.equals(Option.empty(), groupServiceFeature.getNextTry());
    }

    @Test
    public void snoozeSynchronization() {
        GroupServiceFeature groupServiceFeature = createGroupServiceFeature(Target.ENABLED);
        Instant delayUntil = Instant.now().plus(Duration.standardDays(1));
        groupServiceFeatureDao.snoozeSynchronization(Cf.list(groupServiceFeature.getId()),
                delayUntil);

        groupServiceFeature = groupServiceFeatureDao.findById(groupServiceFeature.getId());
        Assert.equals(SynchronizationStatus.SNOOZING, groupServiceFeature.getStatus());
        Assert.equals(Instant.now(), groupServiceFeature.getStatusUpdatedAt());
        Assert.equals(Option.of(delayUntil), groupServiceFeature.getNextTry());
    }

    @Test
    public void findForSynchronization() {
        GroupServiceFeature gsf1 = createGroupServiceFeature(groupService, Target.ENABLED);
        GroupServiceFeature gsf2 = createGroupServiceFeature(groupService2, Target.ENABLED);
        GroupServiceFeature gsf3 = createGroupServiceFeature(groupService3, Target.ENABLED);

        ListF<GroupFeature> forSynchronization = groupServiceFeatureDao.findForSynchronization();
        Assert.equals(3, forSynchronization.size());

        groupServiceFeatureDao.setStatusActual(gsf1.getId(), Target.ENABLED);
        groupServiceFeatureDao.setStatusActual(gsf2.getId(), Target.ENABLED);
        groupServiceFeatureDao.setStatusActual(gsf3.getId(), Target.ENABLED);

        Instant delayUntil = Instant.now().plus(Duration.standardDays(1));
        groupServiceFeatureDao.snoozeSynchronization(Cf.list(gsf1.getId()), delayUntil);
        forSynchronization = groupServiceFeatureDao.findForSynchronization();
        Assert.equals(0, forSynchronization.size());

        DateUtils.shiftTime(Duration.standardDays(2));
        forSynchronization = groupServiceFeatureDao.findForSynchronization();
        Assert.equals(1, forSynchronization.size());
        Assert.equals(gsf1.getGroupId(), forSynchronization.single().getGroupId());

        DateUtils.shiftTimeBack(Duration.standardDays(2));
        groupServiceFeatureDao.setTargetState(gsf2.getId(), Target.DISABLED);
        forSynchronization = groupServiceFeatureDao.findForSynchronization();
        Assert.equals(1, forSynchronization.size());
        Assert.equals(gsf2.getGroupId(), forSynchronization.single().getGroupId());
    }

    @Test
    public void findForSynchronization_parent() {
        createGroupServiceFeature(groupService, Target.ENABLED);
        GroupServiceFeature gsf2 = createGroupServiceFeature(groupService2, Target.ENABLED);

        ListF<GroupFeature> forSynchronization = groupServiceFeatureDao.findForSynchronization();
        Assert.equals(2, forSynchronization.size());

        forSynchronization = groupServiceFeatureDao.findForSynchronization(groupService2.getId());
        Assert.equals(1, forSynchronization.size());
        Assert.equals(gsf2.getGroupId(), forSynchronization.single().getGroupId());
    }

    @Test
    public void countNotActualUpdatedBefore() {
        createGroupServiceFeature(Target.ENABLED);
        int stale = groupServiceFeatureDao.countNotActualUpdatedBefore(Duration.standardDays(1));
        Assert.equals(0, stale);

        DateUtils.shiftTime(Duration.standardDays(2));
        stale = groupServiceFeatureDao.countNotActualUpdatedBefore(Duration.standardDays(1));
        Assert.equals(1, stale);
    }

    @Test
    public void lock() {
        createGroupServiceFeature(groupService, Target.DISABLED); // not actual
        GroupServiceFeature gs2 = createGroupServiceFeature(groupService, Target.ENABLED);
        groupServiceFeatureDao.setStatusActual(gs2.getId(), Target.ENABLED); // enabled actual
        GroupServiceFeature gs3 = createGroupServiceFeature(groupService, Target.DISABLED); // disabled actual
        groupServiceFeatureDao.setStatusActual(gs3.getId(), Target.DISABLED); // enabled actual

        UUID lock = UUID.randomUUID();
        ListF<GroupServiceFeature> locked = groupServiceFeatureDao.findAndLockEnabledOrNotActual(
                group.getId(), productFeature.getFeatureId().get(), lock, Duration.standardHours(1));
        Assert.equals(2, locked.size());

        locked = groupServiceFeatureDao.findAndLockEnabledOrNotActual(
                group.getId(), productFeature.getFeatureId().get(), lock, Duration.standardHours(1));
        Assert.equals(0, locked.size()); // locked

        groupServiceFeatureDao.unlock(lock);
        locked = groupServiceFeatureDao.findAndLockEnabledOrNotActual(
                group.getId(), productFeature.getFeatureId().get(), lock, Duration.standardHours(1));
        Assert.equals(2, locked.size()); // unlocked
    }

    private GroupServiceFeature createGroupServiceFeature(Target target) {
        return createGroupServiceFeature(groupService, target);
    }

    private GroupServiceFeature createGroupServiceFeature(GroupService groupService, Target target) {
        return groupServiceFeatureDao.insert(GroupServiceFeatureDao.InsertData.builder()
                        .groupId(groupService.getGroupId())
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeature.getId())
                        .productTemplateFeatureId(productTemplateFeature.getId())
                        .build(),
                target);
    }
}
