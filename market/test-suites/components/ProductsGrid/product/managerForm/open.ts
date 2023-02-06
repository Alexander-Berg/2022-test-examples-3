'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

import showProductHook from '../hooks/showProduct';

/**
 * Тесты на открытие модального окна по кнопке редактирования услуги
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Кнопка редактирования.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3389',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook({
            details: true,
        }),
        {
            'При нажатии': {
                'открывается модальное окно': makeCase({
                    async test() {
                        const {productName} = this.params;

                        await this.browser.allure.runStep('Проверяем доступность кнопки', () =>
                            this.details.editButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка отображается'),
                        );

                        await this.browser.allure.runStep('Нажимаем на "карандаш"', () =>
                            this.details.editButton.click(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем открытия модального окна с информацией об услуге',
                            () => this.modal.waitForVisible(),
                        );

                        this.setPageObjects({
                            modalTitle() {
                                return this.createPageObject('TitleB2b', this.modal);
                            },
                        });

                        await this.modalTitle
                            .getText()
                            .should.eventually.be.equal(productName, 'Текст заголовка корректный');
                    },
                }),
            },
        },
    ),
});
