import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DeliveryFromWarehouse
 * @param {PageObject.DeliveryFromWarehouse} deliveryFromWarehouse
 */
export default makeSuite('Доставка "Со склада Яндекса".', {
    story: {
        'По умолчанию': {
            'текст соответствует ожидаемому': makeCase({
                async test() {
                    const {expectedElementText} = this.params;
                    const deliveryText = await this.deliveryFromWarehouse.getContainerText();
                    return this.expect(deliveryText).to.be.equal(
                        expectedElementText,
                        'текст блока ожидаемый'
                    );
                },
            }),
        },
    },
});

