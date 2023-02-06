import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';


import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'KM-special',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideProductTabs(actions);
        hideElementBySelector(actions, ProductHeadlineSuite.selector);
    },
    childSuites: [
        {
            ...MainSuite,
            suiteName: 'PromoKinopoisk',
            url: '/product--noutbuk-lenovo-ideapad-320-15/1730364335/special',
        },
    ],
};
