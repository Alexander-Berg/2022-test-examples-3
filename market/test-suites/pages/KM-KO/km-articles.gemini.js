import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import KMArticleVideoSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductVideo';
import KMProductExternalArticlesSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductExternalArticles';
import TopOffersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/TopOffers';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
} from '@self/platform/spec/gemini/helpers/hide';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';


export default {
    suiteName: 'KM-articles',
    url: '/product--smartfon-apple-iphone-11-64gb/558171067/articles',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions);
                const selector = [
                    ProductHeadlineSuite.selector,
                    KMArticleVideoSuite.selector,
                    KMProductExternalArticlesSuite.selector,
                    JournalScrollboxSuite.selector,
                    TopOffersSuite.selector,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
        },
        ProductHeadlineSuite,
        {
            ...KMArticleVideoSuite,
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482/articles',
        },
        KMProductExternalArticlesSuite,
        JournalScrollboxSuite,
        TopOffersSuite,
    ],
};
