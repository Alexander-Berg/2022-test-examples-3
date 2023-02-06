'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на сохранение новой ставки
 * @param {PageObject.ModelsPromotionListItem} item - товар из списка
 * @param {PageObject.TextFieldLevitan} textField - поле ввода ставки
 * @param {PageObject.RatesControlBar} bar - ссылка установки ставки
 * @param {PageObject.ButtonB2b} submitButton - кнопка применения ставки
 * @param {Object} params
 * @param {string} params.value - применяемое значение
 * @param {string} params.status - статус применения ставки
 */
export default makeSuite('Применение ставки.', {
    issue: 'VNDFRONT-2249',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При успешном сохранении ставки': {
            'отображается значок часов': makeCase({
                async test() {
                    const {value, status} = this.params;

                    await this.textField.setValue(value);

                    await this.browser.allure.runStep('Ожидаем появления сайдбара управления ставками', () =>
                        this.bar.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Проверяем текст кнопки', () =>
                        this.submitButton
                            .getText()
                            .should.eventually.be.equal('Назначить ставки (1)', 'Текст кнопки корректный'),
                    );

                    await this.browser.allure.runStep('Применяем новую ставку', () => this.submitButton.click());

                    await this.allure.runStep('Ожидаем скрытия сайдбара управления ставками', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visible = await this.bar.isVisible();

                                return visible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Не удалось дождаться скрытия сайдбара',
                        ),
                    );

                    await this.browser.allure.runStep('Проверяем текст статуса', () =>
                        this.item.applyingBidIndicator
                            .getText()
                            .should.eventually.be.equal(status, 'Текст статуса корректный'),
                    );
                },
            }),
        },
    },
});
