import {makeSuite, makeCase} from 'ginny';
import SearchResult from '@self/platform/spec/page-objects/SearchResult';
import SearchSnippetRating from '@self/platform/spec/page-objects/containers/SearchSnippet/Rating';

/**
 * Тесты на список выдачи с включенной сортировкой по отзывам
 * @param {PageObject.SearchResults} searchResults
 */
export default makeSuite('Список выдачи.', {
    environment: 'kadavr',
    story: {
        'При сортировке по отзывам': {
            'товары в выдаче должны быть расположены в порядке уменьшения количества отзывов': makeCase({
                id: 'm-touch-1835',
                issue: 'MOBMARKET-6739',
                async test() {
                    const snippetsCount = await this.searchResults.snippetsCount();

                    const indexes = [];
                    for (let i = 4; i <= snippetsCount; ++i) {
                        indexes.push(i);
                    }

                    return Promise.all(
                        indexes.map(async i => {
                            const previousSearchResult = this.createPageObject(SearchResult, {
                                parent: this.searchResults,
                                root: `${SearchResult.root}:nth-child(${i - 1})`,
                            });
                            const previousSearchProduct = this.createPageObject(SearchSnippetRating, {
                                parent: previousSearchResult,
                            });

                            const currentSearchResult = this.createPageObject(SearchResult, {
                                parent: this.searchResults,
                                root: `${SearchResult.root}:nth-child(${i})`,
                            });
                            const currentSearchProduct = this.createPageObject(SearchSnippetRating, {
                                parent: currentSearchResult,
                            });

                            const previousReviewsCount = await previousSearchProduct.getReviewsCount();
                            const reviewsCount = await currentSearchProduct.getReviewsCount();

                            return this.expect(previousReviewsCount >= reviewsCount)
                                .to.be.equal(true, `Проверяем, что у ${i - 1}-го сниппета количество отзывов ` +
                                    `(${previousReviewsCount} отзывов) не меньше чем у ${i}-го (${reviewsCount} ` +
                                    'отзывов)');
                        })
                    );
                },
            }),
        },
    },
});
