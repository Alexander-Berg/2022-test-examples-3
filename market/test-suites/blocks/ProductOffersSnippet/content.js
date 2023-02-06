import {makeCase, makeSuite} from 'ginny';


/**
 * Тесты на показ информации в блоке ProductOffersSnippet
 */
export default makeSuite('Содержимое сниппета в топ6', {
    params: {
        deliveryTexts: 'Тексты о доставке (массив)',
    },
    story: {
        'Ожидаемое': makeCase({
            async test() {
                const {
                    deliveryTexts,
                } = this.params;

                if (deliveryTexts) {
                    const deliveryTextPromise = this.productOffersSnippet.delivery.getText();

                    await Promise.all(
                        deliveryTexts.map(text =>
                            deliveryTextPromise
                                .should.eventually.to.have.string(
                                    text,
                                    `В тексте о доставке должен быть текст: ${text}`
                                )
                        )
                    );
                }
            },
        }),
    },
});
