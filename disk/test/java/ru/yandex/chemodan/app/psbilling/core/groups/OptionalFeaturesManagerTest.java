package ru.yandex.chemodan.app.psbilling.core.groups;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.passport.tvmauth.exception.PermissionDenied;

public class OptionalFeaturesManagerTest extends AbstractPsBillingCoreTest {
    public static final PassportUid TEST_UID = PassportUid.cons(3000185708L);

    @Autowired
    private OptionalFeaturesManager optionalFeaturesManager;
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private FeatureDao featureDao;
    @Autowired
    private GroupServiceFeatureDao groupServiceFeatureDao;
    @Autowired
    private GroupServiceDao groupServiceDao;

    private UUID productFeatureID;
    private GroupProduct lettersArchiveAvailableProduct;
    private GroupProduct lettersArchiveProduct;
    private Group group;

    @Before
    public void setup() {
        lettersArchiveAvailableProduct = psBillingProductsFactory.createGroupProduct();
        FeatureEntity lettersArchiveAvailableFeature = psBillingProductsFactory.createFeature(
                FeatureType.TOGGLEABLE,
                b -> b.code(OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.REQUIRED_CODE));
        productFeatureID = psBillingProductsFactory.createProductFeature(
                lettersArchiveAvailableProduct.getUserProductId(), lettersArchiveAvailableFeature
        ).getId();

        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(PsBillingGroupsFactory.DEFAULT_CLIENT_ID, TEST_UID);
        group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(paymentInfo).type(GroupType.ORGANIZATION).ownerUid(PassportUid.MIN_VALUE));

        lettersArchiveProduct = psBillingProductsFactory.createGroupProduct(
                b -> b.code(OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.PRODUCT_CODE)
        );
        FeatureEntity lettersArchiveFeature = psBillingProductsFactory.createFeature(
                FeatureType.TOGGLEABLE,
                b -> b.code(OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.FEATURE_CODE));
        psBillingProductsFactory.createProductFeature(lettersArchiveProduct.getUserProductId(), lettersArchiveFeature);
    }

    @Test(expected = PermissionDenied.class)
    public void checkGroupHasPermissionToEnable() {
        optionalFeaturesManager.enable(
                group, TEST_UID, OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.FEATURE_CODE);
    }

    @Test
    public void testEnable() {
        GroupService groupService = groupServicesManager.createGroupService(
                new SubscriptionContext(lettersArchiveAvailableProduct, group, TEST_UID, Option.empty(), false)
        );

        groupServiceFeatureDao.insert(
                GroupServiceFeatureDao.InsertData.builder()
                        .groupId(group.getId())
                        .groupServiceId(groupService.getId())
                        .productFeatureId(productFeatureID)
                        .build(),
                Target.ENABLED
        );

        optionalFeaturesManager.enable(
                group, TEST_UID, OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.FEATURE_CODE);

        Assert.assertTrue(groupServiceDao.find(group.getId(), lettersArchiveProduct.getId()).isNotEmpty());
    }

    @Test
    public void testDisable() {
        testEnable();
        optionalFeaturesManager.disable(
                group, TEST_UID, OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.FEATURE_CODE);
        Assert.isFalse(featureDao.isEnabledForGroup(group.getId(),
                OptionalFeaturesManager.LETTERS_ARCHIVE_ACTIVATION.FEATURE_CODE));
    }
}
