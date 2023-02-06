package ru.yandex.chemodan.app.psbilling.core.billing.users;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;

@RunWith(SpringJUnit4ClassRunner.class)
public class BillingActionsReportingServiceTest extends AbstractPsBillingCoreTest {

    @Autowired
    BillingActionsReportingService billingActionsReportingService;

    @Test
    public void compatibilityTests() {
        UserProductPriceEntity priceE = psBillingProductsFactory.createUserProductPrice();
        UserProductPrice price = userProductManager.findPrice(priceE.getId());
        billingActionsReportingService.builder(BillingActionsReportingService.Action.PROLONG_AUTO)
                .status("success")
                .order(psBillingOrdersFactory.createOrder(uid))
                .userProductPrice(price)
                .userProduct(price.getPeriod().getUserProduct())
                .finish();
        System.out.println(PurchaseReportingServiceMockConfiguration.resHolder.get(0));
    }

    /*


AbstractSubscriptionProcessor
"action: prolong_auto, status: success, order_id: {}, uid: {}, product_code: {}, " +
                            "period: {}, price: {}, currency: {}, old_expiration_date:{}, new_expiration_date: {}, package_name: {}, " +
                            "service_billing_status: {}"
action: prolong_auto, status: success, order_id: 86071, uid: 405701328, product_code: mail_pro_b2c_premium1000_inapp_google, period: 1Y, price: 1390, currency: RUB, old_expiration_date:2022-06-14T21:34:15.000Z, new_expiration_date: 2022-06-15T21:04:39.000Z, package_name: ru.yandex.disk, service_billing_status: PAID


InappSubscriptionProcessor.java
action: order_upgraded, status: success, order_id: {}, uid: {}, product_code: {}," +
                                " period: {}, price: {}, currency: {}, package_name: {}, new_expiration_date: {}"
action: order_upgraded, status: success, order_id: 328907, uid: 681085036, product_code: mail_pro_b2c_standard100_inapp_apple_for_disk, period: 1M, price: 69, currency: RUB, package_name: ru.yandex.disk, new_expiration_date: 2022-07-14T09:44:48.000Z

"action: buy_new, status: success, order_id: {}, uid: {}, product_code: {}, period: {}, " +
                                "price: {}, currency: {}, package_name: {}, new_expiration_date: {}",
action: buy_new, status: success, order_id: 318757, uid: 310580935, product_code: mail_pro_b2c_standard100_inapp_apple_for_disk, period: 1M, price: 69, currency: RUB, package_name: ru.yandex.disk, new_expiration_date: 2022-07-10T20:31:08.000Z

TrustSubscriptionProcessor.java
"action: process order, status: {}, status_code: {}, status_description: {}, " +
                            "order_id: {}, uid: {}, subscription: {}, period: {}, price: {}, currency: {}, " +
                            "package_name: {}"
action: process order, status: started, status_code: null, status_description: null, order_id: 06b8d705-ce8a-4352-b0ac-203b1595faad, uid: 561672775, subscription: SubscriptionResponse[orderId=06b8d705-ce8a-4352-b0ac-203b1595faad, productId=PS_BILLING_mail_pro_b2c_premium200_v20220414_exp3_year_subs, subscriptionPeriod=1Y, subscriptionPeriodCount=0, subscriptionUntil=<null>, finishTime=<null>, paymentIds=[5a423a3bb25f85ef76ca5697b9ee6b9a], regionId=225, currentAmount={}, subscriptionState=2, createdAt=<null>, graceIntervals=<null>, holdIntervals=<null>, otherAttributes={uid=561672775, order_ts=1655242045.336, product_type=subs, current_qty=0.00, developer_payload={\\\"tariff_name\\\": \\\"\\\\u041f\\\\u0440\\\\u0435\\\\u043c\\\\u0438\\\\u0443\\\\u043c 200 \\\\u0413\\\\u0411 \\\\u043d\\\\u0430 \\\\u0433\\\\u043e\\\\u0434\\\", \\\"blocks_visibility\\\": {\\\"header\\\": false}, \\\"title\\\": \\\"\\\\u042f\\\\u043d\\\\u0434\\\\u0435\\\\u043a\\\\u0441 360\\\"}, product_name=PS_BILLING_mail_pro_b2c_premium200_v20220414_exp3_year_subs, parent_service_product_id=PS_BILLING_mail_pro_b2c_premium200_v20220414_exp3_year_app, ready_to_pay=1}, status=success, statusCode=<null>, statusDesc=<null>], period: 1Y, price: null, currency: None, package_name: null

"action: buy_new, status: payment_failed, order_id: {}, uid: {}, product_code: {}, period: {}, " +
                        "error_code: {}, error_message: {}, package_name: {}"
action: buy_new, status: payment_failed, order_id: e29e0113-cdd9-41fc-a7db-087e17a5e716, uid: 4063835710, product_code: mail_pro_b2c_standard100_discount20_v20210525, period: UserProductPeriod[entity=UserProductPeriodEntity[code=mail_pro_b2c_standard100_discount20_v20210525_month_subs, period=1M, userProductId=da25ce24-7685-4da2-a349-33ab71c51a50, trustFiscalTitle=Some(Яндекс 360 Стандарт 100 ГБ на месяц (скидка -20%)), packageName=None, startPeriodDuration=None, startPeriodCount=None, id=2fc2aa92-c178-4fe4-96c9-d3b8b0d517fa, createdAt=2021-05-27T17:28:16.696Z], productProvider=ru.yandex.bolts.function.Function0$1@63dcaeed, pricesProvider=ru.yandex.bolts.function.Function$1@79204a64], error_code: payment_timeout, error_message: timeout while waiting for payment data, package_name: null

"action: {}, status: success, order_id: {}, uid: {}, product_code: {}, period: {}, price: {}, " +
                        "currency: {}, old_order_id: {}, old_service_id: {}, new_expiration_date: {}, package_name: {}",
action: buy_new, status: success, order_id: f0e0316c-c7f0-4052-b45d-16e9c5c42ef1, uid: 41145159, product_code: mail_pro_b2c_premium200_v20220414_exp3, period: 1Y, price: 1290, currency: RUB, old_order_id: null, old_service_id: null, new_expiration_date: 2023-06-14T21:40:40.000Z, package_name: null

TrustRefundService.java
 "action: refund, status: failed, order_id: {}, refund_id: {}, uid: {}, product_code: {}, period: {}, " +
                        "price: {}, currency: {}, package_name: {}"
N/A
"action: refund, status: success, order_id: {}, refund_id: {}, uid: {}, product_code: {}, " +
                            "period: {}, price: {}, currency: {}, package_name: {}"
action: refund, status: success, order_id: a61f7d1a-7175-451c-bb5e-9eea757fb8f8, refund_id: 62a907b04f5c6e029196e5d7, uid: 1005710826, product_code: mail_pro_b2c_premium1000_v20220414_exp3, period: UserProductPeriod[entity=UserProductPeriodEntity[code=PS_BILLING_mail_pro_b2c_premium1000_v20220414_exp3_year_subs, period=1Y, userProductId=11a915da-238e-4f1b-ae3f-0268c410d5bd, trustFiscalTitle=Some(Яндекс 360 Премиум 1 ТБ на год), packageName=None, startPeriodDuration=None, startPeriodCount=None, id=ad652792-7c30-4149-be26-9bf2b4e82018, createdAt=2022-04-27T14:40:58.567Z], productProvider=ru.yandex.bolts.function.Function0$1@48a7aae, pricesProvider=ru.yandex.bolts.function.Function$1@4e8baa09], price: 2290, currency: RUB, package_name: null

UserServiceManager.java
"action: unsubscribe, status: success, order_id: {}, uid: {}, product_code: {}, period: {}, " +
                        "price: {}, currency: {}, package_name: {}"
N/A
     */
}
