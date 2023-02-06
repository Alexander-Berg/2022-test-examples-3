import {makeCase, makeSuite} from '@yandex-market/ginny';
import SearchSnippetParameters from '@self/platform/spec/page-objects/containers/SearchSnippet/Parameters';

export default makeSuite('Элемент', {
    environment: 'kadavr',
    story: {
        'Должен отображаться': makeCase({
            async test() {
                await this.browser.waitForVisible(SearchSnippetParameters.root, 10000);
                return this.browser.assertView('plain', SearchSnippetParameters.root);
            },
        }),
    },
});
