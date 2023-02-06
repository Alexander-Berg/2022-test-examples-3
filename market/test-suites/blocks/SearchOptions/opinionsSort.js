import {makeSuite, makeCase} from 'ginny';

const OPINIONS_SORT = 'opinions';

/**
 * @param {PageObject.SearchOptions} searchOptions
 * @param {PageObject.SearchSnippetRating} snippetRating
 */
export default makeSuite('Блок сортировки.', {
    feature: 'Сортировка по отзывам',
    params: {
        opinions: 'Число отзывов, ожидаемое у первого сниппета после сортировки',
    },
    story: {
        'При применении сортировки': {
            'выдача должна сортироваться по отзывам': makeCase({
                environment: 'kadavr',
                async test() {
                    await this.searchOptions.setSort(OPINIONS_SORT);

                    await this.browser.yaParseUrl()
                        .should.eventually.be.link({
                            query: {
                                how: OPINIONS_SORT,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipPathname: true,
                        });

                    const firstSnippetOpinions = await this.snippetRating.getReviewsCount();

                    return this.expect(firstSnippetOpinions)
                        .to.be.equal(this.params.opinions, 'Число отзывов первого сниппета равно ожидаемому');
                },
            }),
        },
    },
});
