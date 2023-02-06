'use strict';

import {importSuite, mergeSuites, makeSuite, makeCase} from 'ginny';

import formSubmit from '../formSubmit';

/**
 * Тест на блок NewVirtualVendor для вендора
 * @param {PageObject.Form} form
 * @param {PageObject.InfoPanel} infoPanel
 */
export default makeSuite('Создание нового бренда.', {
    id: 'vendor_auto-48',
    issue: 'VNDFRONT-1775',
    environment: 'kadavr',
    feature: 'Оферта',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления формы создания бренда', () => this.form.waitForExist());
            },
        },
        {
            'При успешной отправке формы': mergeSuites(formSubmit, {
                'показывается заглушка "Анкета отправлена"': makeCase({
                    async test() {
                        await this.allure.runStep('Ожидаем появления заглушки', () => this.infoPanel.waitForExist());

                        await this.infoPanel.getTitle().should.eventually.equal('Анкета отправлена');

                        await this.infoPanel
                            .getText()
                            .should.eventually.equal('В ближайшее время менеджер Маркета рассмотрит вашу заявку.');
                    },
                }),
            }),
        },
        importSuite('NewVirtualVendor/offerRequired', {
            pageObjects: {
                popup() {
                    return this.createPageObject('PopupB2b');
                },
            },
        }),
        importSuite('DocumentUpload/templates', {
            suiteName: 'Шаблоны гарантийного письма при добавлении нового бренда.',
            meta: {
                feature: 'Оферта',
                issue: 'VNDFRONT-3426',
                id: 'vendor_auto-877',
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
            suiteName: 'Загрузка большого файла гарантийного письма при добавлении нового бренда.',
            meta: {
                feature: 'Оферта',
                issue: 'VNDFRONT-3426',
                id: 'vendor_auto-752',
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
                issue: 'VNDFRONT-3426',
                id: 'vendor_auto-47',
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
        importSuite('Link', {
            suiteName: 'Ссылка на условия оферты',
            meta: {
                id: 'vendor_auto-450',
                issue: 'VNDFRONT-1844',
                environment: 'kadavr',
            },
            params: {
                caption: 'условия оферты',
                url: 'https://yandex.ru/legal/vendor/',
                target: '_blank',
                external: true,
            },
            pageObjects: {
                offerLink() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('OffertaLegalLink', this.form);
                },
                link() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Link', this.offerLink);
                },
            },
        }),
        {
            'При вводе в заявку невалидных значений': {
                'отображаются хинты о необходимости ввода корректного значения': makeCase({
                    issue: 'VNDFRONT-3426',
                    id: 'vendor_auto-51',

                    async test() {
                        await this.browser.allure.runStep(
                            'Проверяем, что поля в форме заявки корректно предзаполнены',
                            async () => {
                                await this.form
                                    .getFieldValue('company')
                                    .should.eventually.be.equal('Автотесты', 'Поле "Название компании" заполнено');

                                await this.form
                                    .getFieldValue('name')
                                    .should.eventually.be.equal('Иванов Иван', 'Поле "Контактное лицо" заполнено');

                                await this.form
                                    .getFieldValue('phone')
                                    .should.eventually.be.equal('+79876543210', 'Поле "Телефон" заполнено');

                                await this.form
                                    .getFieldValue('email')
                                    .should.eventually.be.equal('auto@test', 'Поле "Электронная почта" заполнено');

                                await this.form
                                    .getFieldValue('address', 'textarea')
                                    .should.eventually.be.equal(
                                        'Бенуа, литера Щ',
                                        'Поле "Почтовый адрес с индексом" заполнено',
                                    );
                            },
                        );

                        await this.browser
                            .yaSafeAction(this.form.submitButton.isEnabled(), false)
                            .should.eventually.equal(false, 'Кнопка [Отправить анкету] отображается и неактивна');

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
