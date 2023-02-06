const parseUrl = require('url').parse;
const { assert } = require('chai');
const { adminUITest2 } = hermione.ctx.testUsers;

describe('Ввод платежных реквизитов', () => {
    beforeEach(function() {
        return this.browser
            // авторизоваться под администратором и перейти по ссылке "/portal/balance/contract"
            .login({ ...adminUITest2, retpath: '/portal/balance/contract' })
            .waitForVisible('.create-contract-form')
            .hideCaret();
    });

    describe('Положительные', () => {
        it('1. Внешний вид страницы с формой создания нового плательщика', function() {
            /* alias: pos-1-view */

            return this.browser
                .setViewportSize({ width: 2000, height: 1200 })
                // выбрать "Новый плательщик" из списка плательщиков
                .click('.person-id__radio_type_new-person-id')
                // по-умолчанию выбран раздел "Физическое лицо"
                .getText('.create-contract-form__person-type .radiobox__radio_checked_yes')
                .then(text => {
                    assert.equal(text, 'Физическое лицо');
                })
                // внешний вид страницы ввода реквизитов [plain]
                .assertView('plain', '.create-contract-page');
        });

        it('2. Ссылка на оферту', function() {
            /* alias: pos-2-offer */

            return this.browser
                // ссылка на оферту есть
                .setViewportSize({ width: 2000, height: 2500 })
                .waitForVisible('.create-contract-form__terms .link')
                .getText('.create-contract-form__terms .link')
                .then(text => {
                    assert.equal(text, 'Оферту');
                })
                // ссылка ведет на "https://yandex.ru/legal/oferta_connect"
                .getAttribute('.create-contract-form__terms .link', 'href')
                .then(href => {
                    assert.isTrue(href.indexOf('https://yandex.ru/legal/oferta_connect') === 0);
                });
        });

        it('3. Ссылка на помощь', function() {
            /* alias: pos-3-help */

            return this.browser
                // ссылка на помощь есть
                .setViewportSize({ width: 2000, height: 2500 })
                .waitForVisible('.create-contract-form__help .link')
                .getText('.create-contract-form__help .link')
                .then(text => {
                    assert.equal(text, 'Задайте вопрос нашей службе поддержки');
                })
                // ссылка ведет на "https://yandex.ru/support/connect/troubleshooting.html"
                .getAttribute('.create-contract-form__help .link', 'href')
                .then(href => {
                    assert.isTrue(href.indexOf('https://yandex.ru/support/connect/troubleshooting.html') === 0);
                });
        });

        it('4. Юридическое лицо без промокода', function() {
            const formData = {
                long_name: 'ООО \"ромашка\"',
                phone: '1234567',
                email: '111@mail.ru',
                postal_code: '192076',
                postal_address: 'СПБ, Литейный 50',
                legal_address: 'СПБ, Литейный 50',
                inn: '7826125214',
                kpp: '784001001',
                bik: '044030653',
                account: '40703810955000100001',
            };

            /* alias: pos-4-legal */

            return this.browser
                .disableAnimations('*')
                .setViewportSize({ width: 2000, height: 2500 })

                // выбрать "Новый плательщик" из списка плательщиков
                .click('.person-id__radio_type_new-person-id')

                // выбрать вкладку "юридическое лицо"
                .click('.create-contract-form__radio_type_legal')

                // внешний вид вкладки юрлицо [plain]
                .assertView('plain', '.create-contract-form__form')

                // в поле "полное название организации" ввести "ООО "ромашка"",
                // в поле "телефон" ввести "1234567"
                // в поле "электронный адрес" ввести "111@mail.ru"
                // в поле "почтовый индекс" ввести "192076"
                // в поле "адрес для корреспонденции" ввести "СПБ, Литейный 50"
                // в поле "юридический адрес" ввести "СПБ, Литейный 50"
                // в поле "ИНН" ввести "7826125214",
                // в поле "КПП" ввести "784001001",
                // в поле "БИК" ввести "044030653",
                // в поле "р/с" ввести "40703810955000100001"
                .fillForm('create-contract-form', formData)

                // внешний вид заполненной формы [form-filled]
                .assertView('form-filled', '.create-contract-form__form')

                // нажать на кнопку "подключить"
                .click('.create-contract-form__controls .button2_type_submit')

                // // произошел переход на страницу с полем пополнения баланса
                .waitForVisible('.balance-page', 15000)
                .getUrl()
                .then(url => parseUrl(url).path)
                .then(path => {
                    assert.equal(path, '/portal/balance');
                });
        });

        it('5. Физическое лицо без промокода', function() {
            const formData = {
                last_name: 'Иванов',
                first_name: 'Иван',
                middle_name: 'Иванович',
                phone: '123456',
                email: '111@mail.ru',
            };

            /* alias: pos-5-natural */

            return this.browser
                .disableAnimations('*')
                .setViewportSize({ width: 2000, height: 2500 })

                // выбрать "Новый плательщик" из списка плательщиков
                .click('.person-id__radio_type_new-person-id')

                // выбрать вкладку "физическое лицо"
                .click('.create-contract-form__radio_type_natural')

                // внешний вид формы [plain]
                .setViewportSize({ width: 2000, height: 2500 })
                .assertView('plain', '.create-contract-form__form')

                // в поле "фамилия" ввести "Иванов"
                // в поле "имя" ввести "Иван"
                // в поле "отчество" ввести "Иванович"
                // в поле "телефон" ввести "123456"
                // в поле "электронная почта" ввести "111@mail.ru"
                .fillForm('create-contract-form', formData)

                // внешний вид заполненной формы [filled-form]
                .assertView('form-filled', '.create-contract-form__form')

                // нажать на кнопку "подключить"
                .click('.create-contract-form__controls .button2_type_submit')

                // произошел переход на страницу с полем пополнения баланса
                .waitForVisible('.balance-page', 15000)
                .getUrl()
                .then(url => parseUrl(url).path)
                .then(path => {
                    assert.equal(path, '/portal/balance');
                });
        });

        it('6. Внешний вид страницы с плательщиками', function() {
            /* alias: pos-6-persons */

            return this.browser
                // по-умолчанию выбран первый плательщик из списка
                .waitForVisible('.person-id .radiobox__radio_checked_yes.radiobox__radio:first-child')

                // внешний вид страницы [plain]
                .setViewportSize({ width: 2000, height: 1200 })
                .assertView('plain', '.create-contract-page');
        });
    });

    describe('Отрицательные', () => {
        it('1. Юридическое лицо, обязательные поля не заполнены', function() {
            /* alias: neg-1-legal-incorrect */
            return this.browser
                .setViewportSize({ width: 1500, height: 1200 })

                // выбрать "Новый плательщик" из списка плательщиков
                .click('.person-id__radio_type_new-person-id')

                // выбрать вкладку "юридическое лицо"
                .click('.create-contract-form__radio_type_legal')

                // нажать на кнопку "подключить"
                .click('.create-contract-form__controls .button2_type_submit')

                // появились подсказки о незаполненных полях
                .waitForVisible('.form__error', 5000)

                // внешний вид формы с ошибками [form-errors]
                .assertView('form-errors', '.create-contract-form')

                // переход не произошел
                .getUrl()
                .then(url => parseUrl(url).path)
                .then(path => {
                    assert.equal(path, '/portal/balance/contract');
                });
        });

        it('2. Физическое лицо, обязательные поля не заполнены', function() {
            /* alias: neg-2-natural-incorrect */
            return this.browser

                .setViewportSize({ width: 1500, height: 1200 })

                // выбрать "Новый плательщик" из списка плательщиков
                .click('.person-id__radio_type_new-person-id')

                // выбрать вкладку "физическое лицо"
                .click('.create-contract-form__radio_type_natural')

                // нажать на кнопку "подключить"
                .click('.create-contract-form__controls .button2_type_submit')

                // появились подсказки о незаполненных полях
                .waitForVisible('.form__error', 5000)

                // внешний вид формы с ошибками [form-errors]
                .assertView('form-errors', '.create-contract-form')

                // переход не произошел
                .getUrl()
                .then(url => parseUrl(url).path)
                .then(path => {
                    assert.equal(path, '/portal/balance/contract');
                });
        });
    });
});
