package ru.yandex.chemodan.app.psbilling.core.billing.users;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function0;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.InappStore;
import ru.yandex.chemodan.app.psbilling.core.entities.products.BillingType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.ReceiptProcessResult;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.TrialService;
import ru.yandex.chemodan.app.psbilling.core.mocks.TrustClientMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProduct;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPeriod;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.mpfs.MpfsClient;
import ru.yandex.chemodan.trust.client.InappStoreType;
import ru.yandex.chemodan.trust.client.responses.AppleReceiptResponse;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.chemodan.trust.client.responses.ProcessInappReceiptResponse;
import ru.yandex.chemodan.util.exception.A3ExceptionWithStatus;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
public class ReceiptHandlingTests extends AbstractPsBillingCoreTest {
    @Autowired
    private TrustClientMockConfiguration trustClientMockConfiguration;
    private final PassportUid uid = PassportUid.MAX_VALUE;

    @Autowired
    private ReceiptHandlerV2 receiptHandlerV2;
    @Autowired
    private MpfsClient mpfsClient;
    @Autowired
    FeatureFlags featureFlags;
    @Autowired
    TrialService trialService;

    @Before
    public void setUp() throws Exception {
        featureFlags.getAllowUserGetBoughtInappProduct().setValue("true");
    }

    @Test
    public void testReceipt_EmptyReceipt_NoSubscription() {
        Option<Product> existProduct = Option.empty();

        testEmptyReceipt(InappStore.APPLE_APPSTORE, "ru.yandex.mail",
                existProduct,
                processInappReceipt -> Assert.isEmpty(processInappReceipt.apply().getOrders()));
    }

    @Test
    public void testReceipt_EmptyReceipt_SameProductSubscription() {
        Option<Product> existProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();

        testEmptyReceiptError(InappStore.APPLE_APPSTORE, "ru.yandex.mail",
                existProduct, "multiuser_store");
    }

    @Test
    public void testReceipt_EmptyReceipt_DifferentStoreSubscription() {
        Option<Product> existProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();
        testEmptyReceiptError(InappStore.GOOGLE_PLAY, "ru.yandex.mail",
                existProduct, "wrong_app");
    }

    @Test
    public void testReceipt_EmptyReceipt_B2BSubscription() {
        Option<Product> existProduct = new Product("org_disk_default_v1", "", BillingType.GROUP, uid).option();

        testEmptyReceipt(InappStore.APPLE_APPSTORE, "ru.yandex.mail",
                existProduct,
                processInappReceipt -> Assert.isEmpty(processInappReceipt.apply().getOrders()));
    }

