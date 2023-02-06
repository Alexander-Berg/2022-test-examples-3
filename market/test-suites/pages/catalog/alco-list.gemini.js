import SearchSnippetCardSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';

import AdultWarning from '@self/root/src/widgets/content/AdultWarning/components/View/__pageObject/index.desktop';

import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'AlcoList',
    url: {
        pathname: '/catalog--vino/82914/list',
        query: {
            viewtype: 'list',
            glfilter: '16156194:16156197;16156144:16156158', // красное сухое
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            ...SearchSnippetCardSuite,
            before(actions, find) {
                actions.click(find(AdultWarning.acceptButton));
            },
            capture(actions) {
                actions.waitForElementToShow(SearchSnippetCardSuite.selector, 8000);
            },
        },
    ],
};
