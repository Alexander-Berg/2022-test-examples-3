import {makeSuite, makeCase} from 'ginny';


const ratingColors = [
    'empty',
    'horrible',
    'bad',
    'normal',
    'good',
    'excellent',
];

/**
 * Тест на блок badge.
 * @param {PageObject.Badge} badge
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
                    return this.badge
                        .getRatingText()
                        .then(ratingValue => ratingColors[Number(ratingValue)])
                        .then(ratingClassName => this.badge.root.yaHasClass(`badge_rating_${ratingClassName}`))
                        .should.eventually.be.equal(true, 'Цвет иконки рейтинга корректный');
                },
            }),
        },
    },
});
