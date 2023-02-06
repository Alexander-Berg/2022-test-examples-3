'use strict';

const setIdentifiersInCookies = require('./utils/set-identifiers-in-cookies');

const AMORE = /addons\.mozilla\.org$/;
const WEBSTORE = /chrome\.google\.com$/;

/**
 * @param {String} target
 * @return {Boolean}
 */
function isPromoTarget(target) {
    return ['promo_install', 'promo_details', 'promo_body'].indexOf(target) !== -1;
}

/**
 * Promo middleware.
 *
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function promoMiddleware(req, res, next) {
    const url = req.extractedData.url;
    const hostname = req.extractedData.hostname;
    const target = req.extractedData.target;
    const clid = req.extractedData.clid;
    const affId = req.extractedData.affId;

    if (isPromoTarget(target) && (AMORE.test(hostname) || WEBSTORE.test(hostname))) {
        setIdentifiersInCookies(res, affId, clid);
        res.statusCode = 302;
        res.setHeader('Location', url);
        res.end();
    } else {
        next();
    }
}

/**
 * @type {promoMiddleware}
 */
module.exports = promoMiddleware;
