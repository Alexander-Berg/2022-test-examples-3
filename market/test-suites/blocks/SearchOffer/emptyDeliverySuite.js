import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.SearchSnippetDelivery} deliveryInfo
 */
export default makeSuite('Сроки и способы доставки.', {
    environment: 'kadavr',
    story: {
        'Текст доставки': {
            'скрыт': makeCase({
                async test() {
                    const isExisting = await this.deliveryInfo.isExisting().catch(() => false);
                    return this.browser.allure.runStep(
                        'Проверяем отображение доставки',
                        () => this.expect(isExisting).to.be.equal(false, 'Доставка скрыта')
                    );
                },
            }),
        },
    },
});
