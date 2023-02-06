import {prepareSuite, makeSuite} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import PopupSuite from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms/popup';
import DealsTerms from '@self/platform/spec/page-objects/components/DealsTerms';
import DealsDescriptionPopup from '@self/project/src/components/DealDescription/__pageObject';

import {
    offerMock,
    offerPromoTypePromocode,
    offerPromoTypeNPLusM,
    offerPromoTypeGift,
} from '../fixtures/offer.mock';

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
    description: 'Акция "Промокод за покупку"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promos: [offerPromoTypePromocode]}, offerMock.wareId);
            await this.browser.setState('report', decorateStateWithSearchDefaults(offer));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3043',
        issue: 'MARKETVERSTKA-32170',
    },
}, {
    description: 'Акция "Больше за ту же цену"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promos: [offerPromoTypeNPLusM]}, offerMock.wareId);
            await this.browser.setState('report', decorateStateWithSearchDefaults(offer));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3041',
        issue: 'MARKETVERSTKA-32172',
    },
}, {
    description: 'Акция "Подарок"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promos: [offerPromoTypeGift]}, offerMock.wareId);
            await this.browser.setState('report', decorateStateWithSearchDefaults(offer));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3042',
        issue: 'MARKETVERSTKA-32171',
    },
}];


export default makeSuite('Попап на акционном бейдже.', {
    environment: 'kadavr',
    story: createStories(dealsPopupStories, ({hooks, meta}) => prepareSuite(PopupSuite, {
        pageObjects: {
            dealsBadge() {
                return this.createPageObject(DealsTerms, {parent: this.snippetCard2});
            },
            dealsDescriptionPopup() {
                return this.createPageObject(DealsDescriptionPopup);
            },
        },
        hooks,
        meta,
    })),
});
