import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отсутствие виджета средней цены
 * @param {PageObject.AveragePrice} averagePrice
 */
export default makeSuite('Виджет средней цены.', {
    story: {
        'Виджет отсутствует на странице': makeCase({
            test() {
                return this.averagePrice.isExisting()
                    .should.eventually.to.be.equal(
                        false,
                        'Виджет отсутствует на странице'
                    );
            },
        }),
    },
});
