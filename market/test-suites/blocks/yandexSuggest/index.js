import {mergeSuites, makeSuite, makeCase} from 'ginny';
import Search2 from '@self/platform/spec/page-objects/search2';
import YandexSuggestContent from '@self/platform/spec/page-objects/YandexSuggestContent';

async function doSearch(term) {
    await this.input.setValue(term);
    await this.button.click();
}

/**
 * Тесты на показ саджеста.
 */
export default makeSuite('Блок саджеста.', {
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    search2: () => this.createPageObject(Search2),
                    input: () => this.search2.input,
                    button: () => this.search2.button,
                    suggestContent: () => this.createPageObject(YandexSuggestContent),
                    suggestItem: () => this.suggestContent.item,
                });
                this.doSearch = doSearch.bind(this);
            },

            'По умолчанию': {
                'отображается': makeCase({
                    id: 'marketfront-2503',
                    issue: 'MARKETVERSTKA-28792',
                    async test() {
                        await this.input.setValue('hello');
                        await this.suggestContent.waitForVisible();
                        return this.suggestItem.isVisible()
                            .should.eventually.equal(true, 'Попап саджеста должен быть открыт и видны элементы саджеста');
                    },
                }),
            },
            'Не появляется при клике в пустой инпут': makeCase({
                id: 'marketfront-2500',
                issue: 'MARKETVERSTKA-28792',
                async test() {
                    await this.doSearch('hello');
                    // возвращаемся на главную
                    await this.browser.yaOpenPage('market:index');
                    // иначе инпут не получит нормально фокус
                    await this.browser.yaExecAsyncClientScript('window.focus');
                    // кликаем в инпут
                    await this.input.click();
                    // почему то пару раз
                    await this.input.click();
                    // проверяем что саджест появляется
                    return this.suggestContent.isVisible()
                        .should.eventually.equal(false, 'Попап саджеста не должен быть открыт');
                },
            }),
        }
    ),
});
