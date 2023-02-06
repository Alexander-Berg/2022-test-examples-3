import {prepareSuite, makeSuite} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import DiscountBadgeSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/discountBadge';
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';

import {offerMock} from '../fixtures/offer.mock';

export default makeSuite('Скидочный бейдж.', {
    environment: 'kadavr',
    story: prepareSuite(DiscountBadgeSuite, {
        hooks: {
            async beforeEach() {
                const offer = createOffer(offerMock, offerMock.wareId);
                await this.browser.setState('report', offer);
                return this.browser.yaOpenPage('touch:list', {
                    nid: offerMock.navnodes[0].id,
                    slug: offerMock.navnodes[0].slug,
                    viewtype: 'grid',
                });
            },
        },
        meta: {
            id: 'm-touch-2469',
            issue: 'MOBMARKET-10328',
        },
        pageObjects: {
            snippetPrice() {
                return this.createPageObject(SearchSnippetPrice);
            },
        },
    }),
});
