import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.SearchSnippetDelivery} snippetDelivery
 */
export default makeSuite('Доставка "Со склада Яндекса".', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'текст соответствует ожидаемому': makeCase({
                async test() {
                    const {expectedElementText} = this.params;
                    const deliveryText = await this.snippetDelivery.getText();
                    return this.expect(deliveryText).to.be.equal(
                        expectedElementText,
                        'текст блока ожидаемый'
                    );
                },
            }),
        },
    },
});
