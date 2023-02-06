const config = require('config');
const assert = require('chai').assert;

const authUrl = `${config.get('passport.host')}/auth?retpath=${config.get('operator.host')}`;

describe('Авторизация в passport.yandex.ru', async function() {
    it('Должно срабатывать перенаправление на Паспорт, если пользователь не залогинен', async function() {
        const { browser } = this;

        await browser.deleteCookie();
        await browser.url('/');

        const url = await browser.getUrl();

        assert.equal(url, authUrl);
    });
});
