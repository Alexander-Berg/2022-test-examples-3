'use strict';

import {mergeSuites, PageObject, makeSuite, makeCase} from 'ginny';

import PRODUCT_KEYS from 'app/constants/products/keys';

import openProductModalHook from '../hooks/openProductModal';

const TextNextLevitan = PageObject.get('TextNextLevitan');

/**
 * Тест на подключение услуги
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.Modal} modal - модальное окно изменения данных об услуге
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.pageRouteName - ключ страницы
 * @param {number} params.vendor - ID вендора
 */
export default makeSuite('Подключение услуги.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-3389',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При сохранении формы': {
            'услуга подключается вендору': makeCase({
                async test() {
                    const {productKey, productName} = this.params;
                    const clientId = '1012';
                    const contractId = '33';
                    const cmsPageId = '55851';

                    await this.form.setFieldValueByName('clientId', clientId, 'ID клиента в Балансе');

                    await this.form.setFieldValueByName('contractId', contractId, 'ID контракта');

                    await this.browser.allure.runStep('Устанавливаем чекбокс предоплатного договора', () =>
                        this.form.checkboxEndsWithId('isContractTypePrepaid').click(),
                    );

                    if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                        await this.form.setFieldValueByName('cmsPageId', cmsPageId, 'ID страницы CMS');
                    }

                    await this.form.submit('Сохранить');

                    await this.modal.waitForHidden();

                    // После закрытия модалки на body по неизвестной причине навешивается класс с overflow: hidden
                    await this.browser.allure.runStep('Включаем скроллинг документа', () =>
                        this.browser.yaExecute(() => {
                            document.body.removeAttribute('class');
                        }),
                    );

                    this.setPageObjects({
                        messages() {
                            return this.createPageObject('Messages');
                        },
                    });

                    await this.allure.runStep('Ожидаем появления сообщения об успешном подключении услуги', () =>
                        this.messages.waitForExist(),
                    );

                    await this.messages
                        .getMessageText()
                        .should.eventually.be.equal(`Услуга «${productName}» подключена`, 'Текст ошибки корректный');

                    this.setPageObjects({
                        product() {
                            return this.createPageObject(
                                'Product',
                                this.products,
                                this.products.getItemByProductKey(productKey),
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
                        contractFlagCaption() {
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
                        await this.offerFlagCaption
                            .getText()
                            .should.eventually.be.equal('Оферта не принята', 'Статус принятия офферты корректный');

                        await this.contractFlagCaption
                            .getText()
                            .should.eventually.be.equal('Предоплатный договор', 'Тип договора корректный');

                        await this.balanceOrderLink
                            .isVisible()
                            .should.eventually.be.equal(true, 'Ссылка на заказ в Балансе отображается');

                        await this.clientIdValue
                            .getText()
                            .should.eventually.be.equal(clientId, 'ID клиента в Балансе корректный');

                        await this.contractIdValue
                            .getText()
                            .should.eventually.be.equal(contractId, 'ID контракта корректный');

                        let secondColumnThroughIndex = 2;

                        // Проверка ID страницы CMS для услуги бренд-зоны
                        if (productKey === PRODUCT_KEYS.BRAND_ZONE) {
                            this.setPageObjects({
                                cmsPageIdValue() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        /*
                                         * Инкрементируем индекс поля с тарифом, так как для услуги бренд-зоны
                                         * оно расположено под полем с ID CMS.
                                         */
                                        this.secondColumn.getPlainDataByIndex(secondColumnThroughIndex++),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                            });

                            await this.cmsPageIdValue
                                .getText()
                                .should.eventually.be.equal(cmsPageId, 'ID страницы CMS корректный');
                        }

                        // Проверка тарифа для услуг бренд-зоны и Маркет.Аналитики
                        if ([PRODUCT_KEYS.BRAND_ZONE, PRODUCT_KEYS.ANALYTICS].includes(productKey)) {
                            this.setPageObjects({
                                tariffValue() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.secondColumn.getPlainDataByIndex(secondColumnThroughIndex),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                            });

                            await this.browser.allure.runStep('Ожидаем появления текущего тарифа', () =>
                                this.tariffValue.waitForExist(),
                            );

                            await this.tariffValue
                                .getText()
                                .should.eventually.be.equal('Спекулянт', 'Название тарифа корректное');
                        }

                        // Проверка подключённых категорий Маркет.Аналитики
                        if (productKey === PRODUCT_KEYS.ANALYTICS) {
                            this.setPageObjects({
                                categoriesValue() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.firstColumn.getPlainDataByIndex(2),
                                        `${TextNextLevitan.root} + ${TextNextLevitan.root}`,
                                    );
                                },
                                categoriesSpinner() {
                                    return this.createPageObject('SpinnerLevitan', this.categoriesValue);
                                },
                                analyticsManagerView() {
                                    return this.createPageObject('AnalyticsManagerView', this.details);
                                },
                                analyticsRecommendationCaption() {
                                    return this.createPageObject(
                                        'TextNextLevitan',
                                        this.analyticsManagerView,
                                        this.analyticsManagerView.recommendation,
                                    );
                                },
                            });

                            await this.browser.allure.runStep(
                                'Ожидаем появления количества подключённых категорий',
                                () =>
                                    this.browser.waitUntil(
                                        async () => {
                                            const existing = await this.categoriesSpinner.isExisting();

                                            return existing === false;
                                        },
                                        this.browser.options.waitforTimeout,
                                        'Спиннер отображается',
                                    ),
                            );

                            await this.categoriesValue
                                .getText()
                                .should.eventually.be.equal('0 категорий', 'Количество категорий корректное');

                            await this.browser.allure.runStep(
                                'Ожидаем появления сообщения о необходимости регистрации партнера Аналитической ' +
                                    'платформы',
                                () => this.analyticsRecommendationCaption.waitForExist(),
                            );

                            await this.analyticsRecommendationCaption
                                .getText()
                                .should.eventually.be.equal(
                                    'После подключения услуги необходимо зарегистрировать партнёра Аналитической ' +
                                        'платформы.',
                                    'Текст сообщения корректный',
                                );
                        }
                    });
                },
            }),
        },
    }),
});
