'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на недоступность кнопки редактирования услуги для читающего менеджера
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Кнопка редактирования.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3346',
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
            'При просмотре читающим менеджером': {
                отсутствует: makeCase({
                    id: 'vendor_auto-828',
                    test() {
                        return this.browser.allure.runStep('Проверяем недоступность кнопки', () =>
                            this.details.editButton.vndIsExisting().should.eventually.be.equal(false, 'Кнопка скрыта'),
                        );
                    },
                }),
            },
        },
    ),
});
