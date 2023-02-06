'use strict';

const getOffTime = require('./../../utils/get-off-time');

/**
 * Set `yandex.statistics.clid.21` and `sovetnik.aff_id` in cookies.
 *
 * @param {Object} res
 * @param {String|Number} affId
 * @param {String|Number} clid
 * @param {String=} [timeKey = 'half-hour']
 */
function setIdentifiersInCookies(res, affId, clid, timeKey) {
    timeKey = timeKey || 'half-hour';

    if (res && clid && affId) {
        const offTime = getOffTime(timeKey);

        if (offTime) {
            res.cookie('yandex.statistics.clid.21', clid, {
                domain: 'market.yandex.ru',
                httpOnly: true,
                secure: true,
                path: '/',
                maxAge: offTime,
                sameSite: 'Lax',
            });

            res.cookie('sovetnik.aff_id', affId, {
                domain: 'market.yandex.ru',
                httpOnly: true,
                secure: true,
                path: '/',
                maxAge: offTime,
                sameSite: 'Lax',
            });
        }
    }
}

/**
 * @type {setIdentifiersInCookies}
 */
module.exports = setIdentifiersInCookies;
