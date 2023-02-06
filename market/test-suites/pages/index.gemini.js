import footerSuite from '@self/platform/spec/gemini/test-suites/blocks/footer-market';
import header2Suite from '@self/platform/spec/gemini/test-suites/blocks/header2';
import search2Suite from '@self/platform/spec/gemini/test-suites/blocks/search2';
import RegionFormSuite from '@self/platform/spec/gemini/test-suites/blocks/region';
import MenuHorizontalItemsSuite from '@self/platform/spec/gemini/test-suites/blocks/HeaderTabs/horizontalTabs';
import MenuGroupingTabSuite from '@self/platform/spec/gemini/test-suites/blocks/HeaderTabs/navigationMenuGroupingTab';
import MenuActiveTabSuite from '@self/platform/spec/gemini/test-suites/blocks/HeaderTabs/activeTab';
import {hideScrollbar, hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'IndexPage',
    url: {
        pathname: '/',
        query: {
            'no-tests': 1,
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...footerSuite,
            before(actions, find) {
                if (footerSuite.before) {
                    footerSuite.before(actions, find);
                }
            },
        },
        header2Suite,
        search2Suite,
        RegionFormSuite,
        MenuGroupingTabSuite,
        MenuHorizontalItemsSuite,
        MenuActiveTabSuite,
    ],
};
