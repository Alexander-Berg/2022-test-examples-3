import {makeSuite, prepareSuite} from 'ginny';

import DeliveryTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/__text';
import AutomaticallyCalculatedDeliveryDisclaimerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/automaticallyCalculatedDeliveryDisclaimer';

import LegalInfo from '@self/root/src/components/LegalInfo/__pageObject';
import Delivery from '@self/platform/spec/page-objects/components/DeliveryInfo';
import TopOffers from '@self/platform/spec/page-objects/widgets/content/TopOffers';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';

import {route, state} from '@self/platform/spec/hermione/fixtures/delivery/automaticallyCalculated';

export default makeSuite('Автоматический расчёт сроков и стоимости доставки', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.setState(
                'report',
                state
            );

            await this.browser.yaOpenPage(
                'market:product',
                route
            );
        },
        'Дефолтный оффер.': prepareSuite(DeliveryTextSuite, {
            meta: {
                id: 'marketfront-3368',
                issue: 'MARKETVERSTKA-33871',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: ProductDefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: '≈ 420 ₽ курьером, 3-4 дня',
            },
        }),
        'Топ 6.': prepareSuite(DeliveryTextSuite, {
            meta: {
                id: 'marketfront-3368',
                issue: 'MARKETVERSTKA-33871',
            },
            pageObjects: {
                delivery() {
                    return this.createPageObject(Delivery, {
                        parent: `${TopOffers.item}:first-child`,
                    });
                },
            },
            params: {
                expectedText: '≈ 420 ₽ курьером, 3-4 дня',
            },
        }),
        'Дисклеймер.': prepareSuite(AutomaticallyCalculatedDeliveryDisclaimerSuite, {
            meta: {
                id: 'marketfront-3367',
                issue: 'MARKETVERSTKA-33868',
            },
            pageObjects: {
                shopsInfo() {
                    return this.createPageObject(LegalInfo);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                },
            },
        }),
    },
});
