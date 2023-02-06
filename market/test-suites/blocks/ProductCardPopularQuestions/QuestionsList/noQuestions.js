import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductCardPopularQuestions} ProductCardPopularQuestions
 */
export default makeSuite('Лента популярных вопросов без вопросов.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'блок с лентой не отображается': makeCase({
                id: 'marketfront-3714',
                issue: 'MARKETFRONT-362',
                async test() {
                    await this.productCardPopularQuestions.isProductCardPopularQuestionsVisible()
                        .should.eventually.to.be.equal(false, 'Лента популярных вопросов не отображается');
                },
            }),
        },
    },
});
