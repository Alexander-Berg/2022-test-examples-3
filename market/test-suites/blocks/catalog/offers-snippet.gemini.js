import SearchResults from '@self/platform/spec/page-objects/SearchResults';
import SnippetPrice from '@self/project/src/components/SnippetPrice/__pageObject/SnippetPrice';

export default {
    // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A10%3A28.837252.jpg
    suiteName: 'SearchResults',
    selector: SearchResults.searchResult,
    ignore: [
        {every: SnippetPrice.root},
    ],
    capture() {},
};
