import {makeCase, makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';
import SearchSnippetVisualSearchButton from '@self/platform/spec/page-objects/containers/SearchSnippet/VisualSearchButton';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import SearchResults from '@self/platform/spec/page-objects/SearchResults';
import {EmptyVisualSearch} from '@self/root/src/components/EmptyVisualSearch/__pageObject';
import {visualSearchDataMock, initialDataMock, emptyVisualSearchDataMock} from './fixtures/productWithSku';

export default makeSuite('Визуальный поиск', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-79845',
    story: mergeSuites(
        prepareSuite(makeSuite('При клике на кнопку визуального поиска', {
            environment: 'kadavr',
            story: {
                'Показывается плашка "Не нашли похожих товаров"': makeCase({
                    id: 'marketfront-5797',
                    async test() {
                        await this.visualSearch.isVisible();
                        await this.browser.setState('report', emptyVisualSearchDataMock);
                        await this.visualSearch.click();
                        await this.emptyVisualSearch.isVisible();
                    },
                }),
                'Подгружаются новые сниппеты': makeCase({
                    id: 'marketfront-5798',
                    async test() {
                        const count = await this.searchResults.snippetsCount();
                        await this.browser.expect(count).to.be.equal(2, 'На выдаче 2 сниппета');
                        await this.visualSearch.isVisible();
                        await this.browser.setState('report', visualSearchDataMock);
                        await this.visualSearch.click();
                        await this.browser.waitUntil(
                            async () => {
                                const snippetCount = await this.searchResults.snippetsCount();
                                return snippetCount === 6;
                            },
                            4000,
                            'На выдачу загрузились 6 сниппетов'
                        );
                    },
                }),
            },
        }), {
            pageObjects: {
                visualSearch() { return this.browser.createPageObject(SearchSnippetVisualSearchButton); },
                searchResults() { return this.browser.createPageObject(SearchResults); },
                emptyVisualSearch() { return this.browser.createPageObject(EmptyVisualSearch); },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', initialDataMock);
                    await this.browser.yaOpenPage('touch:search', routes.search.default);
                },
            },
        })
    ),
});
