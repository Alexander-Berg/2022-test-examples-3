
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';

import {creditWidgetSuite, creditWidgetWithoutSuite} from '@self/root/src/spec/hermione/test-suites/blocks/credits';
import {prepareStateCarts} from '@self/root/src/spec/hermione/scenarios/credit';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Кредиты.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaScenario(this, prepareStateCarts);

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.OFFER, {offerId: this.params.item.offer.offerId});

                this.setPageObjects({
                    defaultOffer: () => this.createPageObject(DefaultOffer),
                });
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока с главной инофрмацией об оффере',
                    () => this.defaultOffer.waitForVisible()
                );
            },
        },
        prepareSuite(creditWidgetSuite, {
            meta: {
                id: 'bluemarket-4050',
            },
            params: {
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(creditWidgetWithoutSuite, {
            params: {
                isAuthWithPlugin: true,
            },
        })
    ),
});
