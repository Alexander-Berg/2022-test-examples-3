'use strict';

import {makeSuite, makeCase, mergeSuites} from 'ginny';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест перевода услуги с контракта на оферту
 * (!) Перевод на контракт проверяется при подключении услуги.
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Перевод на оферту.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3462',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При сбрасывании ID контракта': {
            'услуга переводится на оферту': makeCase({
                async test() {
                    await this.browser.allure.runStep('Удаляем значение поля "ID контракта"', () =>
                        this.form.getFieldByName('contractId').vndSetValue(''),
                    );

                    await this.form.submit('Сохранить');

                    await this.modal.waitForHidden();

                    this.setPageObjects({
                        details() {
                            return this.createPageObject('ProductDetails', this.product);
                        },
                        managerView() {
                            return this.createPageObject('ProductManagerView', this.details);
                        },
                        firstColumn() {
                            return this.createPageObject(
                                'ProductColumn',
                                this.managerView,
                                this.managerView.getColumnByIndex(0),
                            );
                        },
                        offerFlagCaption() {
                            return this.createPageObject('TextNextLevitan', this.firstColumn.getFlagByIndex(1));
                        },
                    });

                    await this.browser.allure.runStep('Ожидаем появления деталей услуги', () =>
                        this.details.waitForExist(),
                    );

                    await this.offerFlagCaption
                        .getText()
                        .should.eventually.be.equal('Оферта принята', 'Статус принятия офферты корректный');
                },
            }),
        },
    }),
});
