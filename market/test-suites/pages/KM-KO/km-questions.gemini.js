import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';
import TopOffersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/TopOffers';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';
import Reviews from '@self/platform/spec/page-objects/reviews';
import Counter from '@self/platform/spec/page-objects/Journal/Counter';
import {
    hideFooter,
    hideFooterSubscriptionWrap,
    hideHeader2,
    hideProductTabs,
    hideRegionPopup,
    hideTopmenu,
    hideDevTools,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'KM-questions',
    url: '/product--matras-askona-balance-forma/11613583/questions',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideProductTabs(actions);
        hideHeader2(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    ignore: [
        Reviews.root,
        {every: Counter.root},
    ],
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                initLazyWidgets(actions, 5000);
                const selector = [
                    ProductHeadlineSuite.selector,
                    TopOffersSuite.selector,
                    JournalScrollboxSuite.selector,
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        ProductHeadlineSuite,
        TopOffersSuite,
        JournalScrollboxSuite,
    ],
};
