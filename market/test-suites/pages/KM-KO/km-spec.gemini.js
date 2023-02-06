import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ProductRecipesSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/n-product-recipes';
import LegalInfoSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/n-legal-info';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';
import SeoVersusComparisonsSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/SeoVersusComparisons';
import ProductVideoSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductVideo';
import TopOffersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/TopOffers';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';

import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
} from '@self/platform/spec/gemini/helpers/hide';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'KM-spec',
    url: '/product--noutbuk-asus-vivobook-15-x512/518204228/spec',
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
                initLazyWidgets(actions, 5000);
                const selector = [
                    ProductHeadlineSuite.selector,
                    ProductRecipesSuite.selector,
                    JournalScrollboxSuite.selector,
                    ProductVideoSuite.selector,
                    TopOffersSuite.selector,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
                MainSuite.before(actions);
            },
        },
        ProductHeadlineSuite,
        ProductRecipesSuite,
        LegalInfoSuite,
        SeoVersusComparisonsSuite,
        JournalScrollboxSuite,
        {
            ...ProductVideoSuite,
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482/spec',
            ignore: 'iframe',

        },
        {
            ...TopOffersSuite,
            url: '/product--stiralnaia-mashina-samsung-wf8590nlw9/13202482/reviews',
        },
    ],
};

