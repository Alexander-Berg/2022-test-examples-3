import materialFooterSuite from '@self/platform/spec/gemini/test-suites/blocks/journal/material-footer';
import scrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/journal/scrollbox';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'JournalWidgetsFooter',
    url: '/journal/story/test-suites-journal-article-footer',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        materialFooterSuite,
        scrollboxSuite,
    ],
};
