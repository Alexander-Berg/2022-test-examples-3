import {makeSuite, makeCase} from 'ginny';

import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import OrderDelivery from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderDelivery/__pageObject';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import outlet1 from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/outlet1';
import outlet2 from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/outlet2';

/**
 * Тесты на мультизаказ с дропшипом на странице спасибо.
 */
// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Дропшип + обычный заказ.', {
    feature: 'Дропшип',
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                orderConfirmation: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                }),
                firstOrderDelivery: () => this.createPageObject(OrderDelivery, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.firstDelivery} ${OrderDelivery.root}`,
                }),

                secondOrderDetails: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery}`,
                }),
                secondOrderDelivery: () => this.createPageObject(OrderDelivery, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery} ${OrderDelivery.root}`,
                }),
            });

            return createOrders.call(this);
        },

        'Вывод даты доставки для дропшипа и обычного заказа.': makeCase({
            id: 'bluemarket-2677',
            issue: 'BLUEMARKET-6922',
            async test() {
                const regularText = 'Доставка Яндекса 13–14 ноября';

                await this.firstOrderDelivery.getDeliveryText()
                    .should.eventually.to.be.equal(
                        regularText,
                        `Информация о самовывозе обычного заказа должна быть ${regularText}`
                    );

                const dropshipText = 'Самовывоз в субботу, 11 ноября';

                await this.secondOrderDelivery.getDeliveryText()
                    .should.eventually.to.be.equal(
                        dropshipText,
                        `Информация о самовывозе дропшип-заказа должна быть ${dropshipText}`
                    );
            },
        }),
    },
});

async function createOrders() {
    const regularOrderId = 1111;
    const dropshipOrderId = 2222;

    const orders = [{
        orderId: regularOrderId,
        delivery: {
            deliveryPartnerType: 'YANDEX_MARKET',
            type: 'PICKUP',
            dates: {
                fromDate: '13-11-2000',
                toDate: '14-11-2000',
            },
            outletId: outlet1.id,
        },
    }, {
        orderId: dropshipOrderId,
        delivery: {
            deliveryPartnerType: 'SHOP',
            type: 'PICKUP',
            dates: {
                fromDate: '11-11-2000',
                toDate: '11-11-2000',
            },
            outletId: outlet2.id,
        },
    }];

    const state = {
        data: {
            results: [
                outlet1,
                outlet2,
            ],
            search: {results: []},
        },
    };
    await this.browser.yaScenario(
        this,
        setReportState,
        {state}
    );

    return this.browser.yaScenario(this, prepareThankPage, {
        orders,
    });
}
