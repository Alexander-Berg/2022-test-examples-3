'use strict';

import {PageObject, makeSuite, makeCase} from 'ginny';

const ButtonLevitan = PageObject.get('ButtonLevitan');

/**
 * Тесты на изменение статуса подключения услуги "Отзывы за баллы"
 * @param {PageObject.OpinionsPromotionBalance} balance - блок счёта услуги "Отзывы за баллы"
 */
export default makeSuite('Блок "Счёт".', {
    feature: 'Отзывы за баллы',
    issue: 'VNDFRONT-3989',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При клике по тумблеру': {
            'изменяется статус подключения услуги': makeCase({
                id: 'vendor_auto-1305',
                async test() {
                    this.setPageObjects({
                        tumbler() {
                            return this.createPageObject('SwitchLevitan', this.balance);
                        },
                        confirmation() {
                            return this.createPageObject('Modal');
                        },
                        acceptButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                this.confirmation,
                                `${ButtonLevitan.root}:nth-of-type(1)`,
                            );
                        },
                        cancelButton() {
                            return this.createPageObject(
                                'ButtonLevitan',
                                this.confirmation,
                                `${ButtonLevitan.root}:nth-of-type(2)`,
                            );
                        },
                        closeIcon() {
                            return this.createPageObject('IconB2b', this.confirmation);
                        },
                        cutoffs() {
                            return this.createPageObject('CutoffsPanel');
                        },
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
                        secondToast() {
                            return this.createPageObject(
                                'NotificationLevitan',
                                this.toasts,
                                this.toasts.getItemByIndex(1),
                            );
                        },
                    });

                    await this.allure.runStep('Ожидаем появления блока "Счёт"', () => this.balance.waitForExist());

                    await this.allure.runStep('Проверяем состояние тумблера', async () => {
                        await this.tumbler.isVisible().should.eventually.be.equal(true, 'Тумблер отображается');

                        await this.tumbler.isPressed().should.eventually.be.equal(true, 'Тумблер включён');
                    });

                    /**
                     * Закрытие диалога подтверждения по крестику
                     */
                    await this.tumbler.click();

                    await this.allure.runStep('Ожидаем появления диалога подтверждения', () =>
                        this.confirmation.waitForVisible(),
                    );

                    await this.closeIcon.click();

                    await this.confirmation.waitForHidden();

                    await this.allure.runStep('Проверяем состояние тумблера', () =>
                        this.tumbler.isPressed().should.eventually.be.equal(true, 'Тумблер включён'),
                    );

                    /**
                     * Закрытие диалога подтверждения по кнопке отмены
                     */
                    await this.tumbler.click();

                    await this.allure.runStep('Ожидаем появления диалога подтверждения', () =>
                        this.confirmation.waitForVisible(),
                    );

                    await this.cancelButton.click();

                    await this.confirmation.waitForHidden();

                    await this.allure.runStep('Проверяем состояние тумблера', () =>
                        this.tumbler.isPressed().should.eventually.be.equal(true, 'Тумблер включён'),
                    );

                    /**
                     * Отключение услуги
                     */
                    await this.tumbler.click();

                    await this.allure.runStep('Ожидаем появления диалога подтверждения', () =>
                        this.confirmation.waitForVisible(),
                    );

                    await this.acceptButton.click();

                    await this.confirmation.waitForHidden();

                    await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения об отключении', () =>
                        this.firstToast.waitForVisible(),
                    );

                    await this.firstToast
                        .getText()
                        .should.eventually.equal('Услуга отключена', 'Текст всплывающего сообщения корректный');

                    await this.allure.runStep('Проверяем состояние тумблера', () =>
                        this.tumbler.isPressed().should.eventually.be.equal(false, 'Тумблер выключен'),
                    );

                    await this.browser.allure.runStep('Ожидаем появления блока с катофами', () =>
                        this.cutoffs.waitForVisible(),
                    );

                    /**
                     * Подключение услуги
                     */
                    await this.tumbler.click();

                    await this.browser.allure.runStep('Ожидаем показа всплывающего сообщения об отключении', () =>
                        this.secondToast.waitForVisible(),
                    );

                    await this.secondToast
                        .getText()
                        .should.eventually.equal('Услуга активирована', 'Текст всплывающего сообщения корректный');

                    await this.allure.runStep('Проверяем состояние тумблера', () =>
                        this.tumbler.isPressed().should.eventually.be.equal(true, 'Тумблер включён'),
                    );

                    await this.allure.runStep('Ожидаем скрытия блока катофов', () =>
                        this.browser.waitUntil(
                            async () => {
                                const visibility = await this.cutoffs.isVisible();

                                return visibility === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Блок катофов не скрылся',
                        ),
                    );
                },
            }),
        },
    },
});
