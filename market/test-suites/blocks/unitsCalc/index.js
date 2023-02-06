import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок CartItemUnitsCalc
 * @param {PageObject.CartItemUnitsCalc} unitsCalc
 */
export default makeSuite('Калькулятор упаковок у элемента.', {
    feature: 'UnitsCalc',
    story: {
        'По умолчанию': {
            'у элемента выводится калькулятор упаковок': makeCase({
                environment: 'kadavr',
                async test() {
                    return this.unitsCalc.getUnitCalcText()
                        .should.eventually.be.equal(this.params.expectedText, 'отображается калькулятор под заголовком в корзине в формате 1 уп = 1,33м²');
                },
            }),
        },
    },
});
