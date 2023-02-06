import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
// eslint-disable-next-line no-restricted-imports
import * as _ from 'lodash';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

import OrderTotal from '@self/root/src/components/OrderTotal/__pageObject';
import SupplierString from '@self/root/src/components/SupplierString/__pageObject';
import Link from '@self/root/src/components/Link/__pageObject';
import {OrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';
import {OrderInfo} from '@self/root/src/components/OrderInfo/__pageObject';
import {OrderHeader} from '@self/root/src/components/OrderHeader/__pageObject';
import {OrderInfoLine} from '@self/root/src/components/OrderInfo/OrderInfoLine/__pageObject';

/**
 * Тесты на фарме на карточке заказа для списка заказов
 * @param {PageObject.OrderCard} orderCard - карточка заказа
 */
module.exports = makeSuite('Дропшип.', {
    feature: 'Дропшип',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal, {parent: this.orderCard}),
                    supplierString: () => this.createPageObject(SupplierString, {parent: this.orderCard}),
                    supplierStringLink: () => this.createPageObject(Link, {parent: this.supplierString}),
                    orderHeader: () => this.createPageObject(OrderHeader),
                    orderStatus: () => this.createPageObject(OrderStatus),
                    orderInfo: () => this.createPageObject(OrderInfo),
                    deliveryInfo: () => this.createPageObject(OrderInfoLine, {
                        parent: this.orderInfo,
                        root: '[data-auto="delivery-info"]',
                    }),
                });
            },
            'Саммари по заказу': {
                beforeEach() {
                    return createOrder.call(this, {
                        status: 'PROCESSING',
                    });
                },
                'должно иметь пункт "Товары"': makeCase({
                    id: 'bluemarket-647',
                    issue: 'BLUEMARKET-3637',
                    environment: 'kadavr',
                    async test() {
                        await this.orderTotal.isVisible()
                            .should.eventually.to.be.equal(true, 'Саммари должно быть отображено');

                        await this.orderTotal.getItemsText()
                            .should.eventually.to.have.string(
                                'Товары',
                                'Пункт с товарами должен иметь подпись "Товары"'
                            );
                    },
                }),
                'не должен иметь пункт "Доставка"': makeCase({
                    id: 'bluemarket-2718',
                    issue: 'BLUEMARKET-5989',
                    environment: 'kadavr',
                    async test() {
                        await this.orderTotal.isDeliveryVisible()
                            .should.eventually.to.be.equal(false, 'пункта "Доставка" не должно быть');
                    },
                }),
            },
            'Строка со ссылкой на информацию о продавце': {
                beforeEach() {
                    return createOrder.call(this, {
                        status: 'PROCESSING',
                    });
                },
                'должна отображаться, при клике открывать страницу инфо о продавце': makeCase({
                    id: 'bluemarket-647',
                    issue: 'BLUEMARKET-3637',
                    environment: 'kadavr',
                    async test() {
                        await this.supplierString.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Строка со ссылкой на информацию о продавце должна быть видна'
                            );

                        await this.supplierStringLink.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Ссылка на информацию о продавце должна быть видна'
                            );

                        await this.supplierStringLink.click();

                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/suppliers/info-by-order',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
            'В статусе PICKUP': {
                beforeEach() {
                    return createOrder.call(this, {
                        status: 'PICKUP',
                        substatus: 'PICKUP_SERVICE_RECEIVED',
                        rgb: 'BLUE',
                    });
                },
                'статус заказа "Можно получить"': makeCase({
                    id: 'bluemarket-2718',
                    issue: 'BLUEMARKET-5989',
                    environment: 'kadavr',
                    async test() {
                        await this.orderStatus.getText().should.eventually.to.be.equal('Можно получить',
                            'Статус должен быть "Можно получить"');
                    },
                }),
            },
            'Пункт о доставке': {
                beforeEach() {
                    return createOrder.call(this, {
                        status: 'DELIVERED',
                    });
                },
                'отображается как "Пункт выдачи"': makeCase({
                    id: 'bluemarket-2718',
                    issue: 'BLUEMARKET-5989',
                    environment: 'kadavr',
                    async test() {
                        await this.deliveryInfo.getTitleText()
                            .should.eventually.to.be.equal('Пункт выдачи',
                                'Пункт должен иметь заголовок "Пункт выдачи"');
                    },
                }),
            },
        }
    ),
});

async function createOrder({status, substatus, rgb}) {
    const isKadavr = this.getMeta('environment') === 'kadavr';
    const {browser} = this;
    const outletId = isKadavr ? generateRandomId() : undefined;
    const supplierId = isKadavr ? generateRandomId() : undefined;

    const orders = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            substatus,
            rgb,
            region: this.params.region,
            orders: [{
                items: [{
                    skuId: checkoutItemIds.dropship.skuId,
                    supplierId,
                }],
                deliveryType: 'PICKUP',
                outletId,
                // Признак фармы start
                delivery: {
                    deliveryPartnerType: 'SHOP',
                },
                // Признак фармы end
            }],
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
            // Признак фармы start
            fulfilment: false,
            // Признак фармы end
        }
    );

    const order = _.get(orders, ['orders', 0]);
    const orderId = _.get(order, ['id']);

    return this.browser.yaOpenPage(this.params.pageId, {orderId});
}
