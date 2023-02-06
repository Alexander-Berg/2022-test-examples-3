package ru.yandex.chemodan.app.psbilling.core.billing.users;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderStatus;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.chemodan.mpfs.MpfsUser;
import ru.yandex.chemodan.mpfs.UserBlockedException;
import ru.yandex.chemodan.mpfs.UserNotInitializedException;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.chemodan.util.exception.PermanentHttpFailureException;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.io.http.HttpException;
import ru.yandex.misc.io.http.HttpStatus;
import ru.yandex.misc.test.Assert;

import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice.DEFAULT_REGION;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserBillingServiceTest extends AbstractPaymentTest {

    private UserProductPrice price;
    @Autowired
    private MpfsClient mpfsClient;

    @Before
    public void initStubs() {
        featureFlags.getSubscriptionOrderUseCreateIfNotExists().resetValue();
        featureFlags.getCheckTrustOrderId().resetValue();

        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(builder -> {
            builder.billingType(BillingType.TRUST);
            builder.code("TEST");
            builder.allowAutoProlong(true);
            builder.titleTankerKeyId(psBillingTextsFactory.create().getId());
            builder.singleton(true);
            builder.trustServiceId(Option.of(111));
            return builder;
        });
        psBillingTextsFactory.loadTranslations();
        price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        psBillingProductsFactory
                .createUserProductPrices(price.getPeriod().getId(), b -> b.regionId(DEFAULT_REGION));

        psBillingProductsFactory.addUserProductToProductSet("set", userProduct.getId());
        initStubs(price);
    }

    @Test
    public void testInitPaymentWithSubscriptionOrderUseOnlyCreate() {
        featureFlags.getSubscriptionOrderUseCreateIfNotExists().setValue("true");
        testInitPayment();
    }

    @Test
    public void testInitPaymentWithSubscriptionOrderUseOnlyCreateOrderPaid() {
        featureFlags.getSubscriptionOrderUseCreateIfNotExists().setValue("true");

        String trustProductId = price.getPeriod().getTrustProductId(true);

        Order order = createOrder(trustProductId, Option.empty());
        psBillingOrdersFactory.createOrUpdateOrder(order, o -> o.status(Option.of(OrderStatus.PAID)));

        Mockito.doReturn(order.getTrustOrderId())
                .when(trustClient)
                .createSubscription(any());

        Order expected = createOrder(trustProductId, Option.empty());
        Assert.equals(expected.getStatus(), OrderStatus.PAID);
    }

    @Test
    public void testInitPaymentWithOrderTerminalStatusesCheckTrustOrderId() {
        featureFlags.getCheckTrustOrderId().setValue("true");

        String trustProductId = price.getPeriod().getTrustProductId(true);
        Order order = createOrder(trustProductId, Option.empty());

        for (OrderStatus status : OrderStatus.TERMINAL_STATUSES) {
            psBillingOrdersFactory.createOrUpdateOrder(order, o -> o.status(Option.of(status)));

            Mockito.doReturn(order.getTrustOrderId())
                    .when(trustClient)
                    .createSubscription(any());

            Assert.assertThrows(
                    () -> createOrder(trustProductId, Option.empty()),
                    A3ExceptionWithStatus.class,
                    (e) -> e.getHttpStatusCode() == HttpStatus.SC_400_BAD_REQUEST);
        }
    }

    @Test
    public void testValidCurrency() {
        UserProductPrice usdPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(price.getPeriod().getId(),
                        x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD")).getId());
        String trustProductId = price.getPeriod().getTrustProductId(true);
        psBillingProductsFactory.addProductsToBucket("bucket", usdPrice.getPeriod().getUserProductId());
        psBillingUsersFactory.createUserService(userId, usdPrice);

        Assert.equals(createOrder(trustProductId, Option.empty()).getUserProductPriceId(), usdPrice.getId());
        Assert.equals(createOrder(trustProductId, Option.of("USD")).getUserProductPriceId(), usdPrice.getId());
        Assert.equals(createOrder(trustProductId, Option.of("RUB")).getUserProductPriceId(), usdPrice.getId());
    }

    @Test
    public void testInitPayment() {
        UserProductPrice usdPrice = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(price.getPeriod().getId(),
                        x -> x.regionId(UserProductPrice.DEFAULT_REGION).currencyCode("USD")).getId());
        String trustProductId = price.getPeriod().getTrustProductId(true);

        Assert.equals(createOrder(trustProductId, Option.empty()).getUserProductPriceId(), price.getId());
        Assert.equals(createOrder(trustProductId, Option.of("USD")).getUserProductPriceId(), usdPrice.getId());
        Assert.assertThrows(() -> createOrder(trustProductId, Option.of("EUR")), NoSuchElementException.class);
    }

    private boolean testIsDiskProEnableWithHttpError(Exception e) {
        PassportUid uid = PassportUid.cons(1);
        Mockito.when(mpfsClient.getFeatureToggles(any(MpfsUser.class))).thenThrow(e);
        return billingService.isDiskProEnable(uid);
    }

    @Test
    public void testDiskProEnable400() {
        boolean result = testIsDiskProEnableWithHttpError(new UserNotInitializedException(MpfsUser.of(1)));
        Assert.assertFalse(result);
    }

    @Test
    public void testDiskProEnable403() {
        boolean result = testIsDiskProEnableWithHttpError(new UserBlockedException(MpfsUser.of(1)));
        Assert.assertFalse(result);
    }

    @Test
    public void testDiskProEnable404() {
        boolean result = testIsDiskProEnableWithHttpError(new PermanentHttpFailureException("some", 420));
        Assert.assertFalse(result);
    }

    @Test
    public void testDiskProEnable5xxRetries() {
        PassportUid uid = PassportUid.cons(1);
        Mockito.when(mpfsClient.getFeatureToggles(any(MpfsUser.class)))
                .thenThrow(new HttpException())
                .thenReturn(Cf.map("disk_pro", true));

        boolean result = billingService.isDiskProEnable(uid);
        Assert.assertTrue(result);
    }


}
