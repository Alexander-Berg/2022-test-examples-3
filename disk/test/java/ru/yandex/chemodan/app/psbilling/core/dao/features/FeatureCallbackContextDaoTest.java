package ru.yandex.chemodan.app.psbilling.core.dao.features;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.features.FeatureCallbackContext;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.misc.test.Assert;

public class FeatureCallbackContextDaoTest extends AbstractPsBillingCoreTest {
    @Autowired
    private FeatureCallbackContextDao featureCallbackContextDao;

    @Test
    public void insert() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        Group group = psBillingGroupsFactory.createGroup();
        FeatureCallbackContextDao.InsertData.InsertDataBuilder builder =
                FeatureCallbackContextDao.InsertData.builder().featureId(feature.getId());
        FeatureCallbackContext inserted = featureCallbackContextDao.insertOrUpdate(
                builder.uid(Option.of("123")).build());

        Assert.equals(feature.getId(), inserted.getFeatureId());
        Assert.equals(Option.of("123"), inserted.getUidO());

        builder.uid(Option.empty());

        inserted = featureCallbackContextDao.insertOrUpdate(builder.groupId(Option.of(group.getId())).build());

        Assert.equals(feature.getId(), inserted.getFeatureId());
        Assert.equals(Option.of(group.getId()), inserted.getGroupIdO());

        Assert.assertThrows(() -> featureCallbackContextDao.insertOrUpdate(builder.uid(Option.of("123")).build()),
                IllegalArgumentException.class);
    }

    @Test
    public void doubleInsert() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        Group group = psBillingGroupsFactory.createGroup();
        FeatureCallbackContextDao.InsertData.InsertDataBuilder builder =
                FeatureCallbackContextDao.InsertData.builder().featureId(feature.getId());
        FeatureCallbackContext context =
                featureCallbackContextDao.insertOrUpdate(builder.uid(Option.of("123")).build());
        FeatureCallbackContext newContext =
                featureCallbackContextDao.insertOrUpdate(builder.uid(Option.of("123")).build());
        Assert.equals(context.getId(), newContext.getId());

        builder.uid(Option.empty());

        context = featureCallbackContextDao.insertOrUpdate(builder.groupId(Option.of(group.getId())).build());
        Assert.assertNotEquals(context.getId(), newContext.getId());

        newContext = featureCallbackContextDao.insertOrUpdate(builder.groupId(Option.of(group.getId())).build());
        Assert.equals(context.getId(), newContext.getId());
    }
}
