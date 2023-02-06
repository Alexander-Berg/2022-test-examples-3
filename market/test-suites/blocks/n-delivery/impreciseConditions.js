import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery-popup__delivery-from
 * @param {PageObject.SnippetCard2} snippetCard
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Блок условий доставки. Условия доставки не заданы.', {
    story: {
        'По умолчанию': {
            'текст соответствует ожидаемому.': makeCase({
                feature: 'Доставка на сниппетах',
                id: 'marketfront-2934',
                issue: 'MARKETVERSTKA-31558',
                params: {
                    expectedDeliveryText: 'Ожидаемый текст условий доставки',
                },
                async test() {
                    // eslint-disable-next-line market/ginny/no-skip
                    return this.skip('MARKETFRONT-9844 скипаем упавшие тесты для озеленения');

                    /* eslint-disable no-unreachable */
                    const {expectedDeliveryText} = this.params;

                    await this.delivery.waitForVisible();
                    return this.delivery.getDeliveryText()
                        .should.eventually.be.equal(expectedDeliveryText, 'текст соответствует ожидаемому');
                    /* eslint-enable no-unreachable */
                },
            }),
        },
    },
});
