import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import AutomaticallyCalculatedDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
import AutomaticallyCalculatedDeliveryDisclaimerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDeliveryDisclaimer';
import OfferSummaryDelivery from '@self/platform/spec/page-objects/widgets/parts/OfferSummary/Delivery';
import AutomaticallyCalculatedDeliveryDisclaimer from '@self/platform/spec/page-objects/AutomaticallyCalculatedDeliveryDisclaimer';
import {state, route} from '@self/platform/spec/hermione/fixtures/delivery/automaticallyCalculated';

export default makeSuite('Автоматический расчёт сроков и стоимости доставки', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            await this.browser.setState('report', state);
            await this.browser.yaOpenPage('touch:product', route);
        },
        'Сниппет.': prepareSuite(AutomaticallyCalculatedDeliverySuite, {
            meta: {
                id: 'm-touch-2734',
                issue: 'MOBMARKET-11865',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(OfferSummaryDelivery);
                },
            },
            params: {
                expectedText: /Курьером, [а-я0-9\s-]+ — ≈ 420 ₽/i,
                matchMode: true,
            },
        }),
        'Дисклеймер.': prepareSuite(AutomaticallyCalculatedDeliveryDisclaimerSuite, {
            meta: {
                id: 'm-touch-2737',
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
