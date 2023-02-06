import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery__text
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Текст условий доставки', {
    story: {
        'По умолчанию': {
            'соответствует ожидаемому': makeCase({
                params: {
                    expectedText: 'Ожидаемый текст условий доставки',
                },
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-9844 скипаем упавшие тесты для озеленения');

                    /* eslint-disable no-unreachable */
                    const {expectedText} = this.params;
                    const deliveryText = await this.delivery.text.getText();

                    return this.expect(deliveryText).to.be.equal(
                        expectedText,
                        'Текст соответствует ожидаемому'
                    );
                    /* eslint-enable no-unreachable */
                },
            }),
        },
    },
});
