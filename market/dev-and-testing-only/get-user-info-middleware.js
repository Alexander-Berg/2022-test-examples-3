'use strict';

const UserModel = require('../../mongodb/models/user');

/**
 * @description get user's info from db by their clientId
 * @example http://server.com/user-info/11111111111
 * @param {Object} req
 * @param {Object} res
 * @param {Function} next
 * @returns {Promise<void>}
 */
async function getUserInfoMiddleware(req, res, next) {
    const { clientId } = req.params;

    const userInfo = await UserModel.findOne({ clientId }) || {};

    res.json(userInfo.toObject());
}

module.exports = getUserInfoMiddleware;
