import {prepareSuite, makeSuite} from 'ginny';

import GenericBundleTermSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/genericBundle';
import {prepareKadavrReportStateWithOffers} from '@self/project/src/spec/hermione/fixtures/genericBundle';
import DealsSticker from '@self/platform/spec/page-objects/DealsSticker';
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';

export default makeSuite('Подарок', {
    environment: 'kadavr',
    story: prepareSuite(GenericBundleTermSuite, {
        pageObjects: {
            cartButton() {
                return this.createPageObject(SearchSnippetCartButton);
            },
            dealsSticker() {
                return this.createPageObject(DealsSticker);
            },
        },
        meta: {
            id: 'marketfront-4284',
        },
        hooks: {
            async beforeEach() {
                const {
                    stateWithOffers,
                    primary,
                } = prepareKadavrReportStateWithOffers();

                await this.browser.setState('report', stateWithOffers);
                await this.browser.setState('Carter.items', []);

                return this.browser.yaOpenPage('touch:list', {
                    nid: primary.navnodes[0].id,
                    slug: primary.navnodes[0].slug,
                });
            },
        },
    }),
});

