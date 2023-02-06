import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на блок n-w-reviews.
 * @param {PageObject.Reviews} reviews
 */
export default makeSuite('Пустой список отзывов.', {
    id: 'marketfront-2599',
    issue: 'MARKETVERSTKA-29507',
    feature: 'Структура страницы',
    story: {
        'Если отзывов нет': {
            'отображается текст "Ни одного отзыва".': makeCase({
                async test() {
                    const text = await this.reviews.getEmptyText();
                    await this.expect(text).to.be.equal('Ни одного отзыва');
                },
            }),
        },
    },
});
