const path = require('path');

const PO = require('../../../../page-objects/pages/external-offer');

describe('Внешняя анкета / Препрофайл наниматора/Новый сотрудник', function() {
    it('Проверка обязательных полей', function() {
        const data = require('./proverka_obyazatelnyh_poley/data.json');
        const url = `${data.offer_link}`;

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(url)
            .waitForVisible(PO.externalOfferForm())
            .patchStyle(PO.externalOfferForm(), {
                boxShadow: 'none',
            })
            .assertView('external_questionnaire_form', PO.externalOfferForm())
            .click(PO.externalOfferForm.submit())
            .waitForVisible(PO.externalOfferForm.formError())
            .pause(2000) // scrolling
            .assertView('external_questionnaire_form_errors', PO.externalOfferForm());
    });
    it('Проверка ошибок логина', function() {
        const data = require('./proverka_oshibok_logina/data.json');
        const url = `${data.offer_link}`;
        const header = PO.header();

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(url)
            .waitForVisible(PO.externalOfferForm())
            .patchStyle(PO.externalOfferForm(), {
                boxShadow: 'none',
            })
            .assertView('external_questionnaire_form', PO.externalOfferForm())

            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), 'lo', 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.error())
            // - screenshot: под полем отобразилась ошибка
            .assertView('short_username_error', PO.externalOfferForm.rowUsername())
            .click(PO.externalOfferForm.rowUsername.clear())
            // - do: вести в поле 'Логин' большие буквы, кириллицу, недопустмые символы
            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), '$$vasya$$', 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.error())
            // - screenshot: под полем отобразилась ошибка
            .assertView('invalid_symbols_username_error', PO.externalOfferForm.rowUsername());
    });
    it('Проверка ошибок ЭЦП', function() {
        const data = require('./proverka_oshibok_ecp/data.json');
        const url = `${data.offer_link}`;

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(url)
            .waitForVisible(PO.externalOfferForm())
            .patchStyle(PO.externalOfferForm(), {
                boxShadow: 'none',
            })
            .assertView('external_questionnaire_form', PO.externalOfferForm())
            // - do: заполнить поле 'Контактный телефон' своим номером телефона
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79500406444', 'input')
            // - do: установить галочку в поле 'Я хочу выпустить ЭЦП'
            .setReactSFieldValue(PO.externalOfferForm.fieldEdsNeeded(), true, 'checkbox-confirmed', { screenshot: 'eds' })
            // - assert: появилась кнопка 'Отправить смс для подтверждения'
            .waitForVisible(PO.externalOfferForm.edsSign())
            //- do: клик на кнопку 'Отправить смс для подтверждения'
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsConfirmation())
            // - screenshot: появилось поле для ввода кода из смс [eds_code_input]
            .patchStyle('.Countdown-Counter', {
                backgroundColor: 'black',
            })
            .assertView('eds_code_input', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: ввести неверный код
            .setValue(PO.externalOfferForm.edsInputCode.input(), '1234')
            // - do: клик на кнопку 'Подтвердить'
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsError())
            .patchStyle('.Countdown-Counter', {
                backgroundColor: 'black',
            })
            .assertView('eds_invalid_code_error', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: ввести еще 2 раза неверный код
            .clearFieldValue(PO.externalOfferForm.edsInputCode.input())
            .setValue(PO.externalOfferForm.edsInputCode.input(), '1235')
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsError())
            .clearFieldValue(PO.externalOfferForm.edsInputCode.input())
            .setValue(PO.externalOfferForm.edsInputCode.input(), '1236')
            //  - do: клик на кнопку 'Подтвердить'
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsError())
            // - assert: |
            //     поле для ввода кода пропало
            //     появилась кнопка 'Отправить смс для подтверждения'
            //     появилось сообщение об ошибке
            // - screenshot: пример внешнего вида [eds_invalid_code_3_errors]
            .patchStyle('.Countdown-Counter', {
                backgroundColor: 'black',
            })
            .assertView('eds_invalid_code_3_errors', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: клик на кнопку 'Отправить смс для подтверждения'
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsConfirmation())
            // - screenshot: появилось поле для ввода кода из смс [eds_code_input]
            .patchStyle('.Countdown-Counter', {
                backgroundColor: 'black',
            })
            .assertView('eds_code_input2', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: введить корректный код из смс
            .clearFieldValue(PO.externalOfferForm.edsInputCode.input())
            .setValue(PO.externalOfferForm.edsInputCode.input(), data.verification_code)
            //  - do: клик на кнопку 'Подтвердить'
            .click(PO.externalOfferForm.edsStart())
            // - assert: телефон успешно привязался
            .waitForVisible(PO.externalOfferForm.edsSuccess())
            .assertView('eds_code_input_success', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ]);
    });
    it('Проверка ошибок валидации', function() {
        const data = require('./proverka_oshibok_validacii/data.json');
        const url = `${data.offer_link}`;
        const header = PO.header();

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(url)
            .waitForVisible(PO.externalOfferForm())
            .patchStyle(PO.externalOfferForm(), {
                boxShadow: 'none',
            })
            .assertView('external_questionnaire_form', PO.externalOfferForm())

            // - do: выбрать в поле 'Гражданство' значение 'Российское'
            .addReactSuggestValue({
                block: PO.externalOfferForm.fieldCitizenship(),
                text: 'Russian',
                position: 1,
                clickToFocus: true,
            })
            // - do: заполните поле 'Дата рождения' с разделителями помимо '.' или указав больше символов чем положено
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.1995', 'date')
            // - do: заполните поле 'Контактный телефон' без '+' либо введите менее 10 символов после плюса
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+792688678', 'input')
            // - do: заполните поле 'Личный email' без доменной части, либо используя кириллицу
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'чупакабра.да@будет.com', 'input')
            // - do: введите в поле 'БИК' менее 9 символов
            .setReactSFieldValue(PO.externalOfferForm.fieldBic(), '12345678', 'input')
            // - do: введите в поле 'Расчетный счет' менее 20 символов
            .setReactSFieldValue(PO.externalOfferForm.fieldBankAccount(), '12345678', 'input')
            // - do: клик на кнопку 'Отправить'
            .click(PO.externalOfferForm.submit())
            .waitForVisible(PO.externalOfferForm.formError())
            .pause(2000) // scrolling
            // - screenshot: под всеми заполненными полями выдались
            // соответствующие ошибки [external_questionnaire_form_errors]
            .assertView('external_questionnaire_form_errors', PO.externalOfferForm())
            // - do: заполните все обязательные поля корректными значениями
            .setReactSFieldValue(PO.externalOfferForm.fieldLastName(), 'Фамилия', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstName(), 'Имя', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldLastNameEn(), 'Surname', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstNameEn(), 'Name', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldPhoto(), path.join(__dirname, './userpic.png'), 'attachment')
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), data.login, 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.success()) // ждем успех проверки
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79268867807', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'address', { position: 1 })
            .click(PO.externalOfferForm.rowBic.clear())
            .click(PO.externalOfferForm.rowBankAccount.clear())
            .setReactSFieldValue(PO.externalOfferForm.fieldIsAgree(), true, 'checkbox')
            // - do: в поле 'Дата рождения' введите невалидный год/дата/месяц
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.9999', 'date')
            // - do: клик на кнопку 'Отправить'
            .click(PO.externalOfferForm.submit())
            .waitForVisible(PO.externalOfferForm.formError())
            .pause(2000) // scrolling
            // - screenshot: под полем 'Дата рождения' отобразилась ошибка [external_questionnaire_form_errors2]
            .assertView('external_questionnaire_form_errors2', PO.externalOfferForm());
    });
    it('Отправка анкеты', function() {
        const data = require('./otpravka_ankety/data.json');
        const url = `${data.offer_link}`;
        const header = PO.header();

        return this.browser
            .conditionalLogin('marat')
            .preparePageExtended(url)
            .waitForVisible(PO.externalOfferForm())
            .patchStyle(PO.externalOfferForm(), {
                boxShadow: 'none',
            })
            .assertView('external_questionnaire_form', PO.externalOfferForm())
            // - do: заполнить поле 'Фамилия'
            .setReactSFieldValue(PO.externalOfferForm.fieldLastName(), 'Фамилия', 'input')
            // - do: заполнить поле 'Имя'
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstName(), 'Имя', 'input')
            // - do: заполнить поле 'Отчество'
            .setReactSFieldValue(PO.externalOfferForm.fieldMiddleName(), 'Отчество', 'input')
            // - do: заполнить поле 'Фамилия латиницей'
            .setReactSFieldValue(PO.externalOfferForm.fieldLastNameEn(), 'Surname', 'input')
            // - do: заполнить поле 'Имя латиницей'
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstNameEn(), 'Name', 'input')
            // - do: заполнить поле 'Фото'
            .setReactSFieldValue(PO.externalOfferForm.fieldPhoto(), path.join(__dirname, './userpic.png'), 'attachment')
            // - do: заполнить поле 'Дата рождения')
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.1995', 'date')
            // - do: выбрать значение в поле 'Пол'
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            // - do: заполнить поле 'Логин'
            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), data.login, 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.success()) // ждем успех проверки
            // - do: заполнить поле 'Личный email'
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
            // - do: заполнить поле 'Контактный телефон' своим номером телефона
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79500406444', 'input')
            // - do: выбрать значение в поле 'Операционная система'
            .setReactSFieldValue(PO.externalOfferForm.fieldOS(), 2, 'select')
            // - do: выбрать значение в поле 'Дата выхода на работу'
            .setReactSFieldValue(PO.externalOfferForm.fieldJoinAt(), data.join_at, 'date')
            .click(header) // прячем дейтпикер
            // - do: выбрать значение в поле 'Гражданство'
            .addReactSuggestValue({
                block: PO.externalOfferForm.fieldCitizenship(),
                text: 'Russian',
                position: 1,
                clickToFocus: true,
            })
            .waitForVisible(PO.externalOfferForm.fieldResidenceAddress())
            // - assert: |
            //     появилось поле 'Паспорт'
            //     появилось поле 'СНИЛС' (если выбрано гражданство 'Российское')
            //     появилось поле 'Адрес проживания'
            .assertView('external_questionnaire_group_contract', PO.externalOfferForm.groupContract())
            // - do: выбрать несколько файлов в поле 'Паспорт'
            .setReactSFieldValue(PO.externalOfferForm.fieldPassportPages(), [
                path.join(__dirname, './userpic.png'),
                path.join(__dirname, './userpic2.png'),
            ], 'attachments')
            // - do: заполнить поле 'Адрес проживания'
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'address', { position: 1 })
            // - do: выбрать значение в поле 'Будет ли у вас трудовая книжка к дате выхода на работу?'
            .setReactSFieldValue(PO.externalOfferForm.fieldEmploymentBook(), 2, 'select')
            // - do: заполнить поле 'Наименование банка'
            .setReactSFieldValue(PO.externalOfferForm.fieldBankName(), 'Банк плоской земли', 'input')
            // - do: заполнить поле 'БИК'
            .setReactSFieldValue(PO.externalOfferForm.fieldBic(), '123456789', 'input')
            // - do: заполнить поле 'Расчетный счет'
            .setReactSFieldValue(PO.externalOfferForm.fieldBankAccount(), '12345678901234567890', 'input')
            // - do: заполнить поле 'Комментарий'
            .setReactSFieldValue(PO.externalOfferForm.fieldComment(), 'Comment', 'textarea')
            // - do: поставить галочку в поле согласия передачи персональных данных
            .setReactSFieldValue(PO.externalOfferForm.fieldIsAgree(), true, 'checkbox')
            // - do: поставить галочку в поле 'Принимаю соглашение о неразглашении (NDA)'
            // - screenshot: открылся попап с описанием условий [nda_agreement]
            // - do: клик на 'Принять' в попапе
            .setReactSFieldValue(PO.externalOfferForm.fieldNdaAccepted(), true, 'checkbox-confirmed', { screenshot: 'nda_agreement' })
            // - screenshot: пример внешнего вида заполненной анкеты [external_questionnaire_form_filled]
            .assertView('external_questionnaire_form_filled', PO.externalOfferForm())
            // - do: клик на 'Отправить'
            .click(PO.externalOfferForm.submit())
            // - screenshot: выдало сообщение с благодарностью [external_questionnaire_form_success]
            .waitForHidden(PO.externalOfferForm())
            .assertView('external_questionnaire_form_success', header);
    });
});
