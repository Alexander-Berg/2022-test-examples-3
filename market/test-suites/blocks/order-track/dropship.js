import {
    makeCase,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
// eslint-disable-next-line no-restricted-imports
import * as _ from 'lodash';
import {outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/alcohol';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';

import {TrackTitle} from '@self/root/src/widgets/content/orders/OrderTrack/components/OrderTrack/__pageObject';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';

import {DELIVERY_PARTNER_TYPE} from '@self/root/src/entities/delivery';

/**
 * Тесты на фарме на трекинг заказа
 * @param {PageObject.OrderTrack} orderTrack - страница трекинга
 */
export default makeSuite('Дропшип.', {
    id: 'bluemarket-647',
    issue: 'BLUEMARKET-3637',
    feature: 'Дропшип',
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    trackTitle: () => this.createPageObject(TrackTitle, {parent: this.orderTrack}),
                });

                return createOrder.call(this, {isDropship: true});
            },

            'По умолчанию': {
                'заголовок содержит текст "Будет ждать вас в торговом зале"': makeCase({
                    async test() {
                        await this.trackTitle.isVisible()
                            .should.eventually.to.be.equal(true, 'Заголовок должен быть виден');

                        const text = 'Будет ждать вас в торговом зале';
                        await this.trackTitle.getText()
                            .should.eventually.to.have.string(text, `Заголовок должен содержать текст "${text}"`);
                    },
                }),
            },
        }
    ),
});

async function createOrder({isDropship}) {
    let deliveryPartnerType = DELIVERY_PARTNER_TYPE.YANDEX_MARKET;
    if (isDropship) {
        deliveryPartnerType = DELIVERY_PARTNER_TYPE.SHOP;
    }

    const state = mergeState([
        {
            data: {
                results: [
                    outletMock,
                ],
                search: {results: []},
            },
        },
    ]);

    await this.browser.yaScenario(this, setReportState, {state});

    const orders = await this.browser.yaScenario(
        this,
        prepareOrder,
        {
            region: this.params.region,
            orders: [{
                items: [{
                    skuId: checkoutItemIds.dropship.skuId,
                }],
                deliveryType: 'PICKUP',
                delivery: {
                    deliveryPartnerType,
                },
            }],
            fulfilment: !isDropship,
            paymentType: 'POSTPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
        }
    );

    const order = _.get(orders, ['orders', 0]);
    const orderId = _.get(order, ['id']);

    return this.browser.yaOpenPage(PAGE_IDS_COMMON.ORDERS_TRACK, {orderId});
}
