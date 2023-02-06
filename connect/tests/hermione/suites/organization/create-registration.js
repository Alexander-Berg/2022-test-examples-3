/* eslint-disable max-len */
const { testUsers: { createOrg, adminYO2: admin } } = hermione.ctx;
const { URL } = require('url');
const { assert } = require('chai');

describe('Создание новой организации с /portal/registration', () => {
    describe('Положительные', () => {
        it('1. Создание организации с аккаунтом без организации', function() {
            // alias: pos-1-no-org
            return this.browser

                // авторизоваться под учеткой в паспорте у которой нет организации или зарегистрироваться
                // перейти по ссылке {{CONNECT_HOST}}/portal/registration
                .login({ ...createOrg, retpath: '/portal/registration', waitElement: '.registration-page__header' })

                // открылась страница с "Выберите аккаунт для подключения к Коннекту", кнопками "+ Другой аккаунт" и "Подключить" [plain]
                .assertView('plain', '.registration-page .registration')

                // выбрать аккаунт под которым авторизованы
                .click(`.session-select .session-select__session-name[title="${createOrg.login}"]`)
                .waitForVisible(`.session-select__session_selected .session-select__session-name[title="${createOrg.login}"]`)

                // нажать кнопку "Подключить"
                .setHash('create-org')
                .click('.lego-button_action[type="submit"]');

            /* FIX THIS
                // выполнился переход на страницу "Добро пожаловать в Коннект" {{CONNECT_HOST}}/portal/start/?utm_campaign=org_yo
                .waitForVisible('.registration-page__header', 10000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');

                    assert.equal(parsedUrl.pathname, '/portal/set/any/');
                    assert.equal(new URL(retpath).pathname, '/portal/start/');
                });
            */
        });
    });

    describe('Отрицательные', () => {
        it('1. Вход в организацию с аккаунтом состоящим в организации', function() {
            /* alias: neg-1-has-org */
            return this.browser

                // авторизоваться под учеткой в паспорте у которой есть организация
                // перейти по ссылке {{CONNECT_HOST}}/portal/registration
                .login({ ...admin, retpath: '/portal/registration', waitElement: '.registration-page__header' })

                // открылась страница с "Этот аккаунт уже подключен к Коннекту", кнопками "+ Другой аккаунт" и "Войти"
                .assertView('plain', '.registration-page .registration')

                // выбрать аккаунт под которым авторизованы
                .click(`.session-select .session-select__session-name[title="${admin.login}"]`)

                // нажать кнопку "Войти"
                .setHash('create-org')
                .click('.lego-button_action[type="submit"]')

                // выполнился переход на страницу "Дашборда" {{CONNECT_HOST}}/portal/home
                .waitForVisible('.registration-page__header', 10000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');

                    assert.equal(parsedUrl.pathname, '/portal/set/any/');
                    assert.equal(new URL(retpath).pathname, '/portal/home');
                });
        });

        it('2. Выбор другого аккаунта через "+ Другой аккаунт"', function() {
            /* alias: neg-2-passport */
            return this.browser

                // авторизоваться под учеткой в паспорте у которой есть или нет организации
                // перейти по ссылке {{CONNECT_HOST}}/portal/registration
                .login({ ...admin, retpath: '/portal/registration', waitElement: '.registration-page__header' })

                // открылась страница регистрации
                .assertView('plain', '.registration-page .registration')

                // нажать на "+ Другой аккаунт"
                .click('.session-select .session-select__session_add')

                // выполнился переход на страницу паспорта
                .pause(3000)
                .getUrl()
                .then(url => {
                    assert.equal(new URL(url).hostname, 'passport-test.yandex.ru');
                });
        });

        it('3. Вход в организацию без выбора сессии с аккаунтом состоящим в организации', function() {
            /* alias: neg-3-current-session */
            return this.browser
                // авторизоваться под учеткой в паспорте у которой есть организация
                // перейти по ссылке {{CONNECT_HOST}}/portal/registration?session=current
                .login({ ...admin, retpath: '/portal/registration?session=current', waitElement: 'body' })

                // выполнился переход на страницу "Дашборда" {{CONNECT_HOST}}/portal/home
                .waitForVisible('.registration-page__header', 10000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');

                    assert.equal(parsedUrl.pathname, '/portal/set/any/');
                    assert.equal(new URL(retpath).pathname, '/portal/home');
                });
        });
    });
});
