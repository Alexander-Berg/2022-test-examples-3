import assert from 'assert';
import {makeCase, makeSuite} from 'ginny';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import {genericBundle} from '@self/root/src/spec/hermione/configs/checkout/items';

import {Price} from '@self/root/src/uikit/components/Price/__pageObject';
import {OrderItems} from '@self/root/src/containers/OrderDetails/OrderItems/__pageObject';
import OrderItem from '@self/root/src/components/OrderItem/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotal/__pageObject';


module.exports = makeSuite('Товар + подарок', {
    feature: 'Товар + подарок',
    issue: 'bluemarket-9121',
    id: 'BLUEMARKET-3131',
    environment: 'testing',
    story: {
        async beforeEach() {
            const {orders} = await this.browser.allure.runStep(
                'Оформляем заказ с подарком',
                () => createOrder.call(this)
            );
            const orderId = orders[0].id;

            await this.browser.yaOpenPage(this.params.pageId, {
                orderId,
            });

            this.setPageObjects({
                orderItems: () => this.createPageObject(OrderItems, {
                    parent: `[data-order-id="${orderId}"]`,
                }),
                primaryItem: () => this.createPageObject(OrderItem, {
                    root: `${OrderItem.root}[data-sku-id="${genericBundle.primary.skuId}"]`,
                    parent: this.orderItems,
                }),
                primaryPrice: () => this.createPageObject(Price, {
                    parent: this.primaryItem,
                }),
                giftItem: () => this.createPageObject(OrderItem, {
                    root: `${OrderItem.root}[data-sku-id="${genericBundle.gift.skuId}"]`,
                    parent: this.orderItems,
                }),
                giftPrice: () => this.createPageObject(Price, {
                    parent: this.giftItem,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: `[data-order-id="${orderId}"]`,
                }),
            });

            await this.orderItems.waitForVisible();
        },
        'В списке товаров присутствует основной товар и подарок': makeCase({
            async test() {
                await this.orderItems.items
                    .then(items => items.value.length)
                    .should.eventually.be.equal(2, 'В заказе должно быть два элемента');

                await this.primaryItem.isVisible()
                    .should.eventually.be.equal(true, 'Основной товар должен отображаться');

                await this.giftItem.isVisible()
                    .should.eventually.be.equal(true, 'Подарок должен отображаться');
            },
        }),
        'Цена размазана (итоговая сумма равна стоимости основного товара без подарка)': makeCase({
            async test() {
                const {primary, gift} = genericBundle;
                assert(primary.initialPrice === primary.buyerPrice + gift.buyerPrice,
                    'В тестовых данных правильная скидка (сумма равна стоимости основного товара без подарка)');

                await this.primaryPrice.getPriceValue()
                    .should.eventually.be.equal(primary.buyerPrice,
                        'Стоимость основного товара в заказе должна быть верной');
                await this.giftPrice.getPriceValue()
                    .should.eventually.be.equal(gift.buyerPrice,
                        'Стоимость подарка в заказе должна быть верной');
            },
        }),
        'Присутствует поле “Скидка за товары” с корректной скидкой': makeCase({
            async test() {
                const {primary, gift} = genericBundle;
                assert(primary.initialPrice === primary.buyerPrice + gift.buyerPrice,
                    'В тестовых данных правильная скидка (сумма равна стоимости основного товара без подарка)');

                await this.orderTotal.getPriceValue()
                    .should.eventually.be.equal(primary.initialPrice,
                        'Стоимость заказа должна равняться исходной цене товара (подарок - бесплатно)');
                await this.orderTotal.getDiscount()
                    .should.eventually.be.equal(gift.initialPrice,
                        'Скидка должна равняться исходной цене подарка');
            },
        }),
    },
});

function createOrder() {
    return this.browser.yaScenario(this, prepareOrder, {
        region: this.params.region,
        orders: [{
            items: [genericBundle.primary, genericBundle.gift]
                .map(({skuId, offerId}) => ({
                    skuId,
                    offerId,
                })),
            deliveryType: 'DELIVERY',
        }],
        paymentType: 'POSTPAID',
        paymentMethod: 'CASH_ON_DELIVERY',
        bundleId: 'test',
    });
}
