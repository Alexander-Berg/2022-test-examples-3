import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery__text
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Текст условий самовывоза', {
    feature: 'Самовывоз',
    story: {
        'Текст условий самовывоза cоответствует ожидаемому': makeCase({
            params: {
                expectedText: 'Ожидаемый текст условий самовывоза',
            },
            async test() {
                const {expectedText} = this.params;
                const pickupText = await this.delivery.getPickupText();

                return this.expect(pickupText).to.be.equal(
                    expectedText,
                    'Текст соответствует ожидаемому'
                );
            },
        }),
    },
});
