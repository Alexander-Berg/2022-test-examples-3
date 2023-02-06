const url = require('url');

const passportHosts = require('../constants/passportHosts');
const users = require('../constants/users');

/**
 * Выполняет авторизацию
 *
 * Основное отличие от @yandex-tools/hermione-config/commands/smartLogin.js в том, что
 * retpath берется из this.options.baseUrl (урл от туннелера)
 *
 * @param {Object} env
 * @param {String} username
 * @param {String} retpath
 * @returns {Promise}
 */
module.exports = async function yaLogin(
    env = 'internal',
    username,
    retpath = ''
) {
    const passportHost = passportHosts[env];
    const user = username ? hermione.ctx.testUsers[username] : hermione.ctx.testUsers[users[env]];

    return this
        .url(url.format({
            protocol: 'http',
            host: 'aqua.yandex-team.ru',
            pathname: 'auth.html',
            query: {
                mode: 'auth',
                login: user.username,
                passwd: user.password,
                host: `https://${passportHost}/passport`,
                retpath: url.resolve(this.options.baseUrl, retpath),
            },
        }))
        .waitForExist('body');
};
