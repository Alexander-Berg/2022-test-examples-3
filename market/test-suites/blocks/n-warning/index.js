import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Warning} disclaimer
 * @param {RegExp} pattern - регулярное выражение для текста дисклеймера
 * @param {string} type - тип дисклеймера
 */
export default makeSuite('Дисклеймер', {
    feature: 'Дисклеймеры',
    params: {
        pattern: 'Ожидаемая регулярка для текста дисклеймера',
        type: 'Ожидаемый тип дисклеймера',
    },
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep(`Смотрим наличие дисклеймера "${this.params.pattern}"`, () => (
                        this.disclaimer.isVisible()
                            .should.eventually.equal(true, 'Дисклеймер присутствует')
                    ));
                },
            }),

            'должен иметь правильный тип': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверяем тип дисклеймера', () => (
                        this.disclaimer.hasWarningType(this.params.type)
                            .should.eventually.equal(true, 'Тип дисклеймера правильный')
                    ));
                },
            }),

            'должен содержать правильный текст': makeCase({
                test() {
                    return this.disclaimer.getText()
                        .should.eventually.match(this.params.pattern, 'Текст дисклеймера верный');
                },
            }),
        },
    },
});
