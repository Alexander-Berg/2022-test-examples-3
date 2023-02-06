import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';

import {
    hideDevTools, hideFooter,
    hideFooterSubscriptionWrap,
    hideHeadBanner,
    hideHeader2, hideProductTabs,
    hideRegionPopup,
    hideTopmenu,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {profiles} from '@self/platform/spec/gemini/configs/profiles';

import {createSession, setState, deleteSession} from '@yandex-market/kadavr/plugin/gemini';
import {
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import utils from '@yandex-market/gemini-extended-actions';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';

import {offerMock, similarGoodsOfferMock} from './mocks/offer.mock';

const catalogerMock = makeCatalogerTree('Название категории в Хлебных Крошках', 54726, 54726, {categoryType: 'guru'});

const customOfferMock = {
    ...offerMock,
    titles: {
        raw: 'Длинное название товара M357/OF2176/QQ1337/ASD1234/DF8103/MEM',
        highlighted: [
            {
                value: 'Длинное название товара M357/OF2176/QQ1337/ASD1234/DF8103/MEM/ORLY',
            },
        ],
    },
    description: 'Замоканый оффер.\nКартинка может протухнуть.',
};

const OFFER_WITHOUT_PRODUCT_WAREID = customOfferMock.wareId;

export default {
    suiteName: 'Offer-Card-Without-Product[KADAVR]',
    url: `/offer/${OFFER_WITHOUT_PRODUCT_WAREID}`,
    before(actions) {
        createSession.call(actions);
        setState.call(actions, 'Cataloger.tree', catalogerMock);
        setState.call(actions, 'report', mergeState([
            createOffer(customOfferMock, customOfferMock.wareId),
            createOffer(similarGoodsOfferMock, 'offer1'),
            createOffer(similarGoodsOfferMock, 'offer2'),
            createOffer(similarGoodsOfferMock, 'offer3'),
            {
                data: {
                    search: {
                        total: 3,
                    },
                },
            },
        ]));

        setDefaultGeminiCookies(actions);
        // Тут нельзя заменить на MainSuite.before(actions), потому что надо оставить ProductTabs
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideHeadBanner(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
    },
    after(actions) {
        deleteSession.call(actions);
    },
    childSuites: [
        {
            suiteName: 'Tabs',
            selector: ProductTabs.root,
            capture() {
            },
        },
        {
            suiteName: 'DescriptionTab',
            before(actions) {
                utils.authorize.call(actions, {
                    login: profiles.Recomend2017.login,
                    password: profiles.Recomend2017.password,
                    url: `/offer/${OFFER_WITHOUT_PRODUCT_WAREID}`,
                });
                MainSuite.before(actions);
            },
            after(actions) {
                utils.logout.call(actions);
            },
            childSuites: [
                {
                    suiteName: 'Main',
                    selector: MainSuite.selector,
                    before(actions) {
                        initLazyWidgets(actions, 5000);
                    },
                    capture() {
                    },
                },
                {
                    suiteName: 'SimilarCard',
                    // Not yet implemented
                    /* Из-за того, что выдача похожих товаров подгружается лениво - возникают проблемы при отрисовке,
                       и в итоге, хотя данных в моках для неё достаточно, но выдача не появляется.
                       platform.desktop/widgets/pages/OfferPage/controller.js:214
                       Если тут заменить ленивый SearchSimilar на не-ленивый, то выдача отображается.
                     */
                },
            ],
        },
        {
            suiteName: 'SpecTab',
            url: `offer/${OFFER_WITHOUT_PRODUCT_WAREID}/spec`,
            selector: MainSuite.selector,
            before(actions) {
                hideProductTabs(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'SimilarTab',
            url: `offer/${OFFER_WITHOUT_PRODUCT_WAREID}/similar`,
            selector: MainSuite.selector,
            before(actions) {
                hideProductTabs(actions);
            },
            capture() {
            },
        },
    ],
};
