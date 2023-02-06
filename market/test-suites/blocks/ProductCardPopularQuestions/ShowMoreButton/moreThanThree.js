import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductCardPopularQuestions} ProductCardPopularQuestions
 */
export default makeSuite('Кнопка "Показать еще".', {
    environment: 'kadavr',
    params: {
        expectedButtonUrl: 'Ожидаемая ссылка на страницу вопросов',
    },
    story: {
        'По умолчанию': {
            'кнопка отображается': makeCase({
                id: 'marketfront-3720',
                issue: 'MARKETFRONT-362',
                async test() {
                    await this.productCardPopularQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(true, 'Кнопка "Показать еще" отображается');
                },
            }),
            'содержит ссылку на страницу вопроса': makeCase({
                id: 'marketfront-3720',
                issue: 'MARKETFRONT-362',
                async test() {
                    const currentHref = await this.productCardPopularQuestions.loadMoreButtonHref;

                    await this.expect(currentHref, 'ссылка корректная')
                        .to.be.link({pathname: this.params.expectedButtonUrl}, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
