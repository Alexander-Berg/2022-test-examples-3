package ru.yandex.chemodan.app.psbilling.core.synchronization;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;

public class GroupFeatureSynchronizationTest extends FeatureSynchronizationTest
        <UUID, GroupServiceFeature, GroupServiceFeatureDao.InsertData, GroupFeature, GroupServiceFeatureDao> {
    @Autowired
    private GroupServiceFeatureDao groupServiceFeatureDao;
    @Autowired
    private GroupDao groupDao;

    private Group group1, group2;

    @Before
    public void Init() {
        group1 = psBillingGroupsFactory.createGroup();
        group2 = psBillingGroupsFactory.createGroup();
    }

    @Override
    protected GroupServiceFeature createFeature(UUID groupId, FeatureEntity featureEntity) {
        return createFeature(groupId, featureEntity, 20);
    }

    @Override
    protected GroupServiceFeature createFeature(UUID groupId, FeatureEntity featureEntity, int amount) {
        Group group = groupDao.findById(groupId);
        return psBillingGroupsFactory.createGroupServiceFeature(group, featureEntity,
                x -> x.amount(BigDecimal.valueOf(amount)));
    }

    @Override
    protected GroupServiceFeature createFeature(ProductFeatureEntity productFeatureEntity) {
        Group group = groupDao.findById(getOwner());
        return psBillingGroupsFactory.createGroupServiceFeature(group, productFeatureEntity);
    }

    @Override
    protected ProductFeatureEntity createProductFeature(FeatureEntity featureEntity, int amount) {
        GroupProduct product = psBillingProductsFactory.createGroupProduct();
        return psBillingProductsFactory.createProductFeature(
                product.getUserProductId(), featureEntity, x -> x.amount(BigDecimal.valueOf(amount)));
    }

    @Override
    protected void synchronize(UUID groupId, FeatureEntity feature) {
        groupFeaturesActualizationService.synchronize(groupId, feature.getId());
    }

    @Override
    protected String getSentIdPattern() {
        return "#{groupExternalId}";
    }

    @Override
    protected String getSentIdValue(GroupServiceFeature issuedFeature) {
        return groupDao.findById(issuedFeature.getGroupId()).getExternalId();
    }

    @Override
    protected GroupServiceFeature refresh(GroupServiceFeature issuedFeature) {
        return groupServiceFeatureDao.findById(issuedFeature.getId());
    }

    @Override
    protected UUID getOwner() {
        return group1.getId();
    }

    @Override
    protected UUID getSecondOwner() {
        return group2.getId();
    }
}
