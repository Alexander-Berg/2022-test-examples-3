const path = require('path');

const PO = require('../../../../page-objects/pages/external-offer');

describe('Внешняя анкета / Препрофайл наниматора/Бывший сотрудник', function() {
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
            // - do: заполнить поле 'Фамилия латиницей'
            .setReactSFieldValue(PO.externalOfferForm.fieldLastNameEn(), 'Surname', 'input')
            // - do: заполнить поле 'Имя латиницей'
            .setReactSFieldValue(PO.externalOfferForm.fieldFirstNameEn(), 'Name', 'input')
            // - do: заполнить поле 'Фото'
            .setReactSFieldValue(PO.externalOfferForm.fieldPhoto(), path.join(__dirname, './userpic.png'), 'attachment')
            // - do: заполнить поле 'Дата рождения'
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.1995', 'date')
            // - do: выбрать значение в поле 'Пол'
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            // - do: заполнить поле 'Контактный телефон' своим номером телефона
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79268867807', 'input')
            // - do: заполнить поле 'Личный email'
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
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
            // - do: заполнить поле 'Адрес проживания'
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'address', { position: 1 })
            // - do: выбрать значение в поле 'Операционная система'
            .setReactSFieldValue(PO.externalOfferForm.fieldOS(), 2, 'select')
            // - do: выбрать значение в поле 'Будет ли у вас трудовая книжка к дате выхода на работу?'
            .setReactSFieldValue(PO.externalOfferForm.fieldEmploymentBook(), 2, 'select')
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
