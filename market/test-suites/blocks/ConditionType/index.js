import {makeSuite, makeCase} from 'ginny';


/**
 * @param {this.params.expectedConditionType}
 *
 * @property {PageObject.ConditionType} conditionType
 */
export default makeSuite('Лейбл уценённого товара', {
    story: {
        'По умолчанию': {
            'присутствует на странице': makeCase({
                async test() {
                    const isExisting = this.conditionType.isExisting();

                    await this.browser.allure.runStep('Проверяем наличие лейбла', () =>
                        this.expect(isExisting).to.be.equal(true, 'Лейбл присутствует на странице')
                    );

                    if (!this.params.expectedConditionType) {
                        return Promise.resolve();
                    }

                    const type = this.conditionType.getType();

                    return this.browser.allure.runStep('Проверяем пояснение уценки', () =>
                        this.expect(type).to.be.equal(
                            this.params.expectedConditionType,
                            `Пояснение уценки должно быть "${this.params.expectedConditionType}"`
                        )
                    );
                },
            }),
        },
    },
});
