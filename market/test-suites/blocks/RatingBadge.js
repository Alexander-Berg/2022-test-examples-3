import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на компонент RatingBadge.
 * @param {PageObject.RatingBadge} badge
 */
export default makeSuite('Блок рейтинга отзыва.', {
    story: {
        'По умолчанию': {
            'содержит корректную оценку': makeCase({
                id: 'm-touch-1873',
                test() {
                    return this.badge.getRatingText()
                        .then(Number)
                        .should.eventually
                        .to.be.within(1, 5, 'Значение находится между 1 и 5');
                },
            }),
            'отображает иконку с соответствующим цветом': makeCase({
                id: 'm-touch-1874',
                test() {
                    return Promise.all([
                        this.badge.getRatingText(),
                        this.badge.getRatingValueByColor(),
                    ])
                        .then(([ratingValue, ratingByColor]) => this.browser.allure.runStep(
                            `Сравниваем значение рейтинга '${Math.floor(Number(ratingValue))}' ` +
                            `и полученное из цвета шеврона '${ratingByColor}'`,
                            // стали приходить значения оценок, округленные до 0.5,
                            // а цвет для таких берем из старшего разряда
                            () => Math.floor(Number(ratingValue)) === Number(ratingByColor)
                        ))
                        .should.eventually.be.equal(true, 'Цвет иконки рейтинга корректный');
                },
            }),
        },
    },
});
