import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import ClarifyCategorySuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/category.gemini';
import AppPromoSuite from '@self/platform/spec/gemini/test-suites/blocks/catalog/AppPromo';
import {
    hideRegionPopup,
    hideParanja,
    hideMooa,
    hideModalFloat,
    hideScrollbar,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClarifyCategory from '@self/platform/spec/page-objects/ClarifyingCategories';
import SearchResultList from '@self/platform/spec/page-objects/SearchResults';
import SnippetPrice from '@self/project/src/components/SnippetPrice/__pageObject/SnippetPrice';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import SearchResult from '@self/platform/spec/page-objects/SearchResult';


export default {
    suiteName: 'Search',
    url: '/search?cvredirect=1&text=red',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideMooa(actions);
        hideScrollbar(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideElementBySelector(actions, ClarifyCategory.root);
                hideElementBySelector(actions, SearchResultList.root);
            },
        },
        {
            ...ClarifyCategorySuite,
            before(actions) {
                actions.wait(500);
            },
        },
        {
            suiteName: 'SearchResultTile',
            selector: SearchResult.root,
            ignore: [
                {every: SnippetPrice.root},
            ],
            before(actions) {
                // почему-то этот элемент приводит к сдвигу селекторов, поэтому его скрываем
                hideElementBySelector(actions, SearchOptions.root);
            },
            capture() {},
        },
        AppPromoSuite,
    ],
};
