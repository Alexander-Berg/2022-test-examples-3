import Filters from '@self/platform/spec/gemini/test-suites/blocks/filters/filters';
import SearchSnippetCardSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';

import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';


export default {
    suiteName: 'CatalogGrocery',
    url: {
        pathname: 'catalog--kofe-v-zernakh/73608/list',
        query: {
            cpa: 1,
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        Filters,
        SearchSnippetCardSuite,
    ],
};
