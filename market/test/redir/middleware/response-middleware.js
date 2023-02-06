'use strict'
const crypto = require('../utils/crypto');

const YANDEX_RE = /(?:^|\.)yandex\.(?:ru|by|ua|kz|com\.tr|com)$/;
const AUTO_RU_REG_EXP = /^auto\.(?:ru|by|ua|kz|com\.tr|com)$/;
// [AB] banner https://st.yandex-team.ru/SOVETNIK-17228
const BANNER_REG_EXP = /app\.adjust\.com/;

const STATS_COOKIE_NAME = 'svt-s';
const STATS_COOKIE_MAX_AGE = 1000 * 60 * 60 * 24 * 365 * 10;

/**
 * @description response middleware
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function responseMiddleware(req, res, next) {
    const { stats } = req;
    const { url, hostname } = req.extractedData;

    if (isItOurHostname(hostname)) {
        res.status(302);
        res.location(url);

        if (req.query.type === 'market' && !req.query.from_button) {
            stats.bar.clicks += 1;
        }

        res.cookie(STATS_COOKIE_NAME, crypto.encrypt(JSON.stringify(stats)), {
            secure: true,
            httpOnly: true,
            maxAge: STATS_COOKIE_MAX_AGE,
        });

        res.end();
    } else {
        res.end();

        throw new Error(`${url} is unknown url`);
    }
}

/**
 * @description Returns true if it is our domain
 * @param {string} hostname
 * @returns {boolean}
 */
function isItOurHostname(hostname) {
    return (
        YANDEX_RE.test(hostname) || AUTO_RU_REG_EXP.test(hostname) || BANNER_REG_EXP.test(hostname)
    );
}

module.exports = responseMiddleware;
