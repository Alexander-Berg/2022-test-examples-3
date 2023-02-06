'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

import TitleB2b from 'spec/page-objects/TitleB2b';

import openProductModalHook from '../hooks/openProductModal';

/**
 * Тест на смену тарифа у услуги
 * @param {Object} params
 * @param {string} params.productKey - ключ услуги
 * @param {string} params.productName - название услуги
 * @param {number} params.vendor - ID вендора
 * @param {string} params.currentTariffName - название текущего тарифа у услуги
 * @param {string} params.nextTariffName - название нового тарифа у услуги
 */
export default makeSuite('Редактирование услуги. Смена тарифа.', {
    feature: 'Управление услугами и пользователями',
    issue: 'VNDFRONT-4157',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
        productName: 'Услуга',
    },
    story: mergeSuites(openProductModalHook(), {
        'При смене тарифа': {
            'новый тариф': {
                'применяется корректно': makeCase({
                    async test() {
                        this.setPageObjects({
                            tariffSelect() {
                                return this.createPageObject('TariffSelect', this.form);
                            },
                            popup() {
                                return this.createPageObject('PopupB2b');
                            },
                            currentTariffOnList() {
                                return this.createPageObject(
                                    'TitleB2b',
                                    this.popup.activeBodyPopup,
                                    `[data-e2e="done"] ~ ${TitleB2b.root}`,
                                );
                            },
                            notCurrentTariffOnList() {
                                return this.createPageObject(
                                    'TitleB2b',
                                    this.popup.activeBodyPopup,
                                    `${TitleB2b.root}:first-child`,
                                );
                            },
                            nextTariffOnList() {
                                return this.createPageObject(
                                    'TitleB2b',
                                    this.popup.activeBodyPopup,
                                    `[data-e2e="time"] ~ ${TitleB2b.root}`,
                                );
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
                        });

                        await this.tariffSelect
                            .isVisible()
                            .should.eventually.be.equal(true, 'Селект выбора тарифа отображается');

                        const {currentTariffName, nextTariffName} = this.params;

                        await this.tariffSelect
                            .getText()
                            .should.eventually.be.equal(
                                currentTariffName,
                                'Название текущего тарифа в кнопке корректное',
                            );

                        await this.tariffSelect.click();

                        await this.browser.allure.runStep('Ожидаем появления списка тарифов', () =>
                            this.popup.waitForPopupShown(),
                        );

                        await this.currentTariffOnList
                            .getText()
                            .should.eventually.be.equal(
                                currentTariffName,
                                'Название текущего тарифа в списке корректное',
                            );

                        await this.browser.allure.runStep('Кликаем по другому тарифу', () =>
                            this.notCurrentTariffOnList.root.click(),
                        );

                        await this.allure.runStep('Ожидаем появления группы сообщений', () =>
                            this.toasts.waitForExist(),
                        );

                        await this.allure.runStep('Ожидаем появления сообщения об успешном изменении тарифа', () =>
                            this.firstToast.waitForExist(),
                        );

                        await this.firstToast
                            .getText()
                            .should.eventually.be.equal('Тариф изменён', 'Текст сообщения корректный');

                        await this.currentTariffOnList
                            .getText()
                            .should.eventually.be.equal(
                                currentTariffName,
                                'Название текущего тарифа в списке осталось прежним',
                            );

                        await this.nextTariffOnList
                            .getText()
                            .should.eventually.be.equal(
                                nextTariffName,
                                'Название запланированного тарифа в списке корректное',
                            );

                        await this.browser.allure.runStep(
                            'Кликаем по текущему тарифу, чтобы сбросить запланированный тариф',
                            () => this.currentTariffOnList.root.click(),
                        );

                        await this.allure.runStep('Ожидаем появления группы сообщений', () =>
                            this.toasts.waitForExist(),
                        );

                        await this.allure.runStep('Ожидаем появления сообщения об успешном изменении тарифа', () =>
                            this.firstToast.waitForExist(),
                        );

                        await this.firstToast
                            .getText()
                            .should.eventually.be.equal('Тариф изменён', 'Текст сообщения корректный');

                        await this.browser.allure.runStep('Дожидаемся скрытия часиков у запланированного тарифа', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.nextTariffOnList.isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Запланированный тариф не сбросился',
                            ),
                        );

                        await this.tariffSelect.click();

                        await this.browser.allure.runStep('Ожидаем скрытия списка тарифов', () =>
                            this.popup.waitForPopupHidden(),
                        );

                        await this.tariffSelect
                            .getText()
                            .should.eventually.be.equal(
                                currentTariffName,
                                'Название текущего тарифа в кнопке осталось прежним',
                            );
                    },
                }),
            },
        },
    }),
});