    @Test
    public void testReceipt_NewReceipt_NoSubscription() {
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        Option<Product> existProduct = Option.empty();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                });
    }

    @Test
    public void testReceipt_NewHoldReceipt_NoSubscription() {
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        receiptProduct.get().setSubscriptionState(InappSubscriptionState.ON_HOLD);
        Option<Product> existProduct = Option.empty();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(0, orders.length());
                });
    }

    @Test
    public void testReceipt_NewReceipt_NoActiveSubscription() {
        Product oldProduct = new Product("light", "ru.yandex.mail", BillingType.INAPP_APPLE, uid);

        UserProduct product = createProduct(oldProduct);
        UserServiceEntity userService = psBillingUsersFactory.createUserService(product.getId(),
                x -> x.packageName(Option.of("ru.yandex.mail")).uid(uid.toString()));
        userServiceManager.disableService(userService.getId());


        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        Option<Product> existProduct = Option.empty();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                });
    }

    @Test
    public void testReceipt_NewReceipt_SameProductSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "multiuser_store");
    }

    @Test
    public void testReceipt_Upgrade() {
        String trustOrderId = "someId";
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.of(trustOrderId)).option();
        Option<Product> existProduct = new Product("premium1000", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                true, true, Option.of(trustOrderId)).option();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                    Order order = orders.get(0);
                    String orderProductCode =
                            userProductManager.findPrice(order.getUserProductPriceId()).getPeriod().getUserProduct().getCode();
                    Assert.equals(receiptProduct.get().code, orderProductCode);
                });
    }

    @Test
    public void testReceipt_NewReceipt_DifferentPackageNameSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.disk", BillingType.INAPP_APPLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "wrong_app");
    }

    @Test
    public void testReceipt_NewReceipt_DifferentStoreSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.mail", BillingType.INAPP_GOOGLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "wrong_app");
    }

    @Test
    public void testReceipt_NewReceipt_B2CSubscription() {
        Option<Product> existProduct = new Product("mail_pro_b2c_premium10000", "", BillingType.TRUST, uid,
                true, false, false, Option.empty()).option();
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                true, false, false, Option.of("trust_id")).option();

        UserProduct product;
        product = createProduct(existProduct.get(), "mail360_web");
        psBillingProductsFactory.addUserProductToProductSet("web", product.getId());
        psBillingProductsFactory.addProductSetToBucket("mail360_web", "web");
        psBillingUsersFactory.createUserService(product.getId(), x -> x.uid(uid.toString()));

        product = createProduct(receiptProduct.get(), "mail360_inapp");
        psBillingProductsFactory.addUserProductToProductSet("inapp", product.getId());
        psBillingProductsFactory.addProductSetToBucket("mail360_inapp", "inapp");

        // new service will not conflict
        testReceipt(receiptProduct, Option.empty(),
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                });
    }


    @Test
    public void testReceipt_CurrentUserReceipt_SameProductSubscription() {
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                true, true, Option.empty()).option();
        Option<Product> existProduct = Option.empty();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                });
    }

    @Test
    public void testReceipt_CurrentUserReceipt_DifferentPackageNameSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE, uid, true
                , true, Option.empty()).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.disk", BillingType.INAPP_APPLE, uid).option();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                    Assert.equals(receiptProduct.get().getTrustOrderId(), orders.get(0).getTrustOrderId());
                });
    }

    @Test
    public void testReceipt_CurrentUserReceipt_DifferentStoreSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_GOOGLE, uid,
                true, true, Option.empty()).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();

        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ListF<Order> orders = processInappReceipt.apply().getOrders();
                    Assert.equals(1, orders.length());
                    Assert.equals(receiptProduct.get().getTrustOrderId(), orders.get(0).getTrustOrderId());
                });
    }

    @Test
    public void testReceipt_OtherUserReceipt_NoSubscription() {
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE,
                PassportUid.MIN_VALUE).option();
        Option<Product> existProduct = Option.empty();

        testReceiptError(receiptProduct, existProduct, "multiuser_yandex");
    }

    @Test
    public void testReceipt_OtherUserReceipt_SameProductSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE,
                PassportUid.MIN_VALUE).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "multiuser_store");
    }

    @Test
    public void testReceipt_OtherUserReceipt_DifferentApplicationSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_APPLE,
                PassportUid.MIN_VALUE).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.disk", BillingType.INAPP_APPLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "wrong_app");
    }

    @Test
    public void testReceipt_OtherUserReceipt_DifferentStoreSubscription() {
        Option<Product> receiptProduct = new Product("standard1", "ru.yandex.mail", BillingType.INAPP_GOOGLE,
                PassportUid.MIN_VALUE).option();
        Option<Product> existProduct =
                new Product("standard2", "ru.yandex.mail", BillingType.INAPP_APPLE, uid).option();

        testReceiptError(receiptProduct, existProduct, "wrong_app");
    }

    @Test
    public void testReceipt_MpfsProduct() {
        Option<Product> receiptProduct = new Product("mpfs_product", "ru.yandex.disk", BillingType.INAPP_APPLE,
                uid, false, false, false, Option.empty()).option();
        Option<Product> existProduct = Option.empty();

        testReceipt(receiptProduct, existProduct, processInappReceipt -> {
            ListF<Order> orders = processInappReceipt.apply().getOrders();
            Assert.equals(0, orders.length());
        });
        Mockito.verify(mpfsClient, Mockito.times(1))
                .processInappReceipt(Mockito.eq(receiptProduct.get().uid),
                        Mockito.eq(receiptProduct.get().receiptPackageName),
                        Mockito.eq(InappStoreType.APPLE_APPSTORE.getMpfsValue()), Mockito.eq("RUB"), Mockito.eq(
                                "some_receipt"));

        // если есть активная услуга - тоже проблем при обработке старых чеков быть не должно
        Product mail360product = new Product("light", "ru.yandex.disk", BillingType.INAPP_APPLE, uid);
        UserProduct product = createProduct(mail360product);
        psBillingUsersFactory.createUserService(product.getId(),
                x -> x.packageName(Option.of("ru.yandex.disk")).uid(uid.toString()));

        Mockito.clearInvocations(mpfsClient);
        testReceipt(receiptProduct, existProduct, processInappReceipt -> {
            ListF<Order> orders = processInappReceipt.apply().getOrders();
            Assert.equals(0, orders.length());
        });
        Mockito.verify(mpfsClient, Mockito.times(1))
                .processInappReceipt(Mockito.eq(receiptProduct.get().uid),
                        Mockito.eq(receiptProduct.get().receiptPackageName),
                        Mockito.eq(InappStoreType.APPLE_APPSTORE.getMpfsValue()), Mockito.eq("RUB"), Mockito.eq(
                                "some_receipt"));
    }

    @Test
    public void trialUsedIfRawAppstoreReceiptHaveTrialRecordForExistProduct() {
        testAppstoreTrialStatus(Option.of(true), true, true);
    }

    @Test
    public void TrialNotUsedIfRawAppstoreReceiptHaveTrialRecordForNotExistProduct() {
        testAppstoreTrialStatus(Option.of(false), true, false);
    }

    @Test
    public void trialNotUsedIfRawAppstoreReceiptHaveNoTrialRecord() {
        testAppstoreTrialStatus(Option.of(false), false, true);
    }

    @Test
    public void trialNotSetRawAppstoreReceiptIsEmpty() {
        testAppstoreTrialStatus(Option.empty(), false, true, true);
    }

    @Test
    public void trialNotUsedForAppleIfReceiptIsEmpty() {
        Option<Product> receiptProduct = Option.empty();
        testReceiptCore(InappStore.APPLE_APPSTORE,
                "",
                receiptProduct, Option.empty(),
                processInappReceipt -> {
                    ReceiptProcessResult orders = processInappReceipt.apply();
                    Assert.equals(Option.of(false), orders.getTrialUsed());
                });
    }

    @Test
    public void trialNotUsedForGooglePlayIfNoBoundProducts() {
        testGoogleInappTrial(false, false);
    }

    @Test
    public void trialUsedForGooglePlayIfBoundTrialProducts() {
        testGoogleInappTrial(true, true);
    }

    private void testGoogleInappTrial(boolean expectedTrial, boolean serviceExists) {
        Option<Product> receiptProduct = new Product("standard1", "",
                BillingType.INAPP_GOOGLE, uid, serviceExists, serviceExists, Option.empty(), true).option();

        testReceiptCore(InappStore.GOOGLE_PLAY,
                "",
                receiptProduct, Option.empty(),
                processInappReceipt -> {
                    ReceiptProcessResult orders = processInappReceipt.apply();
                    Assert.equals(Option.of(expectedTrial), orders.getTrialUsed());
                });
    }

    private void testAppstoreTrialStatus(Option<Boolean> expectedTrial, Boolean rawReceiptTrial,
                                         boolean productExists) {
        testAppstoreTrialStatus(expectedTrial, rawReceiptTrial, productExists, false);
    }

    private void testAppstoreTrialStatus(Option<Boolean> expectedTrial, Boolean rawReceiptTrial,
                                         boolean productExists, boolean trustEmptyResponse) {
        Option<Product> receiptProduct = new Product("standard", "ru.yandex.mail", BillingType.INAPP_APPLE, uid,
                false, false, Option.empty()).option();
        receiptProduct.get().setAppleRawReceipt(new AppleRawReceipt(rawReceiptTrial, productExists,
                trustEmptyResponse));
        Option<Product> existProduct = Option.empty();
        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> {
                    ReceiptProcessResult orders = processInappReceipt.apply();
                    Assert.equals(expectedTrial, orders.getTrialUsed());
                });
    }

    private void testReceiptError(Option<Product> receiptProduct, Option<Product> existProduct,
                                  String exceptionCode) {
        testReceipt(receiptProduct, existProduct,
                processInappReceipt -> Assert.assertThrows(processInappReceipt::apply,
                        A3ExceptionWithStatus.class,
                        ex -> ex.getHttpStatusCode() == 400 && ex.getErrorName().equals(exceptionCode)));
    }

    @SuppressWarnings("SameParameterValue")
    private void testEmptyReceiptError(InappStore store, String packageName,
                                       Option<Product> existProduct,
                                       String exceptionCode) {
        testReceiptCore(store, packageName,
                Option.empty(), existProduct,
                processInappReceipt -> Assert.assertThrows(processInappReceipt::apply,
                        A3ExceptionWithStatus.class,
                        ex -> ex.getHttpStatusCode() == 400 && ex.getErrorName().equals(exceptionCode)));
    }

    @SuppressWarnings("SameParameterValue")
    private void testEmptyReceipt(InappStore store, String packageName,
                                  Option<Product> existProduct,
                                  Consumer<Function0<ReceiptProcessResult>> executionHandler) {
        testReceiptCore(store, packageName,
                Option.empty(), existProduct,
                executionHandler);
    }

    private void testReceipt(Option<Product> receiptProduct,
                             Option<Product> existProduct,
                             Consumer<Function0<ReceiptProcessResult>> executionHandler) {
        testReceiptCore(toInappStoreType(receiptProduct.get().receiptBillingType),
                receiptProduct.get().receiptPackageName,
                receiptProduct, existProduct,
                executionHandler);
    }

    private void testReceiptCore(InappStore store, String packageName,
                                 Option<Product> receiptProductO, Option<Product> existProductO,
                                 Consumer<Function0<ReceiptProcessResult>> executionHandler) {
        Option<String> receipt;
        if (receiptProductO.isPresent()) {
            receipt = Option.of("some_receipt");
            Product receiptProduct = receiptProductO.get();
            Assert.equals(store, toInappStoreType(receiptProduct.receiptBillingType));
            Assert.equals(packageName, receiptProduct.receiptPackageName);
            AppleRawReceipt rawReceipt = receiptProduct.appleRawReceipt;
            if (receiptProduct.createProduct) {
                UserProduct receiptProductInfo = createProduct(receiptProduct);

                ProcessInappReceiptResponse response = mockReceiptResponse(store,
                        receiptProductInfo.getProductPeriods().get(0).getCode(),
                        receiptProduct.trustOrderId, receiptProduct.subscriptionState);
                if (receiptProduct.createOrder) {
                    boundProductToUser(receiptProduct.uid, receiptProductInfo,
                            response.getItems().single().getSubscription().getSubscriptionId(),
                            receiptProduct.createUserService, true);
                }

                String productPeriodCode = rawReceipt.isProductExists
                        ? receiptProductInfo.getProductPeriods().get(0).getCode()
                        : "UNKNOWN CODE";
                mockAppstoreRawReceiptResponse(rawReceipt.isTrialUsed, productPeriodCode);

            } else {
                mockReceiptResponse(store, "NOT_PS_BILLING", receiptProduct.trustOrderId,
                        receiptProduct.subscriptionState);
                mockAppstoreRawReceiptResponse(rawReceipt.isTrialUsed, "UNKNOWN CODE");
            }
            if (rawReceipt.isEmpty) {
                trustClientMockConfiguration.mockCheckAppstoreReceipt(new AppleReceiptResponse(null));
            }
        } else {
            receipt = Option.empty();
        }
        if (existProductO.isPresent()) {
            Product existProduct = existProductO.get();
            UserProduct existProductInfo = createProduct(existProduct);

            boundProductToUser(existProduct.uid, existProductInfo, existProduct.trustOrderId,
                    existProduct.createUserService, existProduct.createOrder);
        }

        executionHandler.accept(() -> receiptHandlerV2.processInappReceipt(uid, store, "RUB", receipt,
                packageName));

    }


    private UserProduct createProduct(Product product) {
        return createProduct(product, "self_bucket");
    }

    private UserProduct createProduct(Product product, String selfBucket) {
        Option<UserProduct> userProductO = userProductManager.findByCodeO(product.code);
        if (userProductO.isNotEmpty()) {
            return userProductO.get();
        }
        Option<UUID> trialId;
        if (product.isTrialProduct() && product.receiptBillingType == BillingType.INAPP_GOOGLE) {
            trialId = Option.of(psBillingProductsFactory.createTrialDefinitionWithPeriod().getId());
        } else {
            trialId = Option.empty();
        }

        UserProductEntity userProduct =
                psBillingProductsFactory.createUserProduct(x -> x.trustServiceId(Option.of(116))
                        .billingType(product.receiptBillingType)
                        .code(product.code)
                        .trialDefinitionId(trialId)
                );
        UserProductPeriodEntity userProductPeriod = psBillingProductsFactory.createUserProductPeriod(userProduct,
                CustomPeriodUnit.TEN_MINUTES, x -> x.packageName(Option.of(product.receiptPackageName)));
        psBillingProductsFactory.createUserProductPrices(userProductPeriod, BigDecimal.TEN);
        psBillingProductsFactory.addProductsToBucket(selfBucket, userProduct.getId());

        return userProductManager.findById(userProduct.getId());
    }

    private void boundProductToUser(PassportUid uid, UserProduct userProduct,
                                    String trustOrderId,
                                    boolean createUserService,
                                    boolean createOrder) {
        userInfoService.findOrCreateUserInfo(uid);
        UserProductPeriod userProductPeriod = userProduct.getProductPeriods().get(0);
        UserProductPrice userProductPrice = userProduct.getProductPrices().getTs(userProductPeriod).get(0);
        if (createOrder) {
            Order order = psBillingOrdersFactory.createOrUpdateOrder(uid, userProductPrice.getId(),
                    trustOrderId);
            if (createUserService) {
                UserServiceEntity userService = psBillingUsersFactory.createUserService(userProduct.getId(),
                        x -> x.packageName(userProductPeriod.getPackageName()).uid(uid.toString()));
                orderDao.onSuccessfulOrderPurchase(order.getId(), Option.of(userService.getId()), 1);
                userProduct.getTrialDefinition().ifPresent(trial -> trialService.findOrCreateTrialUsage(trial, uid));
            }
        }
    }

    private InappStore toInappStoreType(BillingType billingType) {
        return Arrays.stream(InappStore.values())
                .filter(x -> x.getBillingType().equals(billingType))
                .findFirst().orElseThrow(RuntimeException::new);
    }


    private ProcessInappReceiptResponse mockReceiptResponse(InappStore store, String productPeriodCode,
                                                            String trustOrderId,
                                                            InappSubscriptionState subscriptionState) {
        return trustClientMockConfiguration.mockProcessInappReceipt(uid, store, productPeriodCode, trustOrderId,
                x -> x.state(subscriptionState));
    }

    private void mockAppstoreRawReceiptResponse(Boolean trialStatus, String productPeriodCode) {
        trustClientMockConfiguration.mockCheckAppstoreReceipt(new AppleReceiptResponse(new AppleReceiptResponse.Result(
                new AppleReceiptResponse.ReceiptInfo(new AppleReceiptResponse.Receipt(
                        Cf.list(new AppleReceiptResponse.InAppPurchase("any", trialStatus, trialStatus,
                                productPeriodCode))))
        )));
    }


    @Getter
    private static class Product {

        private final String code;
        private final String receiptPackageName;
        private final BillingType receiptBillingType;
        private final PassportUid uid;
        private final boolean createProduct;
        private final boolean createUserService;
        private final boolean createOrder;
        private final String trustOrderId;
        private final boolean trialProduct;
        @Setter
        private InappSubscriptionState subscriptionState = InappSubscriptionState.ACTIVE;
        @Setter
        private AppleRawReceipt appleRawReceipt = new AppleRawReceipt(false, true, false);

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid) {
            this(code, receiptPackageName, receiptBillingType, uid, false);
        }

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid,
                       boolean trialProduct) {
            this(code, receiptPackageName, receiptBillingType, uid, true, true, true, Option.empty(), trialProduct);
        }

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid,
                       boolean createUserService, boolean createOrder, Option<String> trustOrderId) {
            this(code, receiptPackageName, receiptBillingType, uid, createUserService, createOrder, trustOrderId,
                    false);
        }

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid,
                       boolean createUserService, boolean createOrder, Option<String> trustOrderId,
                       boolean trialProduct) {
            this(code, receiptPackageName, receiptBillingType, uid, true, createUserService, createOrder,
                    trustOrderId, trialProduct);
        }

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid,
                       boolean createProduct, boolean createUserService, boolean createOrder,
                       Option<String> trustOrderId) {
            this(code, receiptPackageName, receiptBillingType, uid, createProduct, createUserService, createOrder,
                    trustOrderId, false);
        }

        public Product(String code, String receiptPackageName, BillingType receiptBillingType, PassportUid uid,
                       boolean createProduct, boolean createUserService, boolean createOrder,
                       Option<String> trustOrderId, boolean trialProduct) {
            this.code = code;
            this.receiptPackageName = receiptPackageName;
            this.receiptBillingType = receiptBillingType;
            this.uid = uid;
            this.createProduct = createProduct;
            this.createUserService = createUserService;
            this.createOrder = createOrder;
            this.trustOrderId = trustOrderId.orElse(UUID.randomUUID().toString());
            this.trialProduct = trialProduct;
        }

        public Option<Product> option() {
            return Option.of(this);
        }
    }

    @AllArgsConstructor
    private static class AppleRawReceipt {
        private final boolean isTrialUsed;
        private final boolean isProductExists;
        private final boolean isEmpty;
    }
}
