import {makeSuite, makeCase} from 'ginny';

/**
* Тесты на компонент RatingStars.
* @param {PageObject.RatingStars} ratingStars
*/
export default makeSuite('Блок рейтинга со звёздами.', {
    story: {
        'При нажатии на звезду': {
            'должен установиться соответствующий рейтинга и его подпись': makeCase({
                feature: 'Оставление отзыва',
                id: 'marketfront-803',
                issue: 'MARKETVERSTKA-24466',
                params: {
                    ratingToSet: 'Значение рейтинга от 1 до 5',
                },
                test() {
                    const rating = Number(this.params.ratingToSet);

                    return this.ratingStars.setRating(rating)
                        .then(() => this.ratingStars.getRating())
                        .should.eventually.equal(rating, `рейтинг должен установиться в значение ${rating}`);
                },
            }),
        },
    },
});
