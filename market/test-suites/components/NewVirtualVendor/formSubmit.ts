'use strict';

import path from 'path';

import {ContextWithParams} from 'ginny-helpers';

import FormPO from 'spec/page-objects/Form';
import DocumentUploadPO from 'spec/page-objects/DocumentUpload';

export default {
    async beforeEach(
        this: ContextWithParams<{form: FormPO; guaranteeLetter: DocumentUploadPO; trademarkDocument: DocumentUploadPO}>,
    ) {
        this.setPageObjects({
            trademarkDocument(this: ContextWithParams<{form: FormPO}>) {
                return this.createPageObject(
                    'DocumentUpload',
                    this.form,
                    '[data-e2e=trademark-certificate-editable-document]',
                );
            },
            guaranteeLetter(this: ContextWithParams<{form: FormPO}>) {
                return this.createPageObject(
                    'DocumentUpload',
                    this.form,
                    '[data-e2e="guarantee-letter-editable-document"]',
                );
            },
        });

        await this.browser.windowHandleMaximize();
        await this.form.setValue({name: 'company', value: 'Рога и копыта'});
        await this.form.setValue({name: 'site', value: 'hornsandhooves.ru'});
        await this.form.setValue({name: 'brands', value: 'Рога и копыта'});
        await this.form.setValue({name: 'name', value: 'Василий'});
        await this.form.setValue({name: 'phone', value: '+79876543210'});
        await this.form.setValue({name: 'email', value: 'vnd.test@yandex.ru'});
        await this.browser.vndScrollToBottom();
        await this.form.setValue({name: 'address', value: '123456'});
        await this.form.clickAddNewDocument('Добавить свидетельство на товарный знак');
        await this.form.setValue({
            name: 'documentFile',
            value: path.resolve(__dirname, 'localhost.jpg'),
            type: 'file-dropzone',
        });
        await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
            this.browser.waitUntil(
                () => this.trademarkDocument.uploadedFileName.vndIsExisting(),
                this.browser.options.waitforTimeout,
                'Файл не загрузился',
            ),
        );

        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
        if (this.currentTest._meta.id === 'vendor_auto-748') {
            await this.form.clickAddNewDocument('Добавить гарантийное письмо');

            await this.browser.allure.runStep(
                'Дожидаемся появления блока загрузки гарантийного письма',
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                () => this.guaranteeLetter.waitForExist(),
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

                    await this.guaranteeLetter.uploadButton
                        .vndIsExisting()
                        .should.eventually.be.equal(true, 'Кнопка загрузки ещё одного документа отображается');
                },
            );
        }

        await this.browser.allure.runStep('Устанавливаем чекбокс принятия офферты', () =>
            this.form.checkboxEndsWithId('offer').click(),
        );
        /*
         * Некоторые AT падали с фокусом на кнопке сабмита.
         * Есть подозрение, что клик происходит раньше, чем кнопка разблокируется.
         * Поэтому ждём секунду перед тем, как нажимать.
         * UPD: поднял время ожидания, чтобы дождаться загрузки файла.
         */
        await this.browser.pause(5500);
        await this.form.submit();
    },
};
