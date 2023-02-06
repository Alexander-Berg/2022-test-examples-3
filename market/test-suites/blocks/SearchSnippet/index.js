import {makeSuite, mergeSuites} from '@yandex-market/ginny';
import SearchSnippet from '@self/platform/spec/page-objects/containers/SearchSnippet';
import SearchSnippetContainer from '@self/platform/spec/page-objects/SearchSnippetContainer';

import SecondParameter from './SecondParameters';

export default makeSuite('Сниппет выдачи', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                hermione.setPageObjects.call(this, {

                    snippet: () => this.browser.createPageObject(SearchSnippet, {
                        parent: hermione.snippetContainer,
                        root: SearchSnippet.listRoot,
                    }),
                    snippetContainer: () => this.browser.createPageObject(SearchSnippetContainer, {
                        parent: hermione.searchResults,
                    }),
                });
            },
        },
        SecondParameter
    ),
});
