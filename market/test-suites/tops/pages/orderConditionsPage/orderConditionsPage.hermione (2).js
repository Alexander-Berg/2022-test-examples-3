import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {commonParams} from '@self/project/src/spec/hermione/configs/params';

import OrderConditionsPage from '@self/root/src/widgets/pages.touch/OrderConditionsPage/__pageObject';
// TODO: @prvlv починить тесты
// import DeliveryCalculator from '@self/root/src/widgets/content/DeliveryCalculator/components/View/__pageObject';
import FrequentQuestions from '@self/root/src/widgets/content/FrequentQuestions/components/View/__pageObject';
import PayNowOrLater from '@self/root/src/widgets/content/PayNowOrLater/components/View/__pageObject';

import orderConditionsPageCmsMarkup from
    '@self/project/src/spec/hermione/fixtures/orderConditions/orderConditionsPageCmsMarkup';

// TODO: @prvlv починить тесты
// Там же учесть новый тарифы
// + починить пути внутри
// import deliveryCalculator from '@self/platform/spec/hermione/test-suites/blocks/orderConditions/deliveryCalculator';
import payNowOrLater from '@self/platform/spec/hermione/test-suites/blocks/orderConditions/payNowOrLater';
import frequentQuestions from '@self/platform/spec/hermione/test-suites/blocks/orderConditions/frequentQuestions';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Условия доставки', {
    environment: 'kadavr',
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    orderConditionsPage: () => this.createPageObject(OrderConditionsPage),
                    // TODO: @prvlv починить тесты
                    // deliveryCalculator: () =>
                    //     this.createPageObject(DeliveryCalculator),
                    frequentQuestions: () =>
                        this.createPageObject(FrequentQuestions),
                    payNowOrLater: () =>
                        this.createPageObject(PayNowOrLater),
                });
            },
        },

        // TODO: @prvlv починить тесты
        // prepareSuite(deliveryCalculator, {}),
        prepareSuite(payNowOrLater, {
            hooks: {
                beforeEach() {
                    return commonHook.call(this);
                },
            },
        }),
        prepareSuite(frequentQuestions, {
            hooks: {
                beforeEach() {
                    return commonHook.call(this);
                },
            },
        })
    ),
});

async function commonHook() {
    if (this.getMeta('environment') === 'kadavr') {
        await this.browser.setState(
            'Tarantino.data.result',
            [orderConditionsPageCmsMarkup]
        );
    }

    await this.browser.yaOpenPage('market:order-conditions');
}
