'use strict';

const qs = require('qs');

const URLS = require('./utils/urls');

const setIdentifiersInCookies = require('./utils/set-identifiers-in-cookies');
const getBrowser = require('./../utils/get-browser');
const isInteger = require('./../utils/is-integer');

/**
 * @param {String} target
 * @return {Boolean}
 */
function isReferallTarget(target) {
    return target === 'referral';
}

/**
 * Referral middleware.
 *
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function referralMiddleware(req, res, next) {
    const target = req.extractedData.target;
    const clid = req.extractedData.clid;

    if (isReferallTarget(target) && isInteger(clid)) {
        const browserName = getBrowser(req.headers['user-agent']).name.toUpperCase();
        const query = {
            aff_id: 1550,
            clid: clid
        };
        const queryStr = qs.stringify(query);
        const url = URLS[browserName] || `${URLS.LANDING}?${queryStr}`;

        setIdentifiersInCookies(res, 1550, clid);
        res.statusCode = 302;
        res.setHeader('Location', url);
        res.end();
    } else {
        next();
    }
}

/**
 * @type {referralMiddleware}
 */
module.exports = referralMiddleware;
