import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductCardPopularQuestions} ProductCardPopularQuestions
 */
export default makeSuite('Кнопка "Показать еще", если вопросов <= 3.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'кнопка не отображается': makeCase({
                id: 'marketfront-3718',
                issue: 'MARKETFRONT-362',
                async test() {
                    await this.productCardPopularQuestions.isLoadMoreButtonVisible()
                        .should.eventually.to.be.equal(false, 'Кнопка "Показать еще" не отображается');
                },
            }),
        },
    },
});
