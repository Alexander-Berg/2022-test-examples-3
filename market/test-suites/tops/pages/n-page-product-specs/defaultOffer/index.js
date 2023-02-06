import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import CPCDefaultOfferMiniSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOfferMini/cpc';
import CPADefaultOfferMiniSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOfferMini/cpa';

// page-objects
import DefaultOfferMini from '@self/platform/components/DefaultOfferMini/__pageObject';

import {
    stateWithCpcDefaultOffer,
    stateWithCpaDefaultOffer,
    routeParams,
} from '@self/platform/spec/hermione/fixtures/defaultOffer';

const route = 'market:product-spec';

export default makeSuite('Характиристики. Дефолтный оффер.', {
    story: mergeSuites(
        prepareSuite(CPCDefaultOfferMiniSuite, {
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(DefaultOfferMini);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', stateWithCpcDefaultOffer);

                    return this.browser.yaOpenPage(route, routeParams);
                },
            },
        }),
        prepareSuite(CPADefaultOfferMiniSuite, {
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(DefaultOfferMini);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', stateWithCpaDefaultOffer);

                    return this.browser.yaOpenPage(route, routeParams);
                },
            },
        })
    ),
});
