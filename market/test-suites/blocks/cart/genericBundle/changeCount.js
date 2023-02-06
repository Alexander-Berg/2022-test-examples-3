import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Изменение количества основного товара', {
    feature: 'Акция товар + подарок',
    id: 'bluemarket-3124',
    story: {
        'При увеличении количества основного товара': {
            async beforeEach() {
                await this.primaryAmountSelect.plusFromButton();
            },
            'синхронно увеличивается количество подарков': makeCase({
                async test() {
                    const expectedCount = this.yaTestData.bundles.count + 1;

                    await this.browser.allure.runStep(
                        'Проверяем количество подарков',
                        () => this.giftAmountText.getText()
                            .should.eventually.be.equal(`${expectedCount} шт.`,
                                `Количество подарков в корзине должно увеличиться до ${expectedCount}`)
                    );
                },
            }),
            'и уменьшении его количества обратно,': {
                async beforeEach() {
                    await this.primaryAmountSelect.minusFromButton();
                },
                'синхронно изменяется количество подарков': makeCase({
                    async test() {
                        const expectedCount = this.yaTestData.bundles.count;

                        await this.browser.allure.runStep(
                            'Проверяем количество подарков',
                            () => this.giftAmountText.getText()
                                .should.eventually.be.equal(`${expectedCount} шт.`,
                                    `Количество подарков в корзине должно уменьшиться до ${expectedCount}`)
                        );
                    },
                }),
            },
        },
    },
});
