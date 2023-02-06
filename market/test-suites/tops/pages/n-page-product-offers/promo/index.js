import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import DealsTermsSutie from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms';
import DealsTermsPopupSutie from '@self/platform/spec/hermione/test-suites/blocks/n-deals-terms/popup';
import StickersTypeDiscountSuite from '@self/platform/spec/hermione/test-suites/blocks/stickers/_type_discount';
// page-objects
import DealsTerms from '@self/platform/spec/page-objects/components/DealsTerms';
import DiscountBadge from '@self/project/src/components/DiscountBadge/__pageObject';
import DealsDescriptionPopup from '@self/project/src/components/DealDescription/__pageObject';
// mocks
import {guruMock} from '@self/platform/spec/hermione/fixtures/promo/product.mock';
import {offerMock} from '@self/platform/spec/hermione/fixtures/promo/offer.mock';
import {nPlusMPromo, promocodePromo, giftPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';

const badgeStories = [{
    promo: nPlusMPromo,
    meta: {
        issue: 'MARKETVERSTKA-35183',
        id: 'marketfront-3640',
    },
    params: {
        expectedText: 'При покупке 9 ещё 1 в подарок',
    },
    description: 'Бейдж на акции N+M',
}, {
    promo: promocodePromo,
    meta: {
        issue: 'MARKETVERSTKA-35180',
        id: 'marketfront-3637',
    },
    params: {
        expectedText: 'Ещё −400 ₽ по промокоду',
    },
    description: 'Бейдж на акции промокод',
}, {
    promo: giftPromo,
    meta: {
        issue: 'MARKETVERSTKA-35182',
        id: 'marketfront-3639',
    },
    params: {
        expectedText: 'Подарок за покупку',
    },
    description: 'Бейдж на акции подарок',
}];

const popupStories = [{
    promo: nPlusMPromo,
    meta: {
        issue: 'MARKETVERSTKA-35183',
        id: 'marketfront-3640',
    },
    description: 'Попап на акции N+M',
}, {
    promo: promocodePromo,
    meta: {
        issue: 'MARKETVERSTKA-35180',
        id: 'marketfront-3637',
    },
    description: 'Попап на акции промокод',
}, {
    promo: giftPromo,
    meta: {
        issue: 'MARKETVERSTKA-35182',
        id: 'marketfront-3639',
    },
    description: 'Попап на акции подарок',
}];

const prepareState = (promo = null) => {
    const {mock} = guruMock;
    const product = createProduct(mock, mock.id);
    const offer = createOffer(promo ? {
        ...offerMock,
        prices: {
            currency: 'RUR',
            value: '500',
            isDeliveryIncluded: false,
        },
        promo,
    } : offerMock, offerMock.wareId);
    return mergeState([product, offer, {
        data: {
            search: {
                total: 1,
            },
        },
    }]);
};

export default makeSuite('Акции.', {
    environment: 'kadavr',
    feature: 'Скидки и акции',
    story: mergeSuites(
        createStories(badgeStories, ({promo, meta, params, description}) =>
            prepareSuite(DealsTermsSutie, {
                meta,
                params,
                description,
                hooks: {
                    async beforeEach() {
                        const {id: productId, slug} = guruMock.mock;
                        await this.browser.setState('report', prepareState(promo));
                        await this.browser.yaOpenPage('market:product-offers', {productId, slug});
                        return this.dealsBadge.scrollToBadge();
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(DealsDescriptionPopup);
                    },
                },
            })
        ),
        createStories(popupStories, ({promo, meta, description}) =>
            prepareSuite(DealsTermsPopupSutie, {
                meta,
                description,
                hooks: {
                    async beforeEach() {
                        const {id: productId, slug} = guruMock.mock;
                        await this.browser.setState('report', prepareState(promo));
                        await this.browser.yaOpenPage('market:product-offers', {productId, slug});
                        return this.dealsBadge.scrollToBadge();
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(DealsDescriptionPopup);
                    },
                },
            })
        ),
        prepareSuite(StickersTypeDiscountSuite, {
            meta: {
                issue: 'MARKETVERSTKA-35181',
                id: 'marketfront-3638',
            },
            description: 'Бейдж скидки в ценах',
            hooks: {
                async beforeEach() {
                    const {id: productId, slug} = guruMock.mock;
                    await this.browser.setState('report', prepareState());
                    return this.browser.yaOpenPage('market:product-offers', {productId, slug});
                },
            },
            pageObjects: {
                discountSticker() {
                    return this.createPageObject(DiscountBadge);
                },
            },
        })
    ),
});
