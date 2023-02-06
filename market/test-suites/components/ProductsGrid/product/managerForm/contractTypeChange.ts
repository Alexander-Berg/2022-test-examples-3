'use strict';

import {makeSuite, makeCase, mergeSuites} from 'ginny';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест перевода услуги на другой тип договора
 * (!) Перевод на предоплатный договор проверяется при подключении услуги.
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Изменение типа договора.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3459',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При снятии галочки': {
            'услуга переводится на постоплатный договор': makeCase({
                async test() {
                    await this.browser.allure.runStep('Снимаем чекбокс предоплатного договора', () =>
                        this.form.checkboxEndsWithId('isContractTypePrepaid').click(),
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
                        contractFlagCaption() {
                            return this.createPageObject('TextNextLevitan', this.firstColumn.getFlagByIndex(0));
                        },
                    });

                    await this.browser.allure.runStep('Ожидаем появления деталей услуги', () =>
                        this.details.waitForExist(),
                    );

                    await this.contractFlagCaption
                        .getText()
                        .should.eventually.be.equal('Постоплатный договор', 'Тип договора корректный');
                },
            }),
        },
    }),
});
