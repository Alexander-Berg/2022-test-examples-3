import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на компонент RatingChubbyStars.
 * @param {PageObject.RatingChubbyStars} stars
 */
export default makeSuite('Блок рейтинга отзыва.', {
    story: {
        'По умолчанию': {
            'число звезд совпадает с ожидаемым значением рейтинга': makeCase({
                id: 'm-touch-2957',
                async test() {
                    const [ratingStars, ratingData] = await Promise.all([
                        this.stars.getRatingValueByStars(),
                        this.stars.getRatingValueFromData(),
                    ]);
                    await this.expect(ratingData)
                        .to.be.equal(this.params.expectedRatingValue, 'рейтинг совпадает с ожидаемым значением');

                    await this.expect(ratingStars)
                        .to.be.equal(this.params.expectedRatingValue, 'число звезд совпадает с ожидаемым значением');
                },
            }),
        },
    },
});
