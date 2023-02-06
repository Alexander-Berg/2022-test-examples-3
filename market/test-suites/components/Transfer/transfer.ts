'use strict';

import {PageObject, makeSuite, mergeSuites, makeCase} from 'ginny';

const Transfer = PageObject.get('Transfer');

/**
 * @param {PageObject.Transfer} transfer
 */
export default makeSuite('Выполнение перевода.', {
    issue: 'VNDFRONT-4036',
    environment: 'kadavr',
    feature: 'Перевод средств',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    productsControls() {
                        return this.createPageObject('ProductsControls');
                    },
                    balanceTransferButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.productsControls,
                            this.productsControls.balanceTransferButton,
                        );
                    },
                    modal() {
                        return this.createPageObject('ModalLevitan');
                    },
                    source() {
                        return this.createPageObject(
                            'SelectLevitan',
                            this.modal,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            Transfer.sourceSelector,
                        );
                    },
                    destination() {
                        return this.createPageObject(
                            'SelectLevitan',
                            this.modal,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            Transfer.destinationSelector,
                        );
                    },
                    sumField() {
                        return this.createPageObject(
                            'TextFieldLevitan',
                            this.modal,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            Transfer.sumFieldSelector,
                        );
                    },
                    transferButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.modal,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            Transfer.transferButtonSelector,
                        );
                    },
                    cancelButton() {
                        return this.createPageObject(
                            'ButtonLevitan',
                            this.modal,
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            Transfer.cancelButtonSelector,
                        );
                    },
                });

                await this.balanceTransferButton
                    .isExisting()
                    .should.eventually.be.equal(
                        true,
                        'Отображается кнопка открытия модального окна для перевода средств',
                    );

                await this.balanceTransferButton.click();

                await this.modal.waitForOpened();

                await this.browser.allure.runStep(
                    'Проверяем наличие необходимых элементов в модальном окне',
                    async () => {
                        await this.source
                            .isExisting()
                            .should.eventually.be.equal(true, 'Отображается селект с выбором услуги для списания');

                        await this.destination
                            .isExisting()
                            .should.eventually.be.equal(true, 'Отображается селект с выбором услуги для пополнения');

                        await this.sumField
                            .isExisting()
                            .should.eventually.be.equal(true, 'Отображается поле ввода суммы');

                        await this.transferButton
                            .isExisting()
                            .should.eventually.be.equal(true, 'Отображается кнопка [Перевести]');

                        return this.cancelButton
                            .isExisting()
                            .should.eventually.be.equal(true, 'Отображается кнопка [Отмена]');
                    },
                );

                return this.browser.allure.runStep('Заполняем данные формы', async () => {
                    await this.source.click();

                    // выбираем услуги, ждем когда загрузится доступная сумма
                    await this.browser.allure.runStep('Выбираем услугу для списания', () =>
                        this.source.selectItemByIndex(0),
                    );

                    await this.browser.allure.runStep('Ожидаем разблокировки селектора услуги для пополнения', () =>
                        this.browser.waitUntil(
                            () => this.destination.isEnabled(),
                            this.browser.options.waitforTimeout,
                            'Селектор не разблокировался',
                        ),
                    );

                    await this.destination.click();

                    await this.browser.allure.runStep('Выбираем услугу для пополнения', () =>
                        this.destination.selectItemByIndex(0),
                    );

                    return this.browser.allure.runStep('Вводим сумму в поле ввода', () => this.sumField.setValue('10'));
                });
            },
        },
        {
            'После успешного выполнения перевода': {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.setPageObjects({
                        toasts() {
                            return this.createPageObject('NotificationGroupLevitan');
                        },
                        toast() {
                            return this.createPageObject(
                                'NotificationLevitan',
                                this.toasts,
                                this.toasts.getItemByIndex(0),
                            );
                        },
                    });
                },
                'появляется сообщение "Переводим средства"': makeCase({
                    id: 'vendor_auto-1366',
                    async test() {
                        await this.transferButton.click();

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        return this.toast
                            .getText()
                            .should.eventually.equal(
                                'Переводим средства\nВ ближайшее время баланс услуг обновится.',
                                'Текст всплывающего сообщения корректный',
                            );
                    },
                }),
            },
            'Модальное окно закрывается': {
                'при клике на крестик или при клике на кнопку [Отмена]': makeCase({
                    id: 'vendor_auto-1365',
                    async test() {
                        this.setPageObjects({
                            balanceHint() {
                                return this.createPageObject(
                                    'TextLevitan',
                                    this.modal,
                                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                    Transfer.sourceBalanceHintSelector,
                                );
                            },
                            closeButton() {
                                return this.createPageObject('ClickableLevitan', this.modal, '[role="button"]');
                            },
                        });

                        /**
                         * Закрытие окна по клику на кнопку [Отмена]
                         */

                        await this.cancelButton.click();

                        await this.modal.waitForHidden();

                        await this.balanceTransferButton.click();

                        await this.modal.waitForOpened();

                        await this.source.click();

                        await this.browser.allure.runStep('Выбираем услугу для списания', () =>
                            this.source.selectItemByIndex(0),
                        );

                        await this.browser.allure.runStep('Ожидаем разблокировки селектора услуги для пополнения', () =>
                            this.browser.waitUntil(
                                () => this.destination.isEnabled(),
                                this.browser.options.waitforTimeout,
                                'Селектор не разблокировался',
                            ),
                        );

                        await this.browser.allure.runStep('Получаем баланс услуги', () =>
                            this.balanceHint
                                .getText()
                                .should.eventually.be.equal(
                                    'Доступно 100 у.е. (3 000,00 ₽)',
                                    'Суммы на услугах не изменились',
                                ),
                        );

                        /**
                         * Закрытие окна по клику на крестик
                         */

                        await this.destination.click();

                        await this.browser.allure.runStep('Выбираем услугу для пополнения', () =>
                            this.destination.selectItemByIndex(0),
                        );

                        await this.browser.allure.runStep('Вводим сумму в поле ввода', () =>
                            this.sumField.setValue('10'),
                        );

                        await this.browser.allure.runStep('Кликаем на крестик в шапке модального окна', () =>
                            this.closeButton.click(),
                        );

                        await this.modal.waitForHidden();

                        await this.balanceTransferButton.click();

                        await this.modal.waitForOpened();

                        await this.source.click();

                        await this.browser.allure.runStep('Выбираем услугу для списания', () =>
                            this.source.selectItemByIndex(0),
                        );

                        await this.browser.allure.runStep('Ожидаем разблокировки селектора услуги для пополнения', () =>
                            this.browser.waitUntil(
                                () => this.destination.isEnabled(),
                                this.browser.options.waitforTimeout,
                                'Селектор не разблокировался',
                            ),
                        );

                        return this.browser.allure.runStep('Получаем баланс услуги', () =>
                            this.balanceHint
                                .getText()
                                .should.eventually.be.equal(
                                    'Доступно 100 у.е. (3 000,00 ₽)',
                                    'Суммы на услугах не изменились',
                                ),
                        );
                    },
                }),
            },
        },
    ),
});
