const url = require('url');

/**
 * Выполняет авторизацию, взято из staff-www
 *
 * Основное отличие от @yandex-tools/hermione-config/commands/smartLogin.js в том, что
 * retpath берется из this.options.baseUrl (урл от туннелера)
 *
 * @param {String} username
 * @param {String} retpath
 * @returns {Promise}
 */
module.exports = async function conditionalLogin(username, retpath = '') {
    // eslint-disable-next-line no-undef
    const data = hermione.ctx.testUsers[username];

    return this
        .url(url.format({
            protocol: 'https:',
            hostname: 'aqua.yandex-team.ru',
            pathname: '/auth-html',
            query: {
                mode: 'auth',
                login: data.username,
                secretId: data.secretId,
                passportHost: 'passport.yandex-team.ru',
                retpath: url.resolve(this.options.baseUrl, retpath),
            },
        }))
        .waitForExist('body');
};
