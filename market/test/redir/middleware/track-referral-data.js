'use strict';

const crypto = require('../utils/crypto');

const COOKIE_LIFETIME = 1 * 60 * 60 * 1000; // 1 hour

/**
 *
 * Middleware, that sets cookie with referral_transaction_id about offer-redirection.
 *
 * For each redirection from domain 'domain-a.ru', where user has been searching for product
 * to domain 'sovetnik-suggested-domain.ru', we redirecting user to, it writes to cookie, named by the
 * domain, we are redirected to (in our case - sovetnik-suggested-domain.ru).
 *
 * Cookie is set to redir-server domain, so it could be read within any request to backend
 *
 * Lifetime of a cookie is set by COOKIE_LIFETIME, which can be found above
 *
 * domain-key in cookie is encrypted.
 *
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function trackReferralDataMiddleware(req, res, next) {
    if (!req || !req.query) {
        throw new Error('req or req.query is missing');
    }

    const {
        transaction_id,
        offer_direct_domain
    } = req.query;
    
    const offerDirectDomain = getLastTwoLevelsFromDomain(offer_direct_domain);

    if (transaction_id && offerDirectDomain) {
        res.cookie(
            crypto.encrypt(offerDirectDomain),
            transaction_id,
            {
                maxAge: COOKIE_LIFETIME,
                httpOnly: true,
                secure: true
            }
        );
    }

    next();
}

function getLastTwoLevelsFromDomain(domain) {
    if (typeof domain !== 'string') {
        return;
    }

    return domain.split('.').slice(-2).join('.');
}

module.exports = trackReferralDataMiddleware;
