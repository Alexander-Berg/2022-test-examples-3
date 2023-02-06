// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import ReviewsRatingCard from '@self/platform/components/ReviewsRatingCard/__pageObject__';
import StickyOffer from '@self/platform/widgets/parts/StickyOffer/__pageObject';
// helpers
import COOKIES from '@self/platform/constants/cookie';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {hideElementBySelector, hideScrollbar} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {clone} from 'ambar';

// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// mocks
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import productWithCPATop6 from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPATop6.mock';
import {
    cpaType3POfferMock,
    shopInfoMock,
    shopForSupplier,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {
    goodOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';

const extraCookies = [
    ...DEFAULT_COOKIES,
    {
        name: COOKIES.HEAD_SCROLL_BANNER_HIDDEN,
        value: '1',
    },
];

const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
goodRatingOfferMock.shop = {...goodRatingOfferMock.shop, ...shopForSupplier};

// Тест проверяет наличие рейтинга пользователей (звёздочки) на ДО и топ-6 сниппете
function generateTestSuites(offerMock) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    return [
        {
            suiteName: 'DefaultOffer',
            url: `/product--${productWithCPADO.route.slug}/${productWithCPADO.route.productId}`,
            before(actions) {
                createSession.call(actions);
                setState.call(actions, 'Carter.items', []);
                setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                setState.call(actions, 'report', mergeState([
                    cpaDO.state,
                    dataFixture,
                    {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                            },
                        },
                    },
                ]));
                // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                setCookies.setCookies.call(actions, extraCookies);
                disableAnimations(actions);
                hideScrollbar(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            childSuites: [
                {
                    suiteName: 'StarShouldBeVisibleOnDO',
                    before(actions) {
                        hideElementBySelector(actions, StickyOffer.root);
                    },
                    selector: DefaultOffer.root,
                    capture(actions) {
                        actions.waitForElementToShow(`${DefaultOffer.root} ${ReviewsRatingCard.root}`);
                    },
                },
            ],
        },
        {
            suiteName: 'Top-6',
            url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}`,
            before(actions) {
                createSession.call(actions);
                setState.call(actions, 'Carter.items', []);
                setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                setState.call(actions, 'report', mergeState([
                    cpaTop6.state,
                    dataFixture,
                    {
                        data: {
                            search: {
                                total: 2,
                                totalOffers: 2,
                                totalOffersBeforeFilters: 2,
                                totalShopsBeforeFilters: 2,
                                shops: 2,
                                cpaCount: 1,
                            },
                        },
                    },
                ]));
                // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                setCookies.setCookies.call(actions, extraCookies);
                disableAnimations(actions);
            },
            after(actions) {
                deleteSession.call(actions);
            },
            childSuites: [
                {
                    suiteName: 'StarShouldBeVisibleOnTop6',
                    selector: ProductOfferSnippet.root,
                    capture(actions) {
                        actions.waitForElementToShow(`${ProductOfferSnippet.root} ${ProductOfferSnippet.shopRating}`);
                    },
                },
            ],
        },
        // Страницы spec, questions, reviews не могут быть покрыты тестами, т.к. топ-6 на этих страницах ленивый.
        // А лениывые виджеты ходят в реальные бекенды, а не в кадаврика.
    ];
}

export default {
    suiteName: 'CpaRatingKM[KADAVR]',
    childSuites: [
        {
            suiteName: 'GoodRating',
            childSuites: [
                ...generateTestSuites(goodRatingOfferMock),
            ],
        },
    ],
};
