import Filters from '@self/platform/spec/gemini/test-suites/blocks/filters/filters';
import SearchSnippetCardSuite from '@self/platform/spec/gemini/test-suites/blocks/search/snippetCard';
import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import {DEFAULT_COOKIES} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import {setCookies} from '@yandex-market/gemini-extended-actions';


export default {
    suiteName: 'CatalogGuruLightList',
    url: {
        pathname: '/catalog--septiki/56330/list',
        query: {
            viewtype: 'list',
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
        SearchSnippetCardSuite,
    ],
};
