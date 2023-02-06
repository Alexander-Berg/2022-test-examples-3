'use strict';

import {makeSuite, makeCase, mergeSuites} from 'ginny';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест на валидацию ID клиента в Балансе
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Валидация ID клиента в Балансе.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3437',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При сохранении формы': {
            'появляется тултип с ошибкой': makeCase({
                async test() {
                    this.setPageObjects({
                        spinner() {
                            return this.createPageObject('SpinnerLevitan', this.form);
                        },
                    });

                    await this.form.setFieldValueByName('clientId', '123', 'ID клиента в Балансе');

                    await this.form.setFieldValueByName('contractId', '33', 'ID контракта');

                    await this.form.submit('Сохранить');

                    await this.browser.allure.runStep('Ожидаем сохранения формы', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visible = await this.spinner.isVisible();

                                return visible === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Не удалось дождаться сохранения формы',
                        ),
                    );

                    await this.form.submitErrorPopup
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Тултип с ошибкой у кнопки "Сохранить" отображается');

                    await this.form.submitErrorPopup
                        .getText()
                        .should.eventually.be.equal('Неверный ID клиента в Балансе', 'Текст ошибки корректный');
                },
            }),
        },
    }),
});
