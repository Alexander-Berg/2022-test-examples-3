'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест на валидацию полей формы редактирования услуги
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Валидация полей формы редактирования услуги.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3425',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При вводе невалидных данных': {
            'появляются хинты с ошибками': makeCase({
                async test() {
                    const {productKey} = this.params;

                    this.setPageObjects({
                        tooltip() {
                            return this.createPageObject('PopupB2b');
                        },
                    });

                    /**
                     * ID клиента в Балансе
                     */
                    await this.browser.allure.runStep('Вводим в поле "ID клиента в Балансе" невалидное значение', () =>
                        this.form.getFieldByName('clientId').vndSetValue('id', true),
                    );

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal('Укажите число', 'Текст ошибки корректный');

                    await this.browser.allure.runStep('Удаляем значение поля "ID клиента в Балансе"', () =>
                        this.form.getFieldByName('clientId').vndSetValue('', true),
                    );

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal('Поле обязательно для заполнения', 'Текст ошибки корректный');

                    await this.browser.allure.runStep('Вводим в поле "ID клиента в Балансе" валидное значение', () =>
                        this.form.getFieldByName('clientId').vndSetValue('1', true),
                    );

                    await this.browser.allure.runStep('Ожидаем скрытия хинта с ошибкой', () =>
                        this.tooltip.waitForPopupHidden(),
                    );

                    /**
                     * ID контракта
                     */
                    await this.browser.allure.runStep('Вводим в поле "ID контракта" невалидное значение', () =>
                        this.form.getFieldByName('contractId').vndSetValue('id', true),
                    );

                    await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                        this.tooltip.waitForPopupShown(),
                    );

                    await this.tooltip
                        .getActiveText()
                        .should.eventually.be.equal('Укажите число', 'Текст ошибки корректный');

                    await this.browser.allure.runStep('Вводим в поле "ID контракта" валидное значение', () =>
                        this.form.getFieldByName('contractId').vndSetValue('0', true),
                    );

                    await this.browser.allure.runStep('Ожидаем скрытия хинта с ошибкой', () =>
                        this.tooltip.waitForPopupHidden(),
                    );

                    /**
                     * ID страницы CMS (только бренд-зона)
                     */
                    if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                        await this.browser.allure.runStep('Вводим в поле "ID страницы CMS" невалидное значение', () =>
                            this.form.getFieldByName('cmsPageId').vndSetValue('id', true),
                        );

                        await this.browser.allure.runStep('Ожидаем появления хинта с ошибкой', () =>
                            this.tooltip.waitForPopupShown(),
                        );

                        await this.tooltip
                            .getActiveText()
                            .should.eventually.be.equal('Укажите число', 'Текст ошибки корректный');

                        await this.browser.allure.runStep('Вводим в поле "ID страницы CMS" валидное значение', () =>
                            this.form.getFieldByName('cmsPageId').vndSetValue('1', true),
                        );

                        await this.browser.allure.runStep('Ожидаем скрытия хинта с ошибкой', () =>
                            this.tooltip.waitForPopupHidden(),
                        );
                    }
                },
            }),
        },
    }),
});
