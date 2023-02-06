const path = require('path');

const PO = require('../../../../page-objects/pages/external-offer');

describe('Внешняя анкета / Международная', function() {
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

            // - do: заполните поле 'Contact phone number' без '+' либо введите менее 10 символов после плюса
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+792688678', 'input')
            // - do: заполните поле 'Personal email' без доменной части, либо используя кириллицу
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'чупакабра.да@будет.com', 'input')
            // - do: клик на кнопку 'Отправить'
            .click(PO.externalOfferForm.submit())
            .waitForVisible(PO.externalOfferForm.formError())
            .pause(2000) // scrolling
            // - screenshot: под всеми заполненными полями выдались соответствующие ошибки
            // [external_questionnaire_form_errors]
            .assertView('external_questionnaire_form_errors', PO.externalOfferForm())
            // - do: заполните все обязательные поля корректными значениями
            .addReactSuggestValue({
                block: PO.externalOfferForm.fieldCitizenship(),
                text: 'Russian',
                position: 1,
                clickToFocus: true,
            })
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstName(), 'Name', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldLastName(), 'Surname', 'input')
            .setReactSFieldValue(PO.externalOfferForm.preferredFirstAndLastName(), 'Preferred First And Last Name', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldPhoto(), path.join(__dirname, './userpic.png'), 'attachment')
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.01.1997', 'date')
            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), data.login, 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.success()) // ждем успех проверки
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79268867807', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
            .setReactSFieldValue(PO.externalOfferForm.fieldOS(), 2, 'select')
            .waitForVisible(PO.externalOfferForm.fieldResidenceAddress())
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'address', { position: 1 })
            .setReactSFieldValue(PO.externalOfferForm.fieldMainLanguage(), 1, 'select')
            .setReactSFieldValue(PO.externalOfferForm.fieldIsAgree(), true, 'checkbox')
            // - do: клик на кнопку 'Отправить'
            .click(PO.externalOfferForm.submit())
            .waitForVisible(PO.externalOfferForm.formError())
            .pause(2000) // scrolling
            // - screenshot: под полем 'NDA' отобразилась ошибка [external_questionnaire_form_errors2]
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
            // - do: заполнить поле 'Last name'
            .setReactSFieldValue(PO.externalOfferForm.fieldLastName(), 'Surname', 'input')
            // - do: заполнить поле 'First name'
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstName(), 'Name', 'input')
            // - do: заполнить поле 'Preferred first and last name'
            .setReactSFieldValue(PO.externalOfferForm.preferredFirstAndLastName(), 'Preferred First And Last Name', 'input')
            // - do: заполнить поле 'Photo'
            .setReactSFieldValue(PO.externalOfferForm.fieldPhoto(), path.join(__dirname, './userpic.png'), 'attachment')
            // - do: заполнить поле 'Date of birth'
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.1995', 'date', { lang: 'en' })
            // - do: выбрать значение в поле 'Sex'
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            // - do: заполнить поле 'Username'
            .setReactSFieldValue(PO.externalOfferForm.fieldUsername(), data.login, 'input')
            .click(header) // блюрим поле
            .waitForVisible(PO.externalOfferForm.rowUsername.success()) // ждем успех проверки
            // - do: заполнить поле 'Contact phone number'
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79268867807', 'input')
            // - do: заполнить поле 'Personal email'
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
            // - do: выбрать значение в поле 'Operational system'
            .setReactSFieldValue(PO.externalOfferForm.fieldOS(), 2, 'select')
            // - do: выбрать значение в поле 'Hired'
            .setReactSFieldValue(PO.externalOfferForm.fieldJoinAt(), data.join_at, 'date', { lang: 'en' })
            .click(header) // прячем дейтпикер
            // - do: выбрать значение в поле 'Citizenship'
            .addReactSuggestValue({
                block: PO.externalOfferForm.fieldCitizenship(),
                text: 'Russian',
                position: 1,
                clickToFocus: true,
            })
            .waitForVisible(PO.externalOfferForm.fieldResidenceAddress())
            //- assert: |
            //    появилось поле 'Passport and Employment eligibility'
            //    появилось поле 'Residence address'
            .assertView('external_questionnaire_group_contract', PO.externalOfferForm.groupContract())
            // - do: выбрать несколько файлов в поле 'Passport and Employment eligibility'
            .setReactSFieldValue(PO.externalOfferForm.fieldDocuments(), [
                path.join(__dirname, './userpic.png'),
                path.join(__dirname, './userpic2.png'),
            ], 'attachments')
            // - do: заполнить поле 'Residence address'
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'address', { position: 1 })
            // - do: выбрать значение в поле 'Main language'
            .setReactSFieldValue(PO.externalOfferForm.fieldMainLanguage(), 1, 'select')
            // - do: заполнить поле 'Spoken languages'
            .addReactSuggestValue({
                block: PO.externalOfferForm.fieldSpokenLanguages(),
                text: 'german',
                position: 1,
                clickToFocus: true,
            })
            // - do: поставить галочку в поле согласия передачи персональных данных
            .setReactSFieldValue(PO.externalOfferForm.fieldIsAgree(), true, 'checkbox')
            // - do: поставить галочку в поле 'Accept the nondisclosure agreement (NDA)'
            // - screenshot: открылся попап с описанием условий [nda_agreement]
            // - do: клик на 'Accept' в попапе
            .setReactSFieldValue(PO.externalOfferForm.fieldNdaAccepted(), true, 'checkbox-confirmed', { screenshot: 'nda_agreement' })
            // - screenshot: пример внешнего вида заполненной анкеты [external_questionnaire_form_filled]
            .assertView('external_questionnaire_form_filled', PO.externalOfferForm())
            // - do: клик на 'Submit'
            .click(PO.externalOfferForm.submit())
            // - screenshot: выдало сообщение с благодарностью [external_questionnaire_form_success]
            .waitForHidden(PO.externalOfferForm())
            .assertView('external_questionnaire_form_success', header);
    });
});
