import {makeCase, makeSuite} from '@yandex-market/ginny';

export default makeSuite('Сниппет на SERP', {
    environment: 'kadavr',
    feature: 'searchSnippetCard',
    story: {
        'По-умолчанию': {
            'Отображается корректно': makeCase({
                async test() {
                    await this.searchSnippet.waitForVisible();
                    const searchSnippetSelector = await this.searchSnippet.getSelector();
                    return this.browser.assertView('searchSnippet', searchSnippetSelector, {
                        compositeImage: true,
                    });
                },
            }),
        },
    },
});
