package ru.yandex.chemodan.app.psbilling.core.products.selectors;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductLineEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class PrepaidSelectorTest extends AbstractPsBillingCoreTest {
    @Autowired
    private ProductLineSelectorFactory productLineSelectorFactory;
    @Autowired
    private FeatureFlags featureFlags;
    @Autowired
    private PsBillingGroupsFactory groupsFactory;

    private GroupProduct prepaidProduct;
    private GroupProduct postpaidProduct;

    private ProductLineEntity line;

    private final String PRODUCT_SET = UUID.randomUUID().toString();

    @Test
    public void isAvailable__allowed() {
        Group group = groupsFactory.createGroup();
        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__disableFeature() {
        featureFlags.getPrepaidProducts().setValue("false");
        Group group = groupsFactory.createGroup();
        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__twoGroupPaymentWithoutPostpaid() {
        String passportUid = "123456";
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(123L, passportUid);

        Group group = groupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));
        groupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));

        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__oneGroupWithPostpaid() {
        Group group = groupsFactory.createGroup();
        groupsFactory.createActualGroupService(group, postpaidProduct);

        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__twoGroupSecondGroupWithPostpaid() {
        String passportUid = "123456";
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(123L, passportUid);

        Group group = groupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));
        Group group2 = groupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));
        groupsFactory.createActualGroupService(group2, postpaidProduct);

        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group2), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__oneGroupWithNotEnabledPostpaid() {
        Group group = groupsFactory.createGroup();
        groupsFactory.createGroupService(group, postpaidProduct, Target.DISABLED);

        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__oneGroupWithPrepaid() {
        Group group = groupsFactory.createGroup();
        groupsFactory.createActualGroupService(group, prepaidProduct);

        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__withHiddenProduct() {
        Group group = groupsFactory.createGroup();
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID).hidden(true));
        groupsFactory.createActualGroupService(group, groupProduct);

        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(PassportUid.MAX_VALUE),
                        Option.of(group), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__freeWithPostpaid() {
        PassportUid passportUid = PassportUid.cons(123456);
        BalancePaymentInfo paymentInfo = new BalancePaymentInfo(123L, passportUid.toString());

        Group postPaidGroup = groupsFactory.createGroup(x -> x.paymentInfo(paymentInfo));
        Group freeGroup = groupsFactory.createGroup(x -> x.paymentInfo(null));
        groupsFactory.createActualGroupService(postPaidGroup, postpaidProduct);

        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(passportUid),
                        Option.of(postPaidGroup), new SelectionContext()).isAvailable()
        );
        Assert.assertFalse(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(passportUid),
                        Option.of(freeGroup), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__noGroup() {
        PassportUid passportUid = PassportUid.cons(123456);

        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.of(passportUid),
                        Option.empty(), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__noUser() {
        Assert.assertTrue(
                productLineSelectorFactory.prepaidSelector().isAvailable(line, Option.empty(),
                        Option.empty(), new SelectionContext()).isAvailable()
        );
    }

    @Test
    public void isAvailable__missingConflictProductNoUserAvailable() {
        Assert.assertTrue(
                productLineSelectorFactory.missingConflictUserProductInLineSelector(productLineSelectorFactory.availableSelector())
                        .isAvailable(line, Option.empty(), Option.empty(), new SelectionContext())
                        .isAvailable()
        );
    }

    @Test
    public void isAvailable__missingConflictProductNoUserUnavailable() {
        Assert.assertFalse(
                productLineSelectorFactory.missingConflictUserProductInLineSelector(productLineSelectorFactory.unavailableSelector())
                        .isAvailable(line, Option.empty(), Option.empty(), new SelectionContext())
                        .isAvailable()
        );
    }

    @Test
    public void isAvailable__missingConflictProductUserWithoutProduct() {
        Assert.assertTrue(
                productLineSelectorFactory.missingConflictUserProductInLineSelector(productLineSelectorFactory.availableSelector())
                        .isAvailable(line, Option.of(uid), Option.empty(), new SelectionContext())
                        .isAvailable()
        );
    }

    @Test
    public void isAvailable__missingConflictProductUserProductConflict() {
        String bucket = "1";

        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.addProductsToBucket(bucket, userProduct.getId());
        psBillingProductsFactory.addUserProductToProductSet(PRODUCT_SET, userProduct);

        UserProductEntity userProductOtherBucket = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.addProductsToBucket(bucket, userProductOtherBucket.getId());
        psBillingUsersFactory.createUserService(userProduct.getId(), x -> x.uid(uid.toString()));


        Assert.assertTrue(
                productLineSelectorFactory.missingConflictUserProductInLineSelector(productLineSelectorFactory.availableSelector())
                        .isAvailable(line, Option.of(uid), Option.empty(), new SelectionContext())
                        .isAvailable()
        );
    }

    @Test
    public void isAvailable__missingConflictProductUserProductNotConflict() {
        String bucket_1 = "1";
        String bucket_2 = "1";

        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.addProductsToBucket(bucket_1, userProduct.getId());
        psBillingProductsFactory.addUserProductToProductSet(PRODUCT_SET, userProduct);

        UserProductEntity userProductOtherBucket = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.addProductsToBucket(bucket_2, userProductOtherBucket.getId());
        psBillingUsersFactory.createUserService(userProduct.getId(), x -> x.uid(uid.toString()));

        Assert.assertTrue(
                productLineSelectorFactory.missingConflictUserProductInLineSelector(productLineSelectorFactory.availableSelector())
                        .isAvailable(line, Option.of(uid), Option.empty(), new SelectionContext())
                        .isAvailable()
        );
    }

    @Before
    public void setup() {
        featureFlags.getPrepaidProducts().setValue("true");
        this.prepaidProduct = psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        this.postpaidProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        this.line = psBillingProductsFactory.createProductLine(PRODUCT_SET);
    }
}
