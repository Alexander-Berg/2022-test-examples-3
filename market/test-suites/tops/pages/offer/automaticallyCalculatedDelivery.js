import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

// suites
import AutomaticallyCalculatedDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
import AutomaticallyCalculatedDeliveryDisclaimerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDeliveryDisclaimer';
// page-objects
import Delivery from '@self/platform/spec/page-objects/widgets/parts/OfferSummary/Delivery';
import AutomaticallyCalculatedDeliveryDisclaimer from '@self/platform/spec/page-objects/AutomaticallyCalculatedDeliveryDisclaimer';

import {state} from '@self/platform/spec/hermione/fixtures/delivery/automaticallyCalculated';

export default makeSuite('Автоматический расчёт сроков и стоимости доставки', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            await this.browser.setState('report', state);
            await this.browser.yaOpenPage('touch:offer', {offerId: 1});
        },
        'Сниппет.': prepareSuite(AutomaticallyCalculatedDeliverySuite, {
            meta: {
                id: 'm-touch-2735',
                issue: 'MOBMARKET-11865',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery);
                },
            },
            params: {
                expectedText: '≈ 420 ₽ доставка, 3-4 дня',
            },
        }),
        'Дисклеймер.': prepareSuite(AutomaticallyCalculatedDeliveryDisclaimerSuite, {
            meta: {
                id: 'm-touch-2738',
                issue: 'MOBMARKET-11865',
            },
            pageObjects: {
                disclaimer() {
                    return this.createPageObject(AutomaticallyCalculatedDeliveryDisclaimer);
                },
            },
        }),
    }),
});
