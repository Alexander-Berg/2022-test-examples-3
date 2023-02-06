'use strict';

import path from 'path';

import {makeSuite, makeCase, mergeSuites, importSuite} from 'ginny';

/**
 * Тесты для формы редактирования контактных данных
 * @param {PageObject.ContactsForm} contactsForm - форма контактных данных
 * @param {PageObject.ContactsForm} form - форма
 * @param {PageObject.ContactsForm} guaranteeLetter - блок с загрузкой гарантийного письма
 * @param {PageObject.ContactsForm} trademarkDocument - блок с загрузкой свидетельства на товарный знак
 */
export default makeSuite('Форма "Контактные данные". Редактирование.', {
    environment: 'kadavr',
    feature: 'Настройки',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления формы контактных данных', () =>
                    this.contactsForm.waitForExist(),
                );

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
            'При нажатии на кнопку "Отмена"': {
                'несохранённые изменения сбрасываются': makeCase({
                    issue: 'VNDFRONT-3215',
                    id: 'vendor_auto-251',

                    async test() {
                        await this.browser.allure.runStep('Проверяем изначальные значения полей формы', async () => {
                            await this.form
                                .getFieldValue('company')
                                .should.eventually.be.equal(
                                    'Прекрасная компания',
                                    'У поля "Название компании" корректное изначальное значение',
                                );

                            await this.form
                                .getFieldValue('name')
                                .should.eventually.be.equal(
                                    'Евграфий Пискунов',
                                    'У поля "Контактное лицо" корректное изначальное значение',
                                );
                        });

                        await this.browser.allure.runStep('Редактируем контактные данные', async () => {
                            await this.form.setFieldValueByName(
                                'company',
                                'Новое название компании',
                                'Название компании',
                            );

                            await this.form.setFieldValueByName('name', 'Иван Иванов', 'Контактное лицо');
                        });

                        await this.form.clickCancelButton();

                        // Считаем, что форма в режиме чтения, когда отобразится кнопка "Изменить"
                        await this.browser.allure.runStep('Дожидаемся перехода формы в режим чтения', () =>
                            this.browser.waitUntil(
                                () => this.form.editButton.vndIsExisting(),
                                this.browser.options.waitforTimeout,
                                'Форма не перешла в режим чтения',
                            ),
                        );

                        await this.browser.allure.runStep('Проверяем значения полей формы', async () => {
                            await this.form
                                .getReadonlyFieldValue('company')
                                .should.eventually.be.equal(
                                    'Прекрасная компания',
                                    'У поля "Название компании" значение не изменилось',
                                );

                            await this.form
                                .getReadonlyFieldValue('name')
                                .should.eventually.be.equal(
                                    'Евграфий Пискунов',
                                    'У поля "Контактное лицо" значение не изменилось',
                                );
                        });
                    },
                }),
            },
        },
        {
            'При вводе пустых или невалидных значений': {
                'отображаются хинты о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3215',
                    id: 'vendor_auto-256',

                    async test() {
                        await this.browser.allure.runStep(
                            'Проверяем валидацию формата значения у поля "Телефон"',
                            async () => {
                                await this.form
                                    .getFieldValue('phone')
                                    .should.eventually.be.equal(
                                        '+79876543210',
                                        'У поля "Телефон" корректное изначальное значение',
                                    );

                                await this.form.setFieldValueByName('phone', 'эээ', 'Телефон');

                                await this.form
                                    .getFieldValidationErrorPopup('phone')
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Попап с ошибкой у поля "Телефон" отображается');

                                await this.form
                                    .getFieldValidationErrorPopup('phone')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Формат телефона: +74957392222',
                                        'Текст ошибки у поля "Телефон" корректный',
                                    );
                            },
                        );

                        await this.browser.allure.runStep(
                            'Проверяем валидацию формата значения у поля "Электронная почта"',
                            async () => {
                                await this.form
                                    .getFieldValue('email')
                                    .should.eventually.be.equal(
                                        'auto@test',
                                        'У поля "Электронная почта" корректное изначальное значение',
                                    );

                                await this.form.setFieldValueByName('email', 'эээ', 'Электронная почта');

                                await this.form
                                    .getFieldValidationErrorPopup('email')
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Попап с ошибкой у поля "Электронная почта" отображается',
                                    );

                                await this.form
                                    .getFieldValidationErrorPopup('email')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Неверный формат эл. почты',
                                        'Текст ошибки у поля "Электронная почта" корректный',
                                    );
                            },
                        );

                        await this.browser.allure.runStep(
                            'Очищаем значения у всех полей и проверяем наличие попапа с ошибкой',
                            async () => {
                                await this.browser.allure.runStep('Очищаем поле "Компания"', () =>
                                    this.form.getFieldByName('company').vndSetValue(''),
                                );

                                await this.form
                                    .getFieldValidationErrorPopup('company')
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Попап с ошибкой у поля "Компания" отображается');

                                await this.form
                                    .getFieldValidationErrorPopup('company')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Поле обязательно для заполнения',
                                        'Текст ошибки у поля "Компания" корректный',
                                    );

                                await this.browser.allure.runStep('Очищаем поле "Контактное лицо"', () =>
                                    this.form.getFieldByName('name').vndSetValue(''),
                                );

                                await this.form
                                    .getFieldValidationErrorPopup('name')
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Попап с ошибкой у поля "Контактное лицо" отображается',
                                    );

                                await this.form
                                    .getFieldValidationErrorPopup('name')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Поле обязательно для заполнения',
                                        'Текст ошибки у поля "Контактное лицо" корректный',
                                    );

                                await this.browser.allure.runStep('Очищаем поле "Телефон"', () =>
                                    this.form.getFieldByName('phone').vndSetValue(''),
                                );

                                await this.form
                                    .getFieldValidationErrorPopup('phone')
                                    .isVisible()
                                    .should.eventually.be.equal(true, 'Попап с ошибкой у поля "Телефон" отображается');

                                await this.form
                                    .getFieldValidationErrorPopup('phone')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Поле обязательно для заполнения',
                                        'Текст ошибки у поля "Телефон" корректный',
                                    );

                                await this.browser.allure.runStep('Очищаем поле "Электронная почта"', () =>
                                    this.form.getFieldByName('email').vndSetValue(''),
                                );

                                await this.form
                                    .getFieldValidationErrorPopup('email')
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Попап с ошибкой у поля "Электронная почта" отображается',
                                    );

                                await this.form
                                    .getFieldValidationErrorPopup('email')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Поле обязательно для заполнения',
                                        'Текст ошибки у поля "Электронная почта" корректный',
                                    );

                                await this.browser.allure.runStep('Очищаем поле "Почтовый адрес с индексом"', () =>
                                    this.form.getFieldByName('address', 'textarea').vndSetValue(''),
                                );

                                await this.form
                                    .getFieldValidationErrorPopup('address')
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Попап с ошибкой у поля "Почтовый адрес с индексом" отображается',
                                    );

                                await this.form
                                    .getFieldValidationErrorPopup('address')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Поле обязательно для заполнения',
                                        'Текст ошибки у поля "Почтовый адрес с индексом" корректный',
                                    );
                            },
                        );
                    },
                }),
            },
        },
        {
            'При успешном сохранении': {
                'контактные данные обновляются': makeCase({
                    issue: 'VNDFRONT-3215',
                    id: 'vendor_auto-252',

                    async test() {
                        await this.browser.allure.runStep('Проверяем изначальные значения полей формы', async () => {
                            await this.form
                                .getFieldValue('company')
                                .should.eventually.be.equal(
                                    'Прекрасная компания',
                                    'У поля "Название компании" корректное изначальное значение',
                                );

                            await this.form
                                .getFieldValue('name')
                                .should.eventually.be.equal(
                                    'Евграфий Пискунов',
                                    'У поля "Контактное лицо" корректное изначальное значение',
                                );
                        });

                        await this.browser.allure.runStep('Редактируем контактные данные', async () => {
                            await this.form.setFieldValueByName(
                                'company',
                                'Новое название компании',
                                'Название компании',
                            );

                            await this.form.setFieldValueByName('name', 'Иван Иванов', 'Контактное лицо');
                        });

                        await this.form.submit('Сохранить');

                        // Считаем, что форма в режиме чтения, когда отобразится кнопка "Изменить"
                        await this.browser.allure.runStep('Дожидаемся перехода формы в режим чтения', () =>
                            this.browser.waitUntil(
                                () => this.form.editButton.vndIsExisting(),
                                this.browser.options.waitforTimeout,
                                'Форма не перешла в режим чтения',
                            ),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем сохранённые значения полей формы в режиме просмотра',
                            async () => {
                                await this.form
                                    .getReadonlyFieldValue('company')
                                    .should.eventually.be.equal(
                                        'Новое название компании',
                                        'У поля "Название компании" корректное новое значение',
                                    );

                                await this.form
                                    .getReadonlyFieldValue('name')
                                    .should.eventually.be.equal(
                                        'Иван Иванов',
                                        'У поля "Контактное лицо" корректное новое значение',
                                    );
                            },
                        );
                    },
                }),
            },
        },
        {
            'При загрузке и сохранении свидетельства на товарный знак и гарантийного письма': {
                'файлы загружаются и сохраняются корректно': makeCase({
                    issue: 'VNDFRONT-3229',
                    id: 'vendor_auto-44',
                    async test() {
                        await this.form
                            .getDocumentUploadFieldByName('trademarkDocuments')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Поле "Основание использования товарного знака" отображается',
                            );

                        await this.browser.allure.runStep('Добавляем свидетельство на товарный знак', async () => {
                            await this.form
                                .link('Добавить свидетельство на товарный знак')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Кнопка "Добавить свидетельство на товарный знак" отображается',
                                );

                            await this.form.clickAddNewDocument('Добавить свидетельство на товарный знак');

                            await this.browser.allure.runStep(
                                'Дожидаемся появления блока загрузки свидетельства на товарный знак',
                                () => this.trademarkDocument.waitForExist(),
                            );

                            await this.browser.allure.runStep(
                                'Проверяем корректность отображения блока загрузки свидетельства на товарный знак',
                                async () => {
                                    await this.unlimitedCheckbox.icon
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Чекбокс "Не ограничено" выбран');

                                    await this.browser
                                        .yaSafeAction(this.expireDate.input.isEnabled(), true)
                                        .should.eventually.equal(
                                            false,
                                            'Поле "Дата окончания" отображается и неактивно',
                                        );

                                    await this.trademarkDocument.uploadButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                                    await this.trademarkDocument.resetButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');
                                },
                            );

                            await this.form
                                .link('Добавить свидетельство на товарный знак')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Кнопка "Добавить свидетельство на товарный знак" отображается',
                                );

                            await this.form
                                .link('Добавить гарантийное письмо')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка "Добавить гарантийное письмо" отображается');

                            await this.browser.allure.runStep(
                                'Загружаем свидетельство на товарный знак и проверяем отображение формы',
                                async () => {
                                    await this.trademarkDocument.uploadNewFile(
                                        path.resolve(__dirname, 'testData/trademark_certificate.jpg'),
                                    );

                                    await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
                                        this.browser.waitUntil(
                                            () => this.trademarkDocument.uploadedFileName.vndIsExisting(),
                                            this.browser.options.waitforTimeout,
                                            'Файл не загрузился',
                                        ),
                                    );

                                    await this.trademarkDocument.uploadedFileName
                                        .getText()
                                        .should.eventually.be.equal(
                                            'trademark_certificate',
                                            'Название файла отображается корректно',
                                        );

                                    await this.trademarkDocument.uploadButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(
                                            true,
                                            'Кнопка загрузки ещё одного документа отображается',
                                        );
                                },
                            );
                        });

                        await this.browser.allure.runStep('Добавляем гарантийное письмо', async () => {
                            await this.form
                                .link('Добавить гарантийное письмо')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка "Добавить гарантийное письмо" отображается');

                            await this.form.clickAddNewDocument('Добавить гарантийное письмо');

                            await this.browser.allure.runStep(
                                'Дожидаемся появления блока загрузки гарантийного письма',
                                () => this.guaranteeLetter.waitForExist(),
                            );

                            await this.browser.vndScrollToBottom();

                            await this.browser.allure.runStep(
                                'Проверяем корректность отображения блока загрузки гарантийного письма',
                                async () => {
                                    await this.guaranteeLetter.uploadButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                                    await this.guaranteeLetter.instructions
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Инструкция по загрузке файлов отображается');

                                    await this.guaranteeLetter.instructions
                                        .getText()
                                        .should.eventually.includes(
                                            'Скачайте шаблон гарантийного письма',
                                            'Текст инструкции по загрузке файлов корректный',
                                        );

                                    await this.guaranteeLetter.resetButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');
                                },
                            );

                            await this.form
                                .link('Добавить свидетельство на товарный знак')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Кнопка "Добавить свидетельство на товарный знак" отображается',
                                );

                            await this.form
                                .link('Добавить гарантийное письмо')
                                .vndIsExisting()
                                .should.eventually.be.equal(true, 'Кнопка "Добавить гарантийное письмо" отображается');

                            await this.browser.allure.runStep(
                                'Загружаем гарантийное письмо и проверяем корректность отображения формы',
                                async () => {
                                    await this.guaranteeLetter.uploadNewFile(
                                        path.resolve(__dirname, 'testData/guarantee_letter.jpeg'),
                                    );

                                    await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
                                        this.browser.waitUntil(
                                            () => this.guaranteeLetter.uploadedFileName.vndIsExisting(),
                                            this.browser.options.waitforTimeout,
                                            'Файл не загрузился',
                                        ),
                                    );

                                    await this.guaranteeLetter.uploadedFileName
                                        .getText()
                                        .should.eventually.be.equal(
                                            'guarantee_letter',
                                            'Название файла отображается корректно',
                                        );

                                    await this.guaranteeLetter.uploadButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(
                                            true,
                                            'Кнопка загрузки ещё одного документа отображается',
                                        );
                                },
                            );
                        });
                    },
                }),
            },
        },
        {
            'При указании некорректных значений для нового свидетельства на товарный знак': {
                'отображаются хинты о необходимости ввода корректных значений': makeCase({
                    issue: 'VNDFRONT-3229',
                    id: 'vendor_auto-254',
                    async test() {
                        await this.form
                            .getDocumentUploadFieldByName('trademarkDocuments')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Поле "Основание использования товарного знака" отображается',
                            );

                        await this.browser.allure.runStep('Добавляем свидетельство на товарный знак', async () => {
                            await this.form
                                .link('Добавить свидетельство на товарный знак')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Кнопка "Добавить свидетельство на товарный знак" отображается',
                                );

                            await this.form.clickAddNewDocument('Добавить свидетельство на товарный знак');

                            await this.browser.allure.runStep(
                                'Дожидаемся появления блока загрузки свидетельства на товарный знак',
                                () => this.trademarkDocument.waitForExist(),
                            );

                            await this.browser.allure.runStep(
                                'Проверяем корректность отображения блока загрузки свидетельства на товарный знак',
                                async () => {
                                    await this.unlimitedCheckbox.icon
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Чекбокс "Не ограничено" выбран');

                                    await this.browser
                                        .yaSafeAction(this.expireDate.input.isEnabled(), true)
                                        .should.eventually.equal(
                                            false,
                                            'Поле "Дата окончания" отображается и неактивно',
                                        );

                                    await this.trademarkDocument.uploadButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                                    await this.trademarkDocument.resetButton
                                        .vndIsExisting()
                                        .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');
                                },
                            );

                            await this.browser.allure.runStep('Снимаем чекбокс "Не ограничено"', () =>
                                this.unlimitedCheckbox.click(),
                            );

                            await this.browser
                                .yaSafeAction(this.expireDate.input.isEnabled(), false)
                                .should.eventually.equal(true, 'Поле "Дата окончания" отображается и активно');

                            await this.form
                                .getFieldValidationErrorPopup('expireDate')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Сообщение об ошибке у поля "Дата окончания" отображается',
                                );

                            await this.form
                                .getFieldValidationErrorPopup('expireDate')
                                .getText()
                                .should.eventually.be.equal('Укажите дату окончания', 'Текст ошибки корректный');

                            await this.form
                                .getFieldValidationErrorPopup('file')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Сообщение об ошибке у поля c загрузкой файла отображается',
                                );

                            await this.form
                                .getFieldValidationErrorPopup('file')
                                .getText()
                                .should.eventually.be.equal(
                                    'Загрузите не менее одного файла',
                                    'Текст ошибки корректный',
                                );

                            await this.browser.allure.runStep('Вводим в поле "Дата окончания" значение "эээ"', () =>
                                this.expireDate.setValue('эээ'),
                            );

                            await this.form
                                .getFieldValidationErrorPopup('expireDate')
                                .vndIsExisting()
                                .should.eventually.be.equal(
                                    true,
                                    'Сообщение об ошибке у поля "Дата окончания" отображается',
                                );

                            await this.form
                                .getFieldValidationErrorPopup('expireDate')
                                .getText()
                                .should.eventually.includes('Формат даты: ', 'Текст ошибки корректный');

                            await this.trademarkDocument.delete();

                            await this.browser.allure.runStep('Дожидаемся скрытия блока загрузки свидетельства', () =>
                                this.browser.waitUntil(
                                    async () => {
                                        const existing = await this.trademarkDocument.isExisting();

                                        return existing === false;
                                    },
                                    this.browser.options.waitforTimeout,
                                    'Блок загрузки свидетельства не скрылся',
                                ),
                            );
                        });
                    },
                }),
            },
        },
        {
            'При загрузке нового свидетельства на товарный знак с истекшим сроком действия': {
                'отображается хинт о невозможности загрузки файла с истекшим сроком действия': makeCase({
                    issue: 'VNDFRONT-3235',
                    id: 'vendor_auto-255',
                    async test() {
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

                        await this.browser.allure.runStep(
                            'Дожидаемся появления блока загрузки свидетельства на товарный знак',
                            () => this.trademarkDocument.waitForExist(),
                        );

                        await this.browser.allure.runStep(
                            'Проверяем корректность отображения блока загрузки свидетельства на товарный знак',
                            async () => {
                                await this.unlimitedCheckbox.icon
                                    .vndIsExisting()
                                    .should.eventually.be.equal(true, 'Чекбокс "Не ограничено" выбран');

                                await this.browser
                                    .yaSafeAction(this.expireDate.input.isEnabled(), true)
                                    .should.eventually.equal(false, 'Поле "Дата окончания" отображается и неактивно');

                                await this.trademarkDocument.uploadButton
                                    .vndIsExisting()
                                    .should.eventually.be.equal(true, 'Кнопка загрузки документа отображается');

                                await this.trademarkDocument.resetButton
                                    .vndIsExisting()
                                    .should.eventually.be.equal(true, 'Кнопка удаления файла отображается');
                            },
                        );

                        await this.browser.allure.runStep('Снимаем чекбокс "Не ограничено"', () =>
                            this.unlimitedCheckbox.click(),
                        );

                        await this.browser
                            .yaSafeAction(this.expireDate.input.isEnabled(), false)
                            .should.eventually.equal(true, 'Поле "Дата окончания" отображается и активно');

                        await this.form
                            .getFieldValidationErrorPopup('expireDate')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Дата окончания" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('expireDate')
                            .getText()
                            .should.eventually.be.equal('Укажите дату окончания', 'Текст ошибки корректный');

                        await this.form
                            .getFieldValidationErrorPopup('file')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля c загрузкой файла отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('file')
                            .getText()
                            .should.eventually.be.equal('Загрузите не менее одного файла', 'Текст ошибки корректный');

                        await this.browser.allure.runStep(
                            'Вводим в поле "Дата окончания" прошедшую дату "12.08.2011"',
                            () => this.expireDate.setValue('12.08.2011'),
                        );

                        await this.browser.allure.runStep(
                            'Дожидаемся скрытия попапа с ошибкой у поля "Дата окончания"',
                            () =>
                                this.browser.waitUntil(
                                    async () => {
                                        const visible = await this.form
                                            .getFieldValidationErrorPopup('expireDate')
                                            .isVisible();

                                        return visible === false;
                                    },
                                    this.browser.options.waitforTimeout,
                                    'Попап с ошибкой не скрылся',
                                ),
                        );

                        await this.browser.allure.runStep('Загружаем свидетельство на товарный знак', async () => {
                            await this.trademarkDocument.uploadNewFile(
                                path.resolve(__dirname, 'testData/trademark_certificate.jpg'),
                            );

                            await this.browser.allure.runStep('Дожидаемся загрузки файла', () =>
                                this.browser.waitUntil(
                                    () => this.trademarkDocument.uploadedFileName.vndIsExisting(),
                                    this.browser.options.waitforTimeout,
                                    'Файл не загрузился',
                                ),
                            );
                        });

                        await this.form.submit('Сохранить');

                        await this.browser.allure.runStep(
                            'Дожидаемся отображения тултипа с ошибкой у кнопки "Сохранить"',
                            () =>
                                this.browser.waitUntil(
                                    () => this.form.submitErrorPopup.vndIsExisting(),
                                    this.browser.options.waitforTimeout,
                                    'Тултип не отобразился',
                                ),
                        );

                        await this.form.submitErrorPopup
                            .getText()
                            .should.eventually.be.equal(
                                'Нельзя добавить документ, если его срок действия уже закончился',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
        },
        importSuite('DocumentUpload/templates', {
            suiteName: 'Шаблоны гарантийного письма',
            meta: {
                feature: 'Настройки',
                issue: 'VNDFRONT-3215',
                id: 'vendor_auto-876',
            },
            pageObjects: {
                guaranteeLetter() {
                    return this.createPageObject(
                        'DocumentUpload',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.form,
                        '[data-e2e="guarantee-letter-editable-document"]',
                    );
                },
            },
        }),
        importSuite('DocumentUpload/bigFileUpload', {
            suiteName: 'Загрузка большого файла гарантийного письма.',
            meta: {
                feature: 'Настройки',
                issue: 'VNDFRONT-3235',
                id: 'vendor_auto-753',
                environment: 'kadavr',
            },
            params: {
                linkCaption: 'Добавить гарантийное письмо',
            },
            pageObjects: {
                document() {
                    return this.createPageObject(
                        'DocumentUpload',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.form,
                        '[data-e2e="guarantee-letter-editable-document"]',
                    );
                },
            },
        }),
        importSuite('DocumentUpload/bigFileUpload', {
            suiteName: 'Загрузка большого файла свидетельства о товарном знаке.',
            meta: {
                feature: 'Настройки',
                issue: 'VNDFRONT-3235',
                id: 'vendor_auto-253',
                environment: 'kadavr',
            },
            params: {
                linkCaption: 'Добавить свидетельство на товарный знак',
                withExpireDateCheck: true,
            },
            pageObjects: {
                document() {
                    return this.createPageObject(
                        'DocumentUpload',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.form,
                        '[data-e2e=trademark-certificate-editable-document]',
                    );
                },
                unlimitedCheckbox() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('CheckboxB2b', this.document);
                },
                expireDate() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('InputB2b', this.document);
                },
            },
        }),
    ),
});
