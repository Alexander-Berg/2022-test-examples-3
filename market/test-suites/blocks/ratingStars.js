import {makeSuite, makeCase, mergeSuites} from 'ginny';

/**
 * Тесты на звёзды рейтинга
 * @property {PageObject.RatingStars} this.ratingStars - RatingStars
 * @property {object} this.params - содержит ожидаемое количество звезд в блоке (expectedRating)
 */

export default makeSuite('Звёзды рейтинга', {
    feature: 'Звёзды рейтинга',
    story: mergeSuites({
        'По умолчанию': {
            'должны отображать закрашенные звёзды в соответствии с рейтингом.': makeCase({
                test() {
                    // todo: заменить на скрин тесты
                    return this.ratingStars.getRating()
                        .should.eventually.to.be.equal(
                            this.params.expectedRating,
                            'Отображается правильное число закрашенных звезд'
                        );
                },
            }),
        },
    }),
});
