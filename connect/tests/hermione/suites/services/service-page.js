const { adminYO2: user } = hermione.ctx.testUsers;
const { URL } = require('url');
const { assert } = require('chai');

describe('Страница сервиса', () => {
    describe('Положительные', () => {
        it('1. Карточка домена кликабельна на странице вебмастера', function() {
            /* alias: pos-1-domain-card */
            return this.browser
                .login({ ...user, retpath: '/portal/services/webmaster' })
                .waitForVisible('.domain-list')
                // нажать на карточку домена
                .click('a.service-resource__link[href="/portal/services/webmaster/resources/test.ru"]')
                .waitForVisible('.domain-page__content', 10000)
                .getUrl()
                .then(url => {
                    assert.equal(new URL(url).pathname, '/portal/services/webmaster/resources/test.ru');
                });
        });
    });
});
