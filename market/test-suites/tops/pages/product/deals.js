import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// suites
import DealsBadgeSuite from '@self/platform/spec/hermione/test-suites/blocks/DealsBadge';
import DealsBadgePopupSuite from '@self/platform/spec/hermione/test-suites/blocks/DealsBadge/popup';
import DiscountBadgeSuite from '@self/platform/spec/hermione/test-suites/blocks/DiscountBadge';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import DealsTerms from '@self/project/src/components/DealsTerms/__pageObject';
import DiscountBadge from '@self/project/src/components/DiscountBadge/__pageObject';
import OfferDealPopup from '@self/platform/spec/page-objects/OfferDealPopup';

import {productWithPicture, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
import {
    nPlusMPromo,
    promocodePromo,
    giftPromo,
    bonusCardPromo,
    spreadDiscountCountPromo,
} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';
import {offerMock as cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/offer';

import {testShop, anotherTestShop, offerUrls} from './kadavrMocks';

const badgeTop6Stories = [{
    promos: [nPlusMPromo],
    params: {
        expectedText: 'При покупке 9 ещё 1 в подарок (450 ₽/шт.)',
    },
    meta: {
        id: 'm-touch-2229',
        issue: 'MOBMARKET-8830',
    },
    description: 'Бейдж на акции N+M в топ 6',
}, {
    promos: [promocodePromo],
    params: {
        expectedText: '400 ₽ по промокоду tch7i22000255',
    },
    meta: {
        id: 'm-touch-2946',
        issue: 'MOBMARKET-13035',
    },
    description: 'Бейдж на акции промокод в топ 6',
}, {
    promos: [giftPromo],
    params: {
        expectedText: 'Подарок за покупку',
    },
    meta: {
        id: 'm-touch-2947',
        issue: 'MOBMARKET-13035',
    },
    description: 'Бейдж на акции подарок в топ 6',
}, {
    promos: [bonusCardPromo],
    params: {
        expectedText: '100 ₽ по карте клиента',
    },
    meta: {
        id: 'm-touch-3311',
        issue: 'MARKETFRONT-9841',
    },
    description: 'Бэйдж бонус карта в топ-6',
}];

const popupTop6Stories = [{
    promos: [nPlusMPromo],
    meta: {
        id: 'm-touch-2246',
        issue: 'MOBMARKET-8921',
    },
    description: 'Попап на акции N+M в топ 6',
}, {
    promos: [promocodePromo],
    meta: {
        id: 'm-touch-2943',
        issue: 'MOBMARKET-13035',
    },
    description: 'Попап на акции промокод в топ 6',
}, {
    promos: [giftPromo],
    meta: {
        id: 'm-touch-2945',
        issue: 'MOBMARKET-13035',
    },
    description: 'Попап на акции подарок в топ 6',
}, {
    promos: [bonusCardPromo],
    meta: {
        id: 'm-touch-3310',
        issue: 'MARKETFRONT-9841',
    },
    description: 'Попап бонус карта в топ 6',
}];

const badgeDOStories = [{
    promos: [nPlusMPromo],
    params: {
        expectedText: 'При покупке 9 ещё 1 в подарок (450 ₽/шт.)',
    },
    meta: {
        id: 'm-touch-2949',
        issue: 'MOBMARKET-13035',
    },
    description: 'Бейдж на акции N+M в ДО',
}, {
    promos: [promocodePromo],
    params: {
        expectedText: '400 ₽ по промокоду tch7i22000255',
    },
    meta: {
        id: 'm-touch-2943',
        issue: 'MOBMARKET-13035',
    },
    description: 'Бейдж на акции промокод в ДО',
}, {
    promos: [giftPromo],
    params: {
        expectedText: 'Подарок за покупку',
    },
    meta: {
        id: 'm-touch-2945',
        issue: 'MOBMARKET-13035',
    },
    description: 'Бейдж на акции подарок в ДО',
}, {
    promos: [bonusCardPromo],
    params: {
        expectedText: '100 ₽ по карте клиента',
    },
    meta: {
        id: 'm-touch-3308',
        issue: 'MARKETFRONT-9841',
    },
    description: 'Бэйдж бонус карта в ДО',
}, {
    promos: [spreadDiscountCountPromo],
    meta: {
        issue: 'MARKETFRONT-52896',
        id: 'marketfront-3631',
    },
    params: {
        expectedText: '2 шт. – 10%, 5 шт. – 20%',
    },
    description: 'Бейдж на акции за количество на ДО',
}];

const popupDOStories = [{
    promos: [nPlusMPromo],
    meta: {
        id: 'm-touch-2949',
        issue: 'MOBMARKET-13035',
    },
    description: 'Попап на акции N+M в ДО',
}, {
    promos: [promocodePromo],
    meta: {
        id: 'm-touch-2943',
        issue: 'MOBMARKET-13035',
    },
    description: 'Попап на акции промокод в ДО',
}, {
    promos: [giftPromo],
    meta: {
        id: 'm-touch-2945',
        issue: 'MOBMARKET-13035',
    },
    description: 'Попап на акции подарок в ДО',
}, {
    promos: [bonusCardPromo],
    meta: {
        id: 'm-touch-3309',
        issue: 'MARKETFRONT-9841',
    },
    description: 'Попап бонус карта в ДО',
}];

const prepareState = ({promos = null, withDiscount = false, isDefaultOffer = false}) => {
    const offer = createOfferForProduct(
        {
            delivery: cpaOfferMock.delivery,
            urls: offerUrls,
            shop: testShop,
            prices: {
                currency: 'RUR',
                value: '500',
                isDeliveryIncluded: false,
                discount: withDiscount ? {
                    oldMin: '1000',
                    percent: 50,
                } : undefined,
            },
            promos,
            benefit: isDefaultOffer ? {
                type: 'recommended',
                description: 'Хорошая цена от надёжного магазина',
                isPrimary: true,
            } : undefined,
        },
        phoneProductRoute.productId,
        2
    );
    const offerFromAnotherShop = createOfferForProduct(
        {
            urls: offerUrls,
            shop: anotherTestShop,
            prices: {
                currency: 'RUR',
                value: '600',
                isDeliveryIncluded: false,
                discount: withDiscount ? {
                    oldMin: '1000',
                    percent: 50,
                } : undefined,
            },
            promos,
            benefit: undefined,
        },
        phoneProductRoute.productId,
        3
    );
    const dataMixin = {
        data: {
            search: {
                total: 2,
                totalOffers: 2,
                totalOffersBeforeFilters: 2,
                results: [
                    {
                        id: '2',
                        schema: 'offer',
                    },
                    {
                        id: '3',
                        schema: 'offer',
                    },
                ],
            },
        },
    };
    return mergeReportState([
        productWithPicture,
        offer,
        offerFromAnotherShop,
        dataMixin,
    ]);
};

export default makeSuite('Скидки и акции.', {
    environment: 'kadavr',
    story: mergeSuites(
        createStories(badgeTop6Stories, ({promos, meta, params, description}) =>
            prepareSuite(DealsBadgeSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', prepareState({promos}));
                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        return this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                },
                params,
                meta,
                description,
            })
        ),
        createStories(popupTop6Stories, ({promos, meta, description}) =>
            prepareSuite(DealsBadgePopupSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', prepareState({promos}));
                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        return this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(OfferDealPopup);
                    },
                },
                meta,
                description,
            })
        ),
        createStories(badgeDOStories, ({promos, meta, params, description}) =>
            prepareSuite(DealsBadgeSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', prepareState({promos, isDefaultOffer: true}));
                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        return this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                },
                params,
                meta,
                description,
            })
        ),
        createStories(popupDOStories, ({promos, meta, description}) =>
            prepareSuite(DealsBadgePopupSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', prepareState({promos, isDefaultOffer: true}));
                        await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                        return this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                    },
                },
                pageObjects: {
                    dealsBadge() {
                        return this.createPageObject(DealsTerms);
                    },
                    dealsDescriptionPopup() {
                        return this.createPageObject(OfferDealPopup);
                    },
                },
                meta,
                description,
            })
        ),
        prepareSuite(DiscountBadgeSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', prepareState({withDiscount: true, isDefaultOffer: true}));
                    await this.browser.yaOpenPage('touch:product', phoneProductRoute);
                    return this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            meta: {
                id: 'm-touch-2948',
                issue: 'MOBMARKET-13035',
            },
            pageObjects: {
                discountBadge() {
                    return this.createPageObject(DiscountBadge);
                },
            },
        })
    ),
});
