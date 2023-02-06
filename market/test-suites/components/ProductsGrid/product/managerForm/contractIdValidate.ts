'use strict';

import {makeSuite, makeCase, mergeSuites} from 'ginny';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест на валидацию ID контракта для контрактного вендора
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Валидация ID контракта.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3454',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При вводе невалидного значения': {
            'появляется хинт с ошибкой': makeCase({
                async test() {
                    await this.form.setFieldValueByName('contractId', '0', 'ID контракта');

                    this.setPageObjects({
                        tooltip() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal('Поле заполнено некорректно', 'Текст ошибки корректный');

                    await this.browser.allure.runStep('Удаляем значение поля "ID контракта"', () =>
                        this.form.getFieldByName('contractId').vndSetValue('', true),
                    );

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal('Поле обязательно для заполнения', 'Текст ошибки корректный');
                },
            }),
        },
    }),
});
