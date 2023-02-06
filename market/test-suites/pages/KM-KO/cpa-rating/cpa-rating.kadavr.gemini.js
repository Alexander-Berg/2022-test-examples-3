// page-objects
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import PricesOfferSnippet from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import ShopRating from '@self/project/src/components/ShopRating/__pageObject';
import VendorTrust from '@self/project/src/components/VendorTrust/__pageObject__';
// helpers
import {clone} from 'ambar';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideProductTabs, hideMiniCard} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import productWithCPATop6 from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPATop6.mock';
import {
    cpaType3POfferMock,
    shopInfoMock,
    catalogerMock,
    shopForSupplier,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {
    goodOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';

const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
goodRatingOfferMock.shop = {...goodRatingOfferMock.shop, ...shopForSupplier};

function Top6Suite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);
    const rootSnippetSelector = MiniTopOffers.root;
    const ratingSelector = VendorTrust.operationalRating;
    return {
        suiteName: 'ShopRatingShouldBeVisibleOnTop6',
        url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}`,
        before(actions) {
            createSession.call(actions);
            setState.call(actions, 'Carter.items', []);
            setState.call(actions, 'Cataloger.tree', catalogerMock);
            setState.call(actions, 'Cataloger.path', catalogerMock);
            setState.call(actions, 'Cataloger.brand', cpaTop6.vendor);
            setState.call(actions, 'ShopInfo.collections', shopInfoMock);
            setState.call(actions, 'report', mergeState([
                cpaTop6.state,
                dataFixture,
            ]));
            // Нужно ставить куки после обновления стейта кадаврика чтобы страница перезагрузилась
            setDefaultGeminiCookies(actions);
            hideProductTabs(actions);
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [rootSnippetSelector, ratingSelector],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.waitForElementToShow(`${ratingSelector}`);
        },
    };
}

function OffersPageSuite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);
    const rootSnippetSelector = PricesOfferSnippet.root;
    const ratingSelector = ShopRating.supplierRatingContainer;
    return {
        suiteName: 'ShopRatingShouldBeVisibleOnOffersPage',
        url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/offers`,
        before(actions) {
            createSession.call(actions);
            setState.call(actions, 'Carter.items', []);
            setState.call(actions, 'Cataloger.tree', catalogerMock);
            setState.call(actions, 'Cataloger.path', catalogerMock);
            setState.call(actions, 'Cataloger.brand', cpaTop6.vendor);
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
            setDefaultGeminiCookies(actions);
            hideMiniCard(actions);
            hideProductTabs(actions);
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [rootSnippetSelector, ratingSelector],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.waitForElementToShow(`${ratingSelector}`);
        },
    };
}

function Top6TabsSuite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);
    const rootSnippetSelector = TopOfferSnippet.root;
    const ratingSelector = ShopRating.supplierRatingContainer;

    return {
        suiteName: 'ShopRatingShouldBeVisibleOnTop6-OnTabs',
        url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}/spec`,
        before(actions) {
            createSession.call(actions);
            setState.call(actions, 'Carter.items', []);
            setState.call(actions, 'Cataloger.tree', catalogerMock);
            setState.call(actions, 'Cataloger.path', catalogerMock);
            setState.call(actions, 'Cataloger.brand', cpaTop6.vendor);
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
            setDefaultGeminiCookies(actions);
            hideProductTabs(actions);
            hideMiniCard(actions);
            disableAnimations(actions);
            initLazyWidgets(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [rootSnippetSelector, ratingSelector],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.waitForElementToShow(`${ratingSelector}`);
        },
    };
}

export default {
    suiteName: 'CpaRatingKM[KADAVR]',
    childSuites: [
        {
            suiteName: 'GoodRating',
            childSuites: [
                Top6TabsSuite(goodRatingOfferMock),
                Top6Suite(goodRatingOfferMock),
                OffersPageSuite(goodRatingOfferMock),
            ],
        },
    ],
};
