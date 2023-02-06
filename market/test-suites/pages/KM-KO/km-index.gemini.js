import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ProductTitleSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/n-product-title';
import TopOffersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/TopOffers';
import SummarySuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductSummary';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';
import footerSuite from '@self/platform/spec/gemini/test-suites/blocks/footer-market';
import ProductSpecs from '@self/platform/components/ProductSpecs/__pageObject__';
import ProductSticker from '@self/platform/spec/page-objects/n-product-sticker';
import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import ProductVideo from '@self/platform/spec/page-objects/ProductVideo';
import ProductUsefulReviews from '@self/platform/spec/page-objects/widgets/content/ProductUsefulReviews';
import Delivery from '@self/platform/spec/page-objects/n-delivery';
import miniTopOffers from '@self/platform/spec/page-objects/widgets/content/MiniTopOffers';
import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';

import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideElementBySelector,
    hideProductTabs,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import VersusScrollBox from '@self/platform/spec/page-objects/widgets/content/VersusScrollBox/';
import ProductReview from '@self/platform/spec/page-objects/components/ProductReview';


export default {
    suiteName: 'KM-index',
    url: '/product--noutbuk-lenovo-ideapad-5/663684020',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 6000);
                const selector = [
                    SummarySuite.selector,
                    ProductTitleSuite.selector,
                    '[data-zone-data*="AlsoViewed"]',
                    '[data-zone-name="GroupOfWidgets"]',
                    ProductVideo.root,
                    ProductUsefulReviews.root,
                    JournalScrollboxSuite.selector,
                    VersusScrollBox.root,
                    TopOffersSuite.selector,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
        },
        {
            ...SummarySuite,
            ignore: ['[data-zone-data*="AlsoViewed"]', '[data-zone-name="cpa-offer"]', miniTopOffers.root],
            before(actions) {
                initLazyWidgets(actions, 3000);
            },
        },
        {
            suiteName: 'ProductSpecs',
            selector: ProductSpecs.root,
            ignore: Delivery.root,
            before(actions) {
                hideProductTabs(actions);
            },
            capture() {},
        },
        ProductTitleSuite,
        {
            suiteName: 'OftenViewedWithThisProductCarousel',
            selector: '[data-zone-data*="AlsoViewed"]',
            url: '/product--barilla-makarony-maccheroni-n-44-500-g/152408314',
            ignore: [
                {every: ProductSnippet.root},
            ],
            before(actions) {
                initLazyWidgets(actions);
            },
            capture() {},
        },
        {
            suiteName: 'Video',
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482',
            selector: ProductVideo.root,
            ignore: [
                ProductVideo.mainContent,
                {every: ProductVideo.itemVideo},
                {every: ProductVideo.videoUploadedDate},
                {every: ProductVideo.videoViewsCounter},
            ],
            before(actions) {
                initLazyWidgets(actions, 3000);
            },
            capture() {},
        },
        {
            suiteName: 'ProductUsefulReviews',
            selector: ProductUsefulReviews.root,
            ignore: {every: 'picture'},
            before(actions) {
                initLazyWidgets(actions, 6000);
                hideProductTabs(actions);
                new ClientAction(actions).removeElems(ProductReview.root);
            },
            capture() {},
        },
        {
            ...footerSuite,
            capture(actions, find) {
                hideElementBySelector(actions, ProductSticker.root);
                footerSuite.capture(actions, find);
            },
        },
        {
            suiteName: 'Review',
            selector: ProductReview.root,
            before(actions) {
                initLazyWidgets(actions, 6000);
            },
            capture() {},
        },
        JournalScrollboxSuite,
        {
            suiteName: 'AlsoBuyWithThisProductCarousel',
            selector: '[data-zone-name="GroupOfWidgets"]',
            ignore: [
                {every: ProductSnippet.root},
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
            },
            capture() {},

        },
        {
            suiteName: 'TopOffers',
            selector: miniTopOffers.root,
            ignore: [
                {every: miniTopOffers.price},
                {every: miniTopOffers.reviewsCount},
                {every: '[data-autotest-currency="₽"]'},
            ],
            capture() {},
        },
        {
            suiteName: 'DefaultOffer',
            selector: DefaultOffer.root,
            ignore: [
                {every: '[data-autotest-currency="₽"]'},
                '[data-zone-name="reviews-count"]',
            ],
            capture() {},
        },
    ],
};
