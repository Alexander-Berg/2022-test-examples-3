'use strict';

import {PageObject, makeCase, makeSuite, mergeSuites} from 'ginny';

import showProductHook from './hooks/showProduct';

const CutoffsList = PageObject.get('CutoffsList');

/**
 * Тесты на управление размещением услуги
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 */
export default makeSuite('Управление размещением услуги.', {
    issue: 'VNDFRONT-3808',
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
        {
            beforeEach() {
                this.setPageObjects({
                    placementButton() {
                        return this.createPageObject('ButtonLevitan', this.product.setPlacementButton);
                    },
                    toasts() {
                        return this.createPageObject('NotificationGroupLevitan');
                    },
                    toast() {
                        return this.createPageObject('NotificationLevitan', this.toasts, this.toasts.getItemByIndex(0));
                    },
                    modal() {
                        return this.createPageObject('ModalLevitan');
                    },
                    launchButton() {
                        return this.createPageObject('ButtonLevitan', this.modal.footer, '[data-e2e="launch-button"]');
                    },
                });
            },
        },
        {
            'При клике на кнопку [Запустить]/[Остановить]': {
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.product.placement
                        .getText()
                        .should.eventually.be.equal('Работает', 'Текст размещения услуги верный');

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.product.balance
                        .getText()
                        .should.eventually.be.equal('100 у. е.', 'Баланс услуги корректный');

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.placementButton
                        .getText()
                        .should.eventually.be.equal(
                            'Остановить',
                            'Текст на кнопке управления размещением услуги корректный',
                        );
                },
                'изменяется статус размещения услуги': makeCase({
                    id: 'vendor_auto-521',
                    async test() {
                        /**
                         * Остановка услуги
                         */
                        await this.placementButton.click();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal('Услуга отключена', 'Текст всплывающего сообщения корректный');

                        await this.browser.allure.runStep('Ожидаем разблокировки кнопки управления размещением', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.placementButton.isDisabled();

                                    return disabled === false;
                                },
                                15000,
                                'Не удалось дождаться разблокировки кнопки управления размещением',
                            ),
                        );

                        await this.product.placement
                            .getText()
                            .should.eventually.be.equal('Приостановлено', 'Текст статуса услуги верный');

                        await this.placementButton
                            .getText()
                            .should.eventually.be.equal(
                                'Запустить',
                                'Текст на кнопке управления размещением услуги корректный',
                            );

                        /**
                         * Запуск услуги
                         */
                        await this.browser.allure.runStep('Ожидаем разблокировки кнопки управления размещением', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const disabled = await this.placementButton.isDisabled();

                                    return disabled === false;
                                },
                                15000 /* Таймаут ожидания загрузки катофов */,
                                'Не удалось дождаться разблокировки кнопки управления размещением',
                            ),
                        );

                        await this.placementButton.click();

                        await this.modal.waitForOpened();

                        await this.launchButton
                            .isExisting()
                            .should.eventually.be.equal(true, 'Кнопка [Запустить] отображается');

                        await this.launchButton.click();

                        await this.modal.waitForHidden();

                        await this.browser.allure.runStep('Ожидаем показа группы всплывающих сообщений', () =>
                            this.toasts.waitForVisible(),
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем показа всплывающего сообщения с подтверждением',
                            () => this.toast.waitForVisible(),
                        );

                        await this.toast
                            .getText()
                            .should.eventually.equal('Услуга активирована', 'Текст всплывающего сообщения корректный');

                        await this.product.placement
                            .getText()
                            .should.eventually.be.equal('Работает', 'Текст размещения услуги верный');

                        return this.placementButton
                            .getText()
                            .should.eventually.be.equal(
                                'Остановить',
                                'Текст на кнопке управления размещением услуги корректный',
                            );
                    },
                }),
            },
            'При открытии модального окна управления услугой': {
                // @ts-expect-error(TS7023) найдено в рамках VNDFRONT-4580
                async beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.setPageObjects({
                        cutoffs() {
                            return this.createPageObject('TextLevitan', this.modal, CutoffsList.root);
                        },
                        cutoffsSpinner() {
                            return this.createPageObject('SpinnerLevitan', this.product.placement);
                        },
                    });

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.browser.allure.runStep('Ожидаем загрузки катоффов по услуге', () =>
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.browser.waitUntil(
                            async () => {
                                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                                const visible = await this.cutoffsSpinner.isVisible();

                                return visible === false;
                            },
                            15000,
                            'Не удалось дождаться загрузки катоффов',
                        ),
                    );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.product.placement
                        .getText()
                        .should.eventually.be.equal(
                            'Неактивно\nНесколько причин отключения',
                            'Текст размещения услуги верный',
                        );

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    await this.product.balance
                        .getText()
                        .should.eventually.be.equal('100 у. е.', 'Баланс услуги корректный');

                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.placementButton
                        .getText()
                        .should.eventually.be.equal(
                            'Запустить',
                            'Текст на кнопке управления размещением услуги корректный',
                        );
                },
                'отображаются все имеющиеся катоффы, кроме пользовательского': makeCase({
                    id: 'vendor_auto-1379',
                    issue: 'VNDFRONT-4149',
                    async test() {
                        await this.placementButton.click();

                        await this.modal.waitForOpened();

                        return this.cutoffs
                            .getText()
                            .should.eventually.be.equal(
                                'Недостаточно средств — пополните счёт.',
                                'Текст катоффа корректный',
                            );
                    },
                }),
            },
        },
    ),
});
