import {ORDER_STATUS, ORDER_SUBSTATUS} from '@self/root/src/entities/order';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import MyOrderItems from '@self/root/src/components/Orders/OrderItems/__pageObject';
import MyOrderItem from '@self/root/src/components/Orders/OrderItems/OrderItem/__pageObject';

const {
    makeCase,
    makeSuite,
} = require('ginny');
// eslint-disable-next-line no-restricted-modules
const _ = require('lodash');

const checkoutItemIds = require('@self/root/src/spec/hermione/configs/checkout/items');

/**
 * Тесты на содержимое заказа.
 * @param {PageObject.OrderItems} orderItems - товары в заказе
 */
module.exports = makeSuite('Товары в заказе', {
    feature: 'Оффер',
    environment: 'kadavr',
    story: {
        'Элемент заказа': {
            beforeEach() {
                this.setPageObjects({
                    orderItems: () => this.createPageObject(MyOrderItems, {parent: this.myOrder}),
                });

                const {browser} = this;

                return browser.yaScenario(
                    this,
                    'checkoutResource.prepareOrder',
                    {
                        status: ORDER_STATUS.PROCESSING,
                        substatus: ORDER_SUBSTATUS.SHIPPED,
                        region: this.params.region,
                        orders: [{
                            items: [{
                                skuId: checkoutItemIds.asus.skuId,
                            }],
                            deliveryType: 'DELIVERY',
                            delivery: {
                                dates: {
                                    fromDate: '23-02-2024',
                                    toDate: '24-02-2024',
                                },
                                features: [],
                            },
                        }],
                        paymentType: 'POSTPAID',
                        paymentMethod: 'CASH_ON_DELIVERY',
                    }
                )
                    .then(response => {
                        if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
                            const orderId = _.get(response, ['orders', 0, 'id']);

                            return this.browser.yaOpenPage(this.params.pageId, {orderId});
                        }

                        return this.browser.yaOpenPage(this.params.pageId);
                    });
            },

            'по умолчанию': {
                'должен корректно отображаться': makeCase({
                    id: 'bluemarket-2316',
                    issue: 'BLUEMARKET-1741',
                    async test() {
                        return this.orderItems.waitForVisibleRoot()
                        // проверяем видимость всего блока итемов
                            .then(() => this.orderItems.hasVisibleItems())
                            .should.eventually.to.be.equal(true, 'Элемент заказа должен отображаться')
                            // проверяем кол-во итемов (не всего блока) (раньше n-order-item)
                            .then(() => this.orderItems.items)
                            .then(items => items.value.length)
                            .should.eventually.to.be.equal(1, 'Элемент заказа должен быть единственным');
                    },
                }),
            },
            'при клике на заголовок': {
                beforeEach() {
                    this.setPageObjects({
                        orderItem: () => this.createPageObject(
                            MyOrderItem,
                            {
                                parent: this.orderItems,
                            }
                        ),
                    });
                },
                'должен открывать карточку SKU заказа': makeCase({
                    id: 'bluemarket-1461',
                    issue: 'BLUEMARKET-1741',
                    test() {
                        const {browser} = this;

                        return browser.getTabIds()
                            .then(tabIds => Promise.all([
                                browser.yaWaitForNewTab({startTabIds: tabIds}),
                                this.orderItem.titleLinkClick(),
                            ]))
                            .then(([newTabId]) => this.allure.runStep(
                                'Переключаемся на новую вкладку и проверяем URL',
                                () => browser
                                    .switchTab(newTabId)
                                    .then(() => browser.getUrl())
                                    .should.eventually.be.link({
                                        // В этой регулярке не нужно экранировать слэш, но нужно экранировать \d+
                                        pathname: 'product--[0-9a-z_-]+/\\d+',
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                            ));
                    },
                }),
            },
        },
    },
});
