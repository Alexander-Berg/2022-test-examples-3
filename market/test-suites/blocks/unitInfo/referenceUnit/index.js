import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок referenceUnit
 * @param {PageObject.parent} Любой PageObject в котором есть метод getReferenceUnitText()
 */
export default makeSuite('Справочная единица измерения.', {
    story: {
        'По умолчанию': {
            'Содержит цену за квадратный метр.': makeCase({
                params: {
                    expectedText: 'Ожидаемое значение для справочной единицы продажи товара.',
                },
                async test() {
                    const {expectedText = '1 775 ₽/м²'} = this.params;

                    await this.parent.waitForVisible();

                    const referenceUnitText = await this.parent.getReferenceUnitText();

                    return this.expect(referenceUnitText).to.be.equal(
                        expectedText,
                        'В товаре есть справочная единица измерения'
                    );
                },
            }),
        },
    },
});
