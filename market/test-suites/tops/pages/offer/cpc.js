import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {cpcOffer, OFFER_ID, encryptedUrl} from '@self/platform/spec/hermione/fixtures/offer';
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';
import OfferSummaryCpc from '@self/platform/spec/hermione/test-suites/blocks/OfferSummary/cpc';

export default makeSuite('CPC Оффер', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', cpcOffer);

                return this.browser.yaOpenPage('touch:offer', {
                    offerId: OFFER_ID,
                });
            },
        },

        prepareSuite(OfferSummaryCpc, {
            meta: {
                id: 'm-touch-3433',
                issue: 'MARKETFRONT-25903',
            },
            params: {
                url: encryptedUrl,
            },
            pageObjects: {
                offerSummary() {
                    return this.createPageObject(OfferSummary);
                },
            },
        })
    ),
});
