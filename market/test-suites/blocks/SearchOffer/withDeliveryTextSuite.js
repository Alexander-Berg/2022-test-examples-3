import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippetDelivery} deliveryInfo
 */
export default makeSuite('В сроках и способах доставки.', {
    environment: 'kadavr',
    params: {
        deliveryText: 'Текст о доставке',
    },
    story: {
        'Текст доставки': {
            'отображается': makeCase({
                async test() {
                    const {deliveryText} = this.params;

                    await this.browser.allure.runStep(
                        'Проверяем отображение доставки',
                        () => this.deliveryInfo.getText()
                            .should.eventually.to.equal(
                                deliveryText,
                                `Отображается: ${deliveryText}`
                            )
                    );
                },
            }),
        },
    },
});
