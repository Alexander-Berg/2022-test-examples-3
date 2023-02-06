import {makeSuite, makeCase, mergeSuites} from 'ginny';

const COLORS_MAP = {
    1: '#f99f47',
    2: '#f6c000',
    3: '#c1c710',
    4: '#8cb400',
    5: '#359e00',
};

/**
 * Тесты на иконку рейтинга
 * @property {PageObject.Rating} this.rating - иконка рейтинга
 */

export default makeSuite('Иконка рейтинга', {
    feature: 'Иконка рейтинга',
    story: mergeSuites({
        'По умолчанию': {
            'отображается и содержит корректную оценку и цвет': makeCase({
                test() {
                    return this.rating.getRatingValue()
                        .should.eventually.to.be.within(1, 5, 'Значение находится между 1 и 5')
                        .then(ratingValue => {
                            let roundedValue;

                            // копипаста округления рейтинга в нашей текущей реализации
                            if (ratingValue > 4.25) {
                                roundedValue = 5;
                            } else if (ratingValue > 3.5) {
                                roundedValue = 4;
                            } else if (ratingValue > 2.5) {
                                roundedValue = 3;
                            } else if (ratingValue > 1.5) {
                                roundedValue = 2;
                            } else if (ratingValue <= 1.5) {
                                roundedValue = 1;
                            }

                            return this.rating.getStyleColor()
                                .should.eventually.equal(COLORS_MAP[roundedValue], 'Иконка имеет ожидаемый цвет');
                        });
                },
            }),
        },
    }),
});
