'use strict';

/**
 * Postponement middleware.
 *
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 */
function disableMiddleware(req, res, next) {
    const disabledStatus = !!req.cookies['svt-disabled'];
    const timesOfClosings = !!req.cookies['svt-times_of_closings'];

    if (disabledStatus || timesOfClosings) {
        res.clearCookie('svt-disabled', {
            path: '/'
        });

        res.clearCookie('svt-times_of_closings', {
            path: '/'
        });
    }

    next();
}

/**
 * @type {disableMiddleware}
 */
module.exports = disableMiddleware;
