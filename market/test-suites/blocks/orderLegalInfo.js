import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {asus, dropship} from '@self/root/src/spec/hermione/configs/checkout/items';
import {blueCookie} from '@self/root/src/spec/hermione/configs/suppliers';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';

import BeruDeliveryLegalInfo from '@self/root/src/components/SelfLegalInfo/__pageObject';

import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES, DELIVERY_PARTNERS} from '@self/root/src/constants/delivery';

const ID = 11111;

export default makeSuite('Страница заказа с юридической информацией.', {
    params: {
        items: 'Товары',
        paymentType: 'Тип оплаты',
        paymentMethod: 'Метод оплаты',
    },
    feature: 'Алкоголь, Фарма, Дропшип',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    beruDelivery: () => this.createPageObject(BeruDeliveryLegalInfo, {parent: this.suppliersBody}),
                });

                await this.browser.setState('ShopInfo', {collections: {supplierInfo: {id: blueCookie.supplierId}}});

                const result = await this.browser.yaScenario(this, prepareOrder, {
                    region: this.params.region,
                    orders: [{
                        items: this.params.items,
                        deliveryType: this.params.deliveryType,
                        delivery: {
                            deliveryPartnerType: this.params.deliveryPartnerType,
                        },
                    }],
                    paymentType: this.params.paymentType,
                    paymentMethod: this.params.paymentMethod,
                    status: ORDER_STATUS.DELIVERED,
                    fulfilment: this.params.fulfilment,
                });

                const orderId = result.orders[0].id;
                return this.browser.yaOpenPage(
                    PAGE_IDS_COMMON.INFO_BY_ORDER,
                    {orderId}
                );
            },

            'с обычным товаром': makeCase({
                id: 'bluemarket-2707',
                issue: 'BLUEMARKET-6892',
                environment: 'kadavr',
                defaultParams: {
                    items: [{
                        skuId: asus.skuId,
                        offerId: asus.offerId,
                        supplierId: blueCookie.supplierId,
                        count: 1,
                        id: ID,
                    }],
                    paymentType: PAYMENT_TYPE.PREPAID,
                    paymentMethod: PAYMENT_METHOD.YANDEX,
                    deliveryPartnerType: DELIVERY_PARTNERS.YANDEX_MARKET,
                    deliveryType: DELIVERY_TYPES.DELIVERY,
                    fulfilment: true,
                },
                async test() {
                    await this.beruDelivery.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Блок "Доставку выполняет Яндекс.Маркет" должен быть видим'
                        );
                },
            }),

            'с дропшип товаром': makeCase({
                id: 'bluemarket-2707',
                issue: 'BLUEMARKET-6892',
                environment: 'kadavr',
                defaultParams: {
                    items: [{
                        skuId: dropship.skuId,
                        offerId: dropship.offerId,
                        count: 1,
                        id: ID,
                    }],
                    paymentType: PAYMENT_TYPE.POSTPAID,
                    paymentMethod: PAYMENT_METHOD.CASH_ON_DELIVERY,
                    deliveryPartnerType: DELIVERY_PARTNERS.SHOP,
                    deliveryType: DELIVERY_TYPES.PICKUP,
                    fulfilment: false,
                },
                async test() {
                    await this.beruDelivery.isExisting()
                        .should.eventually.be.equal(
                            false,
                            'Блок "Доставку выполняет Яндекс.Маркет" должен отсутствовать'
                        );
                },
            }),
        }
    ),
});
