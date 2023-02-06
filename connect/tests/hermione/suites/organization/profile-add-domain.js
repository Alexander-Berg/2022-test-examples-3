const { URL } = require('url');
const { assert } = require('chai');
const { testUsers: { adminYO1, adminYO2, adminUITest2, sim } } = hermione.ctx;

function checkPositive(browser) {
    return browser
        .waitForVisible('.org-profile_busy', 3000, true)

        // кнопка "+" напротив "Собственные домены:" отображается
        .waitForVisible('.org-profile__field-action_type_add-domain')

        // нажать круг с "+" напротив "Собственные домены:"
        .click('.org-profile__field-action_type_add-domain')
        .waitForVisible('.service-header', 20000)
        .getUrl()
        .then(url => new URL(url))
        .then(({ pathname }) => {
            assert.equal(pathname, '/portal/services/webmaster');
        })

        // открылся попап "Новый домен"
        .waitForVisible('.domain-list', 20000)
        .waitForVisible('.modal_visible_yes.add-domain-modal', 15000);
}

function checkNegative(browser) {
    return browser
        .waitForVisible('.org-profile_busy', 3000, true)

        // напротив "Собственные домены:" нет круга с "+"
        .waitForVisible('.org-profile__field-action_type_add-domain', 1000, true)

        // скриншот профиля организации без круга с "+"
        .waitForVisible('.org-profile__logo-image', 10000)
        .assertView('plain', '.org-profile__content');
}

describe('Добавление домена в профиле организации', () => {
    describe('В организации без технического домена', () => {
        describe('Положительные', () => {
            it('1. Попап добавления домена', function() {
                /* alias: pos-1-without-tech */
                return this.browser
                    // авторизоваться под администратором организации без домена
                    // и перейти по ссылке {{CONNECT_HOST}}/portal/profile
                    .login({ ...adminYO1, retpath: '/portal/profile' })
                    .then(() => checkPositive(this.browser));
            });
        });

        describe('Отрицательные', () => {
            it('1. Кнопки "+" нет', function() {
                /* alias: neg-1-without-tech */
                return this.browser
                    // авторизоваться под администратором организации с доменом
                    // и перейти по ссылке {{CONNECT_HOST}}/portal/profile
                    .login({ ...adminYO2, retpath: '/portal/profile' })
                    .then(() => checkNegative(this.browser));
            });
        });
    });

    describe('В организации с техническим доменом', () => {
        describe('Положительные', () => {
            it('1. Попап добавления домена', function() {
                /* alias: pos-1-with-tech */
                return this.browser
                    // авторизоваться под администратором организации с техническим доменом *.yaconnect.com
                    // перейти по ссылке {{CONNECT_HOST}}/portal/profile
                    .login({ ...adminUITest2, retpath: '/portal/profile' })
                    .then(() => checkPositive(this.browser));
            });
        });

        describe('Отрицательные', () => {
            it('1. Кнопки "+" нет', function() {
                /* alias: neg-1-with-tech */
                return this.browser
                    // авторизоваться под администратором организации
                    // с техническим доменом *.yaconnect.com и с добавленным доменом
                    // перейти по ссылке {{CONNECT_HOST}}/portal/profile
                    .login({ ...sim, retpath: '/portal/profile' })
                    .then(() => checkNegative(this.browser));
            });
        });
    });
});
