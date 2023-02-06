import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

import HeadBanner from '@self/platform/spec/page-objects/HeadBanner';
import Header2 from '@self/platform/spec/page-objects/header2';
import StaticBanner from '@self/platform/spec/page-objects/Banner/StaticBanner';
import VertProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';

import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideFooterSubscriptionWrap,
    hideFooter,
} from '@self/platform/spec/gemini/helpers/hide';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';


export default {
    suiteName: 'Morda',
    url: '/',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'MainSuite',
            selector: MainSuite.selector,
            ignore: [
                Header2.root,
                HeadBanner.root,
                {every: StaticBanner.root},
                {every: VertProductSnippet.root},
                {every: '[data-zone-name="vendor"]'},
                {every: 'picture'},
                {every: 'h3'},
            ],
            before(actions) {
                initLazyWidgets(actions, 5000);
                hideFooterSubscriptionWrap(actions);
                hideFooter(actions);
            },
            capture() {},
        },
    ],
};
