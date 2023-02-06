package ru.yandex.chemodan.app.psbilling.core.dao.groups.impl;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.TrialUsageDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialUsage;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class TrialUsageDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private TrialUsageDao dao;

    @Test
    public void insert() {
        Group group = psBillingGroupsFactory.createGroup();
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x);

        TrialUsage inserted = dao.insert(TrialUsageDao.InsertData.builder()
                .activatedByUid(Option.of(uid))
                .groupId(Option.of(group.getId()))
                .trialDefinitionId(trial.getId())
                .endDate(Instant.now())
                .build());

        AssertHelper.assertEquals(inserted.getActivatedByUid().get(), uid);
        AssertHelper.assertEquals(inserted.getGroupId().get(), group.getId());
        AssertHelper.assertEquals(inserted.getTrialDefinitionId(), trial.getId());
        AssertHelper.assertEquals(inserted.getEndDate(), Instant.now());

        AssertHelper.assertThrows(() -> create(Option.empty(), Option.empty(), ""), IllegalArgumentException.class);
    }

    @Test
    public void insert_notAllData() {
        Group group = psBillingGroupsFactory.createGroup();
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x);

        TrialUsage inserted = dao.insert(TrialUsageDao.InsertData.builder()
                .activatedByUid(Option.empty())
                .groupId(Option.of(group.getId()))
                .trialDefinitionId(trial.getId())
                .endDate(Instant.now())
                .build());

        AssertHelper.assertEquals(inserted.getActivatedByUid(), Option.empty());
        AssertHelper.assertEquals(inserted.getGroupId().get(), group.getId());
        AssertHelper.assertEquals(inserted.getTrialDefinitionId(), trial.getId());
        AssertHelper.assertEquals(inserted.getEndDate(), Instant.now());


        inserted = dao.insert(TrialUsageDao.InsertData.builder()
                .activatedByUid(Option.of(uid))
                .groupId(Option.empty())
                .trialDefinitionId(trial.getId())
                .endDate(Instant.now())
                .build());

        AssertHelper.assertEquals(inserted.getActivatedByUid().get(), uid);
        AssertHelper.assertEquals(inserted.getGroupId(), Option.empty());
        AssertHelper.assertEquals(inserted.getTrialDefinitionId(), trial.getId());
        AssertHelper.assertEquals(inserted.getEndDate(), Instant.now());

        Assert.assertThrows(() -> dao.insert(TrialUsageDao.InsertData.builder()
                        .activatedByUid(Option.empty())
                        .groupId(Option.empty())
                        .trialDefinitionId(trial.getId())
                        .endDate(Instant.now())
                        .build()),
                IllegalArgumentException.class);
    }

    @Test
    public void find() {
        Group group = psBillingGroupsFactory.createGroup();
        Option<Group> groupO = Option.of(group);

        String trial1 = "trial1";
        TrialUsage trialUsage1 = create(Option.empty(), uidO, trial1);
        TrialUsage trialUsage2 = create(groupO, uidO, trial1);
        TrialUsage trialUsage3 = create(groupO, Option.empty(), trial1);

        String trial2 = "trial2";
        create(Option.empty(), uidO, trial2);
        create(groupO, uidO, trial2);
        create(groupO, Option.empty(), trial2);

        ListF<TrialUsage> usages = dao.findTrialUsages(Option.empty(), uidO, trial1);
        AssertHelper.assertSize(usages, 2);
        AssertHelper.assertContains(usages, trialUsage1);
        AssertHelper.assertContains(usages, trialUsage2);

        usages = dao.findTrialUsages(groupO.map(AbstractEntity::getId), Option.empty(), trial1);
        AssertHelper.assertSize(usages, 2);
        AssertHelper.assertContains(usages, trialUsage2);
        AssertHelper.assertContains(usages, trialUsage3);

        usages = dao.findTrialUsages(groupO.map(AbstractEntity::getId), uidO, trial1);
        AssertHelper.assertSize(usages, 3);
        AssertHelper.assertContains(usages, trialUsage1);
        AssertHelper.assertContains(usages, trialUsage2);
        AssertHelper.assertContains(usages, trialUsage3);

        AssertHelper.assertThrows(() -> dao.findTrialUsages(Option.empty(), Option.empty(), trial1),
                IllegalArgumentException.class);

    }

    private TrialUsage create(Option<Group> group, Option<PassportUid> uidO, String trialComparisonKey) {
        TrialDefinitionEntity trial = psBillingProductsFactory.createTrialDefinitionWithPeriod(x ->
                x.singleUsageComparisonKey(Option.of(trialComparisonKey)));

        return dao.insert(TrialUsageDao.InsertData.builder()
                .activatedByUid(uidO)
                .groupId(group.map(AbstractEntity::getId))
                .trialDefinitionId(trial.getId())
                .endDate(Instant.now())
                .build());

    }
}
