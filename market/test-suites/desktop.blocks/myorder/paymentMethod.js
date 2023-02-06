import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import OrderDetailsPrice from '@self/root/src/components/Orders/OrderDetailsPrice/__pageObject';

const {
    makeCase,
    makeSuite,
} = require('ginny');
// eslint-disable-next-line no-restricted-modules
const _ = require('lodash');


/**
 * Тесты на способы оплаты постоплатного заказа.
 * @param {PageObject.MyOrderDetails} myOrderDetailsPrice - информация о заказе.
 */

module.exports = makeSuite('Информация о способе оплаты.', {
    feature: 'Заказ',
    environment: 'kadavr',
    story: {
        'Способ оплаты': {
            beforeEach() {
                this.setPageObjects({
                    myOrderDetailsTitle: () =>
                        this.createPageObject(OrderDetailsPrice, {parent: this.myOrder}),
                });
            },
            'при оплате заказа картой при получении': {
                beforeEach() {
                    return createOrder.call(this, 'POSTPAID', 'CARD_ON_DELIVERY');
                },

                'должен иметь текст "Картой при получении"': makeCase({
                    id: 'bluemarket-157',
                    issue: 'BLUEMARKET-1742',
                    test() {
                        return this.myOrderDetailsTitle.getPaymentMethod()
                            .should.eventually.to.be.equal(
                                'Картой при получении',
                                'Способ оплаты должен быть "Картой при получении"'
                            );
                    },
                }),
            },

            'при оплате заказа наличными': {
                beforeEach() {
                    return createOrder.call(this, 'POSTPAID', 'CASH_ON_DELIVERY');
                },

                'должен иметь текст "Наличными при получении"': makeCase({
                    id: 'bluemarket-2240',
                    issue: 'BLUEMARKET-1742',
                    test() {
                        return this.myOrderDetailsTitle.getPaymentMethod()
                            .should.eventually.to.be.equal(
                                'Наличными при получении',
                                'Способ оплаты должен быть "Наличными при получении"'
                            );
                    },
                }),
            },

            'при оплате заказа банковской картой': {
                beforeEach() {
                    return createOrder.call(this, 'PREPAID', 'YANDEX');
                },

                'должен иметь текст "Картой онлайн"': makeCase({
                    id: 'bluemarket-160',
                    issue: 'BLUEMARKET-1742',
                    test() {
                        return this.myOrderDetailsTitle.getPaymentMethod()
                            .should.eventually.to.be.equal(
                                'Картой онлайн',
                                'Способ оплаты должен быть "Картой онлайн"'
                            );
                    },
                }),
            },
        },
    },
});

function createOrder(paymentType, paymentMethod) {
    const {browser} = this;

    return browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status: ORDER_STATUS.PROCESSING,
            substatus: ORDER_SUBSTATUS.SHIPPED,
            region: this.params.region,
            orders: [{
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'DELIVERY',
                delivery: {
                    dates: {
                        fromDate: '23-02-2024',
                        toDate: '24-02-2024',
                    },
                    features: [],
                },
            }],
            paymentType,
            paymentMethod,
        }
    )
        .then(response => {
            if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                const orderId = _.get(response, ['orders', 0, 'id']);

                return this.browser.yaOpenPage(this.params.pageId, {orderId});
            }

            return this.browser.yaOpenPage(this.params.pageId);
        });
}
