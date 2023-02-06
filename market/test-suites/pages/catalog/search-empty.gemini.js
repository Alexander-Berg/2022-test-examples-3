import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';

import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideAllElementsBySelector} from '@self/platform/spec/gemini/helpers/hide';

export default {
    suiteName: 'CatalogSearchEmpty',
    url: {
        pathname: '/search',
        query: {
            cvredirect: 2,
            text: 'vvvvvvvvvvvv',
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                MainSuite.before(actions);
                initLazyWidgets(actions);
                hideAllElementsBySelector(actions, '[data-zone-name="snippet"]');
            },
        },
        {
            suiteName: 'FeedSnippet',
            selector: '[data-zone-name="snippet"]',
            capture() {},
        },
    ],
};
