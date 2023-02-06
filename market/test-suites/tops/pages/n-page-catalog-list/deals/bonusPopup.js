import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import PopupSuite from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms/popup';
import DealsTermsSutie from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms';
import DealsTerms from '@self/platform/spec/page-objects/components/DealsTerms';
import BonusDescriptionPopup from '@self/platform/spec/page-objects/components/BonusDescription';

import {bonusTypePromo, offerMock} from '../fixtures/offer.mock';

export const decorateStateWithSearchDefaults = state => {
    const dataMixin = {
        data: {
            search: {
                total: 1,
                totalOffers: 1,
            },
        },
    };

    return mergeState([
        state,
        dataMixin,
    ]);
};

const dealsPopupStories = [{
    description: 'По карте клиента',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promo: bonusTypePromo}, offerMock.wareId);
            await this.browser.setState('report', decorateStateWithSearchDefaults(offer));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
}];

export default makeSuite('Акции в сниппете', {
    story: mergeSuites(
        makeSuite('Попап бонус карта', {
            environment: 'kadavr',
            story: createStories(dealsPopupStories, ({hooks}) => prepareSuite(PopupSuite, {
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms, {parent: this.snippetCard2});
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(BonusDescriptionPopup);
                    },
                },
                hooks,
                meta: {
                    id: 'marketfront-4047',
                    issue: 'MARKETFRONT-9840',
                },
            })),
        }),

        makeSuite('Бонус карта', {
            environment: 'kadavr',
            story: createStories(dealsPopupStories, ({hooks}) => prepareSuite(DealsTermsSutie, {
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms, {parent: this.snippetCard2});
                    },
                },
                hooks,
                meta: {
                    id: 'marketfront-4050',
                    issue: 'MARKETFRONT-10866',
                },
                params: {
                    expectedText: 'по карте клиента',
                },
            })),
        })
    ),
});
