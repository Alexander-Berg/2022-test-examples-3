'use strict';

import {mergeSuites, PageObject, makeSuite, makeCase} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import openProductModalHook from '../hooks/openProductModal';

const TextNextLevitan = PageObject.get('TextNextLevitan');

/**
 * Тест на редактирование услуги
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Редактирование услуги.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3432',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При сохранении формы': {
            'данные услуги обновляются': makeCase({
                async test() {
                    const {productKey, productName} = this.params;
                    const clientId = '2020';
                    const contractId = '8';
                    const cmsPageId = '62325';

                    await this.form.setFieldValueByName('clientId', clientId, 'ID клиента в Балансе');

                    await this.form.setFieldValueByName('contractId', contractId, 'ID контракта');

                    if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                        await this.form.setFieldValueByName('cmsPageId', cmsPageId, 'ID страницы CMS');
                    }

                    await this.form.submit('Сохранить');

                    await this.modal.waitForHidden();

                    this.setPageObjects({
                        messages() {
                            return this.createPageObject('Messages');
                        },
                    });

                    await this.allure.runStep('Ожидаем появления сообщения об успешном изменении услуги', () =>
                        this.messages.waitForExist(),
                    );

                    await this.messages
                        .getMessageText()
                        .should.eventually.be.equal(
                            `Данные услуги «${productName}» обновлены`,
                            'Текст сообщения корректный',
                        );

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
                        prepaidFlagCaption() {
                            return this.createPageObject('TextNextLevitan', this.firstColumn.getFlagByIndex(0));
                        },
                        balanceOrderLink() {
                            return this.createPageObject('Link', this.firstColumn, this.firstColumn.balanceOrderLink);
                        },
                        secondColumn() {
                            return this.createPageObject(
                                'ProductColumn',
                                this.managerView,
                                this.managerView.getColumnByIndex(1),
                            );
                        },
                        clientIdValue() {
                            return this.createPageObject(
                                'TextNextLevitan',
                                this.secondColumn.getPlainDataByIndex(0),
                                `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                            );
                        },
                        contractIdValue() {
                            return this.createPageObject(
                                'TextNextLevitan',
                                this.secondColumn.getPlainDataByIndex(1),
                                `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                            );
                        },
                    });

                    await this.browser.allure.runStep(`Ожидаем появления блока услуги "${productName}"`, () =>
                        this.product.waitForExist(),
                    );

                    await this.browser.allure.runStep('Ожидаем появления деталей услуги', () =>
                        this.details.waitForExist(),
                    );

                    await this.browser.allure.runStep('Проверяем корректность деталей услуги', async () => {
                        await this.prepaidFlagCaption
                            .getText()
                            .should.eventually.be.equal('Предоплатный договор', 'Тип договора корректный');

                        await this.offerFlagCaption
                            .getText()
                            .should.eventually.be.equal('Оферта не принята', 'Статус принятия оферты корректный');

                        await this.balanceOrderLink
                            .isVisible()
                            .should.eventually.be.equal(true, 'Ссылка на заказ в Балансе отображается');

                        await this.clientIdValue
                            .getText()
                            .should.eventually.be.equal(clientId, 'ID клиента в Балансе корректный');

                        await this.contractIdValue
                            .getText()
                            .should.eventually.be.equal(contractId, 'ID контракта корректный');

                        // Проверка ID страницы CMS для услуги бренд-зоны
                        if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                            this.setPageObjects({
                                cmsPageIdValue() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.secondColumn.getPlainDataByIndex(2),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                            });

                            await this.cmsPageIdValue
                                .getText()
                                .should.eventually.be.equal(cmsPageId, 'ID страницы CMS корректный');
                        }
                    });
                },
            }),
        },
    }),
});
