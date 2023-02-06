import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на компонент RatingChubbyStars.
 * @param {PageObject.RatingChubbyStars} stars
 */
export default makeSuite('Блок рейтинга отзыва.', {
    story: {
        'По умолчанию': {
            'количество звёзд от 1 до 5': makeCase({
                id: 'm-touch-2957',
                test() {
                    return this.stars.getRatingValueByStars()
                        .then(Number)
                        .should.eventually
                        .to.be.within(1, 5, 'Значение находится между 1 и 5');
                },
            }),
            'число звезд совпадает с рейтингом': makeCase({
                id: 'm-touch-2957',
                test() {
                    return Promise.all([
                        this.stars.getRatingValueByStars(),
                        this.stars.getRatingValueFromData(),
                    ]).then(([ratingStars, ratingData]) => Number(ratingStars) === Number(ratingData))
                        .should.eventually.be.equal(true, 'число звезд совпадает с рейтингом');
                },
            }),
        },
    },
});
