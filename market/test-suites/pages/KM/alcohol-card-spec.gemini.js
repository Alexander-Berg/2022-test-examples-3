import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import DefaultOffers from '@self/platform/spec/gemini/test-suites/blocks/KM/default-offers.gemini';
import Snippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideElementBySelector,
    hideHeadBanner,
    hideHeader,
} from '@self/platform/spec/gemini/helpers/hide';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import DefaultOfferPrice from '@self/platform/spec/page-objects/components/DefaultOffer/DefaultOfferPrice';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';


export default {
    suiteName: 'AlcoholCardSpec',
    url: '/product--vodka-tsarskaia-originalnaia-0-5-l/270940092/spec',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
        ]);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                hideHeadBanner(actions);
                hideElementBySelector(actions, ProductOffersStaticList.root);
                hideHeader(actions);
                actions.wait(1000);
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A14%3A00.286351.jpg
            ...DefaultOffers,
            ignore: [
                DefaultOfferPrice.root,
                ShopRating.root,
            ],
            // scroll near widget and wait it for show
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(
                    'document.querySelector(\'[data-autotest-id="reviews"]\').scrollIntoView()'));
                actions.waitForElementToShow(ProductOffersStaticList.root, 5000);
            },
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T13%3A31%3A23.245743.jpg
            suiteName: 'OfferSnippet',
            selector: Snippet.root,
            ignore: [
                {every: '[data-zone-name="reviews-count"]'},
                Snippet.price,

            ],
            // scroll near widget and wait it for show
            before(actions) {
                // eslint-disable-next-line no-new-func
                actions.executeJS(new Function(
                    'document.querySelector(\'[data-autotest-id="reviews"]\').scrollIntoView()'));
                actions.waitForElementToShow(Snippet.root, 5000);
            },
            capture() {},
        },
    ],
};
