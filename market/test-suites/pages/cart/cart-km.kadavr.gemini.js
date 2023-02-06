// page-objects
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import PricesOfferSnippet from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
// helpers
import {hideProductTabs, hideHeadBanner} from '@self/platform/spec/gemini/helpers/hide';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
// mocks
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import productWithCPATop6 from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPATop6.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {
    catalogerMock,
    cpaType1POfferMock,
    cpaType3POfferMock,
    cpaTypeDSBSOfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {generateCartSuites} from './cart.block';


function makeSuiteByType(offerMock, suiteName) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    return {
        suiteName,
        childSuites: [
            {
                suiteName: 'DefaultOffer',
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
                    setState.call(actions, 'report', cpaDO.state);
                    // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(DefaultOffer.root),
            },
            {
                suiteName: 'Top-6',
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
                    setState.call(actions, 'report', cpaTop6.state);
                    // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(MiniTopOffers.item),
            },
            {
                suiteName: 'Prices',
                url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/offers`,
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
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
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(PricesOfferSnippet.root),
            },
            {
                suiteName: 'Top-6 Spec',
                url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/spec`,
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
                    setState.call(actions, 'report', cpaTop6.state);
                    // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(TopOfferSnippet.root),
            },
            {
                suiteName: 'Top-6 Reviews',
                url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/reviews`,
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
                    setState.call(actions, 'report', cpaTop6.state);
                    // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(TopOfferSnippet.root),
            },
            {
                suiteName: 'Top-6 Q&A',
                url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/questions`,
                before(actions) {
                    createSession.call(actions);
                    setState.call(actions, 'Carter.items', []);
                    setState.call(actions, 'ShopInfo.collections', shopInfoMock);
                    setState.call(actions, 'Cataloger.tree', catalogerMock);
                    setState.call(actions, 'Cataloger.path', catalogerMock);
                    setState.call(actions, 'report', cpaTop6.state);
                    // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
                    setDefaultGeminiCookies(actions);
                    hideProductTabs(actions);
                    hideHeadBanner(actions);
                    initLazyWidgets(actions);
                },
                after(actions) {
                    deleteSession.call(actions);
                },
                childSuites: generateCartSuites(TopOfferSnippet.root),
            },
        ],
    };
}

export default {
    suiteName: 'KM-Cart[KADAVR]',
    url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}`,
    selector: '.main',
    childSuites: [
        makeSuiteByType(cpaType1POfferMock, '1P'),
        makeSuiteByType(cpaType3POfferMock, '3P'),
        makeSuiteByType(cpaTypeDSBSOfferMock, 'DSBS'),
    ],
};
