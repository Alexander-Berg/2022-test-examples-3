'use strict';

import path from 'path';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на принятие оферты админом
 * @param {PageObject.ContactsForm} contactsForm - форма с контактными данными
 * @param {PageObject.ContactsForm} form - форма
 */
export default makeSuite('Форма "Контактные данные". Принятие оферты', {
    environment: 'kadavr',
    feature: 'Настройки',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.allure.runStep('Ожидаем появления формы контактных данных', () =>
                    this.contactsForm.waitForExist(),
                );

                await this.form
                    .getReadonlyFieldValue('offer')
                    .should.eventually.be.equal('не принята', 'У поля "Оферта" корректное изначальное значение');

                await this.form.clickEditButton();

                await this.browser.allure.runStep('Форма перешла в режим редактирования', async () => {
                    /*
                     * Используем yaSafeAction, чтобы не делать дополнительные проверки с isExisting.
                     * Если элемент не будет найден, тест упадет,
                     * но ошибка будет корректно обработана как false.
                     * Если использовать waitUntil, то тест в отчете будет broken,а не failed
                     */

                    await this.browser
                        .yaSafeAction(this.form.cancelButton.isEnabled(), false)
                        .should.eventually.equal(true, 'Кнопка [Отмена] отображается и активна');

                    await this.browser
                        .yaSafeAction(this.form.submitButton.isEnabled(), false)
                        .should.eventually.equal(true, 'Кнопка [Сохранить] отображается и активна');
                });
            },
        },
        {
            'При принятии оферты': {
                'отображается признак и дата принятия оферты': makeCase({
                    issue: 'VNDFRONT-3229',
                    id: 'vendor_auto-41',
                    async test() {
                        await this.browser.allure.runStep('Нажимаем чекбокс принятия оферты', () =>
                            this.checkbox.click(),
                        );

                        await this.form
                            .getDocumentUploadFieldByName('trademarkDocuments')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Поле "Основание использования товарного знака" отображается',
                            );

                        await this.form
                            .link('Добавить свидетельство на товарный знак')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Кнопка "Добавить свидетельство на товарный знак" отображается',
                            );

                        await this.form.clickAddNewDocument('Добавить свидетельство на товарный знак');

                        await this.trademarkDocument.waitForExist();

                        await this.trademarkDocument.uploadNewFile(
                            path.resolve(__dirname, 'testData/trademark_certificate.jpg'),
                        );

                        await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
                            this.browser.waitUntil(
                                () => this.trademarkDocument.uploadedFileName.isExisting(),
                                this.browser.options.waitforTimeout,
                                'Файл не загрузился',
                            ),
                        );

                        await this.browser.allure.runStep('Нажимаем кнопку [Сохранить]', () =>
                            this.form.submitButton.click(),
                        );

                        // Считаем, что форма в режиме чтения, когда отобразится кнопка "Изменить"
                        await this.browser.allure.runStep('Дожидаемся перехода формы в режим чтения', () =>
                            this.browser.waitUntil(
                                () => this.form.editButton.vndIsExisting(),
                                this.browser.options.waitforTimeout,
                                'Кнопка [Изменить] отображается и активна',
                            ),
                        );

                        await this.form
                            .getReadonlyFieldValue('offer')
                            .should.eventually.be.equal(
                                'принята 11 августа 2020 г., 12:42',
                                'У поля "Оферта" корректное новое значение',
                            );
                    },
                }),
            },
        },
        {
            'При принятии оферты и отсутствии свидетельства на товарный знак': {
                'отображается хинт о необходимости загрузки свидетельства': makeCase({
                    issue: 'VNDFRONT-3235',
                    id: 'vendor_auto-257',
                    async test() {
                        await this.browser.allure.runStep('Нажимаем чекбокс принятия оферты', () =>
                            this.checkbox.click(),
                        );

                        await this.form.submit('Сохранить');

                        await this.form.submitErrorPopup
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Тултип с ошибкой у кнопки "Сохранить" отображается');

                        await this.form.submitErrorPopup
                            .getText()
                            .should.eventually.be.equal(
                                'Загрузите свидетельство на товарный знак',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
        },
    ),
});
