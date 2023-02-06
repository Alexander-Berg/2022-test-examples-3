import Filters from '@self/platform/spec/gemini/test-suites/blocks/filters/filters';
import SearchSnippetCardSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';
import VendorPromoSuite from '@self/platform/spec/gemini/test-suites/blocks/vendor-promo-line';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

import {
    hideRegionPopup,
    hideDevTools,
    hideHeader2,
    hideTopmenu,
    hideFooterSubscriptionWrap,
    hideFooter,
} from '@self/platform/spec/gemini/helpers/hide';


export default {
    suiteName: 'CatalogGuruList',
    url: {
        pathname: '/catalog--kholodilniki/71639/list',
        query: {
            viewtype: 'list',
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideHeader2(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        Filters,
        SearchSnippetCardSuite,
        VendorPromoSuite,
    ],
};
