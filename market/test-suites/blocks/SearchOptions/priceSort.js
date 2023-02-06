import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchOptions} searchOptions
 * @param {PageObject.SearchResult} searchResult
 */
export default makeSuite('Блок сортировки.', {
    feature: 'Сортировка по цене',
    params: {
        sortValue: 'Значение сортировки',
        price: 'Цена, ожидаемая у первого сниппета после сортировки',
    },
    story: {
        'При применении сортировки': {
            'выдача должна сортироваться по цене': makeCase({
                environment: 'kadavr',
                async test() {
                    await this.searchOptions.setSort(this.params.sortValue);

                    await this.browser.yaDelay(300);
                    await this.browser.yaParseUrl()
                        .should.eventually.be.link({
                            query: {
                                how: this.params.sortValue,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                            skipPathname: true,
                        });

                    const firstSnippetPrice = await this.snippetPrice.getPrice();

                    return this.expect(firstSnippetPrice)
                        .to.be.equal(this.params.price, 'Цена первого сниппета равна ожидаемой');
                },
            }),
        },
    },
});
