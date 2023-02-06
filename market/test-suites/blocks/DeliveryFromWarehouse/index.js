import {makeSuite, makeCase} from 'ginny';

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

                    return this.deliveryFromWarehouse.getElementText()
                        .should.eventually.be.equal(expectedElementText, 'текст соответствует ожидаемому');
                },
            }),
        },
    },
});
