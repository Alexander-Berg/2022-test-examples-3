import FeedSnippet from '@self/platform/spec/page-objects/FeedSnippet';
import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import Navnode from '@self/platform/containers/Navnode/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideHeader2,
    hideTopmenu,
    hideFooterSubscriptionWrap,
    hideFooter,
    hideHeadBanner,
    hideAllFeedSnippets,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'HubPromoVendor',
    url: '/promo/vendor_promo_for_153061',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
        hideHeadBanner(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: Navnode.root},
            ],
            before(actions) {
                hideAllFeedSnippets(actions);
            },
        },
        {
            suiteName: 'FeedSnippet',
            selector: FeedSnippet.root,
            capture() {
            },
        },
    ],
};
