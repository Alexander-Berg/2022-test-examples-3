import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import AutomaticallyCalculatedDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
import AutomaticallyCalculatedDeliveryDisclaimerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDeliveryDisclaimer';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import AutomaticallyCalculatedDeliveryDisclaimer from '@self/platform/spec/page-objects/AutomaticallyCalculatedDeliveryDisclaimer';
import {state} from '@self/platform/spec/hermione/fixtures/delivery/automaticallyCalculated';

export default makeSuite('Автоматический расчёт сроков и стоимости доставки', {
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            const routeParams = {
                lr: 213,
                text: 'красный',
                deliveryincluded: 0,
                onstock: 1,
            };

            await this.browser.setState('report', state);
            await this.browser.yaOpenPage('touch:search', routeParams);
        },
        'Сниппет.': prepareSuite(AutomaticallyCalculatedDeliverySuite, {
            meta: {
                id: 'm-touch-2736',
                issue: 'MOBMARKET-11865',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(SearchSnippetDelivery);
                },
            },
            params: {
                expectedText: 'Доставка от продавца',
            },
        }),
        'Дисклеймер.': prepareSuite(AutomaticallyCalculatedDeliveryDisclaimerSuite, {
            meta: {
                id: 'm-touch-2739',
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
