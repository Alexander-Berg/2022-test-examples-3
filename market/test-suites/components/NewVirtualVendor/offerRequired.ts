'use strict';

import path from 'path';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на обязательность принятия оферты и загрузку документов
 * @param {PageObject.Form} form
 * @param {PageObject.PopupB2b} popup
 */
export default makeSuite('Обязательность принятия оферты и приложения документов.', {
    story: {
        async beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.browser.windowHandleMaximize();
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'company', value: 'Рога и копыта'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'site', value: 'hornsandhooves.ru'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'brands', value: 'Рога и копыта'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'name', value: 'Василий'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'phone', value: '+79876543210'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'email', value: 'vnd.test@yandex.ru'});
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.browser.vndScrollToBottom();
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.setValue({name: 'address', value: '123456'});

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.submitButton.isEnabled().should.eventually.equal(false, 'Кнопка сабмита заблокирована');

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.browser.allure.runStep(
                'Устанавливаем чекбокс принятия офферты',
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                () => this.form.checkboxEndsWithId('offer').click(),
            );

            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.form.submitButton.isEnabled().should.eventually.equal(true, 'Кнопка сабмита разблокирована');
        },
        'При отсутствии документов': {
            'появляется хинт': makeCase({
                id: 'vendor_auto-38',
                issue: 'VNDFRONT-1842',
                async test() {
                    /*
                     * Некоторые AT падали с фокусом на кнопке сабмита.
                     * Есть подозрение, что клик происходит раньше, чем кнопка разблокируется.
                     * Поэтому ждём секунду перед тем, как нажимать.
                     */
                    await this.browser.pause(1000);
                    await this.form.submit();

                    await this.browser.waitUntil(
                        () => this.popup.activeBodyPopup.isVisible(),
                        this.browser.options.waitforTimeout,
                        'Тултип появился',
                    );

                    await this.popup
                        .getActiveText()
                        .should.eventually.be.equal(
                            'Загрузите свидетельство на товарный знак',
                            'Текст тултипа корректный',
                        );
                },
            }),
        },
        'При загрузке только гарантийного письма': {
            'появляется хинт': makeCase({
                issue: 'VNDFRONT-3426',
                id: 'vendor_auto-52',
                async test() {
                    this.setPageObjects({
                        guaranteeLetter() {
                            return this.createPageObject(
                                'DocumentUpload',
                                this.form,
                                '[data-e2e="guarantee-letter-editable-document"]',
                            );
                        },
                    });
                    await this.form.clickAddNewDocument('Добавить гарантийное письмо');

                    await this.browser.allure.runStep('Дожидаемся появления блока загрузки документа', () =>
                        this.guaranteeLetter.waitForExist(),
                    );

                    await this.browser.vndScrollToBottom();

                    await this.browser.allure.runStep(
                        'Загружаем гарантийное письмо и проверяем корректность отображения формы',
                        async () => {
                            await this.guaranteeLetter.uploadNewFile(path.resolve(__dirname, 'localhost.jpg'));

                            await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
                                this.browser.waitUntil(
                                    () => this.guaranteeLetter.uploadedFileName.vndIsExisting(),
                                    this.browser.options.waitforTimeout,
                                    'Файл не загрузился',
                                ),
                            );

                            await this.guaranteeLetter.uploadedFileName
                                .getText()
                                .should.eventually.be.equal('localhost', 'Название файла отображается корректно');
                        },
                    );

                    await this.browser.pause(1000);
                    await this.form.submit();

                    await this.browser.waitUntil(
                        () => this.popup.activeBodyPopup.isVisible(),
                        this.browser.options.waitforTimeout,
                        'Тултип появился',
                    );

                    await this.popup
                        .getActiveText()
                        .should.eventually.be.equal(
                            'Загрузите свидетельство на товарный знак',
                            'Текст тултипа корректный',
                        );
                },
            }),
        },
    },
});
