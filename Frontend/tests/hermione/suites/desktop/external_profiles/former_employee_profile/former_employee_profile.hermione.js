const path = require('path');

const PO = require('../../../../page-objects/pages/external-offer');

describe('Внешняя анкета / Бывший сотрудник', function() {
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
            // - do: заполнить поле 'Дата рождения'
            .setReactSFieldValue(PO.externalOfferForm.fieldBirthday(), '12.08.2020', 'date')
            // - do: выбрать значение в поле 'Пол'
            .setReactSFieldValue(PO.externalOfferForm.fieldGender(), 'M', 'radio')
            // - do: заполнить поле 'Контактный телефон' своим номером телефона
            .setReactSFieldValue(PO.externalOfferForm.fieldPhone(), '+79217817789', 'input')
            // - do: заполнить поле 'Личный email'
            .setReactSFieldValue(PO.externalOfferForm.fieldHomeEmail(), 'olgakozlova.web@gmail.com', 'input')
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
            // - do: выбрать значение в поле 'СНИЛС' (если гражданство российское)
            .setReactSFieldValue(PO.externalOfferForm.fieldSnils(), path.join(__dirname, './userpic.png'), 'attachment')
            // - do: заполнить поле 'Адрес проживания'
            .setReactSFieldValue(PO.externalOfferForm.fieldResidenceAddress(), 'Москва, Курский вокзал', 'textarea')
            // - do: выбрать значение в поле 'Будет ли у вас трудовая книжка к дате выхода на работу?'
            .setReactSFieldValue(PO.externalOfferForm.fieldEmploymentBook(), 2, 'select')
            // - do: установить галочку в поле 'Я хочу выпустить ЭЦП'
            .setReactSFieldValue(PO.externalOfferForm.fieldEdsNeeded(), true, 'checkbox-confirmed', { screenshot: 'eds' })
            // - assert: |
            //     появилась кнопка 'Отправить смс для подтверждения'
            //     появилась ссылка 'Изменить'
            .assertView('eds_with_phone', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: клик на кнопку 'Отправить смс для подтверждения'
            .click(PO.externalOfferForm.edsStart())
            .waitForVisible(PO.externalOfferForm.edsConfirmation())
            // - screenshot: появилось поле для ввода кода из смс [eds_code_input]
            .patchStyle('.Countdown-Counter', {
                backgroundColor: 'black',
            })
            .assertView('eds_code_input', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
            // - do: ввести код из смс
            .setValue(PO.externalOfferForm.edsInputCode.input(), data.verification_code)
            // - assert: кнопка 'Подтвердить' стала активна
            // - do: клик на кнопку 'Подтвердить'
            .click(PO.externalOfferForm.edsStart())
            // - screenshot: появилось сообщение о подтвержденном телефоне [eds_code_input_success]
            .waitForVisible(PO.externalOfferForm.edsSuccess())
            .assertView('eds_code_input_success', [
                PO.externalOfferForm.rowEdsNeeded(), PO.externalOfferForm.rowEds(),
            ])
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
