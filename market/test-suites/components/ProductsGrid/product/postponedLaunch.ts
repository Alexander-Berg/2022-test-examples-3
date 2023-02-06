'use strict';

import {makeCase, makeSuite, mergeSuites} from 'ginny';
import moment from 'moment';

import showProductHook from './hooks/showProduct';

/**
 * Тесты на выставление даты запуска услуги
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {boolean} params.isManager - флаг наличия роли менеджера (из userStory)
 */
export default makeSuite('Выставление даты запуска услуги.', {
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
                    datePicker() {
                        return this.createPageObject('DatePicker', this.modal);
                    },
                    launchButton() {
                        return this.createPageObject('ButtonLevitan', this.modal.footer, '[data-e2e="launch-button"]');
                    },
                });
            },
        },
        {
            'При клике на кнопку [Запустить]': {
                beforeEach() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    this.setPageObjects({
                        popup() {
                            return this.createPageObject('PopupB2b');
                        },
                        cutoffsSpinner() {
                            return this.createPageObject('SpinnerLevitan', this.product.placement);
                        },
                        cutoffsDescription() {
                            return this.createPageObject('CutoffsDescription', this.product);
                        },
                    });
                },
                'устанавливается дата запуска услуги': makeCase({
                    async test() {
                        const {isManager} = this.params;

                        await this.placementButton
                            .isVisible()
                            .should.eventually.equal(true, 'Кнопка запуска услуги отображается');

                        await this.browser.allure.runStep('Ждём пока кнопка запуска услуги станет активной', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const isDisabled = await this.placementButton.isDisabled();

                                    return isDisabled === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Не удалось дождаться активной кнопки запуска услуги',
                            ),
                        );

                        await this.placementButton.click();

                        await this.modal.waitForOpened();

                        await this.browser.allure.runStep('Проверяем наличие необходимых элементов', async () => {
                            await this.launchButton
                                .isExisting()
                                .should.eventually.be.equal(true, 'Кнопка [Запустить] отображается');

                            if (isManager) {
                                return this.datePicker
                                    .isExisting()
                                    .should.eventually.be.equal(true, 'Кнопка выбора даты отображается');
                            }
                        });

                        if (!isManager) {
                            return;
                        }

                        await this.datePicker.open();

                        await this.browser.allure.runStep('Дожидаемся появления попапа', () =>
                            this.popup.waitForPopupShown(),
                        );

                        await this.datePicker.selectNextMonth();

                        // 5-е число следующего месяца
                        const targetDate = moment().add(1, 'months').set('date', 5);

                        await this.datePicker.selectDate(targetDate);

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

                        await this.browser.allure.runStep('Ожидаем загрузки описания катофов', () =>
                            this.cutoffsDescription.waitForExist(),
                        );

                        return this.product.placement
                            .getText()
                            .should.eventually.be.equal(
                                `Отложенный запуск\nЗапуск — ${targetDate.format('D MMMM YYYY')}`,
                                'Текст статуса услуги верный',
                            );
                    },
                }),
            },
        },
    ),
});
