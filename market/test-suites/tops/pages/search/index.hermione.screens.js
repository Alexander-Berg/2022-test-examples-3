import {makeSuite, mergeSuites} from '@yandex-market/ginny';

// suites
import SecondParametersScreens from '@self/platform/spec/hermione2/test-suites/blocks/SearchSnippet/SecondParameters';
// page-objects
import SearchResults from '@self/platform/spec/page-objects/SearchResults';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница выдачи', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                hermione.setPageObjects.call(this, {
                    searchResults: () => this.browser.createPageObject(SearchResults),
                });
            },
        },
        SecondParametersScreens
    ),
});
