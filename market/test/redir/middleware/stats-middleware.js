'use strict';

const crypto = require('../utils/crypto');

const STATS_COOKIE_NAME = 'svt-s';

/**
 * @description Extracts stats middleware from cookies
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function statsMiddleware(req, res, next) {
    try {
        const { bar: { clicks, shows, closes } } = JSON.parse(crypto.decrypt(req.cookies[STATS_COOKIE_NAME]));

        req.stats = {
            bar: { clicks, shows, closes }
        }
    } catch (e) {
        req.stats = {
            bar: {
                shows: 0,
                clicks: 0,
                closes: 0
            }
        };
    }

    next();
}

module.exports = statsMiddleware;
