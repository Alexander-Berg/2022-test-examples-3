import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
// eslint-disable-next-line no-restricted-imports
import _ from 'lodash';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import {OrderHeader as OrderDetailsHeader} from '@self/root/src/components/OrderHeader/__pageObject';
import {OrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject/index.touch';


/**
 * Тесты на предзака для отдельного заказа
 * @param {PageObject.OrderCard} orderCard - карточка заказа (для списка и для отдельного заказа они разные),
 */
module.exports = makeSuite('Предзаказ.', {
    feature: 'Предзаказ',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    orderDetailsHeader: () => this.createPageObject(OrderDetailsHeader, {parent: this.orderCard}),
                    orderStatus: () => this.createPageObject(OrderStatus, {parent: this.orderCard}),
                });
            },
        },
        {
            'Статус заказа': {
                beforeEach() {
                    return createPreOrder.call(this);
                },
                'по умолчанию': {
                    'должен быть "Оформлен предзаказ"': makeCase({
                        id: 'bluemarket-638',
                        issue: 'BLUEMARKET-3541',
                        environment: 'kadavr',
                        test() {
                            const text = 'Оформлен предзаказ';

                            return this.orderStatus
                                .getText()
                                .should.eventually.to.have.string(
                                    text,
                                    `Текст статуса должен быть "${text}"`
                                );
                        },
                    }),
                },
            },
        }
    ),
});

async function createPreOrder() {
    const {browser} = this;

    const order = await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            region: this.params.region,
            checkoutSkuIds: [checkoutItemIds.preorder.skuId],
            orders: [{
                deliveryType: 'DELIVERY',
            }],
            paymentType: 'PREPAID',
            paymentMethod: 'YANDEX',
            preorder: true,
            status: 'PENDING',
            substatus: 'PREORDER',
        }
    );

    const orderId = _.get(order, ['orders', 0, 'id']);

    return this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDER, {orderId});
}
