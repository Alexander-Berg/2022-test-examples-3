const auth = require('./auth');
const getClient = require('./disk-client');
const DiskArranger = require('./disk-arranger');

const consts = require('../../config').consts;

/**
 * @param {Object} user - пользователь, от лица которого выполняются действия
 * @param {string} user.login
 * @param {string} user.password
 * @param {string} baseUrl - URL приложения, к которому хелпер будет слать запросы
 *
 * @returns {Promise<module.DiskArranger|*>}
 */
module.exports = async function factory(user, baseUrl) {
    const authCookies = await auth(user, consts.PASSPORT_URL + '/passport');
    const client = await getClient(authCookies, baseUrl + '/models/');

    return new DiskArranger(client);
};
