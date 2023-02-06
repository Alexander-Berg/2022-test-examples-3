import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchOptions} SearchOptions
 */
export default makeSuite('Кнопка "Фильтры".', {
    environment: 'kadavr',
    params: {
        pageRoot: 'Ожидаемый path страницы фильтров',
    },
    story: {
        'При нажатии на кнопку': {
            'должен происходить переход на страницу с фильтрами': makeCase({
                async test() {
                    await this.searchOptions.clickOnFiltersButton();

                    return this.browser
                        .yaParseUrl()
                        .should.eventually.be.link({
                            pathname: `\\/${this.params.pageRoot}\\/filters`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
