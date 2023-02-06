import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import {ActionLink} from '@self/root/src/components/OrderActions/Actions/ActionLink/__pageObject';
import {OrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';

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
                    returnOrderActionLink: () => this.createPageObject(
                        ActionLink,
                        {
                            parent: this.orderCard,
                            root: `${ActionLink.root}${ActionLink.returnLink}`,
                        }
                    ),
                    orderStatus: () => this.createPageObject(OrderStatus),
                });
            },
            'Кнопка "Вернуть"': {
                beforeEach() {
                    return createOrder.call(this, {status: 'DELIVERED'});
                },
                'в статусе DELIVERED': {
                    'должна быть видимой': makeCase({
                        id: 'bluemarket-647',
                        issue: 'BLUEMARKET-3637',
                        environment: 'kadavr',
                        async test() {
                            const returnOrderActionLinkSelector = await this.returnOrderActionLink.getSelector();

                            return this.browser.isVisible(returnOrderActionLinkSelector)
                                .should.eventually.to.be.equal(true, 'Кнопка “Вернуть” должна быть видимой');
                        },
                    }),
                },
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
        }
    ),
});

async function createOrder({status, substatus, rgb} = {}) {
    const {browser} = this;

    await browser.yaScenario(
        this,
        'checkoutResource.prepareOrder',
        {
            status,
            substatus,
            rgb,
            region: this.params.region,
            orders: [{
                items: [{skuId: checkoutItemIds.asus.skuId}],
                deliveryType: 'PICKUP',
                // Признак дропшипа start
                delivery: {
                    deliveryPartnerType: 'SHOP',
                },
                // Признак дропшипа end
            }],
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
            // Признак дропшипа start
            fulfilment: false,
            // Признак дропшипа end
        }
    );

    return this.browser.yaOpenPage(this.params.pageId);
}
