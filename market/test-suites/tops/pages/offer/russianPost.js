import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {russianPostOffer, OFFER_ID} from '@self/platform/spec/hermione/fixtures/offer';
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';
import OfferSummaryRussianPostSuite from '@self/platform/spec/hermione/test-suites/blocks/OfferSummary/russianPost';

export default makeSuite('Почта России', {
    environment: 'kadavr',
    feature: 'Почта России',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', russianPostOffer);

                return this.browser.yaOpenPage('touch:offer', {
                    offerId: OFFER_ID,
                });
            },
        },

        prepareSuite(OfferSummaryRussianPostSuite, {
            meta: {
                id: 'm-touch-3107',
                issue: 'MARKETFRONT-6448',
            },
            pageObjects: {
                offerSummary() {
                    return this.createPageObject(OfferSummary);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.offerSummary.waitForVisible();

                    this.params.selector = await this.offerSummary.getSelector(OfferSummary.clickOutButton);
                },
            },
        })
    ),
});
