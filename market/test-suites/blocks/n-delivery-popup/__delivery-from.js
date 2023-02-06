import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery-popup__delivery-from
 * @param {PageObject.SnippetCard2} snippetCard
 * @param {PageObject.DeliveryPopup} deliveryPopup
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Попап доставки. Блок места отправления товара.', {
    story: {
        'По умолчанию': {
            'текст соответствует ожидаемому.': makeCase({
                feature: 'Доставка на сниппетах',
                id: 'marketfront-2935',
                issue: 'MARKETVERSTKA-31559',
                params: {
                    expectedDeliveryTo: 'Ожидаемый текст места прибытия товара',
                },
                async test() {
                    const {expectedDeliveryTo} = this.params;

                    await this.delivery.waitForVisible();
                    await this.delivery.clickInfo();

                    await this.deliveryPopup.waitForVisible();
                    return this.deliveryPopup.getDeliveryFromText()
                        .should.eventually.be.equal(expectedDeliveryTo, 'текст соответствует ожидаемому');
                },
            }),
        },
    },
});
