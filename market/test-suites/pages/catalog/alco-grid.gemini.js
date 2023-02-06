import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import SearchSnippetCellSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCell';
import AdultWarning from '@self/root/src/widgets/content/AdultWarning/components/View/__pageObject/index.desktop';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'AlcoGrid',
    url: {
        pathname: '/catalog--vino/82914/list',
        query: {
            viewtype: 'grid',
            glfilter: '16156194:16156197;16156144:16156158', // красное сухое
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        MainSuite,
        {
            ...SearchSnippetCellSuite,
            before(actions, find) {
                actions.click(find(AdultWarning.acceptButton));
            },
            capture(actions) {
                actions.waitForElementToShow(SearchSnippetCellSuite.selector, 8000);
            },
        },
    ],
};
