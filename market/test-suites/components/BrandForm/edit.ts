'use strict';

import path from 'path';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

const LOGO_SRC = 'https://avatars.mds.yandex.net/get-mpic/1912105/img_id2910750236376969175.png/orig';

/**
 * @param {PageObject.BrandForm} brandForm – форма редактирования бренда
 * @param {PageObject.Form} form - форма
 * @param {PageObject.Suggest} countrySuggest - саджест со списком стран
 * @param {PageObject.FileB2b} file - загрузчик файла с логотипом
 */
export default makeSuite('Редактирование.', {
    feature: 'Настройки',
    environment: 'kadavr',
    issue: 'VNDFRONT-2775',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
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

                    await this.browser
                        .yaSafeAction(this.file.chooseButton.isEnabled(), false)
                        .should.eventually.equal(true, 'Кнопка [Выбрать файл] под логотипом отображается и активна');
                });
            },
        },
        importSuite('Hint', {
            suiteName: 'Подсказка к логотипу.',
            meta: {
                issue: 'VNDFRONT-2775',
                id: 'vendor_auto-260',
                environment: 'kadavr',
            },
            params: {
                text:
                    'Требования к изображению:\n' +
                    'Формат изображения PNG\n' +
                    'Размеры от 200 до 500px\n' +
                    'Фон должен быть белый или прозрачный\n' +
                    'Без полей (рамки вокруг текста/изображения, в которой не содержится никакой информации)',
            },
            pageObjects: {
                hint() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Hint', this.brandForm);
                },
            },
        }),
        {
            'При сохранении значений без изменений': {
                'отображается хинт о необходимости заполнить хотя бы одно поле': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-264',

                    async test() {
                        await this.form.submit('Сохранить');

                        await this.form.submitErrorPopup
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Тултип с ошибкой у кнопки "Сохранить" отображается');

                        await this.form.submitErrorPopup
                            .getText()
                            .should.eventually.be.equal(
                                'Требуется заполнить хотя бы одно поле',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
        },
        {
            'При загрузке логотипа с расширением, отличающимся от png': {
                'отображается хинт про некорректный тип файла': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-261',

                    async test() {
                        await this.brandForm
                            .getBrandLogoSrc('Логотип Cisco')
                            .should.eventually.be.equal(LOGO_SRC, 'У логотипа корректное изначальное значение');

                        await this.form.setValue({
                            type: 'file',
                            value: path.resolve(__dirname, 'testData/Logo.jpg'),
                            fileName: 'Logo.jpg',
                            fieldName: 'Логотип',
                        });

                        await this.form
                            .getFieldValidationErrorPopup('picture')
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Сообщение об ошибке у поля "Логотип" отображается');

                        await this.form
                            .getFieldValidationErrorPopup('picture')
                            .getText()
                            .should.eventually.be.equal('Некорректный тип изображения', 'Текст ошибки корректный');

                        await this.brandForm
                            .getBrandLogoSrc('Логотип Cisco')
                            .should.eventually.be.equal(LOGO_SRC, 'У логотипа значение не изменилось');
                    },
                }),
            },
        },
        {
            'При успешном сохранении': {
                'отображается сообщение о том, что заявка в работе': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-263',

                    async test() {
                        await this.browser.allure.runStep('Проверяем изначальные значения полей формы', async () => {
                            await this.brandForm
                                .getBrandLogoSrc('Логотип Cisco')
                                .should.eventually.be.equal(LOGO_SRC, 'У логотипа корректное изначальное значение');

                            await this.form
                                .getFieldValue('site')
                                .should.eventually.be.equal(
                                    'https://www.cisco.ru/',
                                    'У поля "Сайт" корректное изначальное значение',
                                );
                        });

                        await this.browser.allure.runStep('Редактируем данные о бренде', async () => {
                            await this.form.setValue({
                                type: 'file',
                                value: path.resolve(__dirname, 'testData/Logo.png'),
                                fileName: 'Logo.png',
                                fieldName: 'Логотип',
                            });

                            await this.form.setFieldValueByName(
                                'site',
                                'https://en.wikipedia.org/wiki/Cisco_Systems',
                                'Сайт',
                            );
                        });

                        await this.form.submit('Сохранить');

                        await this.browser.allure.runStep('Дожидаемся отображения сообщения "Заявка в работе"', () =>
                            this.browser.waitUntil(
                                () => this.brandForm.formSavedPanel.vndIsExisting(),
                                this.browser.options.waitforTimeout,
                                'Сообщение "Заявка в работе" не появилось',
                            ),
                        );
                    },
                }),
            },
        },
        {
            'При вводе пустого или невалидного года основания': {
                'отображается хинт о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-524',

                    async test() {
                        await this.form
                            .getFieldValue('foundationYear')
                            .should.eventually.be.equal(
                                '1984',
                                'У поля "Год основания" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep('Вводим в поле "Год основания" значение "1000"', () =>
                            this.form.getFieldByName('foundationYear').vndSetValue('1000'),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Год основания" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .getText()
                            .should.eventually.be.equal('Неверный год', 'Текст ошибки корректный');

                        await this.browser.allure.runStep('Вводим в поле "Год основания" год из будущего "6666"', () =>
                            this.form.getFieldByName('foundationYear').vndSetValue('6666'),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Год основания" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .getText()
                            .should.eventually.be.equal('Неверный год', 'Текст ошибки корректный');

                        await this.browser.allure.runStep('Вводим в поле "Год основания" текст "эээээ"', () =>
                            this.form.getFieldByName('foundationYear').vndSetValue('эээээ'),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Год основания" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .getText()
                            .should.eventually.be.equal('Неверный год', 'Текст ошибки корректный');

                        await this.browser.allure.runStep('Очищаем поле "Год основания"', () =>
                            this.form.getFieldByName('foundationYear').vndSetValue(''),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .vndIsExisting()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Год основания" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('foundationYear')
                            .getText()
                            .should.eventually.be.equal('Нельзя удалять значения полей', 'Текст ошибки корректный');

                        await this.form.submit('Сохранить');

                        await this.form.submitErrorPopup
                            .isVisible()
                            .should.eventually.be.equal(true, 'Тултип с ошибкой у кнопки "Сохранить" отображается');

                        await this.form.submitErrorPopup
                            .getText()
                            .should.eventually.be.equal(
                                'Требуется заполнить хотя бы одно поле',
                                'Текст ошибки корректный',
                            );
                    },
                }),
            },
        },
        {
            'При вводе пустой или невалидной страны': {
                'отображается хинт о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-258',

                    async test() {
                        await this.form
                            .getFieldValue('country')
                            .should.eventually.be.equal('США', 'У поля "Страна" корректное изначальное значение');

                        await this.browser.allure.runStep('Вводим в поле "Страна" несуществующую страну', async () => {
                            await this.countrySuggest.setText('швежая рыба');

                            await this.countrySuggest.waitForPopupItemsCount(0);
                        });

                        await this.form
                            .getFieldValidationErrorPopup('suggest')
                            .isVisible()
                            .should.eventually.be.equal(true, 'Попап с ошибкой отображается');

                        await this.form
                            .getFieldValidationErrorPopup('suggest')
                            .getText()
                            .should.eventually.be.equal('Нельзя удалять значения полей', 'Текст ошибки корректный');

                        await this.browser.allure.runStep('Вводим в поле "Страна" корректное значение', async () => {
                            await this.countrySuggest.setText('швеция');

                            await this.countrySuggest.waitForPopupItemsCount(1);

                            await this.countrySuggest.selectItem(0);
                        });

                        await this.form
                            .getFieldValue('country')
                            .should.eventually.be.equal('Швеция', 'Значение у поля "Страна" заполнилось корректно');

                        await this.browser.allure.runStep('Дожидаемся скрытия попапа с ошибкой', () =>
                            this.browser.waitUntil(
                                async () => {
                                    const visible = await this.form.getFieldValidationErrorPopup('suggest').isVisible();

                                    return visible === false;
                                },
                                this.browser.options.waitforTimeout,
                                'Попап с ошибкой отображается',
                            ),
                        );
                    },
                }),
            },
        },
        {
            'При нажатии на кнопку "Отмена"': {
                'несохранённые изменения сбрасываются': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-262',

                    async test() {
                        await this.browser.allure.runStep('Проверяем изначальные значения полей формы', async () => {
                            await this.brandForm
                                .getBrandLogoSrc('Логотип Cisco')
                                .should.eventually.be.equal(LOGO_SRC, 'У логотипа корректное изначальное значение');

                            await this.form
                                .getFieldValue('site')
                                .should.eventually.be.equal(
                                    'https://www.cisco.ru/',
                                    'У поля "Сайт" корректное изначальное значение',
                                );
                        });

                        await this.browser.allure.runStep('Редактируем данные о бренде', async () => {
                            await this.form.setValue({
                                type: 'file',
                                value: path.resolve(__dirname, 'testData/Logo.png'),
                                fileName: 'Logo.png',
                                fieldName: 'Логотип',
                            });

                            await this.form.setFieldValueByName(
                                'site',
                                'https://en.wikipedia.org/wiki/Cisco_Systems',
                                'Сайт',
                            );
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
                            await this.brandForm
                                .getBrandLogoSrc('Логотип Cisco')
                                .should.eventually.be.equal(LOGO_SRC, 'У логотипа значение не изменилось');

                            await this.form
                                .getReadonlyFieldValue('site')
                                .should.eventually.be.equal(
                                    'https://www.cisco.ru/',
                                    'У поля "Сайт" значение не изменилось',
                                );
                        });
                    },
                }),
            },
        },
        {
            'При вводе невалидной ссылки на описание бренда или программу рекомендаций': {
                'отображается хинт о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3197',
                    id: 'vendor_auto-279',

                    async test() {
                        await this.form
                            .getFieldValue('descriptionSource')
                            .should.eventually.be.equal(
                                'https://www.cisco.ru/',
                                'У поля "Ссылка на описание.URL" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep(
                            'Вводим в поле "Ссылка на описание.URL" невалидное значение "эээээ"',
                            () => this.form.getFieldByName('descriptionSource').vndSetValue('эээээ'),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('url')
                            .isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Ссылка на описание.URL" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('url')
                            .getText()
                            .should.eventually.be.equal('Неверный URL', 'Текст ошибки корректный');

                        await this.form
                            .getFieldValue('recommendedShopsUrl')
                            .should.eventually.be.equal(
                                'https://market.yandex.ru/journal/info/rekomendatsii-magazinov-cisco',
                                'У поля "Программа рекомендаций" корректное изначальное значение',
                            );

                        await this.browser.allure.runStep(
                            'Вводим в поле "Программа рекомендаций" невалидное значение "эээээ"',
                            () => this.form.getFieldByName('recommendedShopsUrl').vndSetValue('эээээ'),
                        );

                        await this.form
                            .getFieldValidationErrorPopup('recommendedShopsUrl')
                            .isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Сообщение об ошибке у поля "Программа рекомендаций" отображается',
                            );

                        await this.form
                            .getFieldValidationErrorPopup('recommendedShopsUrl')
                            .getText()
                            .should.eventually.be.equal('Неверный URL', 'Текст ошибки корректный');
                    },
                }),
            },
        },
        importSuite('FormField/validate', {
            suiteName: 'Валидация текстового поля "Название".',
            meta: {
                id: 'vendor_auto-259',
                issue: 'VNDFRONT-3197',
                environment: 'kadavr',
            },
            params: {
                name: 'name',
                label: 'Название',
                initialValue: 'Cisco',
                maxLength: 500,
            },
        }),
        importSuite('FormField/validate', {
            suiteName: 'Валидация текстового поля "Описание".',
            meta: {
                id: 'vendor_auto-259',
                issue: 'VNDFRONT-3197',
                environment: 'kadavr',
            },
            params: {
                name: 'description',
                label: 'Описание',
                initialValue: 'Описание бренда',
                maxLength: 1000,
                selector: 'textarea',
            },
        }),
        importSuite('FormField/validate', {
            suiteName: 'Валидация текстового поля "Ссылка на описание.Текст ссылки".',
            meta: {
                id: 'vendor_auto-259',
                issue: 'VNDFRONT-3197',
                environment: 'kadavr',
            },
            params: {
                name: 'descriptionSource',
                label: 'Ссылка на описание.Текст ссылки',
                initialValue: 'Текст ссылки',
                maxLength: 120,
                selector: 'input[placeholder="Текст ссылки"]',
                errorName: 'url-text',
                errorText: 'Текст ссылки должен содержать не более 120 символов',
            },
        }),
        importSuite('Hint', {
            suiteName: 'Подсказка к ссылке на программу рекомендаций.',
            meta: {
                issue: 'VNDFRONT-3404',
                id: 'vendor_auto-1175',
                environment: 'kadavr',
            },
            params: {
                text:
                    'Чтобы вы могли рекомендовать магазины на Маркете, ' +
                    'на вашем сайте нужно опубликовать правила, которым магазины ' +
                    'должны соответствовать, чтобы участвовать в программе рекомендаций.' +
                    '\nКак порекомендовать магазины',
            },
            pageObjects: {
                hint() {
                    return this.createPageObject('Hint', 'label[for*="recommendedShopsUrl"]');
                },
            },
        }),
    ),
});
