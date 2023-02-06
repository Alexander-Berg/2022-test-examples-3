'use strict';

import {makeSuite, mergeSuites, makeCase} from 'ginny';

/**
 * Тесты на список услуг
 * @param {PageObject.Products} products - список услуг
 * @param {Object} params
 * @param {number} params.count - количество блоков услуг
 */
export default makeSuite('Список услуг.', {
    issue: 'VNDFRONT-1887',
    environment: 'testing',
    feature: 'Список услуг',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites({
        beforeEach() {
            return this.allure.runStep('Ожидаем появления списка услуг', () => this.products.waitForExist());
        },
        'Отображает корректное количество блоков услуг': makeCase({
            id: 'vendor_auto-74',
            params: {
                count: 'Количество услуг',
            },
            test() {
                return this.products
                    .getItemsCount()
                    .should.eventually.equal(this.params.count, 'Отображаются все блоки');
            },
        }),
    }),
});
