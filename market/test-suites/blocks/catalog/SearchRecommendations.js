import ProductSnippet from '@self/platform/spec/page-objects/VertProductSnippet';
import SearchResultRecommendations from '@self/platform/spec/page-objects/SearchResultRecommendations';

export default {
    suiteName: 'SearchRecommendations',
    selector: SearchResultRecommendations.root,
    ignore: [
        {every: ProductSnippet.root},
    ],
    capture() {},
};
