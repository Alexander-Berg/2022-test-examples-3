// page-objects
import OperationalRatingBadge from '@self/project/src/components/OperationalRating/__pageObject/index.desktop';
import VendorTrust from '@self/project/src/components/VendorTrust/__pageObject__';
import HintWithContent from '@self/project/src/components/HintWithContent/__pageObject';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import MiniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import PricesOfferSnippet from '@self/platform/components/ProductOffers/Snippet/Offer/__pageObject';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';
import OfferInfo from '@self/platform/widgets/content/OfferDetailsPopup/__pageObject';
// helpers
import {clone} from 'ambar';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideProductTabs, hideMiniCard} from '@self/platform/spec/gemini/helpers/hide';
import disableAnimations from '@self/project/src/spec/gemini/helpers/disableAnimations';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
// kadavr
import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import productWithCPADO from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPADO.mock';
import productWithCPATop6 from '@self/project/src/spec/gemini/fixtures/cpa/mocks/productWithCPATop6.mock';
import dataFixture from '@self/project/src/spec/gemini/fixtures/cpa/mocks/dataFixture.mock';
import {
    catalogerMock,
    cpaType3POfferMock,
    shopInfoMock,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/cpaOffer.mock';
import {
    goodOperationalRating,
    mediumOperationalRating,
} from '@self/project/src/spec/gemini/fixtures/cpa/mocks/operationalRatingFixture.mock';


const goodRatingOfferMock = clone(cpaType3POfferMock);
goodRatingOfferMock.supplier = {
    ...goodRatingOfferMock.supplier,
    ...{
        newGradesCount: 666,
        newQualityRating: 4.9,
        newQualityRating3M: 4.9,
        ratingToShow: 4.9,
        ratingType: 3,
        newGradesCount3M: 333,
    },
};
goodRatingOfferMock.supplier.operationalRating = goodOperationalRating;
const mediumRatingOfferMock = clone(cpaType3POfferMock);
mediumRatingOfferMock.supplier.operationalRating = mediumOperationalRating;


function DOSuite(offerMock) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = DefaultOffer.root;
    const badgeSelector = `${rootSnippetSelector} ${VendorTrust.operationalRating}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnDO',
        url: `/product--${productWithCPADO.route.slug}/${productWithCPADO.route.productId}`,
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
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

function Top6Suite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = MiniTopOffers.root;
    const badgeSelector = `${rootSnippetSelector} ${VendorTrust.operationalRating}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnTop6',
        url: `/product--${productWithCPATop6.route.slug}/${productWithCPATop6.route.productId}`,
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
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

function OffersPageSuite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = PricesOfferSnippet.root;
    const badgeSelector = `${rootSnippetSelector} ${OperationalRatingBadge.root}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnOffersPage',
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
            hideMiniCard(actions);
            hideProductTabs(actions);
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

function KOSuite(offerMock) {
    const cpaDO = productWithCPADO.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = DefaultOffer.root;
    const badgeSelector = `${rootSnippetSelector} ${VendorTrust.operationalRating}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnKO',
        url: `/offer/${offerMock.wareId}`,
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
            disableAnimations(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function(`document.querySelector('${rootSnippetSelector}').scrollIntoView()`));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

function Top6TabsSuite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = TopOfferSnippet.root;
    const badgeSelector = `${rootSnippetSelector} ${OperationalRatingBadge.root}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnTop6-OnTabs',
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
            hideMiniCard(actions);
            disableAnimations(actions);
            initLazyWidgets(actions);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            // Тут скроллим к заголовку, т.к. попап открывается горизонтально - и gemini дёргается при скриншоте
            // eslint-disable-next-line no-new-func
            actions.executeJS(new Function('document.querySelector(\'[id="topOffersHeader"]\').scrollIntoView()'));
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

function ProductOffersPopupSuite(offerMock) {
    const cpaTop6 = productWithCPATop6.generateStateAndDataFromMock(offerMock);

    const rootSnippetSelector = OfferInfo.root;
    const badgeSelector = `${rootSnippetSelector} ${OperationalRatingBadge.root}`;
    const hintContentWithoutShadowSelector = `${HintWithContent.content} > div`;

    return {
        suiteName: 'StarAndPopupShouldBeVisibleOnInfoPopup',
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
            disableAnimations(actions);
            // Кликаем на центр сниппета на табе Цены чтобы появился попап
            actions.click(PricesOfferSnippet.root);
            actions.waitForElementToShow(rootSnippetSelector);
        },
        after(actions) {
            deleteSession.call(actions);
        },
        selector: [
            rootSnippetSelector,
            hintContentWithoutShadowSelector,
        ],
        capture(actions) {
            actions.mouseMove(badgeSelector);
            actions.waitForElementToShow(hintContentWithoutShadowSelector);
        },
    };
}

export default {
    suiteName: 'OperationalRatingKM[KADAVR]',
    childSuites: [
        {
            suiteName: 'GoodRating',
            childSuites: [
                DOSuite(goodRatingOfferMock),
                Top6Suite(goodRatingOfferMock),
                OffersPageSuite(goodRatingOfferMock),
                KOSuite(goodRatingOfferMock),
                Top6TabsSuite(goodRatingOfferMock),
                ProductOffersPopupSuite(goodRatingOfferMock),
            ],
        },
        {
            suiteName: 'MediumRating',
            childSuites: [
                OffersPageSuite(mediumRatingOfferMock),
                Top6TabsSuite(mediumRatingOfferMock),
            ],
        },
    ],
};
