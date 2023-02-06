import {prepareSuite, makeSuite} from 'ginny';
import {createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import DealsTermsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms';
import DealsTerms from '@self/platform/spec/page-objects/components/DealsTerms';

import {
    offerMock,
    offerPromoTypePromocode,
    offerPromoTypeNPLusM,
    offerPromoTypeGift,
} from '../fixtures/offer.mock';

const dataMixin = {data: {search: {total: 1}}};

const dealsBadgeStories = [{
    description: 'Акция "Промокод за покупку"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promo: offerPromoTypePromocode}, offerMock.wareId);
            await this.browser.setState('report', mergeState([offer, dataMixin]));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3036',
        issue: 'MARKETVERSTKA-32144',
    },
    params: {
        expectedText: 'Ещё −1 000 ₽ по промокоду',
    },
}, {
    description: 'Акция "Больше за ту же цену"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promos: [offerPromoTypeNPLusM]}, offerMock.wareId);
            await this.browser.setState('report', mergeState([offer, dataMixin]));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3039',
        issue: 'MARKETVERSTKA-32146',
    },
    params: {
        expectedText: 'При покупке 3 ещё 34 в подарок',
    },
}, {
    description: 'Акция "Подарок"',
    hooks: {
        async beforeEach() {
            const offer = createOffer({...offerMock, promos: [offerPromoTypeGift]}, offerMock.wareId);
            await this.browser.setState('report', mergeState([offer, dataMixin]));
            return this.browser.yaOpenPage('market:list', {
                nid: offerMock.navnodes[0].id,
                slug: offerMock.navnodes[0].slug,
            });
        },
    },
    meta: {
        id: 'marketfront-3037',
        issue: 'MARKETVERSTKA-32145',
    },
    params: {
        expectedText: 'Подарок за покупку',
    },
}];

export default makeSuite('Акционный бейдж.', {
    environment: 'kadavr',
    story: createStories(dealsBadgeStories, ({hooks, meta, params}) => prepareSuite(DealsTermsSuite, {
        pageObjects: {
            dealsBadge() {
                return this.createPageObject(DealsTerms, {
                    parent: this.snippetCard2,
                });
            },
        },
        hooks,
        meta,
        params,
    })),
});
