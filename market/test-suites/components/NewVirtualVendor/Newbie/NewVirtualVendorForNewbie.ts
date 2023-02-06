'use strict';

import {importSuite, mergeSuites, makeSuite, makeCase} from 'ginny';

import formSubmit from '../formSubmit';

/**
 * Тест на блок NewVirtualVendor для новичка
 *
 * @param {PageObject.Form} form
 * @param {PageObject.EntryForm} entryForm
 * @param {PageObject.DocumentUpload} trademarkDocument - блок со свидетельством на товарный знак
 * @param {PageObject.DocumentUpload} guaranteeLetter - блок с гарантийным письмом
 *
 */
export default makeSuite('Анкета нового производителя.', {
    issue: 'VNDFRONT-1653',
    environment: 'kadavr',
    feature: 'Оферта',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления тестируемой формы', () => this.form.waitForExist());
            },
        },
        {
            'При успешной отправке формы': mergeSuites(
                formSubmit,
                {
                    beforeEach() {
                        return this.allure.runStep('Ожидаем появления заглушки', () => this.entryForm.waitForExist());
                    },
                },
                {
                    'показывается заглушка "Анкета отправлена"': makeCase({
                        id: 'vendor_auto-50',
                        issue: 'VNDFRONT-1653',
                        environment: 'kadavr',
                        feature: 'Оферта',
                        test() {
                            return this.entryForm.getTitle().should.eventually.equal('Анкета отправлена');
                        },
                    }),
                },
                {
                    'с загрузкой гарантийного письма': {
                        'показывается заглушка "Анкета отправлена"': makeCase({
                            id: 'vendor_auto-748',
                            issue: 'VNDFRONT-3414',
                            environment: 'kadavr',
                            feature: 'Оферта',
                            test() {
                                /**
                                 * Для этого кейса в formSubmit есть логика загрузки гарантийного письма.
                                 * Для других кейсов отправки анкеты загрузка письма не происходит.
                                 */
                                return this.entryForm.getTitle().should.eventually.equal('Анкета отправлена');
                            },
                        }),
                    },
                },
                importSuite('Link', {
                    suiteName: 'Ссылка "Зачем нужен кабинет вендора"',
                    meta: {
                        id: 'vendor_auto-386',
                        issue: 'VNDFRONT-1653',
                        environment: 'kadavr',
                        feature: 'Оферта',
                    },
                    params: {
                        caption: 'Зачем нужен кабинет вендора',
                        url: 'https://yandex.ru/support/vendormarket/room.html',
                    },
                    pageObjects: {
                        link() {
                            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                            return this.createPageObject('Link', this.browser, this.entryForm.link);
                        },
                    },
                }),
            ),
        },
        importSuite('NewVirtualVendor/offerRequired', {
            pageObjects: {
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('DocumentUpload/templates', {
            suiteName: 'Шаблоны гарантийного письма в анкете.',
            meta: {
                feature: 'Оферта',
                issue: 'VNDFRONT-3414',
                id: 'vendor_auto-875',
                environment: 'kadavr',
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
            suiteName: 'Загрузка большого файла гарантийного письма в анкете.',
            meta: {
                feature: 'Оферта',
                issue: 'VNDFRONT-3414',
                id: 'vendor_auto-749',
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
            suiteName: 'Загрузка большого файла свидетельства о товарном знаке в анкете.',
            meta: {
                feature: 'Настройки',
                issue: 'VNDFRONT-3414',
                id: 'vendor_auto-37',
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
                    return this.createPageObject(
                        'CheckboxB2b',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.document,
                    );
                },
                expireDate() {
                    return this.createPageObject(
                        'InputB2b',
                        // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                        this.document,
                    );
                },
            },
        }),
        importSuite('Hint', {
            suiteName: 'Подсказка у поля «Основание использования товарного знака».',
            meta: {
                feature: 'Оферта',
                id: 'vendor_auto-750',
                environment: 'kadavr',
            },
            params: {
                text:
                    'Сервис «Яндекс.Маркет для производителей» доступен только правообладателям товарного знака ' +
                    'или лицам, которым предоставлена исключительная лицензия на использование товарного знака.',
            },
            pageObjects: {
                hint() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Hint', this.form);
                },
            },
        }),
        importSuite('Bell/__unavailable', {
            pageObjects: {
                bell() {
                    return this.createPageObject('Bell');
                },
            },
            meta: {
                feature: 'Колокольчик',
                id: 'vendor_auto-781',
                environment: 'kadavr',
            },
        }),
        {
            'При вводе в анкету невалидных значений': {
                'отображаются хинты о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3414',
                    id: 'vendor_auto-49',

                    async test() {
                        await this.browser.allure.runStep(
                            'Проверяем валидацию формата значения у поля "Представляемые бренды"',
                            async () => {
                                await this.form.setFieldValueByName('brands', ',', 'Представляемые бренды');

                                await this.form
                                    .getFieldValidationErrorPopup('brands')
                                    .isVisible()
                                    .should.eventually.be.equal(
                                        true,
                                        'Попап с ошибкой у поля "Представляемые бренды" отображается',
                                    );

                                await this.form
                                    .getFieldValidationErrorPopup('brands')
                                    .getText()
                                    .should.eventually.be.equal(
                                        'Введите не менее одного бренда',
                                        'Текст ошибки у поля "Представляемые бренды" корректный',
                                    );
                            },
                        );

                        await this.browser.allure.runStep(
                            'Проверяем валидацию формата значения у поля "Телефон"',
                            async () => {
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
                    },
                }),
            },
        },
    ),
});
