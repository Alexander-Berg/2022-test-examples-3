import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';

import {PROMOTION_URL, shopIncutSnippetState} from '@self/project/src/spec/hermione/fixtures/spVendors';

// suites
import IncutSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/incutSnippet';

// page-objects
import OfferCell from '@self/project/src/components/Search/Snippet/Offer/Cell/__pageObject';

export default makeSuite('Магазинная сниппетная врезка.', {
    environment: 'kadavr',
    story: mergeSuites(

        {
            async beforeEach() {
                this.browser.setState('report', shopIncutSnippetState);
                return this.browser.yaOpenPage('market:list', {
                    ...routes.list.phones,
                    viewtype: 'grid',
                });
            },
        },

        prepareSuite(IncutSnippetSuite, {
            pageObjects: {
                snippet() {
                    return this.createPageObject(OfferCell);
                },
            },
            params: {
                url: PROMOTION_URL,
            },
        })
    ),
});
