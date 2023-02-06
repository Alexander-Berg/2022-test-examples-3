import Filters from '@self/platform/spec/gemini/test-suites/blocks/filters/filters';
import SearchSnippetCardSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';

import SearchSnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';


import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

import {setCookies} from '@yandex-market/gemini-extended-actions';


export default {
    suiteName: 'CatalogClusterList',
    url: {
        pathname: '/catalog--platia/57297/list',
        query: {
            viewtype: 'list',
            cpa: 1,
        },
    },
    before(actions) {
        setCookies.setCookies.call(actions, [
            ...DEFAULT_COOKIES,
            {
                name: 'viewtype',
                value: 'list',
            },
        ]);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        Filters,
        {
            ...SearchSnippetCardSuite,
            selector: `${SearchSnippetCard.root}:nth-of-type(1)`,
        },
    ],
};
