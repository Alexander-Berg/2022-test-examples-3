import ClarifyCategory from '@self/root/src/widgets/content/search/Clarify/components/View/__pageObject';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'CatalogSearchClarify',
    url: {
        pathname: '/search',
        query: {
            cvredirect: 2,
            text: 'red',
        },
    },
    before(actions) {
        setDefaultGeminiCookies(actions);
    },
    childSuites: [
        {
            suiteName: 'ClarifyCategory',
            selector: ClarifyCategory.root,
            ignore: [
                {every: '[style*="background-image"]'},
            ],
            capture() {},
        },
    ],
};
