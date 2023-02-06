import SummarySuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductSummary';
import {hideScrollbar, hideRegionPopup, hideDevTools, hideProductTabs} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'AlcoKmIndex',
    url: '/product--vodka-tsarskaia-originalnaia-0-5-l/270940092',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideProductTabs(actions);
        initLazyWidgets(actions, 3000);
    },
    childSuites: [
        SummarySuite,
    ],
};
