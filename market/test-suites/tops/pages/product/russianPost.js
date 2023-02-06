import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import OfferCondition from '@self/platform/components/OfferCondition/__pageObject';
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
import DefaultOfferRussianPostSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer/russianPost';

import {
    buildProductWithDefaultOffer,
    ROUTE,
} from './fixtures/productWithRussianPost';


export default makeSuite('Дефолтный оффер', {
    environment: 'kadavr',
    feature: 'Почта России',
    story: mergeSuites({
        'Содержимое': mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        productOffers() {
                            return this.createPageObject(ProductOffers);
                        },
                        defaultOffer() {
                            return this.createPageObject(DefaultOffer, {
                                parent: this.productOffers,
                            });
                        },
                        offerCondition() {
                            return this.createPageObject(OfferCondition, {
                                parent: this.defaultOffer,
                            });
                        },
                    });

                    const state = buildProductWithDefaultOffer();

                    await this.browser.setState('report', state);
                    await this.browser.yaOpenPage('touch:product', ROUTE);

                    await this.defaultOffer.waitForVisible();
                },
            },

            prepareSuite(DefaultOfferRussianPostSuite, {
                meta: {
                    id: 'm-touch-3106',
                    issue: 'MARKETFRONT-6448',
                },
            })
        ),
    }),
});
