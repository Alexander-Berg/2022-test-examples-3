// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
// helpers
import COOKIES from '@self/platform/constants/cookie';
import {hideHeadBanner} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {setCookies} from '@yandex-market/gemini-extended-actions';

// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// mocks
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import productWithCPATop6 from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPATop6.mock';
import {
    cpaType1POfferMock,
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {generateCartSuites} from './cart.block';

const extraCookies = [
    ...DEFAULT_COOKIES,
    {
        name: COOKIES.HEAD_SCROLL_BANNER_HIDDEN,
        value: '1',
    },
];


function makeSuiteByType(offerMock, suiteName) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    return {
        suiteName,
        childSuites: [
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
                    hideHeadBanner(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(DefaultOffer.root),
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
                    hideHeadBanner(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(ProductOfferSnippet.root),
            },
            // Страницы spec, questions, reviews не могут быть покрыты тестами, т.к. топ-6 на этих страницах ленивый.
            // А лениывые виджеты ходят в реальные бекенды, а не в кадаврика.
        ],
    };
}

export default {
    suiteName: 'KM-Cart[KADAVR]',
    selector: '.main',
    childSuites: [
        makeSuiteByType(cpaType1POfferMock, '1P'),
        makeSuiteByType(cpaType3POfferMock, '3P'),
        makeSuiteByType(cpaTypeDSBSOfferMock, 'DSBS'),
    ],
};
