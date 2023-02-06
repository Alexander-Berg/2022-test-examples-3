import OfferSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/PricesOfferSnippetSuite';
import {setCookies} from '@yandex-market/gemini-extended-actions';
import {
    hideProductTabs,
    hideRegionPopup,
    hideScrollbar,
    hideDevTools,
} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'KM-offers-alco',
    url: '/product--vodka-tsarskaia-originalnaia-0-5-l/270940092/offers',
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'adult',
                value: '1:1:ADULT',
            },
        ]);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        OfferSnippetSuite,
    ],
};
