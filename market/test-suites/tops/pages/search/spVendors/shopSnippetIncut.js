import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';

import {PROMOTION_URL, shopIncutSnippetState} from '@self/project/src/spec/hermione/fixtures/spVendors';

// suites
import IncutSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/incutSnippet';

// page-objects
import SearchSnippet from '@self/platform/spec/page-objects/containers/SearchSnippet';
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';

export default makeSuite('Магазинная сниппетная врезка.', {
    environment: 'kadavr',
    story: mergeSuites(

        {
            async beforeEach() {
                this.browser.setState('report', shopIncutSnippetState);
                return this.browser.yaOpenPage('touch:list', routes.catalog.phones);
            },
        },

        prepareSuite(IncutSnippetSuite, {
            pageObjects: {
                snippet() {
                    return this.createPageObject(SearchSnippet);
                },
                snippetCartButton() {
                    return this.createPageObject(SearchSnippetCartButton);
                },
            },
            params: {
                url: PROMOTION_URL,
            },
        })
    ),
});
