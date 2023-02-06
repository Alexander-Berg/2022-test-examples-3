'use strict';

import {mergeSuites, PageObject, makeSuite, makeCase} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import openProductModalHook from '../hooks/openProductModal';

const TextNextLevitan = PageObject.get('TextNextLevitan');

/**
 * Тест на сохранение деталей услуги без внесения изменений
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Редактирование услуги. Сохранение без изменений.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-4157',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При сохранении формы без изменений': {
            'данные услуги сохраняются': makeCase({
                async test() {
                    const {productName, productKey} = this.params;
                    const clientId = '100200300';
                    const contractId = '—';
                    const cmsPageId = '999';
                    const brandZoneTariff = 'Продажи';
                    const analyticsTariff = '10 категорий';
                    const analyticsCategories = '3 категории';

                    await this.form.submit('Сохранить');

                    await this.modal.waitForHidden();

                    this.setPageObjects({
                        toasts() {
                            return this.createPageObject('NotificationGroupLevitan');
                        },
                        firstToast() {
                            return this.createPageObject(
                                'NotificationLevitan',
                                this.toasts,
                                this.toasts.getItemByIndex(0),
                            );
                        },
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

                    await this.allure.runStep('Ожидаем появления группы сообщений', () => this.toasts.waitForExist());

                    await this.allure.runStep('Ожидаем появления сообщения об успешном изменении услуги', () =>
                        this.firstToast.waitForExist(),
                    );

                    await this.firstToast
                        .getText()
                        .should.eventually.be.equal(
                            `Данные услуги «${productName}» обновлены`,
                            'Текст сообщения корректный',
                        );

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
                            .should.eventually.be.equal('Оферта принята', 'Статус принятия оферты корректный');

                        await this.clientIdValue
                            .getText()
                            .should.eventually.be.equal(clientId, 'ID клиента в Балансе корректный');

                        await this.contractIdValue
                            .getText()
                            .should.eventually.be.equal(contractId, 'ID контракта корректный');

                        // Проверка ID страницы CMS + тарифа для услуги бренд-зоны
                        if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                            this.setPageObjects({
                                cmsPageIdValue() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.secondColumn.getPlainDataByIndex(2),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                                tariff() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.secondColumn.getPlainDataByIndex(3),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                            });

                            await this.cmsPageIdValue
                                .getText()
                                .should.eventually.be.equal(cmsPageId, 'ID страницы CMS корректный');

                            await this.tariff
                                .getText()
                                .should.eventually.be.equal(brandZoneTariff, 'Название тарифа корректное');
                        }

                        // Проверка количества категорий + тарифа для услуги Маркет.Аналитики
                        if (productKey === PRODUCT_KEYS.ANALYTICS) {
                            this.setPageObjects({
                                tariff() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.secondColumn.getPlainDataByIndex(2),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                                categoriesCount() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.firstColumn.getPlainDataByIndex(2),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                            });

                            await this.tariff
                                .getText()
                                .should.eventually.be.equal(analyticsTariff, 'Название тарифа корректное');

                            await this.categoriesCount
                                .getText()
                                .should.eventually.be.equal(analyticsCategories, 'Количество категорий корректное');
                        }
                    });
                },
            }),
        },
    }),
});
