import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery__text
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Текст предзаказа', {
    feature: 'Предзаказ',
    story: {
        'Текст cоответствует ожидаемому': makeCase({
            params: {
                expectedText: 'Ожидаемый текст',
            },
            async test() {
                const {expectedText} = this.params;
                const pickupText = await this.delivery.getDeliveryText();
                return this.expect(pickupText).to.be.equal(
                    expectedText,
                    'Текст соответствует ожидаемому'
                );
            },
        }),
    },
});
