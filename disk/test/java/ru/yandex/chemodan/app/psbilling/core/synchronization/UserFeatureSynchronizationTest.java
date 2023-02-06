package ru.yandex.chemodan.app.psbilling.core.synchronization;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Vec3;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.app.psbilling.core.dao.features.UserServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.entities.features.UserFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.features.UserServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.misc.log.log4j2.appender.RequestIdAppenderUtils;
import ru.yandex.misc.log.reqid.RequestIdStack;
import ru.yandex.misc.regex.Pattern2;
import ru.yandex.misc.test.Assert;

public class UserFeatureSynchronizationTest extends FeatureSynchronizationTest
        <String, UserServiceFeature, UserServiceFeatureDao.InsertData, UserFeature, UserServiceFeatureDao> {

    @Override
    protected UserServiceFeature createFeature(String uid, FeatureEntity featureEntity) {
        return createFeature(uid, featureEntity, 20);
    }

    @Override
    protected UserServiceFeature createFeature(String uid, FeatureEntity featureEntity, int amount) {
        return psBillingUsersFactory
                .createUserServiceFeature(featureEntity, b -> b.amount(BigDecimal.valueOf(amount)), b -> b.uid(uid));
    }

    @Override
    protected UserServiceFeature createFeature(ProductFeatureEntity productFeatureEntity) {
        return psBillingUsersFactory.createUserServiceFeature(productFeatureEntity, x -> x.uid(getOwner()));
    }

    @Override
    protected ProductFeatureEntity createProductFeature(FeatureEntity featureEntity, int amount) {
        UserProductEntity product = psBillingProductsFactory.createUserProduct();
        return psBillingProductsFactory.createProductFeature(product.getId(), featureEntity,
                b -> b.amount(BigDecimal.valueOf(amount))
        );
    }

    @Override
    protected void synchronize(String uid, FeatureEntity feature) {
        userFeaturesActualizationService.synchronize(uid, feature.getId());
    }

    @Override
    protected String getSentIdPattern() {
        return "#{uid}";
    }

    @Override
    protected String getSentIdValue(UserServiceFeature issuedFeature) {
        return issuedFeature.getUid();
    }

    @Override
    protected UserServiceFeature refresh(UserServiceFeature issuedFeature) {
        return userServiceFeatureDao.findById(issuedFeature.getId());
    }

    @Override
    protected String getOwner() {
        return "123";
    }

    @Override
    protected String getSecondOwner() {
        return "321";
    }

    @Test
    public void testUserFeatureToggleLogging() {
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.TOGGLEABLE);
        UserServiceFeature userFeature = createFeature(getOwner(), feature);

        Function0<Vec3<String>> synchronizeWithLogsCapturing = () -> {
            RequestIdStack.Handle ridHandle = RequestIdStack.push();
            try {
                return Pattern2.compile("action=(.*), feature=(.*), user=(.*),\n")
                        .matcher3Groups(RequestIdAppenderUtils.logOfInvocation(
                                () -> synchronize(userFeature.getOwnerId(), feature)));
            } finally {
                ridHandle.popSafely();
            }
        };
        Assert.equals(
                new Vec3<>("activation", feature.getCode(), userFeature.getUid()),
                synchronizeWithLogsCapturing.apply());

        serviceFeatureDao.setTargetState(Cf.list(userFeature.getId()), Target.DISABLED);
        Assert.equals(
                new Vec3<>("deactivation", feature.getCode(), userFeature.getUid()),
                synchronizeWithLogsCapturing.apply());
    }

//
//    @Test
//    public void shouldSetProductTemplateField2() {
//        String uid = "123";
//        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE, b -> b
//                .activationRequestTemplate(postUrlEncoded("http://some.yandex.ru/activate/#{uid}"))
//        );
//        mockPostOk(Mockito.startsWith("http://some.yandex.ru/activate/"));
//
//        UserProductEntity product = psBillingProductsFactory.createUserProduct();
//
//        ProductTemplateEntity productTemplate = psBillingProductsFactory.createProductTemplate(product
//        .getCodeFamily());
//        ProductTemplateFeatureEntity productTemplateFeature =
//                psBillingProductsFactory.createProductTemplateFeature(productTemplate.getId(), feature);
//
//        UserServiceFeature serviceFeature =
//                psBillingUsersFactory.createUserServiceFeature(productTemplateFeature, b1 -> b1.uid(uid));
//
//        assertHasTemplateFeature(serviceFeature);
//    }
}
