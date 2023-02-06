const parseUrl = require('url').parse;
const { assert } = require('chai');
const { testUsers: { jane } } = hermione.ctx;

describe('Онбординг для организации без домена', () => {
    describe('Положительные', () => {
        it('1. Экран приветствия и переход на экран подписки на сервисы', function() {
            /* alias: pos-1-welcome-page */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start"
                .login({ ...jane, retpath: '/start' })

                // отображается экран приветствия
                .waitForVisible('.onboarding-welcome')

                // внешний вид экрана с приветствием [welcome]
                .assertView('welcome', '.onboarding-layout__body')

                // нажать на кнопку "Продолжить"
                .click('.onboarding-welcome .button2')

                // произошел переход на экран подписки на сервисы
                .waitForVisible('.onboarding-subscribe')
                .hideCaret()

                // внешний вид экрана подписки на сервисы [subscribe]
                .assertView('subscribe', '.onboarding-layout__body');
        });

        it('2. Подписка на сервисы на экране подписки', function() {
            /* alias: pos-2-subscribe */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start?step=1"
                .login({ ...jane, retpath: '/start?step=1' })

                // отображается экран подписки на сервисы
                .waitForVisible('.onboarding-subscribe')
                .hideCaret()

                // нажать на кнопку "Подписаться на новости и продолжить"
                .click('.onboarding-subscribe__button')

                // произошел переход на экран приглашения
                .waitForVisible('.onboarding-invite', 10000);
        });

        it('3. Экран подписки и переход на экран приглашения без подписки', function() {
            /* alias: pos-3-subscribe-page */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start"
                .login({ ...jane, retpath: '/start?step=1' })

                // отображается экран подписки на сервисы
                .waitForVisible('.onboarding-subscribe')
                .hideCaret()

                // нажать на кнопку "Подписаться позже"
                .click('.onboarding-subscribe__skip')

                // произошел переход на экран приглашения
                .waitForVisible('.onboarding-invite')

                // внешний вид экрана приглашения [invite]
                .assertView('invite', '.onboarding-layout__body');
        });

        it('4. Приглашение сотрудников в организацию на экране приглашения', function() {
            /* alias: pos-4-invite */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start?step=2"
                .login({ ...jane, retpath: '/start?step=2' })

                // отображается экран приглашения
                .waitForVisible('.onboarding-invite')

                // кнопка "Отправить приглашения" неактивна
                .waitForExist('.invite-form__button.button2_disabled_yes')

                // ввести в инпут "testemail@test.ru"
                .setValue('.invite-form__textinput .textinput__control', 'testemail@test.ru')

                // кнопка "Отправить приглашения" стала активной
                .waitForExist('.invite-form__button.button2_disabled_yes', 1000, true)

                // нажать на кнопку "Отправить приглашения"
                .click('.onboarding-invite__form .invite-form__button')

                // произошел переход на экран сервисов
                .waitForVisible('.onboarding-services', 5000);
        });

        it('5. Экран приглашения и переход на экран сервисов без приглашения', function() {
            /* alias: pos-5-subscribe-page */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start?ыеуз=2"
                .login({ ...jane, retpath: '/start?step=2' })

                // отображается экран приглашения
                .waitForVisible('.onboarding-invite')

                // нажать на кнопку "Пригласить позже"
                .click('.onboarding-invite__button')

                // произошел переход на экран сервисов
                .waitForVisible('.onboarding-services')

                // внешний вид экрана сервисов [services]
                .assertView('services', '.onboarding-layout__body');
        });

        it('6. Экран сервисов и переход на страницу дашборда', function() {
            /* alias: pos-6-services-page */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start?step=3"
                .login({ ...jane, retpath: '/start?step=3' })

                // отображается экран сервисов
                .waitForVisible('.onboarding-services')

                // нажать на кнопку "Подключить"
                .click('.onboarding-services__button')

                // произошел переход на страницу дашборда "/portal/home"
                .getUrl()
                .then(url => parseUrl(url).path)
                .then(path => {
                    assert.equal(path, '/portal/home');
                });
        });
    });

    describe('Отрицательные', () => {
        it('1. Некорректная почта на экране приглашения', function() {
            /* alias: neg-1-invalid-invite */
            return this.browser
                // авторизоваться под администратором организации без домена и перейти по ссылке "/start?step=2"
                .login({ ...jane, retpath: '/start?step=2' })

                // отображается экран приглашения
                .waitForVisible('.onboarding-invite')

                // ввести в инпут "testemail"
                .setValue('.invite-form__textinput .textinput__control', 'testemail')

                // нажать на кнопку "Отправить приглашения"
                .click('.onboarding-invite__form .invite-form__button')

                // отображается ошибка "Укажите корректный адрес почты"
                .waitForVisible('.invite-form .form__error')

                // внешний вид формы с ошибкой при некорректном адресе почты [invalid-email]
                .assertView('invalid-email', '.invite-form');
        });
    });
});
