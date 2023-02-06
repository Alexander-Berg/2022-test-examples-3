'use strict';

import {makeCase, makeSuite, mergeSuites} from 'ginny';

import showProductHook from './product/hooks/showProduct';

/**
 * Тесты на фильтр переключения валюты
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - заголовок блока
 */
export default makeSuite('Переключение валюты.', {
    environment: 'kadavr',
    feature: 'Управление балансом',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        showProductHook(),
        {
            async beforeEach() {
                this.setPageObjects({
                    productsControls() {
                        return this.createPageObject('ProductsControls');
                    },
                    currencySelectField() {
                        return this.createPageObject(
                            'SelectLevitan',
                            this.productsControls,
                            this.productsControls.currencyViewFilter,
                        );
                    },
                    balance() {
                        return this.createPageObject('TextLevitan', this.product, this.product.balance);
                    },
                });

                await this.browser.allure.runStep('Выбираем отображение баланса в у. е.', async () => {
                    await this.currencySelectField.click();
                    await this.currencySelectField.waitForSelectShown();
                    return this.currencySelectField.selectItemByIndex(0);
                });

                await this.currencySelectField
                    .getText()
                    .should.eventually.be.equal('Показывать в у.е.', 'В селекте выбрана верная опция');

                return this.balance.getText().should.eventually.be.equal('100 у. е.', 'Баланс отображается в у. е.');
            },
        },
        {
            'Меняется валюта в отображении баланса услуги': {
                'при переключении фильтра': makeCase({
                    async test() {
                        await this.browser.allure.runStep('Выбираем отображение баланса в рублях', async () => {
                            await this.currencySelectField.click();
                            await this.currencySelectField.waitForSelectShown();
                            return this.currencySelectField.selectItemByIndex(1);
                        });

                        await this.balance
                            .getText()
                            .should.eventually.be.equal('3 000 ₽', 'Баланс отображается в рублях');

                        // возвращаем отображение баланса обратно, чтобы не сломать другие АТ
                        await this.browser.allure.runStep('Выбираем отображение баланса в у. е.', async () => {
                            await this.currencySelectField.click();
                            await this.currencySelectField.waitForSelectShown();
                            return this.currencySelectField.selectItemByIndex(0);
                        });

                        return this.currencySelectField
                            .getText()
                            .should.eventually.be.equal('Показывать в у.е.', 'В селекте выбрана верная опция');
                    },
                }),
            },
        },
    ),
});
