import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductAnswersList} productAnswersList
 */
export default makeSuite('Блок списка ответов с количеством меньше 10.', {
    params: {
        answersCount: 'Количество вопросов на странице',
    },
    story: {
        'По умолчанию': {
            'кнопка "Показать еще" не отображается': makeCase({
                id: 'm-touch-2239',
                issue: 'MOBMARKET-9101',
                feature: 'Структура страницы',
                async test() {
                    const isLoadMoreButtonShown = await this.productAnswersList.isLoadMoreButtonShown();
                    await this.expect(isLoadMoreButtonShown)
                        .to.be.equal(false, 'Кнопка "Показать еще" не отображается');
                },
            }),
            'Отображается верное количество вопросов': makeCase({
                id: 'm-touch-2240',
                issue: 'MOBMARKET-9102',
                feature: 'Структура страницы',
                async test() {
                    const productAnswersCount = await this.productAnswersList.productAnswersCount;
                    await this.expect(productAnswersCount)
                        .to.be.equal(this.params.answersCount, 'Отображается верное количество вопросов');
                },
            }),
        },
    },
});
