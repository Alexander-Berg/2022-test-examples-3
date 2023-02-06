import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createOffer} from '@yandex-market/kadavr/mocks/Report/helpers';

import PromocodeSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/promocode';
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';
import OfferDealPopup from '@self/platform/spec/page-objects/OfferDealPopup';

import {offerMock, offerPromoTypePromocode} from '../fixtures/offer.mock';

export default makeSuite('Акционный бейдж.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Акция "Промокод за покупку"', {
            story: prepareSuite(PromocodeSuite, {
                hooks: {
                    async beforeEach() {
                        const offer = createOffer({...offerMock, promos: [offerPromoTypePromocode]}, offerMock.wareId);
                        await this.browser.setState('report', offer);
                        return this.browser.yaOpenPage('touch:list', {
                            nid: offerMock.navnodes[0].id,
                            slug: offerMock.navnodes[0].slug,
                        });
                    },
                },
                meta: {
                    id: 'm-touch-2467',
                    issue: 'MOBMARKET-10326',
                },
                params: {
                    expectedText: 'Промокод\n–\n1 000₽',
                },
                pageObjects: {
                    snippetPrice() {
                        return this.createPageObject(SearchSnippetPrice);
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(OfferDealPopup);
                    },
                },
            }),
        })
    ),
});
