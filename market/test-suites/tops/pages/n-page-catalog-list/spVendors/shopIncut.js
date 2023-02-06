import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';

import {PROMOTION_URL, shopIncutState} from '@self/project/src/spec/hermione/fixtures/spVendors';

// suites
import IncutSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/incutSnippet';

// page-objects
import PremiumOffersGallery from '@self/platform/components/PremiumOffersGallery/__pageObject';
import OfferCard from '@self/platform/components/PremiumOffersGallery/OfferCard/__pageObject';

export default makeSuite('Магазинная врезка.', {
    environment: 'kadavr',
    story: mergeSuites(

        {
            async beforeEach() {
                this.browser.setState('report', shopIncutState);
                return this.browser.yaOpenPage('market:list', routes.list.phones);
            },
        },

        prepareSuite(IncutSnippetSuite, {
            pageObjects: {
                snippet() {
                    return this.createPageObject(OfferCard, {parent: PremiumOffersGallery.root});
                },
            },
            params: {
                url: PROMOTION_URL,
            },
        })
    ),
});
