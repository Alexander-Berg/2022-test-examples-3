/* eslint-disable no-console */
const { URL } = require('url');
const { testUsers, cacheMode } = hermione.ctx;
const { getContext } = require('@yandex-int/infratest-utils/lib/hermione-get-context');
const createDataPath = require('../helpers/createDataPath');

const DEFAULT_PASSWORD = process.env.CONNECT_TOKENS__USER_PASS;

const DEFAULT_WAIT_ELEMENT = [
    // legacy react15
    '.header__user .unit__avatar_complete',
    // fresh react
    '.header__current-user .user__avatar',
    // legacy react15 + lego user2
    '.header .user-account__pic',
    // fresh react + lego user2
    '.app-layout__header .user-account__pic',
].join(', ');

module.exports = function(params = {}) {
    const testContext = getContext(this.executionContext);
    const cachePath = createDataPath('cache', testContext);

    const {
        retpath = '/',
        login = testUsers.alex.login,
        password = DEFAULT_PASSWORD,
        bunker = false,
        waitElement = DEFAULT_WAIT_ELEMENT,
    } = params;

    if (!password) {
        console.log('Не задан пароль для пользователя.');
        console.log('Пароль можно задать для CI через секрет и переменную окружения CONNECT_TEST_USER_PASSWD,' +
            'для локального запуска через секрет connect_local_config и свойство tokens.userPass.');
    }

    let auth = this.deleteCookie();

    if (cacheMode !== 'read') {
        let url = new URL('http://aqua.yandex-team.ru/auth.html');

        url.searchParams.set('mode', 'auth');
        url.searchParams.set('login', login);
        url.searchParams.set('passwd', password);
        url.searchParams.set('host', 'https://passport-test.yandex.ru/passport');

        auth = auth
            .url(url.href)
            .waitForExist('body');
    }

    return auth
        .url('/portal/ping')
        .waitForVisible('body', 5000)
        // Стираю куку hash на своем домене перед тем как зайти на страницу
        .setHash('')
        .setCookie({
            name: 'dumps-test-path',
            value: cachePath,
        })
        .setBunker(bunker)
        .waitForVisible('body', 5000)
        .url(retpath)
        .waitForVisible(waitElement, 5000);
};
