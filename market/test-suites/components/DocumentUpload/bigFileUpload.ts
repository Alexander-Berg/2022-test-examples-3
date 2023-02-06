'use strict';

import {makeSuite, makeCase} from 'ginny';

// @ts-expect-error(TS7016) найдено в рамках VNDFRONT-4580
import {MAX_FILE_SIZE_IN_MEGABYTES} from 'app/constants/files';

/**
 * Тест на загрузку больших файлов в блок гарантийного письма
 *
 * @param {PageObject.Form} form - форма
 * @param {PageObject.DocumentUpload} document - блок с загрузкой документа
 * @param {PageObject.CheckboxB2b} [unlimitedCheckbox] - чекбокс «Срок действия не ограничен»
 * @param {PageObject.InputB2b} [expireDate] - дата окончания действия документа
 *
 */
export default makeSuite('Загрузка большого файла.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        [`При загрузке файла размером > ${MAX_FILE_SIZE_IN_MEGABYTES} МБ`]: {
            'отображается хинт о превышении допустимого размера файла': makeCase({
                async test() {
                    const {linkCaption, withExpireDateCheck} = this.params;

                    await this.form
                        .link(linkCaption)
                        .vndIsExisting()
                        .should.eventually.be.equal(true, `Кнопка "${linkCaption}" отображается`);

                    await this.form.clickAddNewDocument(linkCaption);

                    await this.browser.allure.runStep('Дожидаемся появления блока загрузки документа', () =>
                        this.document.waitForExist(),
                    );

                    await this.browser.vndScrollToBottom();

                    await this.browser.allure.runStep(
                        'Проверяем корректность отображения блока загрузки документа',
                        async () => {
                            await this.document.uploadButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                            await this.document.resetButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');

                            if (withExpireDateCheck) {
                                await this.unlimitedCheckbox.icon
                                    .vndIsExisting()
                                    .should.eventually.be.equal(true, 'Чекбокс "Не ограничено" выбран');

                                await this.browser
                                    .yaSafeAction(this.expireDate.input.isEnabled(), true)
                                    .should.eventually.equal(false, 'Поле "Дата окончания" отображается и неактивно');
                            }
                        },
                    );

                    await this.browser.allure.runStep(
                        'Загружаем большой файл и проверяем отображение формы',
                        async () => {
                            await this.document.uploadNewFile('autotests_fixtures/bigFile');

                            await this.browser.allure.runStep('Дожидаемся появления ошибки загрузки файла', () =>
                                this.browser.waitUntil(
                                    () => this.document.fileError.vndIsExisting(),
                                    this.browser.options.waitforTimeout,
                                    'Ошибка загрузки не появилась',
                                ),
                            );

                            await this.document.uploadButton
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Ещё одна кнопка загрузки документа отображается');

                            await this.document.fileError
                                .getText()
                                .should.eventually.be.equal(
                                    `Размер файла превысил ${MAX_FILE_SIZE_IN_MEGABYTES} МБ`,
                                    'Текст отображается корректно',
                                );
                        },
                    );

                    await this.document.removeUploadedFile();

                    await this.browser.allure.runStep('Дожидаемся скрытия формы с ошибкой загрузки файла', () =>
                        this.browser.waitUntil(
                            async () => {
                                const existing = await this.document.fileError.vndIsExisting();

                                return existing === false;
                            },
                            this.browser.options.waitforTimeout,
                            'Форма с ошибкой загрузки файла не скрылась',
                        ),
                    );
                },
            }),
        },
    },
});
