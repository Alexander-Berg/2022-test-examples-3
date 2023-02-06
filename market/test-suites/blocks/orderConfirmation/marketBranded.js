import {makeSuite, makeCase} from 'ginny';

import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import OrderDelivery from '@self/root/src/widgets/parts/OrderConfirmation/components/OrderDelivery/__pageObject';
import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

/**
 * Время прибытия заказа на точку самовывоза на странице спасибо.
 */
// eslint-disable-next-line import/no-commonjs
export default testPalmId => makeSuite('Состав заказа', {
    feature: 'Время прибытия заказа на точку самовывоза',
    environment: 'kadavr',
    issue: 'MARKETPROJECT-53152',
    story: {
        beforeEach() {
            this.setPageObjects({
                orderConfirmation: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                }),
                orderDelivery: () => this.createPageObject(OrderDelivery, {
                    parent: this.confirmationPage,
                }),
            });

            return createOrders.call(this);
        },

        'Вывод даты доставки для брендированного ПВЗ': makeCase({
            id: testPalmId,
            async test() {
                const regularText = 'Доставка Яндекса в пятницу, 23 февраля к 12:00';

                await this.orderDelivery.getDeliveryText()
                    .should.eventually.to.be.contain(
                        regularText,
                        `Информация о самовывозе обычного заказа должна быть ${regularText}`
                    );
            },
        }),
    },
});

async function createOrders() {
    const orders = [{
        orderId: 1111,
        delivery: {
            deliveryPartnerType: 'YANDEX_MARKET',
            type: 'PICKUP',
            dates: {
                fromDate: '23-02-2024',
                toDate: '23-02-2024',
                toTime: '12:00',
            },
            outletId: yandexMarketPickupPoint.id,
        },
    }];

    const state = {
        data: {
            results: [yandexMarketPickupPoint],
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
