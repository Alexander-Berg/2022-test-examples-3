import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import AllFiltersFooter from '@self/platform/widgets/content/AllFilters/Footer/__pageObject';

import {
    hideRegionPopup,
    hideDevTools,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';

import ClickOnMenuClosesFilterPopup from '@self/platform/spec/gemini/test-suites/blocks/HeaderTabs/closeFilterPopupOnMenuClick';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'AllFiltersPage',
    url: '/catalog--mobilnye-telefony/54726/filters',
    ignore: {every: 'input'},
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: [
                {every: AllFiltersFooter.productLink},
            ],
            before(actions) {
                hideAllElementsBySelector(actions, AllFiltersFooter.productLink);
                MainSuite.before(actions);
            },
        },
        ClickOnMenuClosesFilterPopup,
        {
            suiteName: 'ProductLink',
            selector: AllFiltersFooter.productLink,
            capture() {},
        },
    ],
};
