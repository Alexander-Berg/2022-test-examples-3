'use strict';

import {PageObject, makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

import showProductHook from './hooks/showProduct';

const DropdownB2bNext = PageObject.get('DropdownB2bNext');
const Recharge = PageObject.get('Recharge');

/**
 * Тесты на выставление счета
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {string} params.settingsUrl - ссылка на страницу настроек
 */
export default makeSuite('Выставление счёта.', {
    issue: 'VNDFRONT-3807',
    environment: 'kadavr',
    feature: 'Управление услугами и пользователями',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(
        showProductHook({
            managerView: false,
            details: false,
        }),
        importSuite('Link', {
            suiteName: 'Ссылка в подсказке',
            meta: {
                id: 'vendor_auto-518',
                environment: 'kadavr',
            },
            params: {
                caption: '«Настройки»',
                comparison: {
                    skipHostname: true,
                },
            },
            hooks: {
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const {settingsUrl} = this.params;

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.url = settingsUrl;
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.params.exist = Boolean(settingsUrl);

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.setPageObjects({
                        hint() {
                            return this.createPageObject('Hint', this.product);
                        },
                        icon() {
                            return this.createPageObject('IconLevitan', this.hint);
                        },
                        dropdown() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('DropdownB2bNext', this.browser, DropdownB2bNext.active);
                        },
                        link() {
                            return this.createPageObject('Link', this.dropdown, 'a');
                        },
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Ожидаем появления значка подсказки', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.icon.waitForExist(),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.icon.click();

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.browser.allure.runStep('Ожидаем появления подсказки', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.dropdown.waitForExist(),
                    );
                },
            },
        }),
        {
            'При клике на кнопку [Пополнить счёт]': {
                'открывается модальное окно.': {
                    // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                    async beforeEach() {
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.setPageObjects({
                            productsControls() {
                                return this.createPageObject('ProductsControls');
                            },
                            rechargeButton() {
                                return this.createPageObject(
                                    'ButtonLevitan',
                                    this.productsControls,
                                    this.productsControls.rechargeButton,
                                );
                            },
                            modal() {
                                return this.createPageObject('ModalLevitan');
                            },
                            productSelectField() {
                                return this.createPageObject('SelectLevitan', this.modal);
                            },
                            sumField() {
                                return this.createPageObject('TextFieldLevitan', this.modal);
                            },
                            payButton() {
                                return this.createPageObject(
                                    'ButtonLevitan',
                                    this.modal,
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    Recharge.payButtonSelector,
                                );
                            },
                        });

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.rechargeButton
                            .isExisting()
                            .should.eventually.be.equal(
                                true,
                                'Отображается кнопка открытия модального окна для пополнения',
                            );

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.rechargeButton.click();

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.modal.waitForOpened();

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        await this.browser.allure.runStep(
                            'Проверяем наличие необходимых элементов на странице',
                            async () => {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                await this.productSelectField
                                    .isExisting()
                                    .should.eventually.be.equal(true, 'Отображается селект с выбором услуги');

                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                await this.sumField
                                    .isExisting()
                                    .should.eventually.be.equal(true, 'Отображается поле ввода суммы');

                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                return this.payButton
                                    .isExisting()
                                    .should.eventually.be.equal(true, 'Отображается кнопка [Пополнить]');
                            },
                        );

                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        return this.browser.allure.runStep('Заполняем данные формы', async () => {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.productSelectField.click();
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.productSelectField.waitForSelectShown();
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            await this.productSelectField.selectItemByIndex(0);

                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.browser.allure.runStep(
                                'Вводим сумму в поле ввода',
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                () => this.sumField.setValue('10'),
                            );
                        });
                    },
                    'При клике на кнопку [Пополнить] ': {
                        'в соседней вкладке открывается ссылка на баланс': {
                            'после выбора услуги и ввода суммы в у. е.': makeCase({
                                id: 'vendor_auto-32',
                                test() {
                                    return this.browser.allure.runStep(
                                        'Нажимаем на кнопку [Пополнить] и переходим на новую вкладку',
                                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                        () => this.browser.vndWaitForChangeTab(() => this.payButton.click()),
                                    );
                                },
                            }),
                        },
                    },
                    'При клике на крестик или при клике на кнопку [Отмена]': {
                        'модальное окно закрывается': {
                            'после выбора услуги и ввода суммы в у. е.': makeCase({
                                id: 'vendor_auto-515',
                                async test() {
                                    this.setPageObjects({
                                        balanceHint() {
                                            return this.createPageObject(
                                                'TextLevitan',
                                                this.modal,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                Recharge.balanceHintSelector,
                                            );
                                        },
                                        cancelButton() {
                                            return this.createPageObject(
                                                'ButtonLevitan',
                                                this.modal,
                                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                                Recharge.cancelButtonSelector,
                                            );
                                        },
                                        closeButton() {
                                            return this.createPageObject(
                                                'ClickableLevitan',
                                                this.modal,
                                                '[role="button"]',
                                            );
                                        },
                                    });

                                    /**
                                     * Закрытие окна по клику на крестик
                                     */

                                    await this.browser.allure.runStep(
                                        'Кликаем на крестик в шапке модального окна',
                                        () => this.closeButton.click(),
                                    );

                                    await this.modal.waitForHidden();

                                    await this.rechargeButton.click();

                                    await this.modal.waitForOpened();

                                    await this.browser.allure.runStep('Выбираем услугу для пополнения', async () => {
                                        await this.productSelectField.click();
                                        await this.productSelectField.waitForSelectShown();
                                        return this.productSelectField.selectItemByIndex(0);
                                    });

                                    await this.browser.allure.runStep('Получаем баланс услуги', () =>
                                        this.balanceHint
                                            .getText()
                                            .should.eventually.be.equal(
                                                'Доступно 100 у.е. (3 000,00 ₽)',
                                                'Баланс услуги не изменился',
                                            ),
                                    );

                                    /**
                                     * Закрытие окна по клику на кнопку [Отмена]
                                     */

                                    await this.browser.allure.runStep('Вводим сумму в поле ввода', () =>
                                        this.sumField.setValue('10'),
                                    );

                                    await this.cancelButton.click();

                                    await this.modal.waitForHidden();

                                    await this.rechargeButton.click();

                                    await this.modal.waitForOpened();

                                    await this.browser.allure.runStep('Выбираем услугу для пополнения', async () => {
                                        await this.productSelectField.click();
                                        await this.productSelectField.waitForSelectShown();
                                        return this.productSelectField.selectItemByIndex(0);
                                    });

                                    return this.browser.allure.runStep('Получаем баланс услуги', () =>
                                        this.balanceHint
                                            .getText()
                                            .should.eventually.be.equal(
                                                'Доступно 100 у.е. (3 000,00 ₽)',
                                                'Баланс услуги не изменился',
                                            ),
                                    );
                                },
                            }),
                        },
                    },
                    'При попытке ввода некорректного значения в поле суммы': {
                        'значение поля остается пустым': makeCase({
                            id: 'vendor_auto-528',
                            async test() {
                                await this.browser.allure.runStep(
                                    'Вводим в поле суммы некорректные значения',
                                    async () => {
                                        await this.sumField.setValue('-10');
                                        await this.sumField.setValue('*$!@#$%^&*(){}|+-=');
                                        return this.sumField.setValue('abc');
                                    },
                                );

                                return this.sumField.value.should.eventually.be.equal(
                                    '',
                                    'Некорректные значения не вводятся',
                                );
                            },
                        }),
                    },
                },
            },
        },
    ),
});
