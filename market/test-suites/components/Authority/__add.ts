'use strict';

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на добавление логина для роли
 * @param {PageObject.MultiTextInput} multiTextInput - поле добавления логина
 * @param {PageObject.Tag} tag - тег с добавленным логином
 * @param {Object} params
 * @param {string} params.login – добавляемый логин
 */
export default makeSuite('Добавление логина для роли.', {
    feature: 'Управление пользователями',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При добавлении логина': {
            'в списке появляется тег': makeCase({
                async test() {
                    const {login} = this.params;

                    await this.browser.allure.runStep('Ожидаем появления поля ввода логинов', () =>
                        this.multiTextInput.waitForVisible(),
                    );

                    await this.browser.allure.runStep('Ожидаем загрузки списка логинов', () =>
                        this.browser.waitUntil(
                            () => this.multiTextInput.input.isEnabled(),
                            /*
                             * Запросы выполняются поочередно, поэтому максимальный таймаут n-го запроса
                             * равен n * 5000, где n - кол-во ролей
                             */
                            85000,
                            'Поле ввода логинов не разблокировалось',
                        ),
                    );

                    await this.multiTextInput.setValue(login);

                    await this.browser.allure.runStep('Кликаем по кнопке добавления логина', () =>
                        this.multiTextInput.button.click(),
                    );

                    await this.browser.allure.runStep('Ожидаем появления тега в списке', () => this.tag.waitForExist());

                    await this.tag.root
                        .getText()
                        .should.eventually.be.equal(login, 'Тег с добавленным логином появился');
                },
            }),
        },
    },
});
